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
    private final MetricsInetSocketAddressConverter mconverter = new MetricsInetSocketAddressConverter();
    private final BaseInetSocketAddressConverter[] converters = {bconverter, mconverter};

    // The main differences:
    //    - Metrics can be port only (wildcard host)
    //    - Bind must have a hostname (it is advertised)


    // --------- TESTS THAT ARE IDENTICAL FOR BOTH CONVERTERS ----------
    @Test
    public void shouldThrowExceptionNoClosingBracket() {
        String input = "[1:2:3:4";

        for (BaseInetSocketAddressConverter c : converters) {
            assertThatThrownBy(() -> c.convert(input))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    public void shouldConvertWithPort() {

        String input = "localhost:8080";

        for (BaseInetSocketAddressConverter c : converters) {
            InetSocketAddress result = c.convert(input);

            assertThat(result).isNotNull();
            assertThat(result.getHostName()).isEqualTo("localhost");
            assertThat(result.getPort()).isEqualTo(8080);
        }
    }

    @Test
    public void shouldConvertWithoutPort() {

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
    }

    @Test
    public void shouldThrowExceptionWithInvalidPort() {

        String input = "localhost:abc";

        for (BaseInetSocketAddressConverter c : converters) {
            assertThatThrownBy(() -> c.convert(input))
                .isInstanceOf(NumberFormatException.class);
        }
    }

    // IPv6 with brackets
    //    empty brackets - host name empty
    //    nothing after ] - use default port
    //    ]: no port - use default port
    //    ]:port - use port
    // [1:2:3:4]12223 - missing colon should fail


    // --------- TESTS THAT ARE DIFFERENT FOR EACH CONVERTER ----------

    @Test
    public void onlyColonAndPortSpecified() throws UnknownHostException {

        String input = ":8080";

        // Bind converter requires hostname
        assertThatThrownBy(() -> bconverter.convert(input))
                .isInstanceOf(IllegalArgumentException.class);

        // Metrics converter accepts port only (uses wildcard)
        InetSocketAddress result = mconverter.convert(input);
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("0.0.0.0");
        assertThat(result.getPort()).isEqualTo(8080);
    }

    @Test
    public void handleOnlyColon() {

        String input = ":";

        // Metrics should use default port and wildcard
        InetSocketAddress result = mconverter.convert(input);
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("0.0.0.0");
        assertThat(result.getPort()).isEqualTo(mconverter.getDefaultPort());

        // Bind should throw exception - hostname is required
        assertThatThrownBy(() -> bconverter.convert(input))
                .isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    public void handleEmptyString() {

        String input = "";

        // Metrics should use default port and wildcard
        InetSocketAddress result = mconverter.convert(input);
        assertThat(result).isNotNull();
        assertThat(result.getHostName()).isEqualTo("0.0.0.0");
        assertThat(result.getPort()).isEqualTo(mconverter.getDefaultPort());

        // Bind should throw exception - hostname is required
        assertThatThrownBy(() -> bconverter.convert(input))
                .isInstanceOf(IllegalArgumentException.class);
    }

    //@Test
    public void shouldConvertIPv6() {

        String input = "[1:2:3:4]"; // no port
        input = "[1:2:3:4]:"; // colon but no port
        input = "[1:2:3:4]:1000"; // with port
        input = "1:2:3:4:1000"; // no brackets with port
        input = "1:2:3:4"; // no brackets no port
    }

}
