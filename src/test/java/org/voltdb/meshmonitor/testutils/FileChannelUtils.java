/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.testutils;

import java.nio.ByteBuffer;

public class FileChannelUtils {

    public static FakeWritableByteChannel newWritableChannel(boolean isSlowConnection) {
        return new FakeWritableByteChannel(isSlowConnection);
    }

    public static ByteBufferReadableByteChannel newReadableChannel(ByteBuffer data) {
        return new ByteBufferReadableByteChannel(data);
    }
}
