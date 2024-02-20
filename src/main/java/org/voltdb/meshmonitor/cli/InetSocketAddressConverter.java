/* This file is part of VoltDB.
 * Copyright (C) 2023 Volt Active Data Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.voltdb.meshmonitor.cli;

import java.net.InetSocketAddress;

import picocli.CommandLine;

public class InetSocketAddressConverter implements CommandLine.ITypeConverter<InetSocketAddress> {

    private static final int DEFAULT_PORT = 12222;

    @Override
    public InetSocketAddress convert(String value) {
        int port = DEFAULT_PORT;

        int pos = value.lastIndexOf(':');
        if (pos > 0) {
            port = Integer.parseInt(value.substring(pos + 1));
            return new InetSocketAddress(value.substring(0, pos), port);
        }

        return new InetSocketAddress(value, port);
    }
}
