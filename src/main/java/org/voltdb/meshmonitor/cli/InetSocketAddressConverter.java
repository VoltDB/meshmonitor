/*
 * Copyright (C) 2024 Volt Active Data Inc.
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

        int pos = value.lastIndexOf(':');
        if (pos >= 0) {
            port = Integer.parseInt(value.substring(pos + 1));
            return new InetSocketAddress(value.substring(0, pos), port);
        }

        return new InetSocketAddress(value, port);
    }
}
