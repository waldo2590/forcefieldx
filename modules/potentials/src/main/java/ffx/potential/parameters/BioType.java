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
package ffx.potential.parameters;

import java.util.Comparator;

/**
 * The BioType class maps PDB identifiers to atom types.
 *
 * @author Michael J. Schnieders
 * @since 1.0
 * @version $Id: $
 */
public final class BioType extends BaseType implements Comparator<String> {

    public int index;
    public final String atomName;
    public final String moleculeName;
    public int atomType;
    public final String bonds[];

    /**
     * BioType Constructor.
     *
     * @param index
     *            int
     * @param atomName
     *            String
     * @param moleculeName
     *            String
     * @param atomType
     *            int
     * @param bonds an array of {@link java.lang.String} objects.
     */
    public BioType(int index, String atomName, String moleculeName, int atomType, String bonds[]) {
        super(ForceField.ForceFieldType.BIOTYPE, Integer.toString(index));
        this.index = index;
        this.atomName = atomName;
        if (moleculeName != null) {
            this.moleculeName = moleculeName.replace(',', ' ').replace('"', ' ').trim();
        } else {
            this.moleculeName = null;
        }
        this.atomType = atomType;
        this.bonds = bonds;
    }

    /**
     * <p>incrementIndexAndType</p>
     *
     * @param indexIncrement a int.
     * @param typeIncrement a int.
     */
    public void incrementIndexAndType(int indexIncrement, int typeIncrement) {
        index += indexIncrement;
        atomType += typeIncrement;
        setKey(Integer.toString(index));
    }

    /**
     * {@inheritDoc}
     *
     * Nicely formatted biotype.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(String.format("biotype  %5d  %-4s  \"%-23s\"  %5d", index, atomName,
                                                           moleculeName, atomType));
        if (bonds != null && bonds.length > 0) {
            for (int i = 0; i < bonds.length; i++) {
                sb.append(String.format("  %-4s", bonds[i]));
            }
        }
        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    public int compare(String s1, String s2) {

        int t1 = Integer.parseInt(s1);
        int t2 = Integer.parseInt(s2);

        if (t1 < t2) {
            return -1;
        }
        if (t1 > t2) {
            return 1;
        }

        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || !(other instanceof BioType)) {
            return false;
        }
        BioType bioType = (BioType) other;
        if (bioType.index == this.index) {
            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + index;
        return hash;
    }
}
