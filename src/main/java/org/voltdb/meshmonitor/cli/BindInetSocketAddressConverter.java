/*
 * Copyright (C) 2024-2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.cli;

public class BindInetSocketAddressConverter extends BaseInetSocketAddressConverter {

    @Override
    protected int getDefaultPort() {
        return 12222;
    }

    @Override
    protected boolean requiresHostname() {
        return true;
    }

    @Override
    protected boolean treatPlainValueAsPort() {
        return false;
    }
}
