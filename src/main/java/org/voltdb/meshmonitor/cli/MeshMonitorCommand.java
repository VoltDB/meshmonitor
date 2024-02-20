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
package org.voltdb.meshmonitor.cli;

import org.voltdb.meshmonitor.ConsoleLogger;
import org.voltdb.meshmonitor.MeshMonitor;
import org.voltdb.meshmonitor.ServerManager;
import org.voltdb.meshmonitor.metrics.SimplePrometheusMetricsServer;
import picocli.CommandLine;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "meshmonitor")
public class MeshMonitorCommand implements Callable<Integer> {

    @CommandLine.Option(names = { "-v", "--version" }, versionHelp = true, description = "Display version info")
    boolean versionInfoRequested;

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "Display help")
    boolean usageHelpRequested;

    @CommandLine.Option(names = { "-c", "--min-hiccup" }, description = "Mininum latency in milliseconds to report", defaultValue = "20")
    private int minHiccupSizeMilliseconds;

    @CommandLine.Option(names = { "-p", "--ping" }, description = "Ping interval in milliseconds", defaultValue = "5")
    private int pingIntervalMilliseconds;

    @CommandLine.Option(names = { "-i", "--interval" }, description = "Reporting interval in seconds", defaultValue = "10")
    private int reportIntervalSeconds;

    @CommandLine.Option(names = { "-b",
            "--bind" }, description = "Bind address in format ipv4[:port]", defaultValue = "127.0.0.1:12222", converter = InetSocketAddressConverter.class)
    private InetSocketAddress bindAddress;

    @CommandLine.Option(names = { "-m",
            "--metrics-bind" }, description = "Bind address of a metrics server port in format ipv4[:port]", defaultValue = "127.0.0.1:12223", converter = InetSocketAddressConverter.class)
    private InetSocketAddress metricsBindAddress;

    @CommandLine.Option(names = { "-d", "--disable-metrics" }, description = "Disable starting of Prometheus compatible metrics endpoint", defaultValue = "false")
    private boolean disableMetrics;

    @CommandLine.Option(names = { "-x", "--debug" }, description = "Enable debug logging", defaultValue = "false")
    private boolean enableDebugLogging;

    @CommandLine.Option(names = { "--host-name" }, description = "Host name to use when reporting metrics")
    private String hostName = getLocalHost();

    @CommandLine.Parameters(arity = "0..*", description = "List of servers to ping", converter = InetSocketAddressConverter.class)
    private List<InetSocketAddress> servers = new ArrayList<>();

    static String getLocalHost() {
        try {
            return Inet4Address.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new MeshMonitorCommand());
        if (commandLine.isUsageHelpRequested()) {
            commandLine.usage(System.out);
            return;
        }
        else if (commandLine.isVersionHelpRequested()) {
            commandLine.printVersionHelp(System.out);
            return;
        }

        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        System.out.println(
                CommandLine.Help.Ansi.AUTO.string(
                        """
                                @|green      __  ___          __                          _ __           \s
                                    /  |/  /__  _____/ /_  ____ ___  ____  ____  (_) /_____  _____
                                   / /|_/ / _ \\/ ___/ __ \\/ __ `__ \\/ __ \\/ __ \\/ / __/ __ \\/ ___/
                                  / /  / /  __(__  ) / / / / / / / / /_/ / / / / / /_/ /_/ / /   \s
                                 /_/  /_/\\___/____/_/ /_/_/ /_/ /_/\\____/_/ /_/_/\\__/\\____/_/ \s
                                 |@"""));

        ConsoleLogger consoleLogger = new ConsoleLogger(enableDebugLogging);
        ServerManager serverManager = new ServerManager(consoleLogger, Duration.ofMillis(pingIntervalMilliseconds));

        MeshMonitor meshMonitor = new MeshMonitor(
                consoleLogger,
                serverManager,
                bindAddress,
                servers,
                reportIntervalSeconds,
                minHiccupSizeMilliseconds);

        if (!disableMetrics) {
            SimplePrometheusMetricsServer server = new SimplePrometheusMetricsServer(
                    new ConsoleLogger(enableDebugLogging),
                    hostName,
                    metricsBindAddress,
                    serverManager);

            server.start();
        }

        return meshMonitor.start();
    }
}
