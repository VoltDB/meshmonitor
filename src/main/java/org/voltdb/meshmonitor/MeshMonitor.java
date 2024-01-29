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

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;
import org.voltdb.meshmonitor.kubernetes.PodWatcher;

public class MeshMonitor {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder()
                    .setUncaughtExceptionHandler((t, e) -> System.err.printf("[%s] %s", t.getName(), e))
                    .build()
    );

    private static final int MIN_HICCUP_SIZE_MILLISECONDS = 20;
    private static final int REPORT_INTERVAL_SECONDS = 10;

    public static void main(String[] args) throws IOException {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);

//        String currentNamespace = System.getenv("CURRENT_NAMESPACE");
//
//        try (PodWatcher podWatcher = new PodWatcher(currentNamespace, "kind=meshmonitor-daemon")) {
//            podWatcher.start();
//            acceptLoop();
//        }
    }

    static void acceptLoop() throws IOException {
        try (ServerSocketChannel ssc = ServerSocketChannel.open()) {
            InetAddress localHost = Inet4Address.getLocalHost();
            ssc.socket().bind(new InetSocketAddress(localHost, 22222));

            while (ssc.isOpen()) {
                SocketChannel sc = ssc.accept();
                scheduleNewMonitor(sc);
            }
        }
    }

    private static void scheduleNewMonitor(SocketChannel sc) {
        Monitor m = new Monitor(sc);
        SCHEDULER.scheduleAtFixedRate(
                () -> m.printResults(MIN_HICCUP_SIZE_MILLISECONDS),
                REPORT_INTERVAL_SECONDS,
                REPORT_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }
}
