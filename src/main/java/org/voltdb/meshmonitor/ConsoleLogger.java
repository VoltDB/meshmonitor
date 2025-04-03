/*
 * Copyright (C) 2024-2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor;

import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import picocli.CommandLine;

public class ConsoleLogger {

    private enum LogLevel {
        INFO("green"),
        WARN("yellow"),
        ERROR("red");

        private final String consoleColor;

        LogLevel(String consoleColor) {
            this.consoleColor = consoleColor;
        }

        @Override
        public String toString() {
            return consoleColor;
        }
    }

    private static final DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .toFormatter();

    public static final String EMPTY_HOST_ADDRESS = "";

    private final PrintWriter out;
    private final boolean enableDebugLogging;

    public ConsoleLogger(PrintWriter out, boolean enableDebugLogging) {
        this.out = out;
        this.enableDebugLogging = enableDebugLogging;
    }

    public void log(String message, Object... args) {
        log(EMPTY_HOST_ADDRESS, LogLevel.INFO, message, args);
    }

    public void log(SocketAddress socketAddress, String message, Object... args) {
        String hostAddress = ((InetSocketAddress) socketAddress).getAddress().getHostAddress();
        log(hostAddress, LogLevel.INFO, message, args);
    }

    private void log(String hostAddress, LogLevel logLevel, String message, Object... args) {
        String dateTime = TIME_FORMATTER.format(LocalDateTime.now());
        String colourfulDateTime = CommandLine.Help.Ansi.AUTO.string(String.format("@|%s %s|@", logLevel, dateTime));
        String colourfulHostAddress = CommandLine.Help.Ansi.AUTO.string(String.format("@|%s [%15s]|@", logLevel, hostAddress));

        out.println(colourfulDateTime + " " + colourfulHostAddress + " " + String.format(message, args));
    }

    public void debug(InetSocketAddress socketAddress, String format, Object... args) {
        String hostAddress = socketAddress.getAddress().getHostAddress();

        if (enableDebugLogging) {
            log(hostAddress, LogLevel.INFO, String.format(format, args));
        }
    }

    public void warn(InetSocketAddress socketAddress, String message, Object... args) {
        String hostAddress = socketAddress.getAddress().getHostAddress();
        log(hostAddress, LogLevel.WARN, message, args);
    }

    public void error(InetSocketAddress socketAddress, String message, Object... args) {
        String hostAddress = socketAddress.getAddress().getHostAddress();
        log(hostAddress, LogLevel.ERROR, message, args);
    }

    public void fatalError(String message, Exception e) {
        log(EMPTY_HOST_ADDRESS, LogLevel.ERROR, message + ". " + e.getMessage());
        if (enableDebugLogging) {
            e.printStackTrace(out);
        }
    }
}
