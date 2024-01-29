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

import org.HdrHistogram.Histogram;
import org.HdrHistogram.SynchronizedHistogram;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

class PrintingHistogram extends SynchronizedHistogram {

    public static final int NUMBER_OF_SIGNIFICANT_VALUE_DIGITS = 3;
    public static final int HIGHEST_TRACKABLE_VALUE = 24 * 60 * 60 * 1000;

    private final String title;

    public PrintingHistogram(String title) {
        super(HIGHEST_TRACKABLE_VALUE, NUMBER_OF_SIGNIFICANT_VALUE_DIGITS);
        this.title = title;
    }

    public boolean printResults(PrintStream ps, Socket socket, int minHiccupSize) {
        if (getMaxValue() > minHiccupSize) {
            logHistogram(ps, title, socket);

            return true;
        }

        return false;
    }

    private void logHistogram(PrintStream ps, String title, Socket socket) {
        String timeNow = ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
        ps.printf("%s %-22s %-33s  %s      - MaxLat: %4d Avg: %6.2f 99th-Pct: %3d %3d %3d %3d \n",
                timeNow,
                socket.getLocalSocketAddress(),
                socket.getRemoteSocketAddress(),
                title,
                getMaxValue(),
                getMean(),
                getValueAtPercentile(99.0),
                getValueAtPercentile(99.9),
                getValueAtPercentile(99.99),
                getValueAtPercentile(99.999)
        );
    }
}
