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

public abstract class BaseInetSocketAddressConverter implements CommandLine.ITypeConverter<InetSocketAddress> {

    protected abstract int getDefaultPort();
    protected abstract boolean requiresHostname();
    protected abstract boolean treatPlainValueAsPort();

    @Override
    public InetSocketAddress convert(String value) {
        value = value.trim();

        // start with null host and default port, to be replaced as we go
        String host;
        int port = getDefaultPort();

        // IPv6 must start with brackets
        if (value.startsWith("[")) {

            // and must have a close bracket
            int closeBracket = value.indexOf("]");
            if (closeBracket == -1) {
                throw new IllegalArgumentException("IPv6 address missing closing bracket");
            }

            // host is within the brackets
            host = value.substring(1, closeBracket);

            String remainder = value.substring(closeBracket + 1);

            if (remainder.isEmpty() || remainder.equals(":")) {
                // no port provided, keep the default port
            } else if (remainder.startsWith(":")) {
                port = Integer.parseInt(remainder.substring(1));
            } else {
                throw new IllegalArgumentException("Invalid format after IPv6 address");
            }

            validateHost(host);
            validatePort(port);
            if (host.isEmpty() && treatPlainValueAsPort()) {
                return new InetSocketAddress(port);
            } else {
                return new InetSocketAddress(host, port);
            }

        } else {

            int lastColon = value.lastIndexOf(":");

            if (lastColon == -1) {
                // there is no colon
                return handlePlainValue(value);
            }

            if (value.indexOf(":") != lastColon) {
                // There is more than one colon

                // Either it's invalid or it's an IPv6 address without brackets, which we require
                // otherwise there is ambiguity between the port vs. the last group of the address
                throw new IllegalArgumentException("Too many colons or IPv6 address missing brackets");
            }

            if (value.equals(":")) {
                // there is only just a colon, treat this the same as ""
                return handlePlainValue("");
            }

            if (lastColon == 0) {
                // no hostname given, but the colon indicates value should be the port
                if (requiresHostname()) {
                    throw new IllegalArgumentException("Hostname is required. Please provide a valid FQDN or IP address.");
                } else {
                    // skip over the colon and handle Plain value as a port
                    port = Integer.parseInt(value.substring(1));
                    validatePort(port);
                    return new InetSocketAddress(port);
                }
            }

            // there is one and only one colon
            host = value.substring(0, lastColon);
            String portString = value.substring(lastColon + 1);
            if (!portString.isEmpty()) {
                port = Integer.parseInt(portString);
            }
            validateHost(host);
            validatePort(port);
            return new InetSocketAddress(host, port);
        }
    }

    private InetSocketAddress handlePlainValue(String value) {
        if (value.isEmpty()) {
            if (requiresHostname()) {
                throw new IllegalArgumentException("Hostname is required. Please provide a valid FQDN or IP address.");
            } else {
                // use default port + wildcard
                return new InetSocketAddress(getDefaultPort());
            }
        } else if (treatPlainValueAsPort() && isValidPort(value)) {
            int port = Integer.parseInt(value);
            return new InetSocketAddress(port); // wildcard
        } else {
            validateHost(value);
            return new InetSocketAddress(value, getDefaultPort());
        }
    }

    private void validateHost(String host) {
        if (requiresHostname() && (host == null || host.isEmpty())) {
            throw new IllegalArgumentException("Hostname is required. Please provide a valid FQDN or IP address.");
        }
    }

    private void validatePort(int port) {
        if (!portInValidRange(port)) {
            throw new IllegalArgumentException("Port must be in range 1 - 65535");
        }
    }

    private boolean isValidPort(String input) {
        try {
            int port = Integer.parseInt(input);
            return portInValidRange(port);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean portInValidRange(int port) {
        return port >=1 && port <= 65535;
    }
}
