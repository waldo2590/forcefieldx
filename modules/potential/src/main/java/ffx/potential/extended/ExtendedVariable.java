package ffx.potential.extended;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.apache.commons.math3.util.FastMath.PI;
import static org.apache.commons.math3.util.FastMath.sin;
import static org.apache.commons.math3.util.FastMath.sqrt;

import ffx.potential.bonded.Atom;

/**
 * A generalized extended system variable.
 * Notes:
 *  1. Bonded terms interpolate linearly between the two lamedh end states.
 *  2. VdW and PME use softcoring (nonlinear) as with lambda.
 *  3. Treatment of ESVs: 
 *      a. Bonded terms are handled at the level of ForceFieldEnergy (or, generically, at Potential).
 *      b. PME and vdW scaling and derivatives are handled inside these classes' inner loops.
 *  4. Interaction of lamedh with lambda:
 *      a. dU/dLambda/dLamedh is taken to be nil.
 *      b. Lambda (OSRW) statistics are to be collected only at zero or unity lamedh.
 *      c. Lambda-scaling on atoms and their potential terms stacks multiplicatively with lamedh scaling.
 *              e.g. An ASH-ALA transition is coupled to lambda, while lamedh couples ASH-ASP.
 *                   ASH-HD2 vdW interactions in this case are scaled by lambda*lamedh.
 *              TODO: Decide on the form of the softcore vdW denominator scaling.
 *      d. Bonded term potential interpolation along lambda is handled at the DualTopology level
 *              and thus naturally encapsulates lamedh.
 *      e. PME and vdW interaction is handled explicitly in these classes.
 *              Since there are no lambda derivs at intermediate lamedh and since these
 *              derivatives take the same form, the vdW code for dUdLambda is reused: 
 *              albeit with an extra dimension (for multiple lamedhs).
 * @author slucore
 */
public abstract class ExtendedVariable {
    
    // System handles
    private static int esvIndexer = 0;
    public final int index;
    protected List<Atom> atoms = new ArrayList<>();
    
    // Lamedh variables
    protected double lambda;                        // ESVs travel on {0,1}
    private double theta;                           // Propagates lamedh particle via "lamedh=sin(theta)^2"
    private double halfThetaVelocity = 0.0;         // from OSRW, start theta with zero velocity
    private final double thetaMass = prop("esv-thetaMass", 1.0e-18);            // from OSRW, reasonably 100 a.m.u.
    private final double thetaFriction = prop("esv-thetaFriction", 1.0e-19);    // from OSRW, reasonably 60/ps
    private final Random stochasticRandom = ThreadLocalRandom.current();
    
    private final double betat;
    private double discrBias, dDiscrBiasdL;
    
    public ExtendedVariable(double biasMag, double initialLamedh) {
        index = esvIndexer++;
        betat = System.getProperty("esv-bias") == null ? biasMag : 
                Double.parseDouble(System.getProperty("esv-bias"));
        lambda = initialLamedh;
        theta = Math.asin(Math.sqrt(lambda));
    }
    
    public ExtendedVariable(double biasMag) {
        this(biasMag, 1.0);
    }    
    public ExtendedVariable() {
        this(0.0, 1.0);
    }
    
    public List<Atom> getAtoms() {
        List<Atom> ret = new ArrayList<>();
        ret.addAll(atoms);
        return ret;
    }
    
    public boolean containsAtom(Atom atom) {
        return atoms.contains(atom);
    }
    
    /**
     * Propagate lambda using Langevin dynamics.
     * Check that temperature goes to the value used below (when set as a constant) even when sim is decoupled.
     */
    public void propagate(double dEdLdh, double currentTemperature, double dt) {
        double rt2 = 2.0 * ThermoConstants.R * currentTemperature * thetaFriction / dt;
        double randomForce = sqrt(rt2) * stochasticRandom.nextGaussian() / ThermoConstants.randomConvert;
        double dEdL = -dEdLdh * sin(2.0 * theta);
        halfThetaVelocity = (halfThetaVelocity * (2.0 * thetaMass - thetaFriction * dt)
                + ThermoConstants.randomConvert2 * 2.0 * dt * (dEdL + randomForce))
                / (2.0 * thetaMass + thetaFriction * dt);
        theta = theta + dt * halfThetaVelocity;

        if (theta > PI) {
            theta -= 2.0 * PI;
        } else if (theta <= -PI) {
            theta += 2.0 * PI;
        }

        double sinTheta = sin(theta);
        lambda = sinTheta * sinTheta;
        discrBias = -(4*betat - (lambda-0.5)*(lambda-0.5)) + betat;
        dDiscrBiasdL = -8*betat*(lambda-0.5);
    }
    
    public final void setLambda(double lambda) {
        this.lambda = lambda;
        theta = Math.asin(Math.sqrt(lambda));
        discrBias = -(4*betat - (lambda-0.5)*(lambda-0.5)) + betat;
        dDiscrBiasdL = -8*betat*(lambda-0.5);
    }
    public final double getLambda() {
        return lambda;
    }
    public final int getIndex() {
        return index;
    }
    
    @Override
    public String toString() {
        return String.format("ESV%d", index);
    }
    
    /**
     * From Shen&Huang 2016; drives ESVs to zero/unity.
     * bias = 4B*(L-0.5)^2
     */
    public double getDiscretizationBiasEnergy() {
        return discrBias;
    }
    
    /**
     * dBiasdL = -8B*(L-0.5)
     */
    public double getdDiscretizationBiasdL() {
        return dDiscrBiasdL;
    }

    public static int prop(String key, int defaultVal) {
        return (System.getProperty(key) != null) ? Integer.parseInt(System.getProperty(key)) : defaultVal;
    }
    public static double prop(String key, double defaultVal) {
        return (System.getProperty(key) != null) ? Double.parseDouble(System.getProperty(key)) : defaultVal;
    }
    
    /**
     * Implementations should fill the protected atoms[] array and set the 
     * esvLambda value of any affected bonded terms.
     */
    public abstract void readyup();
    
}
