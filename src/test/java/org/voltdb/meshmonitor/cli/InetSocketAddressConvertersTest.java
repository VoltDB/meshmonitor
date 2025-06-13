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

public class InetSocketAddressConvertersTest {

    // This test covers both custom converter classes:
    private final BindInetSocketAddressConverter bconverter = new BindInetSocketAddressConverter();
    // Metrics Converter:
    //    - can be port only (wildcard host)
    //    - default port is 12223

    private final MetricsInetSocketAddressConverter mconverter = new MetricsInetSocketAddressConverter();
    // Bind Converter:
    //    - must have a hostname (it is advertised)
    //    - defaults to port 12222

    private final BaseInetSocketAddressConverter[] converters = {bconverter, mconverter};

    private void testIllegalArgument(BaseInetSocketAddressConverter converter, String input) {
        assertThatThrownBy(() -> converter.convert(input))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // --------- TESTS THAT ARE IDENTICAL FOR BOTH CONVERTERS ----------
    @Test
    public void ipv6MissingOneBracket() {
        String input = "[1:2:3:4";

        for (BaseInetSocketAddressConverter c : converters) {
            testIllegalArgument(c, input);
        }

        input = "1:2:3:4]";

        for (BaseInetSocketAddressConverter c : converters) {
            testIllegalArgument(c, input);
        }
    }

    @Test
    public void ipv6NoBrackets() {
        String input = "1:2:3:4";

        for (BaseInetSocketAddressConverter c : converters) {
            testIllegalArgument(c, input);
        }

        input = "1:2:3:4:1000"; // no brackets with port

        for (BaseInetSocketAddressConverter c : converters) {
            testIllegalArgument(c, input);
        }
    }

    @Test
    public void ipv6NoColon() {
        String input = "[1:2:3:4]8080";

        for (BaseInetSocketAddressConverter c : converters) {
            testIllegalArgument(c, input);
        }
    }

    @Test
    public void hostnameWithPort() {

        String input = "localhost:8080";

        for (BaseInetSocketAddressConverter c : converters) {
            InetSocketAddress result = c.convert(input);

            assertThat(result).isNotNull();
            assertThat(result.getHostName()).isEqualTo("localhost");
            assertThat(result.getPort()).isEqualTo(8080);
        }

        input = "10.0.0.1:8080";

        for (BaseInetSocketAddressConverter c : converters) {
            InetSocketAddress result = c.convert(input);

            assertThat(result).isNotNull();
            assertThat(result.getHostName()).isEqualTo("10.0.0.1");
            assertThat(result.getPort()).isEqualTo(8080);
        }

        input = "[1:2:3:4]:8080";

        for (BaseInetSocketAddressConverter c : converters) {
            InetSocketAddress result = c.convert(input);

            assertThat(result).isNotNull();
            assertThat(result.getHostName()).isEqualTo("1:2:3:4");
            assertThat(result.getPort()).isEqualTo(8080);
        }
    }

    @Test
    public void hostnameOnly() {

        String input = "localhost";

        // Bind accepts localhost, adds default port
        InetSocketAddress result = bconverter.convert(input);
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("localhost");
        assertThat(result.getPort()).isEqualTo(bconverter.getDefaultPort());

        // Metrics accepts localhost, adds different default port
        result = mconverter.convert(input);
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("localhost");
        assertThat(result.getPort()).isEqualTo(mconverter.getDefaultPort());

        input = "10.0.0.1";

        // Bind accepts IPv4 host, adds default port
        result = bconverter.convert(input);
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo(input);
        assertThat(result.getPort()).isEqualTo(bconverter.getDefaultPort());

        // Metrics accepts IPv4 host, adds different default port
        result = mconverter.convert(input);
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo(input);
        assertThat(result.getPort()).isEqualTo(mconverter.getDefaultPort());

        input = "[1:2:3:4]";

        // Bind accepts IPv4 host, adds default port
        result = bconverter.convert(input);
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("1:2:3:4");
        assertThat(result.getPort()).isEqualTo(bconverter.getDefaultPort());

        // Metrics accepts IPv4 host, adds different default port
        result = mconverter.convert(input);
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("1:2:3:4");
        assertThat(result.getPort()).isEqualTo(mconverter.getDefaultPort());
    }

    @Test
    public void hostnameColonNoPort() { // should ignore the extra colon

        String input = "localhost:";

        // Bind accepts localhost, adds default port
        InetSocketAddress result = bconverter.convert(input);
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("localhost");
        assertThat(result.getPort()).isEqualTo(bconverter.getDefaultPort());

        // Metrics accepts localhost, adds different default port
        result = mconverter.convert(input);
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("localhost");
        assertThat(result.getPort()).isEqualTo(mconverter.getDefaultPort());

        input = "10.0.0.1:";

        // Bind accepts IPv4 host, adds default port
        result = bconverter.convert(input);
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("10.0.0.1");
        assertThat(result.getPort()).isEqualTo(bconverter.getDefaultPort());

        // Metrics accepts IPv4 host, adds different default port
        result = mconverter.convert(input);
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("10.0.0.1");
        assertThat(result.getPort()).isEqualTo(mconverter.getDefaultPort());

        input = "[1:2:3:4]:";

        // Bind accepts IPv4 host, adds default port
        result = bconverter.convert(input);
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("1:2:3:4");
        assertThat(result.getPort()).isEqualTo(bconverter.getDefaultPort());

        // Metrics accepts IPv4 host, adds different default port
        result = mconverter.convert(input);
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("1:2:3:4");
        assertThat(result.getPort()).isEqualTo(mconverter.getDefaultPort());
    }

    @Test
    public void invalidPort() {

        String input = "localhost:abc";

        for (BaseInetSocketAddressConverter c : converters) {
            assertThatThrownBy(() -> c.convert(input))
                .isInstanceOf(NumberFormatException.class);
        }
    }

    // --------- TESTS THAT ARE DIFFERENT FOR EACH CONVERTER ----------

    @Test
    public void colonPort() throws UnknownHostException {

        String input = ":8080";

        // Bind converter requires hostname
        testIllegalArgument(bconverter, input);

        // Metrics converter accepts port only (uses wildcard)
        InetSocketAddress result = mconverter.convert(input);
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("0.0.0.0");
        assertThat(result.getPort()).isEqualTo(8080);
    }

    @Test
    public void emptyBracketsColonPort() throws UnknownHostException {

        String input = "[]:8080";

        // Bind converter requires hostname
        testIllegalArgument(bconverter, input);

        // Metrics converter accepts port only (uses wildcard)
        InetSocketAddress result = mconverter.convert(input);
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("0.0.0.0");
        assertThat(result.getPort()).isEqualTo(8080);
    }

    @Test
    public void colonOnly() {

        String input = ":";

        // Metrics should use default port and wildcard
        InetSocketAddress result = mconverter.convert(input);
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("0.0.0.0");
        assertThat(result.getPort()).isEqualTo(mconverter.getDefaultPort());

        // Bind should throw exception - hostname is required
        testIllegalArgument(bconverter, input);
    }

    @Test
    public void emptyString() {

        String input = "";

        // Metrics should use default port and wildcard
        InetSocketAddress result = mconverter.convert(input);
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("0.0.0.0");
        assertThat(result.getPort()).isEqualTo(mconverter.getDefaultPort());

        // Bind should throw exception - hostname is required
        testIllegalArgument(bconverter, input);
    }
}
