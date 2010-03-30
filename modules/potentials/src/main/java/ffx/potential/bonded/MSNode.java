/**
 * Title: Force Field X
 * Description: Force Field X - Software for Molecular Biophysics.
 * Copyright: Copyright (c) Michael J. Schnieders 2001-2010
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

import static ffx.utilities.HashCodeUtil.hash;
import static ffx.utilities.HashCodeUtil.SEED;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.J3DGraphics2D;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.vecmath.Color3f;

/**
 * The MSNode class forms the basic unit that all data classes extend.
 */
public class MSNode extends DefaultMutableTreeNode implements ROLS {

    private static final long serialVersionUID = 1L;
    // UNIT TESTING FLAG
    public static boolean UNIT_TESTING = false;

    static {
        try {
            boolean b = Boolean.parseBoolean(System.getProperty("ffe.junit", "false"));
            UNIT_TESTING = b;
        } catch (Exception e) {
            UNIT_TESTING = false;
        }
    }
    public final int MultiScaleLevel;
    private String name;
    protected boolean selected = false;

    /**
     * Default MSNode Constructor
     */
    public MSNode() {
        name = null;
        MultiScaleLevel = ROLS.MaxLengthScale;
    }

    /**
     * Constructs a MSNode with the name n.
     */
    public MSNode(String n) {
        name = n;
        MultiScaleLevel = ROLS.MaxLengthScale;
    }

    public MSNode(String n, int multiScaleLevel) {
        this.name = n;
        this.MultiScaleLevel = multiScaleLevel;
    }

    /**
     * Returns true if Class c can be below this Class in the Hierarchy
     *
     * @param c
     *            Class
     * @return boolean
     */
    public boolean canBeChild(Class c) {
        try {
            int multiScaleLevel = c.getDeclaredField("MultiScaleLevel").getInt(
                    null);
            if (multiScaleLevel >= this.MultiScaleLevel) {
                return false;
            }
        } catch (Exception e) {
            return true;
        }
        return true;
    }

    public boolean destroy() {
        if (getParent() != null) {
            removeFromParent();
        }
        name = null;
        selected = false;
        return true;
    }

    @Override
    public void drawLabel(Canvas3D graphics, J3DGraphics2D g2d, Node node) {
        if (!isSelected()) {
            return;
        }
        MSNode dataNode;
        for (Enumeration e = children(); e.hasMoreElements();) {
            dataNode = (MSNode) e.nextElement();
            dataNode.drawLabel(graphics, g2d, node);
        }
    }

    /**
     * Overidden equals method that returns true if object is not equal to this
     * object, is of the same class as this, and has the same name.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        MSNode other = (MSNode) object;
        return name.equals(other.getName());
    }

    /**
     * Returns an ArrayList of all Atoms below the present MSNode.
     */
    public ArrayList<Atom> getAtomList() {
        ArrayList<Atom> arrayList = new ArrayList<Atom>();
        Enumeration e = depthFirstEnumeration();
        while (e.hasMoreElements()) {
            MSNode dataNode = (MSNode) e.nextElement();
            if (dataNode instanceof Atom) {
                arrayList.add((Atom) dataNode);
            }
        }
        return arrayList;
    }

    /**
     * If <code>this</code> MSNode or any MSNode below it <code>equals</code>
     * the argument, that MSNode is returned.
     */
    public MSNode contains(MSNode msNode) {
        Enumeration e = depthFirstEnumeration();
        while (e.hasMoreElements()) {
            MSNode node = (MSNode) e.nextElement();
            if (node.equals(msNode)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Returns an ArrayList of all Bonds below the present MSNode.
     */
    public ArrayList<ROLS> getBondList() {
        ArrayList<ROLS> arrayList = new ArrayList<ROLS>();
        return getList(Bond.class, arrayList);
    }

    /**
     * Returns an ArrayList of all Angles below the present MSNode.
     */
    public ArrayList<ROLS> getAngleList() {
        ArrayList<ROLS> arrayList = new ArrayList<ROLS>();
        return getList(Angle.class, arrayList);
    }

    /**
     * Returns an ArrayList of all Stretch-Bends below the present MSNode.
     */
    public ArrayList<ROLS> getStretchBendList() {
        ArrayList<ROLS> arrayList = new ArrayList<ROLS>();
        return getList(StretchBend.class, arrayList);
    }

    /**
     * Returns an ArrayList of all Urey-Bradleys below the present MSNode.
     */
    public ArrayList<ROLS> getUreyBradleyList() {
        ArrayList<ROLS> arrayList = new ArrayList<ROLS>();
        return getList(UreyBradley.class, arrayList);
    }

    /**
     * Returns an ArrayList of all Out-of-Plane Bends below the present MSNode.
     */
    public ArrayList<ROLS> getOutOfPlaneBendList() {
        ArrayList<ROLS> arrayList = new ArrayList<ROLS>();
        return getList(OutOfPlaneBend.class, arrayList);
    }

    /**
     * Returns an ArrayList of all Torsions below the present MSNode.
     */
    public ArrayList<ROLS> getTorsionList() {
        ArrayList<ROLS> arrayList = new ArrayList<ROLS>();
        return getList(Torsion.class, arrayList);
    }

    /**
     * Returns an ArrayList of all Pi-Orbital Torsions below the present MSNode.
     */
    public ArrayList<ROLS> getPiOrbitalTorsionList() {
        ArrayList<ROLS> arrayList = new ArrayList<ROLS>();
        return getList(PiOrbitalTorsion.class, arrayList);
    }

    /**
     * Returns an ArrayList of all Torsion-Torsions below the present MSNode.
     */
    public ArrayList<ROLS> getTorsionTorsionList() {
        ArrayList<ROLS> arrayList = new ArrayList<ROLS>();
        return getList(TorsionTorsion.class, arrayList);
    }

    @Override
    public double[] getCenter(boolean w) {
        double[] Rc = {0, 0, 0};
        double sum = 0, mass = 1;
        ArrayList<Atom> atomList = getAtomList();
        for (Atom a : atomList) {
            if (w) {
                mass = a.getMass();
                sum += mass;
            }
            Rc[0] += mass * a.getX();
            Rc[1] += mass * a.getY();
            Rc[2] += mass * a.getZ();
        }
        if (!w) {
            sum = atomList.size();
        }
        for (int i = 0; i < 3; i++) {
            Rc[i] /= sum;
        }
        return Rc;
    }

    /**
     * Returns an ArrayList of the MSNode's Children (instead of using an
     * Enumeration).
     */
    public ArrayList<MSNode> getChildList() {
        ArrayList<MSNode> l = new ArrayList<MSNode>();
        Enumeration e = children();
        while (e.hasMoreElements()) {
            l.add((MSNode) e.nextElement());
        }
        return l;
    }

    /**
     * Returns a ListIterator containing this MSNode's children.
     */
    public ListIterator<MSNode> getChildListIterator() {
        return getChildList().listIterator();
    }

    public double getExtent() {
        double extent = 0.0;
        for (Enumeration e = children(); e.hasMoreElements();) {
            MSNode node = (MSNode) e.nextElement();
            double temp = node.getExtent();
            if (temp > extent) {
                extent = temp;
            }
        }
        return extent;
    }

    @Override
    public ArrayList<ROLS> getList(Class c, ArrayList<ROLS> nodes) {
        if (c.isInstance(this)) {
            nodes.add(this);
        }
        if (isLeaf() || !canBeChild(c)) {
            return nodes;
        }
        for (Enumeration e = children(); e.hasMoreElements();) {
            ROLS node = (ROLS) e.nextElement();
            node.getList(c, nodes);
        }
        return nodes;
    }

    @Override
    public long getMSCount(Class c, long count) {
        if (c.isInstance(this)) {
            count++;
        }
        if (!canBeChild(c)) {
            return count;
        }
        for (Enumeration e = children(); e.hasMoreElements();) {
            MSNode node = (MSNode) e.nextElement();
            count += node.getMSCount(c, count);
        }
        return count;
    }

    @Override
    public ROLS getMSNode(Class c) {
        TreeNode[] nodes = getPath();
        for (TreeNode n : nodes) {
            if (c.isInstance(n)) {
                ROLS msm = (ROLS) n;
                return msm;
            }
        }
        return null;
    }

    public int getMultiScaleLevel() {
        return MultiScaleLevel;
    }

    @Override
    public double getMW() {
        double weight = 0.0;
        for (ListIterator<Atom> li = getAtomList().listIterator(); li.hasNext();) {
            weight += li.next().getMass();
        }
        return weight;
    }

    /**
     * Returns the name of this MSNode.
     */
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return hash(SEED, name.hashCode());
    }

    public boolean isSelected() {
        return selected;
    }

    /**
     * Prints the MSNode's name
     */
    public void print() {
        System.out.println(name);
    }

    @Override
    public void setColor(RendererCache.ColorModel colorModel, Color3f color,
            Material mat) {
        for (Enumeration e = children(); e.hasMoreElements();) {
            MSNode node = (MSNode) e.nextElement();
            node.setColor(colorModel, color, mat);
        }
    }

    /**
     * Sets the name of this NodeObect to n.
     */
    public void setName(String n) {
        name = n;
    }

    public void setSelected(boolean b) {
        selected = b;
        for (Enumeration e = children(); e.hasMoreElements();) {
            MSNode node = (MSNode) e.nextElement();
            node.setSelected(b);
        }
    }

    @Override
    public void setView(RendererCache.ViewModel viewModel,
            List<BranchGroup> newShapes) {
        for (Enumeration e = children(); e.hasMoreElements();) {
            MSNode node = (MSNode) e.nextElement();
            node.setView(viewModel, newShapes);
        }
    }

    /**
     * Overidden toString method returns the MSNode's name
     */
    @Override
    public String toString() {
        return name;
    }

    @Override
    public void update() {
        for (Enumeration e = children(); e.hasMoreElements();) {
            MSNode node = (MSNode) e.nextElement();
            node.update();
        }
    }
}
