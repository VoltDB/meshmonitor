/*
 * Copyright (C) 2024-2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.testutils;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class FileChannelUtilsTest {

    private byte[] createTestData(int size) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) (i % 128);
        }
        return data;
    }

    @Test
    public void testSimplePutHeap() {
        // Give
        byte[] srcData = createTestData(10);
        byte[] dstData = new byte[10];
        Arrays.fill(dstData, (byte) -1);

        ByteBuffer src = ByteBuffer.wrap(srcData);
        ByteBuffer dst = ByteBuffer.wrap(dstData);

        int srcPos = src.position();
        int dstPos = dst.position();

        // When
        // Copy 5 bytes from src[1..5] to dst[2..6]
        FileChannelUtils.byteBufferAbsolutePut(dst, 2, src, 1, 5);

        // Then
        byte[] expectedDst = {-1, -1, 1, 2, 3, 4, 5, -1, -1, -1};
        assertThat(expectedDst).containsExactly(dst.array());

        // Verify positions unchanged
        assertEquals(srcPos, src.position());
        assertEquals(dstPos, dst.position());
    }

    @Test
    public void testSimplePutDirect() {
        // Given
        byte[] srcData = createTestData(10);
        byte[] dstData = new byte[10];
        Arrays.fill(dstData, (byte) -1);

        ByteBuffer src = ByteBuffer.allocateDirect(10);
        src.put(srcData).flip();

        ByteBuffer dst = ByteBuffer.allocateDirect(10);
        dst.put(dstData).clear();

        // When
        // Copy 5 bytes from src[1..5] to dst[2..6]
        FileChannelUtils.byteBufferAbsolutePut(dst, 2, src, 1, 5);

        // Then
        byte[] actualDst = new byte[10];
        dst.get(actualDst);

        byte[] expectedDst = {-1, -1, 1, 2, 3, 4, 5, -1, -1, -1};
        assertThat(expectedDst).containsExactly(actualDst);
    }

    @Test
    public void testPutToSelfNoOverlap() {
        // Given
        byte[] data = createTestData(10);
        ByteBuffer buf = ByteBuffer.wrap(data);
        int pos = buf.position();

        // When
        // Copy bytes 0,1,2 (src offset=0, len=3) to indices 5,6,7 (dst index=5)
        FileChannelUtils.byteBufferAbsolutePut(buf, 5, buf, 0, 3);

        // Then
        byte[] expected = {0, 1, 2, 3, 4, 0, 1, 2, 8, 9};
        assertThat(expected).containsExactly(buf.array());
        assertEquals(pos, buf.position());
    }

    @Test
    public void testPutZeroLength() {
        // Given
        byte[] srcData = createTestData(5);
        byte[] dstData = new byte[5];
        byte[] originalDstData = Arrays.copyOf(dstData, dstData.length);

        ByteBuffer src = ByteBuffer.wrap(srcData);
        ByteBuffer dst = ByteBuffer.wrap(dstData);
        int srcPos = src.position();
        int dstPos = dst.position();

        // When
        FileChannelUtils.byteBufferAbsolutePut(dst, 1, src, 1, 0); // Length is 0

        // Then
        assertThat(originalDstData).containsExactly(dst.array()); // Destination unchanged
        assertEquals(srcPos, src.position());
        assertEquals(dstPos, dst.position());
    }

    @Test(expected = ReadOnlyBufferException.class)
    public void testPutToReadOnlyBuffer() {
        // Given
        byte[] srcData = createTestData(5);
        byte[] dstData = new byte[5];

        ByteBuffer src = ByteBuffer.wrap(srcData);
        ByteBuffer dst = ByteBuffer.wrap(dstData).asReadOnlyBuffer();

        // When & Then
        FileChannelUtils.byteBufferAbsolutePut(dst, 0, src, 0, 5);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testPutNegativeIndex() {
        // Given
        ByteBuffer src = ByteBuffer.allocate(5);
        ByteBuffer dst = ByteBuffer.allocate(5);

        // When & Then
        FileChannelUtils.byteBufferAbsolutePut(dst, -1, src, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testPutNegativeOffset() {
        // Given
        ByteBuffer src = ByteBuffer.allocate(5);
        ByteBuffer dst = ByteBuffer.allocate(5);

        // When & Then
        FileChannelUtils.byteBufferAbsolutePut(dst, 0, src, -1, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testPutNegativeLength() {
        // Given
        ByteBuffer src = ByteBuffer.allocate(5);
        ByteBuffer dst = ByteBuffer.allocate(5);

        // When & Then
        FileChannelUtils.byteBufferAbsolutePut(dst, 0, src, 0, -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testPutIndexOverflow() {
        // Given
        ByteBuffer src = ByteBuffer.allocate(5);
        ByteBuffer dst = ByteBuffer.allocate(5); // limit = 5

        // When & Then
        FileChannelUtils.byteBufferAbsolutePut(dst, 3, src, 0, 3); // index=3, length=3 -> needs dst[3..5], requires limit >= 6
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testPutOffsetOverflow() {
        // Given
        ByteBuffer src = ByteBuffer.allocate(5); // limit = 5
        ByteBuffer dst = ByteBuffer.allocate(5);

        // When & Then
        FileChannelUtils.byteBufferAbsolutePut(dst, 0, src, 3, 3); // offset=3, length=3 -> needs src[3..5], requires limit >= 6
    }
}
