package org.voltdb.meshmonitor;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ConsoleLoggerTest {

    public static ConsoleLogger loggerForTest() {
        return new ConsoleLogger(new PrintWriter(System.out), false);
    }

    public static ConsoleLogger loggerForTest(StringWriter target) {
        return new ConsoleLogger(new PrintWriter(target), false);
    }
}
