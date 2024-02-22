package org.voltdb.meshmonitor;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.SynchronizedHistogram;
import picocli.CommandLine;

import java.net.InetSocketAddress;

public class HistogramLogger {

    private final ConsoleLogger consoleLogger;

    public HistogramLogger(ConsoleLogger consoleLogger) {
        this.consoleLogger = consoleLogger;
    }

    public void printResults(Monitor monitor, long minHiccupSize) {
        MeshMonitorTimings currentTimings = monitor.getTimings();
        InetSocketAddress remoteId = monitor.getRemoteId();

        String receive = printAndReset(currentTimings.receiveHistogram(), minHiccupSize);
        String delta = printAndReset(currentTimings.deltaHistogram(), minHiccupSize);
        String send = printAndReset(currentTimings.sendHistogram(), minHiccupSize);

        consoleLogger.log(remoteId, receive + "|" + delta + "|" + send);
    }

    private String printAndReset(HistogramWithDelta histogram, long minHiccupSize) {
        SynchronizedHistogram deltaHistogram = histogram.getDeltaHistogram();
        String receive = format(deltaHistogram, minHiccupSize);
        deltaHistogram.reset();

        return receive;
    }

    private String format(Histogram deltaHistogram, long minHiccupSizeMicroseconds) {
        double minHiccupSize = minHiccupSizeMicroseconds / 1000.0;
        String max = getFormatWithColours(deltaHistogram.getMaxValue() / 1000.0, minHiccupSize);
        String mean = getFormatWithColours(deltaHistogram.getMean() / 1000.0, minHiccupSize);
        String p99 = getFormatWithColours(deltaHistogram.getValueAtPercentile(99.0) / 1000.0, minHiccupSize);
        String p999 = getFormatWithColours(deltaHistogram.getValueAtPercentile(99.9) / 1000.0, minHiccupSize);
        String p9999 = getFormatWithColours(deltaHistogram.getValueAtPercentile(99.99) / 1000.0, minHiccupSize);

        return max + " " + mean + " " + p99 + " " + p999 + " " + p9999;
    }

    private String getFormatWithColours(double value, double minHiccupSize) {
        if (value > 999.9) {
            String formatted = "%4.1fs".formatted(value / 1000.0);
            return CommandLine.Help.Ansi.ON.string("@|bold,red " + formatted + "|@");
        }

        String formatted = "%5.1f".formatted(value);
        if (value > minHiccupSize) {
            return CommandLine.Help.Ansi.ON.string("@|bold,yellow " + formatted + "|@");
        }

        return formatted;
    }

    public void printHeader() {
        String max = CommandLine.Help.Ansi.ON.string("@|bold,underline   Max|@");
        String mean = CommandLine.Help.Ansi.ON.string("@|bold,underline  Mean|@");
        String p99 = CommandLine.Help.Ansi.ON.string("@|bold,underline    99|@");
        String p999 = CommandLine.Help.Ansi.ON.string("@|bold,underline  99.9|@");
        String p9999 = CommandLine.Help.Ansi.ON.string("@|bold,underline 99.99|@");
        String singleHistogramHeader = max + " " + mean + " " + p99 + " " + p999 + " " + p9999;

        String receive = "----------ping-(ms)----------";
        String delta = "---------jitter-(ms)---------";
        String send = "----timestamp-diff-(ms)------";

        consoleLogger.log(receive + " " + delta + " " + send);
        consoleLogger.log(singleHistogramHeader + "|" + singleHistogramHeader + "|" + singleHistogramHeader);
    }
}
