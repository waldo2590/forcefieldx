/**
 * Title: Force Field X
 * Description: Force Field X - Software for Molecular Biophysics
 * Copyright: Copyright (c) Michael J. Schnieders 2001-2012
 *
 * This file is part of Force Field X.
 *
 * Force Field X is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published
 * by the Free Software Foundation.
 *
 * Force Field X is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Force Field X; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */
package ffx.potential.bonded;

import java.util.EnumSet;
import java.util.Hashtable;
import java.util.ListIterator;
import java.util.logging.Logger;

import javax.media.j3d.Canvas3D;
import javax.media.j3d.J3DGraphics2D;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.vecmath.Color3f;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import static ffx.utilities.HashCodeUtil.SEED;
import static ffx.utilities.HashCodeUtil.hash;

/**
 * The Residue class represents individual amino acids or nucleic acid bases.
 *
 * @author Michael J. Schnieders
 * @version $Id: $
 */
public class Residue extends MSGroup {

    private static final Logger logger = Logger.getLogger(ffx.potential.bonded.Residue.class.getName());

    public enum AA {

        GLYCINE, ALANINE, VALINE, LEUCINE, ILLUECINE, SERINE, THREONINE,
        CYSTIENE, PROLINE, PHENYLALANINE, TYROSINE, TYPTOPHAN, ASPARTATE,
        ASPARTAMINE, GLUTAMATE, GLUTAMINE, METHIONINE, LYSINE, ARGININE,
        HISTIDINE;
    }

    public enum AA1 {

        G, A, V, L, I, S, T, C, P, F, Y, W, D, N, E, Q, M, K, R, H, U, Z, O, B,
        J, f, a, n, m, X;
    }

    public enum AA3 {

        GLY, ALA, VAL, LEU, ILE, SER, THR, CYS, PRO, PHE, TYR, TRP, ASP, ASN,
        GLU, GLN, MET, LYS, ARG, HIS, HID, HIE, ORN, AIB, PCA, FOR, ACE, NH2,
        NME, UNK;
    }

    public enum NA {

        ADENINE, CYTOSINE, GUANINE, URACIL, DEOXYADENINE, DEOXYCYTOSINE,
        DEOXYGUANINE, THYMINE, MONOPHOSPHATE, DIPHOSPHATE, TRIPHOSPHATE;
    }

    public enum NA1 {

        A, C, G, U, D, I, B, T, P, Q, R, X;
    }

    public enum NA3 {

        A, C, G, U, DA, DC, DG, DT, MPO, DPO, TPO, UNK;
    }

    public enum ResidueType {

        NA, AA, UNK;
    }

    public enum SSType {

        NONE, HELIX, SHEET, TURN;
    }
    private static final long serialVersionUID = 1L;
    private static Point3d point3d = new Point3d();
    private static Point2d point2d = new Point2d();
    /** Constant <code>NA1Set</code> */
    public static EnumSet NA1Set = EnumSet.allOf(NA1.class);
    /** Constant <code>NA3Set</code> */
    public static EnumSet NA3Set = EnumSet.allOf(NA3.class);
    /** Constant <code>NASet</code> */
    public static EnumSet NASet = EnumSet.allOf(NA.class);
    /** Constant <code>NA1toNA3</code> */
    public static Hashtable<NA1, NA3> NA1toNA3 = new Hashtable<NA1, NA3>();
    /** Constant <code>NA3Color</code> */
    public static Hashtable<NA3, Color3f> NA3Color = new Hashtable<NA3, Color3f>();
    /** Constant <code>AA1Set</code> */
    public static EnumSet AA1Set = EnumSet.allOf(AA1.class);
    /** Constant <code>AA3Set</code> */
    public static EnumSet AA3Set = EnumSet.allOf(AA3.class);
    /** Constant <code>AASet</code> */
    public static EnumSet AASet = EnumSet.allOf(AA.class);
    /** Constant <code>AA1toAA3</code> */
    public static Hashtable<AA1, AA3> AA1toAA3 = new Hashtable<AA1, AA3>();
    /** Constant <code>AA3Color</code> */
    public static Hashtable<AA3, Color3f> AA3Color = new Hashtable<AA3, Color3f>();
    /** Constant <code>SSTypeColor</code> */
    public static Hashtable<SSType, Color3f> SSTypeColor = new Hashtable<SSType, Color3f>();
    /** Constant <code>Ramachandran="new String[17]"</code> */
    public static String Ramachandran[] = new String[17];

    static {
        NA1 na1[] = NA1.values();
        NA3 na3[] = NA3.values();
        for (int i = 0; i < NA1.values().length; i++) {
            NA1toNA3.put(na1[i], na3[i]);
        }
    }

    static {
        NA3Color.put(NA3.A, RendererCache.RED);
        NA3Color.put(NA3.C, RendererCache.MAGENTA);
        NA3Color.put(NA3.G, RendererCache.BLUE);
        NA3Color.put(NA3.U, RendererCache.YELLOW);
        NA3Color.put(NA3.DA, RendererCache.RED);
        NA3Color.put(NA3.DC, RendererCache.MAGENTA);
        NA3Color.put(NA3.DG, RendererCache.BLUE);
        NA3Color.put(NA3.DT, RendererCache.ORANGE);
        NA3Color.put(NA3.MPO, RendererCache.GREEN);
        NA3Color.put(NA3.DPO, RendererCache.GREEN);
        NA3Color.put(NA3.TPO, RendererCache.GREEN);
        NA3Color.put(NA3.UNK, RendererCache.CYAN);
    }

    static {
        AA1 aa1[] = AA1.values();
        AA3 aa3[] = AA3.values();
        for (int i = 0; i < AA1.values().length; i++) {
            AA1toAA3.put(aa1[i], aa3[i]);
        }
    }

    static {
        AA3Color.put(AA3.ALA, RendererCache.GRAY);
        AA3Color.put(AA3.ARG, RendererCache.BLUE);
        AA3Color.put(AA3.ASN, RendererCache.BLUE);
        AA3Color.put(AA3.ASP, RendererCache.RED);
        AA3Color.put(AA3.CYS, RendererCache.YELLOW);
        AA3Color.put(AA3.GLN, RendererCache.BLUE);
        AA3Color.put(AA3.GLU, RendererCache.RED);
        AA3Color.put(AA3.GLY, RendererCache.GRAY);
        AA3Color.put(AA3.ILE, RendererCache.GRAY);
        AA3Color.put(AA3.LEU, RendererCache.GRAY);
        AA3Color.put(AA3.LYS, RendererCache.BLUE);
        AA3Color.put(AA3.MET, RendererCache.YELLOW);
        AA3Color.put(AA3.PHE, RendererCache.GREEN);
        AA3Color.put(AA3.PRO, RendererCache.ORANGE);
        AA3Color.put(AA3.SER, RendererCache.BLUE);
        AA3Color.put(AA3.THR, RendererCache.BLUE);
        AA3Color.put(AA3.TRP, RendererCache.GREEN);
        AA3Color.put(AA3.TYR, RendererCache.GREEN);
        AA3Color.put(AA3.VAL, RendererCache.GRAY);
        AA3Color.put(AA3.HIS, RendererCache.BLUE);
        AA3Color.put(AA3.HIE, RendererCache.BLUE);
        AA3Color.put(AA3.HID, RendererCache.BLUE);
        AA3Color.put(AA3.ORN, RendererCache.ORANGE);
        AA3Color.put(AA3.AIB, RendererCache.ORANGE);
        AA3Color.put(AA3.PCA, RendererCache.ORANGE);
        AA3Color.put(AA3.FOR, RendererCache.RED);
        AA3Color.put(AA3.ACE, RendererCache.RED);
        AA3Color.put(AA3.NH2, RendererCache.BLUE);
        AA3Color.put(AA3.NME, RendererCache.BLUE);
        AA3Color.put(AA3.UNK, RendererCache.MAGENTA);
    }

    static {
        SSTypeColor.put(SSType.NONE, RendererCache.WHITE);
        SSTypeColor.put(SSType.SHEET, RendererCache.PINK);
        SSTypeColor.put(SSType.HELIX, RendererCache.BLUE);
        SSTypeColor.put(SSType.TURN, RendererCache.YELLOW);
    }

    static {
        Ramachandran[0] = "Default (Extended)       [-135.0  135.0]";
        Ramachandran[1] = "Alpha Helix (R)          [ -57.0  -47.0]";
        Ramachandran[2] = "Alpha Helix (L)          [  57.0   47.0]";
        Ramachandran[3] = "3-10 Helix               [ -49.0  -26.0]";
        Ramachandran[4] = "Pi Helix                 [ -57.0  -70.0]";
        Ramachandran[5] = "Polyproline II Helix     [ -79.0  149.0]";
        Ramachandran[6] = "Parallel Beta Strand     [-119.0  113.0]";
        Ramachandran[7] = "Antiparallel Beta Strand [-139.0  135.0]";
        Ramachandran[8] = "Beta-Hairpin 2' (i+1)    [  90.0 -170.0]";
        Ramachandran[9] = "Beta-Hairpin 2' (i+2)    [ -80.0  -10.0]";
        Ramachandran[10] = "Beta-Hairpin 1' (i+1)    [  57.0   47.0]";
        Ramachandran[11] = "Beta-Hairpin 1' (i+2)    [  57.0   47.0]";
        Ramachandran[12] = "Beta-Hairpin 1  (i+1)    [ -57.0  -47.0]";
        Ramachandran[13] = "Beta-Hairpin 1  (i+2)    [ -57.0  -47.0]";
        Ramachandran[14] = "Beta-Hairpin 1  (i+3)    [  90.0 -170.0]";
        Ramachandran[15] = "Beta-Hairpin 3' (i+1)    [  57.0   47.0]";
        Ramachandran[16] = "Beta-Hairpin 3' (i+2)    [ -80.0  -10.0]";
    }
    /**
     * The residue number of this atom in a chain.
     */
    private int resNumber;
    /**
     * Possibly redundant PDB chain ID.
     */
    private Character chainID;
    /**
     * Unique segID.
     */
    private String segID;
    /**
     * Secondary structure type.
     */
    private SSType ssType = SSType.NONE;
    /**
     * Residue type.
     */
    protected ResidueType residueType = ResidueType.UNK;
    private AA3 aa;
    private NA3 na;

    /**
     * Default Constructor where num is this Residue's position in the Polymer.
     *
     * @param num a int.
     * @param rt a {@link ffx.potential.bonded.Residue.ResidueType} object.
     */
    public Residue(int num, ResidueType rt) {
        resNumber = num;
        residueType = rt;
        assignResidueType();
    }

    /**
     * <p>Constructor for Residue.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param rt a {@link ffx.potential.bonded.Residue.ResidueType} object.
     */
    public Residue(String name, ResidueType rt) {
        super(name);
        residueType = rt;
        assignResidueType();
    }

    /**
     * Name is the residue's 3 letter abbreviation and num is its position in
     * the Polymer.
     *
     * @param name a {@link java.lang.String} object.
     * @param num a int.
     * @param rt a {@link ffx.potential.bonded.Residue.ResidueType} object.
     */
    public Residue(String name, int num, ResidueType rt) {
        this(name, rt);
        resNumber = num;
    }

    /**
     * Name is the residue's 3 letter abbreviation and num is its position in
     * the Polymer.
     *
     * @param name a {@link java.lang.String} object.
     * @param resNumber a int.
     * @param rt a {@link ffx.potential.bonded.Residue.ResidueType} object.
     * @param chainID a {@link java.lang.Character} object.
     * @param segID a {@link java.lang.String} object.
     */
    public Residue(String name, int resNumber, ResidueType rt, Character chainID,
            String segID) {
        this(name, rt);
        this.resNumber = resNumber;
        this.chainID = chainID;
        this.segID = segID;
    }

    /**
     * As above, with atoms being a FNode with this Residue's atoms as child
     * nodes
     *
     * @param name a {@link java.lang.String} object.
     * @param num a int.
     * @param atoms a {@link ffx.potential.bonded.MSNode} object.
     * @param rt a {@link ffx.potential.bonded.Residue.ResidueType} object.
     */
    public Residue(String name, int num, MSNode atoms, ResidueType rt) {
        super(name, atoms);
        resNumber = num;
        residueType = rt;
        assignResidueType();
        finalize(true);
    }

    /**
     * {@inheritDoc}
     *
     * Allows adding Atoms to the Residue.
     */
    @Override
    public MSNode addMSNode(MSNode o) {
        Atom currentAtom = null;
        if (o instanceof Atom) {
            Atom newAtom = (Atom) o;
            Character newAlt = newAtom.getAltLoc();
            MSNode atoms = getAtomNode();
            currentAtom = (Atom) atoms.contains(newAtom);
            if (currentAtom == null) {
                currentAtom = newAtom;
                atoms.add(newAtom);
                setFinalized(false);
            } else {
                /**
                 * Allow overwriting of the root alternate
                 * conformer (' ' or 'A').
                 */
                Character currentAlt = currentAtom.getAltLoc();
                if (currentAlt.equals(' ') || currentAlt.equals('A')) {
                    if (!newAlt.equals(' ') && !newAlt.equals('A')) {
                        newAtom.xyzIndex = currentAtom.xyzIndex;
                        atoms.remove(currentAtom);
                        currentAtom = newAtom;
                        atoms.add(currentAtom);
                        setFinalized(false);
                    }
                }
            }
        } else {
            logger.warning("Only an Atom can be added to a Residue.");
        }
        return currentAtom;
    }
    
    /**
     * <p>deleteAtom</p>
     *
     * @param atomToDelete a {@link ffx.potential.bonded.Atom} object.
     */
    public void deleteAtom(Atom atomToDelete) {
        MSNode atoms = getAtomNode();
        if (atoms.contains(atomToDelete) != null) {
            logger.info(" The following atom is being deleted from the model:\n" 
                    + atomToDelete.toString());
            atoms.remove(atomToDelete);
        }
    }

    private void assignResidueType() {
        String name = getName().toUpperCase();
        switch (residueType) {
            case AA:
                aa = null;
                try {
                    if (name.length() == 3) {
                        aa = AA3.valueOf(name);
                    } else if (name.length() == 1) {
                        AA1 aa1 = AA1.valueOf(name);
                        aa = AA1toAA3.get(aa1);
                    }
                } catch (Exception e) {
                    aa = AA3.UNK;
                }
                break;
            case NA:
                na = null;
                try {
                    if (name.length() == 2 || name.length() == 3) {
                        na = NA3.valueOf(name);
                    } else if (name.length() == 1) {
                        NA1 na1 = NA1.valueOf(name);
                        na = NA1toNA3.get(na1);
                    }
                } catch (Exception e) {
                    na = NA3.UNK;
                }
                break;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawLabel(Canvas3D canvas, J3DGraphics2D g2d, Node node) {
        if (RendererCache.labelResidues) {
            double d[] = getCenter();
            point3d.x = d[0];
            point3d.y = d[1];
            point3d.z = d[2];
            RendererCache.getScreenCoordinate(canvas, node, point3d, point2d);
            g2d.drawString(getName(), (float) point2d.x, (float) point2d.y);
        }
        if (RendererCache.labelAtoms) {
            super.drawLabel(canvas, g2d, node);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Overidden equals method that return true if object is not equals to this,
     * is of the same class, has the same parent Polymer and the same sequence
     * number.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Residue other = (Residue) object;
        if (getParent() == null || other.getParent() == null) {
            return (getResidueNumber() == other.getResidueNumber());
        } else if (getParent() == other.getParent()) {
            return (getResidueNumber() == other.getResidueNumber());
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * The Finalize method should be called once all atoms have been added to
     * the Residue. Geometry objects (Bonds, Angles, etc) are then formed,
     * followed by a determination of under-constrained (Dangeling) atoms.
     */
    @Override
    public void finalize(boolean finalizeGeometry) {
        setFinalized(false);
        getAtomNode().setName("Atoms (" + getAtomList().size() + ")");
        if (finalizeGeometry) {
            // constructValenceTerms();
            collectValenceTerms();
            removeLeaves();
        }
        // findDangelingAtoms();
        setCenter(getMultiScaleCenter(false));
        setFinalized(true);
    }

    /**
     * Returns the position of this Residue's Alpha Carbon (if it is an amino
     * acid).
     *
     * @return a {@link javax.vecmath.Vector3d} object.
     */
    public Vector3d getAlpha3d() {
        Atom a = (Atom) getAtomNode("CA");
        if (a == null) {
            return null;
        }
        Vector3d temp = new Vector3d();
        a.getV3D(temp);
        return temp;
    }

    /**
     * Returns this Residues Parent Polymer name.
     *
     * @return a {@link java.lang.Character} object.
     */
    public Character getChainID() {
        return chainID;
    }

    // Public data access methods
    /**
     * Returns this Residue's sequence number.
     *
     * @return a int.
     */
    public int getResidueNumber() {
        return resNumber;
    }

    /** {@inheritDoc} */
    @Override
    public final int hashCode() {
        int hash = hash(SEED, getParent().hashCode());
        return hash(hash, getResidueNumber());
    }

    /**
     * {@inheritDoc}
     *
     * Prints "Residue Number: x" to stdout.
     */
    @Override
    public void print() {
        logger.info(toString());
        super.print();
    }

    /**
     * <p>printSideChainCOM</p>
     */
    public void printSideChainCOM() {
        Atom a;
        Vector3d v = new Vector3d();
        Vector3d v2 = new Vector3d();
        int count = 0;
        for (ListIterator li = getAtomList().listIterator(); li.hasNext();) {
            a = (Atom) li.next();
            String id = a.getName();
            if (!id.equals("CA") && !id.equals("N") && !id.equals("C")
                && !id.equals("O")) {
                a.getV3D(v2);
                v.add(v2);
                count++;
            } else if (id.equals("CA")) {
                a.print();
            }
        }
        v.scale(1.0f / count);
        logger.info(getName() + " " + v.toString());
    }

    /** {@inheritDoc} */
    @Override
    public void setColor(RendererCache.ColorModel newColorModel, Color3f color,
                         Material mat) {
        // If Color by Residue, pass this Residue's Color
        if (newColorModel == RendererCache.ColorModel.RESIDUE) {
            switch (residueType) {
                case AA:
                    color = AA3Color.get(aa);
                    break;
                case NA:
                    color = NA3Color.get(na);
                    break;
                default:
                    color = null;
            }
            if (color == null) {
                return;
            }
            mat = RendererCache.materialFactory(color);
        } else if (newColorModel == RendererCache.ColorModel.STRUCTURE) {
            color = SSTypeColor.get(ssType);
            mat = RendererCache.materialFactory(color);
        }
        super.setColor(newColorModel, color, mat);
    }

    /**
     * <p>setNumber</p>
     *
     * @param n a int.
     */
    public void setNumber(int n) {
        resNumber = n;
    }

    /**
     * <p>Setter for the field <code>chainID</code>.</p>
     *
     * @param c a {@link java.lang.Character} object.
     */
    public void setChainID(Character c) {
        chainID = c;
    }

    /**
     * <p>Getter for the field <code>segID</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSegID() {
        return segID;
    }

    /**
     * <p>setSSType</p>
     *
     * @param ss a {@link ffx.potential.bonded.Residue.SSType} object.
     */
    public void setSSType(SSType ss) {
        ssType = ss;
    }
    private String shortString = null;

    /** {@inheritDoc} */
    @Override
    public String toString() {
        if (shortString == null) {
            shortString = new String("" + resNumber + "-" + getName());
        }
        return shortString;
    }
}
