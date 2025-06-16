/*
 * Copyright (C) 2024-2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.cli;

public class MetricsInetSocketAddressConverter extends BaseInetSocketAddressConverter {

    @Override
    protected int getDefaultPort() {
        return 12223;
    }

    @Override
    protected boolean requiresHostname() {
        return false;
    }

    @Override
    protected boolean treatPlainValueAsPort() {
        return true;
    }
}
