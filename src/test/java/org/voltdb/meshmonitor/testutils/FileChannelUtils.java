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

    /**
     * It simulates put method that is available only since Java 16:
     * <p>
     * public ByteBuffer put(int index, ByteBuffer src, int offset, int length);
     */
    public static void byteBufferAbsolutePut(ByteBuffer dst, int index, ByteBuffer src, int offset, int length) {
        if ((index < 0) || (offset < 0) || (length < 0) ||
            (index > dst.limit() - length) ||
            (offset > src.limit() - length)) {
            throw new IndexOutOfBoundsException(
                    String.format("dst.limit()=%d, index=%d, src.limit()=%d, offset=%d, length=%d",
                            dst.limit(), index, src.limit(), offset, length)
            );
        }

        for (int i = offset, j = index; i < offset + length; i++, j++) {
            dst.put(j, src.get(i));
        }
    }
}
