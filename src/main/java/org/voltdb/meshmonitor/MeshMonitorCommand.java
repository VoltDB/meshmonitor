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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import picocli.CommandLine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@CommandLine.Command(
        name = "meshmonitor"
)
public class MeshMonitorCommand implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-h", "--min-hiccup"},
            description = "Mininum latency in milliseconds to report",
            defaultValue = "20")
    private int minHiccupSizeMilliseconds;

    @CommandLine.Option(
            names = {"-i", "--interval"},
            description = "Reporting interval in seconds",
            defaultValue = "10")
    private int reportIntervalSeconds;

    @CommandLine.Option(
            names = {"-b", "--bind"},
            description = "Bind address in format ipv4:port",
            defaultValue = "127.0.0.1:12222",
            converter = InetSocketAddressConverter.class)
    private InetSocketAddress bindAddress;

    @CommandLine.Option(
            names = {"-m", "--metrics-bind"},
            description = "Bind address of a metrics server port in format ipv4:port",
            defaultValue = "127.0.0.1:12222",
            converter = InetSocketAddressConverter.class)
    private InetSocketAddress metricsBindAddress;

    @CommandLine.Option(
            names = {"-d", "--disable-metrics"},
            description = "Disable starting of Prometheus compatible metrics endpoint",
            defaultValue = "false")
    private boolean disableMetrics;

    @CommandLine.Parameters(
            arity = "1..*",
            description = "List of servers to ping",
            converter = InetSocketAddressConverter.class
    )
    private List<InetSocketAddress> servers = new ArrayList<>();

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder()
                    .setUncaughtExceptionHandler((t, e) -> System.err.printf("[%s] %s", t.getName(), e))
                    .build()
    );

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MeshMonitorCommand()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws IOException {
        System.out.println("Starting meshmonitor. Binding to address " + bindAddress);
        try (ServerSocketChannel ssc = ServerSocketChannel.open()) {
            ssc.socket().bind(bindAddress);

            for (InetSocketAddress connectAddress : servers) {
                System.out.println("Connecting to: " + connectAddress);
                SocketChannel sc = SocketChannel.open(connectAddress);
                scheduleNewMonitor(sc, minHiccupSizeMilliseconds, reportIntervalSeconds);
            }

            while (ssc.isOpen()) {
                SocketChannel sc = ssc.accept();
                System.out.println("Received connection from: " + sc.getRemoteAddress());
                scheduleNewMonitor(sc, minHiccupSizeMilliseconds, reportIntervalSeconds);
            }
        }

        return 0;
    }

    private static void scheduleNewMonitor(SocketChannel sc, int minHiccupSize, int reportInterval) {
        Monitor m = new Monitor(sc);
        SCHEDULER.scheduleAtFixedRate(
                () -> m.printResults(minHiccupSize),
                reportInterval,
                reportInterval,
                TimeUnit.SECONDS
        );
    }
}
