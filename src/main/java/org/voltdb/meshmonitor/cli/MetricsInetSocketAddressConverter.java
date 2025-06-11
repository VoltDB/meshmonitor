/*
 * Copyright (C) 2024-2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.cli;

import java.net.InetSocketAddress;
import picocli.CommandLine;

public class MetricsInetSocketAddressConverter implements CommandLine.ITypeConverter<InetSocketAddress> {

    public static final int DEFAULT_PORT = 12223;

    @Override
    public InetSocketAddress convert(String value) {

        // Host:port format
        int pos = value.lastIndexOf(':');
        if (pos >= 0) {
            int port = Integer.parseInt(value.substring(pos + 1));
            String host = value.substring(0, pos);

            // If host is in IPv6 format, remove the brackets
            if (host.startsWith("[") && host.endsWith("]")) {
                host = host.substring(1, host.length()-1);
            }

            // No host given, just colon followed by port - bind to all interfaces (wildcard) same as port-only
            if (host.equals("")) {
                return new InetSocketAddress(port);
            }

            return new InetSocketAddress(host, port);
        }

        // port only - bind to all interfaces (wildcard)
        if (value.matches("^\\d+$")) { // digits only
            return new InetSocketAddress(Integer.parseInt(value));
        }

        // empty string - use wildcard and default port
        if (value.equals("")) {
            return new InetSocketAddress(DEFAULT_PORT);
        }

        // value is hostname only
        return new InetSocketAddress(value, DEFAULT_PORT);
    }
}
