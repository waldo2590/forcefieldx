/**
 * Title: Force Field X.
 *
 * Description: Force Field X - Software for Molecular Biophysics.
 *
 * Copyright: Copyright (c) Michael J. Schnieders 2001-2013.
 *
 * This file is part of Force Field X.
 *
 * Force Field X is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * Force Field X is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Force Field X; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ffx.potential.nonbonded;

import java.util.logging.Logger;

import edu.rit.pj.IntegerSchedule;
import edu.rit.util.Range;

/**
 * A fixed schedule that load balances work chunks across threads.
 *
 * @author Michael J. Schnieders
 * @since 1.0
 *
 */
public class SpatialDensitySchedule extends IntegerSchedule {

    private static final Logger logger = Logger.getLogger(SpatialDensitySchedule.class.getName());
    private final int nAtoms;
    private final int atomsPerChunk[];
    private final double loadBalancePercentage;
    private int nThreads;
    private Range chunkRange;
    private boolean threadDone[];
    private Range ranges[];

    /**
     * <p>Constructor for SpatialDensitySchedule.</p>
     *
     * @param nThreads a int.
     * @param nAtoms a int.
     * @param atomsPerChunk an array of int.
     * @param loadBalancePercentage a double.
     */
    public SpatialDensitySchedule(int nThreads, int nAtoms,
            int atomsPerChunk[], double loadBalancePercentage) {
        this.nAtoms = nAtoms;
        this.atomsPerChunk = atomsPerChunk;
        this.nThreads = nThreads;

        threadDone = new boolean[nThreads];
        ranges = new Range[nThreads];

        if (loadBalancePercentage > 0.01 && loadBalancePercentage <= 1.0) {
            this.loadBalancePercentage = loadBalancePercentage;
        } else {
            this.loadBalancePercentage = 1.0;
        }
    }

    /**
     * {@inheritDoc}
     *
     * This is a fixed schedule.
     */
    @Override
    public boolean isFixedSchedule() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(int nThreads, Range chunkRange) {
        this.nThreads = nThreads;
        this.chunkRange = chunkRange;

        if (nThreads != threadDone.length) {
            threadDone = new boolean[nThreads];
        }
        for (int i = 0; i < nThreads; i++) {
            threadDone[i] = false;
        }

        if (nThreads != ranges.length) {
            ranges = new Range[nThreads];
        }

        defineRanges();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Range next(int threadID) {
        if (!threadDone[threadID]) {
            threadDone[threadID] = true;
            return ranges[threadID];
        }
        return null;
    }

    private void defineRanges() {
        int lb = chunkRange.lb();
        int ub = chunkRange.ub();
        int thread = 0;
        int start = 0;
        int total = 0;

        // Calculate the total number of atoms that will be place on the grid.
        for (int i=0; i<atomsPerChunk.length; i++) {
            total += atomsPerChunk[i];
        }
        int goal = (int) ((total * loadBalancePercentage) / nThreads);

        total = 0;
        for (int i = lb; i <= ub; i++) {
            int chunksLeft = ub - i + 1;
            int threadsLeft = nThreads - thread;
            // Count the number of atoms in each work chunk.
            total += atomsPerChunk[i];
            // Check if the load balancing goal has been reached.
            if (total > goal || chunksLeft <= threadsLeft) {
                int stop = i;
                // Define the range for this thread.
                Range current = ranges[thread];
                if (current == null
                        || current.lb() != start
                        || current.ub() != stop) {
                    ranges[thread] = new Range(start, stop);
                    //logger.info(String.format("Range for thread %d %s %d.", thread, ranges[thread], total));
                }

                // Initialization for the next thread.
                thread++;
                start = i + 1;
                total = 0;
                // The last thread gets the rest of the work chunks.
                if (thread == nThreads - 1) {
                    stop = ub;
                    current = ranges[thread];
                    if (current == null
                            || current.lb() != start
                            || current.ub() != stop) {
                        ranges[thread] = new Range(start, stop);
                        //logger.finest(String.format("Range for thread %d %s %d.", thread, ranges[thread], total));
                    }
                    break;
                }
            } else if (i == ub) {
                // The final range may not meet the goal.
                int stop = i;
                Range current = ranges[thread];
                if (current == null
                        || current.lb() != start
                        || current.ub() != stop) {
                    ranges[thread] = new Range(start, stop);
                    //logger.info(String.format("Range for thread %d %s %d.", thread, ranges[thread], total));
                }
            }
        }

        // No work for remaining threads.
        for (int i = thread + 1; i < nThreads; i++) {
            ranges[i] = null;
        }
    }
}
