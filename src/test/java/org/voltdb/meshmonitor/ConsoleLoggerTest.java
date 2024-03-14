/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
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
