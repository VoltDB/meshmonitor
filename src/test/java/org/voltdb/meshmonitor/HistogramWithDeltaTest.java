package org.voltdb.meshmonitor;

import org.HdrHistogram.SynchronizedHistogram;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.voltdb.meshmonitor.ConsoleLoggerTest.loggerForTest;

class HistogramWithDeltaTest {

    @Test
    void shouldUpdateBothHistograms() {
        // Given
        SynchronizedHistogram histogram = new SynchronizedHistogram(1_000_000_000L, 3);
        HistogramWithDelta histogramWithDelta = new HistogramWithDelta(
                loggerForTest(),
                "test",
                histogram
        );

        long value = TimeUnit.MILLISECONDS.toNanos(1);

        // When
        histogramWithDelta.recordValueWithExpectedInterval(value, value);

        // Then
        assertThat(histogramWithDelta.getCumulativeHistogram().getCountAtValue(value)).isEqualTo(1);
        assertThat(histogramWithDelta.getDeltaHistogram().getCountAtValue(value)).isEqualTo(1);
    }

    @Test
    void shouldMakeACopyOfTheHistogram() {
        // Given
        SynchronizedHistogram histogram = new SynchronizedHistogram(1_000_000_000L, 3);
        HistogramWithDelta histogramWithDelta = new HistogramWithDelta(
                loggerForTest(),
                "test",
                histogram
        );

        long value = TimeUnit.MILLISECONDS.toNanos(1);

        // When
        histogramWithDelta.recordValueWithExpectedInterval(value, value);

        // Then
        assertThat(histogramWithDelta.getCumulativeHistogram().getCountAtValue(value)).isEqualTo(1);
        assertThat(histogramWithDelta.getDeltaHistogram().getCountAtValue(value)).isEqualTo(1);
        assertThat(histogram.getCountAtValue(value)).isZero();
    }

    @Test
    void shouldUseIndependentHistograms() {
        // Given
        SynchronizedHistogram histogram = new SynchronizedHistogram(1_000_000_000L, 3);
        HistogramWithDelta histogramWithDelta = new HistogramWithDelta(
                loggerForTest(),
                "test",
                histogram
        );

        long value = TimeUnit.MILLISECONDS.toNanos(1);

        // When
        histogramWithDelta.recordValueWithExpectedInterval(value, value);
        histogramWithDelta.getDeltaHistogram().reset();

        // Then
        assertThat(histogramWithDelta.getCumulativeHistogram().getCountAtValue(value)).isEqualTo(1);
        assertThat(histogramWithDelta.getDeltaHistogram().getCountAtValue(value)).isZero();
    }

    @Test
    void shouldLogErrorOnlyWhenValueExceedsMaximumTrackableValue() {
        // Given
        ConsoleLogger consoleLogger = mock(ConsoleLogger.class);

        SynchronizedHistogram histogram = new SynchronizedHistogram(1000L, 3);
        HistogramWithDelta histogramWithDelta = new HistogramWithDelta(
                consoleLogger,
                "test",
                histogram
        );

        long regularValue = 200L;
        long valueThatExceedsMaximum = 2000L;

        // When
        histogramWithDelta.recordValueWithExpectedInterval(regularValue, 100L);
        histogramWithDelta.recordValueWithExpectedInterval(valueThatExceedsMaximum, 100L);

        // Then
        verify(consoleLogger, times(1)).log(
                eq("ERROR: Record for %s histogram exceeds maximum tracked value %d"),
                eq("test"),
                eq(valueThatExceedsMaximum)
        );
    }
}
