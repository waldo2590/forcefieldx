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
package ffx.potential.parsers;

import java.io.File;
import java.util.ArrayList;

import ffx.potential.bonded.Atom;
import ffx.potential.bonded.Bond;
import ffx.potential.bonded.MolecularAssembly;

/**
 * The MergeFilter class allows Force Field X to treat merging of Systems
 * just like opening a file from a hard disk or socket.
 *
 * @author Michael J. Schnieders
 * @since 1.0
 * @version $Id: $
 */
public class MergeFilter extends SystemFilter {

    /**
     * <p>Constructor for MergeFilter.</p>
     *
     * @param f a {@link ffx.potential.bonded.MolecularAssembly} object.
     * @param a a {@link java.util.ArrayList} object.
     * @param b a {@link java.util.ArrayList} object.
     */
    public MergeFilter(MolecularAssembly f, ArrayList<Atom> a, ArrayList<Bond> b) {
        super(new File(""), f, null, null);
        atomList = a;
        bondList = b;
    }

    /** {@inheritDoc} */
    @Override
    public boolean readFile() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean writeFile(File saveFile, boolean append) {
        return false;
    }
}
