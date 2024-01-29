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

import com.google.common.util.concurrent.Uninterruptibles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

public class Monitor {

    public static final Duration PING_INTERVAL = Duration.ofMillis(5);

    private final PrintingHistogram m_receiveHistogram = new PrintingHistogram("delta receive");
    private final PrintingHistogram m_sendHistogram = new PrintingHistogram("delta send");
    private final PrintingHistogram m_deltaHistogram = new PrintingHistogram("delta timestamp");

    private final SocketChannel m_sc;
    private boolean firstRun = true;

    public Monitor(SocketChannel sc) {
        m_sc = sc;

        new SendThread(sc).start();
        new ReceiveThread(sc).start();
    }

    public void printResults(int minHiccupSize) {
        boolean haveOutliers = false;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PrintStream ps = new PrintStream(baos)) {
            if (firstRun) {
                System.out.printf("%s %-22s %-33s connected to remote endpoint %s\n",
                        ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()),
                        m_sc.socket().getRemoteSocketAddress(),
                        m_sc.socket().getLocalSocketAddress(),
                        m_sc.socket().getRemoteSocketAddress()
                );
                firstRun = false;
            }

            haveOutliers |= m_receiveHistogram.printResults(ps, m_sc.socket(), minHiccupSize);
            m_receiveHistogram.reset();

            haveOutliers |= m_deltaHistogram.printResults(ps, m_sc.socket(), minHiccupSize);
            m_deltaHistogram.reset();

            haveOutliers |= m_sendHistogram.printResults(ps, m_sc.socket(), minHiccupSize);
            m_sendHistogram.reset();
        }

        if (haveOutliers) {
            System.out.print(baos.toString(StandardCharsets.UTF_8));
        } else {
            System.out.printf("%s %-22s %-33s Threshold not reached: %dms. Nothing to report.\n",
                    ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()),
                    m_sc.socket().getLocalSocketAddress(),
                    m_sc.socket().getRemoteSocketAddress(),
                    minHiccupSize
            );
        }
    }

    private void log(String message) {
        System.err.println(ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()) + " " + message);
    }

    private class ReceiveThread extends Thread {
        public ReceiveThread(SocketChannel sc) {
            super(sc.socket().getRemoteSocketAddress() + " receive thread");
        }

        @Override
        public void run() {
            try {
                long lastRecvTime = System.currentTimeMillis();
                while (true) {
                    long receivedTimestamp = receiveTimestamp();
                    long now = System.currentTimeMillis();

                    long valueToRecord = now - lastRecvTime;
                    if (valueToRecord > m_receiveHistogram.getHighestTrackableValue() || valueToRecord < 0) {
                        log("ERROR: Delta between receives was " + valueToRecord);
                    } else {
                        m_receiveHistogram.recordValueWithExpectedInterval(valueToRecord, 5);
                    }

                    lastRecvTime = now;
                    //Abs because clocks can be slightly out of sync...
                    valueToRecord = Math.abs(now - receivedTimestamp);
                    if (valueToRecord > m_deltaHistogram.getHighestTrackableValue()) {
                        log("ERROR: Delta between remote send time and recorded receive time was " + valueToRecord);
                    } else {
                        m_deltaHistogram.recordValueWithExpectedInterval(valueToRecord, 5);
                    }
                }
            } catch (Exception e) {
                log(e.getMessage());
                System.exit(-1);
            }
        }

        private long receiveTimestamp() throws IOException {
            ByteBuffer recvBuf = ByteBuffer.allocate(8);
            while (recvBuf.hasRemaining()) {
                m_sc.read(recvBuf);
            }
            recvBuf.flip();
            return recvBuf.getLong();
        }
    }

    private class SendThread extends Thread {
        public SendThread(SocketChannel sc) {
            super(sc.socket().getRemoteSocketAddress() + " send thread");
        }

        @Override
        public void run() {
            long lastRuntime = System.currentTimeMillis();
            while (true) {
                try {
                    Uninterruptibles.sleepUninterruptibly(PING_INTERVAL);

                    long now = System.currentTimeMillis();
                    long valueToRecord = now - lastRuntime;
                    if (valueToRecord > m_sendHistogram.getHighestTrackableValue() || valueToRecord < 0) {
                        log("ERROR: Delta betweens sends was " + valueToRecord);
                    } else {
                        m_sendHistogram.recordValueWithExpectedInterval(valueToRecord, 5);
                    }
                    lastRuntime = now;
                    sendTimestamp(now);
                } catch (Exception e) {
                    log(e.getMessage());
                    System.exit(-1);
                }
            }
        }

        private void sendTimestamp(long now) throws IOException {
            ByteBuffer sendBuf = ByteBuffer.allocate(8);
            sendBuf.putLong(now);
            sendBuf.flip();

            while (sendBuf.hasRemaining()) {
                m_sc.write(sendBuf);
            }
        }
    }
}
