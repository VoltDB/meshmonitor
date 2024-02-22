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

@CommandLine.Command(
        name = "meshmonitor",
        sortOptions = false,
        headerHeading = "Usage:%n%n",
        synopsisHeading = "%n",
        descriptionHeading = "%nDescription:%n%n",
        parameterListHeading = "%nParameters:%n",
        optionListHeading = "%nOptions:%n",
        header = "Detects network jitter and reports on it.",
        description = "Tool for monitoring network issues such as network delays " +
                      "and instability, mysterious timeouts, hangs, and scheduling " +
                      "problems that delay message passing. A common use for this tool " +
                      "is when sites are experiencing dead host timeouts without any " +
                      "obvious network event.")
public class MeshMonitorCommand implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-v", "--version"},
            versionHelp = true,
            description = "Display version info")
    boolean versionInfoRequested;

    @CommandLine.Option(
            names = {"-h", "--help"},
            usageHelp = true,
            description = "Display help")
    boolean usageHelpRequested;

    @CommandLine.Option(
            names = {"-t", "--threshold"},
            description = "When printing histograms latency values exceeding this threshold will be printed in yellow",
            defaultValue = "20")
    private int minHiccupSizeMilliseconds;

    @CommandLine.Option(
            names = {"-p", "--ping"},
            description = "Ping interval in milliseconds",
            defaultValue = "5")
    private int pingIntervalMilliseconds;

    @CommandLine.Option(
            names = {"-i", "--interval"},
            description = "Interval at which timing histograms will be printed to the console (in seconds)",
            defaultValue = "10")
    private int reportIntervalSeconds;

    @CommandLine.Option(
            names = {"-b", "--bind"},
            description = "Bind address in format ipv4[:port]",
            defaultValue = "127.0.0.1:12222",
            converter = InetSocketAddressConverter.class)
    private InetSocketAddress bindAddress;

    @CommandLine.Option(
            names = {"-q", "--quiet"},
            description = "Do not print histograms to console.")
    private boolean quiet;

    @CommandLine.Option(
            names = {"-m", "--metrics-bind"},
            description = "Bind address of a metrics server port in format ipv4[:port]",
            defaultValue = "127.0.0.1:12223",
            converter = InetSocketAddressConverter.class)
    private InetSocketAddress metricsBindAddress;

    @CommandLine.Option(
            names = {"-d", "--disable-metrics"},
            description = "Disable starting of Prometheus compatible metrics endpoint",
            defaultValue = "false")
    private boolean disableMetrics;

    @CommandLine.Option(
            names = {"-x", "--debug"},
            description = "Enable debug logging",
            defaultValue = "false")
    private boolean enableDebugLogging;

    @CommandLine.Parameters(arity = "0..*", description = "List of servers to ping", converter = InetSocketAddressConverter.class)
    private List<InetSocketAddress> servers = new ArrayList<>();

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    static String getLocalHost() {
        try {
            return Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new MeshMonitorCommand());
        if (commandLine.isUsageHelpRequested()) {
            commandLine.usage(System.out);
            return;
        } else if (commandLine.isVersionHelpRequested()) {
            commandLine.printVersionHelp(System.out);
            return;
        }

        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        validateOptions();

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
                    metricsBindAddress,
                    serverManager);

            server.start();
        }

        return meshMonitor.start(!quiet);
    }

    private void validateOptions() {
        if (reportIntervalSeconds < 1) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Invalid argument: Reporting interval must be greater than zero.\n");
        }

        if (minHiccupSizeMilliseconds < 1) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Invalid argument: Minimum latency to report should be greater than zero.\n");
        }
    }
}
