/*
 * Copyright (C) 2024-2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.cli;

import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MetricsInetSocketAddressConverterTest {

    private final MetricsInetSocketAddressConverter converter = new MetricsInetSocketAddressConverter();

    @Test
    public void shouldConvertWithPort() {
        // Given
        String input = "localhost:8080";

        // When
        InetSocketAddress result = converter.convert(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("localhost");
        assertThat(result.getPort()).isEqualTo(8080);
    }

    @Test
    public void shouldConvertWithoutPort() {
        // Given
        String input = "localhost";

        // When
        InetSocketAddress result = converter.convert(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("localhost");
        assertThat(result.getPort()).isEqualTo(MetricsInetSocketAddressConverter.DEFAULT_PORT);
    }

    @Test
    public void shouldThrowExceptionWithInvalidPort() {
        // Given
        String input = "localhost:abc";

        // When & Then
        assertThatThrownBy(() -> converter.convert(input))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    public void emptyStringShouldDefaultToWildcard() {
        // Given
        String input = "";

        // When
        InetSocketAddress result = converter.convert(input);

        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("0.0.0.0");
        assertThat(result.getPort()).isEqualTo(MetricsInetSocketAddressConverter.DEFAULT_PORT);
    }

    @Test
    public void shouldThrowExceptionOnInputWithOnlyColon() {
        // Given
        String input = ":";

        // When & Then
        assertThatThrownBy(() -> converter.convert(input))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldDefaultToWildcardIfONlyColonAndPortIsSpecified() throws UnknownHostException {
        // Given
        String input = ":8080";

        // When
        InetSocketAddress result = converter.convert(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("0.0.0.0");
        assertThat(result.getPort()).isEqualTo(8080);
    }
}
