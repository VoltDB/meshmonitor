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
