/* This file is part of VoltDB.
 * Copyright (C) 2023 Volt Active Data Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.voltdb.meshmonitor;

import java.net.InetSocketAddress;

import org.HdrHistogram.SynchronizedHistogram;

public class PrintingHistogram {

    private final ConsoleLogger logger;
    private final String title;

    private final SynchronizedHistogram histogram;
    private final SynchronizedHistogram deltaHistogram;

    public PrintingHistogram(ConsoleLogger logger, String title, SynchronizedHistogram histogram) {
        this.logger = logger;
        this.title = title;

        this.histogram = histogram.copy();
        this.deltaHistogram = histogram.copy();
    }

    public boolean hasOutliers(long minHiccupSize) {
        return deltaHistogram.getMaxValue() > minHiccupSize;
    }

    public void printResultsAndReset(InetSocketAddress remoteId) {
        logDeltaHistogram(remoteId, title);
        deltaHistogram.reset();
    }

    private void logDeltaHistogram(InetSocketAddress remoteId, String title) {
        logger.log(remoteId,
                "%-18s - Max: %4.1fms   Mean: %4.1fms   99: %4.1fms  99.9: %4.1fms  99.99: %4.1fms  99.999: %4.1fms",
                title,
                deltaHistogram.getMaxValue() / 1000.0,
                deltaHistogram.getMean() / 1000.0,
                deltaHistogram.getValueAtPercentile(99.0) / 1000.0,
                deltaHistogram.getValueAtPercentile(99.9) / 1000.0,
                deltaHistogram.getValueAtPercentile(99.99) / 1000.0,
                deltaHistogram.getValueAtPercentile(99.999) / 1000.0);
    }

    public void recordValueWithExpectedInterval(long value, long expectedIntervalBetweenValueSamples) {
        if (value > histogram.getHighestTrackableValue() || value < 0) {
            logger.log("ERROR: Record for %s histogram exceeds maximum tracked value %d", title, value);
        } else {
            histogram.recordValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);
            deltaHistogram.recordValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);
        }
    }

    public SynchronizedHistogram getCumulativeHistogram() {
        return histogram;
    }
}
