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

public class InetSocketAddressConverter implements CommandLine.ITypeConverter<InetSocketAddress> {

    public static final int DEFAULT_PORT = 12222;

    @Override
    public InetSocketAddress convert(String value) {
        int port = DEFAULT_PORT;
        String host;

        int pos = value.lastIndexOf(':');
        if (pos >= 0) {
            // Host and port provided
            host = value.substring(0, pos);
            port = Integer.parseInt(value.substring(pos + 1));
        } else {
            // Only hostname provided, use default port
            host = value.trim();
        }

        if (host.isEmpty()) {
            throw new IllegalArgumentException("Hostname is required. Please provide a valid FQDN or IP address.");
        }

        // Remove IPv6 brackets if present
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }

        return new InetSocketAddress(host, port);
    }
}
