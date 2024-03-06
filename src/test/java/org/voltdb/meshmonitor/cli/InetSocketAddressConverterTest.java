package org.voltdb.meshmonitor.cli;

import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InetSocketAddressConverterTest {

    private final InetSocketAddressConverter converter = new InetSocketAddressConverter();

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
        assertThat(result.getPort()).isEqualTo(InetSocketAddressConverter.DEFAULT_PORT);
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
    public void emptyStringShouldDefaultToLocalhost() {
        // Given
        String input = "";

        // When
        InetSocketAddress result = converter.convert(input);

        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("localhost");
        assertThat(result.getPort()).isEqualTo(InetSocketAddressConverter.DEFAULT_PORT);
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
    public void shouldDefaultToLocalhostIfONlyColonAndPortIsSpecified() throws UnknownHostException {
        // Given
        String input = ":8080";

        // When
        InetSocketAddress result = converter.convert(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("localhost");
        assertThat(result.getPort()).isEqualTo(8080);
    }
}
