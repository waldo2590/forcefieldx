// ******************************************************************************
//
// Title:       Force Field X.
// Description: Force Field X - Software for Molecular Biophysics.
// Copyright:   Copyright (c) Michael J. Schnieders 2001-2020.
//
// This file is part of Force Field X.
//
// Force Field X is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License version 3 as published by
// the Free Software Foundation.
//
// Force Field X is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
// FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
// details.
//
// You should have received a copy of the GNU General Public License along with
// Force Field X; if not, write to the Free Software Foundation, Inc., 59 Temple
// Place, Suite 330, Boston, MA 02111-1307 USA
//
// Linking this library statically or dynamically with other modules is making a
// combined work based on this library. Thus, the terms and conditions of the
// GNU General Public License cover the whole combination.
//
// As a special exception, the copyright holders of this library give you
// permission to link this library with independent modules to produce an
// executable, regardless of the license terms of these independent modules, and
// to copy and distribute the resulting executable under terms of your choice,
// provided that you also meet, for each linked independent module, the terms
// and conditions of the license of that module. An independent module is a
// module which is not derived from or based on this library. If you modify this
// library, you may extend this exception to your version of the library, but
// you are not obligated to do so. If you do not wish to do so, delete this
// exception statement from your version.
//
// ******************************************************************************
package ffx.potential.nonbonded.implicit;

import static java.lang.Double.isInfinite;
import static java.lang.Double.isNaN;
import static java.lang.String.format;
import static org.apache.commons.math3.util.FastMath.PI;
import static org.apache.commons.math3.util.FastMath.pow;
import static org.apache.commons.math3.util.FastMath.sqrt;

import edu.rit.pj.IntegerForLoop;
import edu.rit.pj.ParallelRegion;
import edu.rit.pj.ParallelTeam;
import ffx.crystal.Crystal;
import ffx.crystal.SymOp;
import ffx.numerics.atomic.AtomicDoubleArray;
import ffx.numerics.atomic.AtomicDoubleArray3D;
import ffx.potential.bonded.Atom;
import ffx.potential.parameters.ForceField;
import ffx.potential.utils.EnergyException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parallel computation of Born radii chain rule terms via the Grycuk method.
 *
 * @author Michael J. Schnieders
 * @since 1.0
 */
public class BornGradRegion extends ParallelRegion {

  private static final Logger logger = Logger.getLogger(BornRadiiRegion.class.getName());
  private static final double PI4_3 = 4.0 / 3.0 * PI;
  /** Constant factor used with quadrupoles. */
  private static final double oneThird = 1.0 / 3.0;

  private final BornCRLoop[] bornCRLoop;
  /** An ordered array of atoms in the system. */
  protected Atom[] atoms;
  /** Periodic boundary conditions and symmetry. */
  private Crystal crystal;
  /** Atomic coordinates for each symmetry operator. */
  private double[][][] sXYZ;
  /** Neighbor lists for each atom and symmetry operator. */
  private int[][][] neighborLists;
  /** Base radius of each atom. */
  private double[] baseRadius;
  /**
   * Overlap scale factor for each atom, when using the Hawkins, Cramer & Truhlar pairwise
   * descreening algorithm.
   *
   * <p>G. D. Hawkins, C. J. Cramer and D. G. Truhlar, "Parametrized Models of Aqueous Free Energies
   * of Solvation Based on Pairwise Descreening of Solute Atomic Charges from a Dielectric Medium",
   * J. Phys. Chem., 100, 19824-19839 (1996).
   */
  private double[] overlapScale;
  /** Flag to indicate if an atom should be included. */
  private boolean[] use;
  /** If true, the descreening size of atoms is based on their force field vdW radius */
  private final boolean descreenWithVDW;
  /** If true, hydrogen atoms displace solvent */
  private final boolean descreenWithHydrogen;
  /** If true, bonded atoms displace solvent */
  private final boolean descreen12;
  /** GK cut-off distance squared. */
  private double cut2;
  /** Forces all atoms to be considered during Born radius updates. */
  private boolean nativeEnvironmentApproximation;
  /** Born radius of each atom. */
  private double[] born;
  /** Gradient array for each thread. */
  private AtomicDoubleArray3D grad;
  /** Shared array for computation of Born radii gradient. */
  private AtomicDoubleArray sharedBornGrad;

  public BornGradRegion(int nt, ForceField forceField) {
    bornCRLoop = new BornCRLoop[nt];
    for (int i = 0; i < nt; i++) {
      bornCRLoop[i] = new BornCRLoop();
    }
    descreenWithVDW = forceField.getBoolean("DESCREEN_VDW", false);
    descreenWithHydrogen = forceField.getBoolean("DESCREEN_HYDROGEN", true);
    descreen12 = forceField.getBoolean("DESCREEN_12", true);
  }

  /**
   * Execute the InitializationRegion with the passed ParallelTeam.
   *
   * @param parallelTeam The ParallelTeam instance to execute with.
   */
  public void executeWith(ParallelTeam parallelTeam) {
    sharedBornGrad.reduce(parallelTeam, 0, atoms.length - 1);
    try {
      parallelTeam.execute(this);
    } catch (Exception e) {
      String message = " Exception evaluating Born radii chain rule gradient.\n";
      logger.log(Level.SEVERE, message, e);
    }
  }

  public void init(
      Atom[] atoms,
      Crystal crystal,
      double[][][] sXYZ,
      int[][][] neighborLists,
      double[] baseRadius,
      double[] overlapScale,
      boolean[] use,
      double cut2,
      boolean nativeEnvironmentApproximation,
      double[] born,
      AtomicDoubleArray3D grad,
      AtomicDoubleArray sharedBornGrad) {
    this.atoms = atoms;
    this.crystal = crystal;
    this.sXYZ = sXYZ;
    this.neighborLists = neighborLists;
    this.baseRadius = baseRadius;
    this.overlapScale = overlapScale;
    this.use = use;
    this.cut2 = cut2;
    this.nativeEnvironmentApproximation = nativeEnvironmentApproximation;
    this.born = born;
    this.grad = grad;
    this.sharedBornGrad = sharedBornGrad;
  }

  @Override
  public void run() {
    try {
      int nAtoms = atoms.length;
      execute(0, nAtoms - 1, bornCRLoop[getThreadIndex()]);
    } catch (Exception e) {
      String message =
          "Fatal exception computing Born radii chain rule term in thread "
              + getThreadIndex()
              + "\n";
      logger.log(Level.SEVERE, message, e);
    }
  }

  /**
   * Compute Born radii chain rule terms for a range of atoms via the Grycuk method.
   *
   * @since 1.0
   */
  private class BornCRLoop extends IntegerForLoop {

    private final double factor = -pow(PI, oneThird) * pow(6.0, (2.0 * oneThird)) / 9.0;
    private final double[] dx_local;
    private int threadID;
    // Extra padding to avert cache interference.
    private long pad0, pad1, pad2, pad3, pad4, pad5, pad6, pad7;
    private long pad8, pad9, pada, padb, padc, padd, pade, padf;

    BornCRLoop() {
      dx_local = new double[3];
    }

    @Override
    public void run(int lb, int ub) {

      double[] x = sXYZ[0][0];
      double[] y = sXYZ[0][1];
      double[] z = sXYZ[0][2];

      int nSymm = crystal.spaceGroup.symOps.size();
      for (int iSymOp = 0; iSymOp < nSymm; iSymOp++) {
        SymOp symOp = crystal.spaceGroup.symOps.get(iSymOp);
        double[][] transOp = new double[3][3];
        double[][] xyz = sXYZ[iSymOp];
        crystal.getTransformationOperator(symOp, transOp);
        for (int i = lb; i <= ub; i++) {
          if (!nativeEnvironmentApproximation && !use[i]) {
            continue;
          }

          // Check the value of the Born radii chain rule term.
          double bornGrad = sharedBornGrad.get(i);
          if (isInfinite(bornGrad) || isNaN(bornGrad)) {
            throw new EnergyException(
                format(" %s\n Born radii CR %d %8.3f", atoms[i], i, bornGrad), true);
          }

          final double ri = baseRadius[i];
          final double descreenRi = (descreenWithVDW) ? atoms[i].getVDWType().radius / 2.0 : ri;
          final double scaledRi = descreenRi * overlapScale[i];
          final double xi = x[i];
          final double yi = y[i];
          final double zi = z[i];
          final double rbi = born[i];
          double termi = PI4_3 / (rbi * rbi * rbi);
          termi = factor / pow(termi, (4.0 * oneThird));

          int[] list = neighborLists[iSymOp][i];
          for (int k : list) {
            if (!nativeEnvironmentApproximation && !use[k]) {
              continue;
            }
            final double baseRk = baseRadius[k];
            if (k != i) {
              if (!descreen12 && atoms[i].isBonded(atoms[k])) {
                // No descreening between bonded atoms.
                continue;
              }
              dx_local[0] = xyz[0][k] - xi;
              dx_local[1] = xyz[1][k] - yi;
              dx_local[2] = xyz[2][k] - zi;
              double r2 = crystal.image(dx_local);
              if (r2 > cut2) {
                continue;
              }
              final double xr = dx_local[0];
              final double yr = dx_local[1];
              final double zr = dx_local[2];
              final double r = sqrt(r2);

              // Atom i being descreeened by atom k.
              if (rbi < 50.0) {
                if (descreenWithHydrogen || !atoms[k].isHydrogen()) {
                  final double descreenRk =
                      (descreenWithVDW) ? atoms[k].getVDWType().radius / 2.0 : baseRk;
                  final double scaledRk = descreenRk * overlapScale[k];
                  double de = integralDerivative(r, r2, ri, scaledRk);
//                logger.info(format(
//                    " Born radii chain rule term %d %d r=%16.8f ri=%16.8f sk=%16.8f de=%16.8f", i, k,
//                    r, ri, sk, de));
                  if (isInfinite(de) || isNaN(de)) {
                    logger.warning(
                        format(" Born radii chain rule term is unstable %d %d %16.8f", i, k, de));
                  }
                  double dbr = termi * de / r;
                  de = dbr * sharedBornGrad.get(i);
//                logger.info(
//                    format(" Born radii chain rule terms %d %d rbi=%16.8f bornGrad=%16.8f de=%16.8f",
//                        i, k,
//                        rbi, sharedBornGrad.get(i), de));
                  incrementGradient(i, k, de, xr, yr, zr, transOp);
                }
              }

              // Atom k being descreeened by atom i.
              double rbk = born[k];
              if (rbk < 50.0) {
                if (descreenWithHydrogen || !atoms[i].isHydrogen()) {
                  double termk = PI4_3 / (rbk * rbk * rbk);
                  termk = factor / pow(termk, (4.0 * oneThird));
                  double de = integralDerivative(r, r2, baseRk, scaledRi);
//                logger.info(format(
//                    " Born radii chain rule term %d %d r=%16.8f ri=%16.8f sk=%16.8f de=%16.8f", k, i,
//                    r, rk, si, de));
                  if (isInfinite(de) || isNaN(de)) {
                    logger.warning(
                        format(" Born radii chain rule term is unstable %d %d %16.8f", k, i, de));
                  }
                  double dbr = termk * de / r;
                  de = dbr * sharedBornGrad.get(k);
//                logger.info(
//                    format(" Born radii chain rule terms %d %d rbk=%16.8f bornGrad=%16.8f de=%16.8f",
//                        k, i,
//                        rbk, sharedBornGrad.get(k), de));
                  incrementGradient(i, k, de, xr, yr, zr, transOp);
                }
              }
            } else if (iSymOp > 0 && rbi < 50.0) {
              if (descreenWithHydrogen || !atoms[k].isHydrogen()) {
                dx_local[0] = xyz[0][k] - xi;
                dx_local[1] = xyz[1][k] - yi;
                dx_local[2] = xyz[2][k] - zi;
                double r2 = crystal.image(dx_local);
                if (r2 > cut2) {
                  continue;
                }
                final double xr = dx_local[0];
                final double yr = dx_local[1];
                final double zr = dx_local[2];
                final double r = sqrt(r2);

                // Atom i being descreeened by atom k.
                final double descreenRk =
                    (descreenWithVDW) ? atoms[k].getVDWType().radius / 2.0 : baseRk;
                final double scaledRk = descreenRk * overlapScale[k];
                double de = integralDerivative(r, r2, ri, scaledRk);
                if (isInfinite(de) || isNaN(de)) {
                  logger.warning(
                      format(" Born radii chain rule term is unstable %d %d %d %16.8f", iSymOp, i, k,
                          de));
                }
                double dbr = termi * de / r;
                de = dbr * sharedBornGrad.get(i);
                incrementGradient(i, k, de, xr, yr, zr, transOp);
              }
              // For symmetry mates, atom k is not descreeened by atom i.
            }
          }
        }
      }
    }

    @Override
    public void start() {
      threadID = getThreadIndex();
    }

    /**
     * Use pairwise descreening to compute derivative of the integral of 1/r^6 with respect to r.
     *
     * @param r separation distance.
     * @param r2 separation distance squared.
     * @param radius base radius of descreened atom.
     * @param scaledRadius scaled radius descreening atom.
     * @return the derivative.
     */
    private double integralDerivative(double r, double r2, double radius, double scaledRadius) {
      double de = 0.0;
      // Descreen only if the descreened atom does not engulf the descreener.
      if (radius < r + scaledRadius) {
        // Atom i is engulfed by atom k.
        if (radius + r < scaledRadius) {
          double uik = scaledRadius - r;
          double uik2 = uik * uik;
          double uik4 = uik2 * uik2;
          de = -4.0 * PI / uik4;
        }

        // Lower integration bound depends on atoms sizes and separation.
        double sk2 = scaledRadius * scaledRadius;
        if (radius + r < scaledRadius) {
          // Atom i is engulfed by atom k.
          double lik = scaledRadius - r;
          double lik2 = lik * lik;
          double lik4 = lik2 * lik2;
          de = de + 0.25 * PI * (sk2 - 4.0 * scaledRadius * r + 17.0 * r2) / (r2 * lik4);
        } else if (r < radius + scaledRadius) {
          // Atoms are overlapped, begin integration from ri.
          double lik = radius;
          double lik2 = lik * lik;
          double lik4 = lik2 * lik2;
          de = de + 0.25 * PI * (2.0 * radius * radius - sk2 - r2) / (r2 * lik4);
        } else {
          // No overlap between atoms.
          double lik = r - scaledRadius;
          double lik2 = lik * lik;
          double lik4 = lik2 * lik2;
          de = de + 0.25 * PI * (sk2 - 4.0 * scaledRadius * r + r2) / (r2 * lik4);
        }
        // Upper integration bound is always the same.
        double uik = r + scaledRadius;
        double uik2 = uik * uik;
        double uik4 = uik2 * uik2;
        de = de - 0.25 * PI * (sk2 + 4.0 * scaledRadius * r + r2) / (r2 * uik4);
      }

      return de;
    }

    /**
     * Accumulate a contribution to the gradient and dU/dX/dL.
     *
     * @param i index of atom i.
     * @param k index of atom k.
     * @param dE partial derivative of the energy with respect to R.
     * @param xr x-component of the separation vector.
     * @param yr y-component of the separation vector.
     * @param zr z-component of the separation vector.
     */
    private void incrementGradient(
        int i, int k, double dE, double xr, double yr, double zr, double[][] transOp) {
      double dedx = dE * xr;
      double dedy = dE * yr;
      double dedz = dE * zr;
      grad.add(threadID, i, dedx, dedy, dedz);
      final double dedxk = dedx * transOp[0][0] + dedy * transOp[1][0] + dedz * transOp[2][0];
      final double dedyk = dedx * transOp[0][1] + dedy * transOp[1][1] + dedz * transOp[2][1];
      final double dedzk = dedx * transOp[0][2] + dedy * transOp[1][2] + dedz * transOp[2][2];
      grad.sub(threadID, k, dedxk, dedyk, dedzk);
    }
  }
}
