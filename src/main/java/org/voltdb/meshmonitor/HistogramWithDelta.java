package org.voltdb.meshmonitor;

import org.HdrHistogram.SynchronizedHistogram;

public class HistogramWithDelta {

    private final ConsoleLogger logger;
    private final String title;

    private final SynchronizedHistogram histogram;
    private final SynchronizedHistogram deltaHistogram;

    public HistogramWithDelta(ConsoleLogger logger, String title, SynchronizedHistogram histogram) {
        this.logger = logger;
        this.title = title;

        this.histogram = histogram.copy();
        this.deltaHistogram = histogram.copy();
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

    public SynchronizedHistogram getDeltaHistogram() {
        return deltaHistogram;
    }

    public void resetDeltaHistogram() {
        deltaHistogram.reset();
    }
}
