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
package ffx.xray;

import ffx.crystal.Crystal;
import ffx.crystal.HKL;
import ffx.crystal.ReflectionList;
import ffx.crystal.Resolution;
import ffx.crystal.SpaceGroup;

import java.io.File;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tim Fenn<br>
 *
 * This method parses CCP4 MTZ files:<br>
 * 
 * @see <a href="http://www.ccp4.ac.uk/html/maplib.html" target="_blank">
 *
 * @see <a href="http://www.ccp4.ac.uk/dist/html/library.html" target="_blank">
 */
public class MTZFilter {

    private static final Logger logger = Logger.getLogger(MTZFilter.class.getName());

    private class column {

        public String label;
        public char type;
        public int id;
    }

    private class dataset {

        public String project;
        public String dataset;
        public double lambda;
        public double[] cell = new double[6];
    }

    private static enum Header {

        VERS, TITLE, NCOL, SORT, SYMINF, SYMM, RESO, VALM, COL, COLUMN, NDIF,
        PROJECT, CRYSTAL, DATASET, DCELL, DWAVEL, BATCH,
        END, NOVALUE;

        public static Header toHeader(String str) {
            try {
                return valueOf(str);
            } catch (Exception ex) {
                return NOVALUE;
            }
        }
    }
    final private ArrayList<column> columns = new ArrayList();
    final private ArrayList<dataset> datasets = new ArrayList();
    private boolean headerparsed = false;
    private String title;
    private int h, k, l, fo, sigfo, rfree;
    public int ncol;
    public int nrfl;
    public int nbatches;
    public int sgnum;
    public String sgname;
    public double reslow;
    public double reshigh;

    // null constructor
    public MTZFilter() {
    }

    /*
    public boolean readFile(){
    this(readFile(molecularAssembly.getFile()));
    }
     */
    public ReflectionList getReflectionList(File mtzFile) {
        ByteOrder b = ByteOrder.nativeOrder();
        FileInputStream fis;
        DataInputStream dis;
        try {
            fis = new FileInputStream(mtzFile);
            dis = new DataInputStream(fis);

            byte headeroffset[] = new byte[4];
            byte bytes[] = new byte[80];
            int offset = 0;

            // eat "MTZ" title
            dis.read(bytes, offset, 4);
            String mtzstr = new String(bytes);

            // header offset
            dis.read(headeroffset, offset, 4);

            // machine stamp
            dis.read(bytes, offset, 4);
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            int stamp = bb.order(ByteOrder.BIG_ENDIAN).getInt();
            String stampstr = Integer.toHexString(stamp);
            switch (stampstr.charAt(0)) {
                case '1':
                case '3':
                    if (b.equals(ByteOrder.LITTLE_ENDIAN)) {
                        b = ByteOrder.BIG_ENDIAN;
                    }
                    break;
                case '4':
                    if (b.equals(ByteOrder.BIG_ENDIAN)) {
                        b = ByteOrder.LITTLE_ENDIAN;
                    }
                    break;
            }

            bb = ByteBuffer.wrap(headeroffset);
            int headeroffseti = bb.order(b).getInt();

            // skip to header and parse
            dis.skipBytes((headeroffseti - 4) * 4);

            for (Boolean parsing = true; parsing; dis.read(bytes, offset, 80)) {
                mtzstr = new String(bytes);
                parsing = parse_header(mtzstr);
            }
        } catch (EOFException eof) {
            System.out.println("EOF reached ");
        } catch (IOException ioe) {
            System.out.println("IO Exception: " + ioe.getMessage());
            return null;
        }

        h = k = l = fo = sigfo = rfree = -1;
        parse_columns();

        if (fo < 0) {
            logger.info("insufficient information in MTZ header to generate Reflection List");
            return null;
        }

        column c = (column) columns.get(fo);
        dataset d = (dataset) datasets.get(c.id - 1);

        if (logger.isLoggable(Level.INFO)) {
            StringBuffer sb = new StringBuffer();
            sb.append(String.format("\nOpening %s\n", mtzFile.getName()));
            sb.append(String.format("setting up Reflection List based on MTZ:\n"));
            sb.append(String.format("  spacegroup #: %d (name: %s)\n",
                    sgnum, SpaceGroup.spaceGroupNames[sgnum - 1]));
            sb.append(String.format("  resolution: %8.3f\n", 0.9999 * reshigh));
            sb.append(String.format("  cell: %8.3f %8.3f %8.3f %8.3f %8.3f %8.3f\n",
                    d.cell[0], d.cell[1], d.cell[2], d.cell[3], d.cell[4], d.cell[5]));
            logger.info(sb.toString());
        }

        Crystal crystal = new Crystal(d.cell[0], d.cell[1], d.cell[2],
                d.cell[3], d.cell[4], d.cell[5], SpaceGroup.spaceGroupNames[sgnum - 1]);
        Resolution resolution = new Resolution(0.9999 * reshigh);

        return new ReflectionList(crystal, resolution);
    }

    public boolean readFile(File mtzFile, ReflectionList reflectionlist,
            RefinementData refinementdata) {
        ByteOrder b = ByteOrder.nativeOrder();
        FileInputStream fis;
        DataInputStream dis;
        try {
            fis = new FileInputStream(mtzFile);
            dis = new DataInputStream(fis);

            byte headeroffset[] = new byte[4];
            byte bytes[] = new byte[80];
            int offset = 0;

            // eat "MTZ" title
            dis.read(bytes, offset, 4);
            String mtzstr = new String(bytes);

            // header offset
            dis.read(headeroffset, offset, 4);

            // machine stamp
            dis.read(bytes, offset, 4);
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            int stamp = bb.order(ByteOrder.BIG_ENDIAN).getInt();
            String stampstr = Integer.toHexString(stamp);
            switch (stampstr.charAt(0)) {
                case '1':
                case '3':
                    if (b.equals(ByteOrder.LITTLE_ENDIAN)) {
                        b = ByteOrder.BIG_ENDIAN;
                    }
                    break;
                case '4':
                    if (b.equals(ByteOrder.BIG_ENDIAN)) {
                        b = ByteOrder.LITTLE_ENDIAN;
                    }
                    break;
            }

            bb = ByteBuffer.wrap(headeroffset);
            int headeroffseti = bb.order(b).getInt();

            // skip to header and parse
            dis.skipBytes((headeroffseti - 4) * 4);

            for (Boolean parsing = true; parsing; dis.read(bytes, offset, 80)) {
                mtzstr = new String(bytes);
                parsing = parse_header(mtzstr);
            }

            // reopen to start at beginning
            fis = new FileInputStream(mtzFile);
            dis = new DataInputStream(fis);

            // skip initial header
            dis.skipBytes(80);

            // column identifiers
            h = k = l = fo = sigfo = rfree = -1;
            parse_columns();

            if (h < 0 || k < 0 || l < 0) {
                String message = "Fatal error in MTZ file - no H K L indexes?\n";
                logger.log(Level.SEVERE, message);
                return false;
            }

            // read in data
            int nread, nignore, nres;
            nread = nignore = nres = 0;
            float data[] = new float[ncol];
            for (int i = 0; i < nrfl; i++) {
                for (int j = 0; j < ncol; j++) {
                    dis.read(bytes, offset, 4);
                    bb = ByteBuffer.wrap(bytes);
                    data[j] = bb.order(b).getFloat();
                }
                int ih = (int) data[h];
                int ik = (int) data[k];
                int il = (int) data[l];
                HKL hkl = reflectionlist.getHKL(ih, ik, il);
                if (hkl != null) {
                    if (fo > 0 && sigfo > 0) {
                        refinementdata.fsigf(hkl.index(), data[fo], data[sigfo]);
                    }
                    if (rfree > 0) {
                        refinementdata.freer(hkl.index(), (int) data[rfree]);
                    }
                    nread++;
                } else {
                    HKL tmp = new HKL(ih, ik, il);
                    if (Crystal.invressq(reflectionlist.crystal, tmp)
                            > reflectionlist.resolution.invressq_limit()) {
                        nres++;
                    } else {
                        nignore++;
                    }
                }
            }
            StringBuffer sb = new StringBuffer();
            sb.append(String.format("\nOpening %s\n", mtzFile.getName()));
            sb.append(String.format("MTZ file type (machine stamp): %s\n",
                    stampstr));
            sb.append(String.format("# HKL read in:                             %d\n",
                    nread));
            sb.append(String.format("# HKL NOT read in (too high resolution):   %d\n",
                    nres));
            sb.append(String.format("# HKL NOT read in (not in internal list?): %d\n",
                    nignore));
            sb.append(String.format("# HKL in internal list:                    %d\n",
                    reflectionlist.hkllist.size()));
            if (logger.isLoggable(Level.INFO)) {
                logger.info(sb.toString());
            }

            if (rfree < 0) {
                refinementdata.generateRFree();
            }
        } catch (EOFException eof) {
            System.out.println("EOF reached ");
        } catch (IOException ioe) {
            System.out.println("IO Exception: " + ioe.getMessage());
            return false;
        }

        return true;
    }

    private Boolean parse_header(String str) {
        Boolean parsing = true;
        column col;
        dataset dset;

        int ndset;
        String[] strarray = str.split("\\s+");

        if (headerparsed) {
            if (Header.toHeader(strarray[0]) == Header.END) {
                return false;
            } else {
                return true;
            }
        }

        switch (Header.toHeader(strarray[0])) {
            case TITLE:
                title = str.substring(5);
                break;
            case NCOL:
                ncol = Integer.parseInt(strarray[1]);
                nrfl = Integer.parseInt(strarray[2]);
                nbatches = Integer.parseInt(strarray[3]);
                break;
            case SORT:
                break;
            case SYMINF:
                sgnum = Integer.parseInt(strarray[4]);
                if (strarray[5].startsWith("'")) {
                    sgname = strarray[5].substring(1, strarray[5].length() - 1);
                } else {
                    sgname = strarray[5];
                }
                break;
            case SYMM:
                break;
            case RESO:
                double r1 = Math.sqrt(1.0 / Float.parseFloat(strarray[1]));
                double r2 = Math.sqrt(1.0 / Float.parseFloat(strarray[2]));
                reslow = Math.max(r1, r2);
                reshigh = Math.min(r1, r2);
                break;
            case VALM:
                break;
            case NDIF:
                int ndif = Integer.parseInt(strarray[1]);
                break;
            case COL:
            case COLUMN:
                col = new column();
                columns.add(col);
                col.label = strarray[1];
                col.type = strarray[2].charAt(0);
                col.id = Integer.parseInt(strarray[5]);
                break;
            case PROJECT:
                ndset = Integer.parseInt(strarray[1]);
                try {
                    dset = (dataset) datasets.get(ndset - 1);
                } catch (IndexOutOfBoundsException e) {
                    dset = new dataset();
                    datasets.add(dset);
                }
                dset.project = strarray[2];
                break;
            case CRYSTAL:
                break;
            case DATASET:
                ndset = Integer.parseInt(strarray[1]);
                try {
                    dset = (dataset) datasets.get(ndset - 1);
                } catch (IndexOutOfBoundsException e) {
                    dset = new dataset();
                    datasets.add(dset);
                }
                dset.dataset = strarray[2];
                break;
            case DCELL:
                ndset = Integer.parseInt(strarray[1]);
                try {
                    dset = (dataset) datasets.get(ndset - 1);
                } catch (IndexOutOfBoundsException e) {
                    dset = new dataset();
                    datasets.add(dset);
                }
                dset.cell[0] = Double.parseDouble(strarray[2]);
                dset.cell[1] = Double.parseDouble(strarray[3]);
                dset.cell[2] = Double.parseDouble(strarray[4]);
                dset.cell[3] = Double.parseDouble(strarray[5]);
                dset.cell[4] = Double.parseDouble(strarray[6]);
                dset.cell[5] = Double.parseDouble(strarray[7]);
                break;
            case DWAVEL:
                ndset = Integer.parseInt(strarray[1]);
                try {
                    dset = (dataset) datasets.get(ndset - 1);
                } catch (IndexOutOfBoundsException e) {
                    dset = new dataset();
                    datasets.add(dset);
                }
                dset.lambda = Double.parseDouble(strarray[2]);
                break;
            case BATCH:
                break;
            case END:
                headerparsed = true;
                parsing = false;
                break;
            default:
                break;
        }

        return parsing;
    }

    private void parse_columns() {

        // TODO: allow user to set mtz strings to look for in properties
        int nc = 0;
        StringBuffer sb = new StringBuffer();
        for (Iterator i = columns.iterator(); i.hasNext(); nc++) {
            column c = (column) i.next();
            String label = c.label.trim();
            if (label.equalsIgnoreCase("H") && c.type == 'H') {
                h = nc;
            } else if (label.equalsIgnoreCase("K") && c.type == 'H') {
                k = nc;
            } else if (label.equalsIgnoreCase("L") && c.type == 'H') {
                l = nc;
            } else if ((label.equalsIgnoreCase("free")
                    || label.equalsIgnoreCase("freer")
                    || label.equalsIgnoreCase("freerflag")
                    || label.equalsIgnoreCase("rfree")
                    || label.equalsIgnoreCase("rfreeflag")
                    || label.equalsIgnoreCase("r-free-flags")
                    || label.equalsIgnoreCase("test"))
                    && c.type == 'I') {
                sb.append(String.format("Reading R Free column: \"%s\"\n", c.label));
                rfree = nc;
            } else if ((label.equalsIgnoreCase("f")
                    || label.equalsIgnoreCase("fp")
                    || label.equalsIgnoreCase("fo")
                    || label.equalsIgnoreCase("fobs"))
                    && c.type == 'F') {
                sb.append(String.format("Reading Fo column: \"%s\"\n", c.label));
                fo = nc;
            } else if ((label.equalsIgnoreCase("sigf")
                    || label.equalsIgnoreCase("sigfp")
                    || label.equalsIgnoreCase("sigfo")
                    || label.equalsIgnoreCase("sigfobs"))
                    && c.type == 'Q') {
                sb.append(String.format("Reading sigFo column: \"%s\"\n", c.label));
                sigfo = nc;
            }
        }
        if (logger.isLoggable(Level.INFO)) {
            logger.info(sb.toString());
        }
    }

    static void print_header(MTZFilter mfile) {
        StringBuffer sb = new StringBuffer();
        sb.append("title: " + mfile.title + "\n");
        sb.append("sg: " + mfile.sgname + " sgnum: " + mfile.sgnum + "\n");
        sb.append("res: " + mfile.reslow + " " + mfile.reshigh + "\n");
        sb.append("nrfl: " + mfile.nrfl + "\n");
        sb.append("ncol: " + mfile.ncol + "\n");

        int ndset = 1;
        for (Iterator i = mfile.datasets.iterator(); i.hasNext(); ndset++) {
            dataset d = (dataset) i.next();

            sb.append("  dset " + ndset + ": " + d.dataset + "\n");
            sb.append("  project " + ndset + ": " + d.project + "\n");
            sb.append("  wavelength " + ndset + ": " + d.lambda + "\n");
            sb.append("  cell " + ndset + ": "
                    + d.cell[0] + " " + d.cell[1] + " " + d.cell[2] + " "
                    + d.cell[3] + " " + d.cell[4] + " " + d.cell[5] + "\n");
            sb.append("\n");
        }
        if (logger.isLoggable(Level.INFO)) {
            logger.info(sb.toString());
        }
    }
}
