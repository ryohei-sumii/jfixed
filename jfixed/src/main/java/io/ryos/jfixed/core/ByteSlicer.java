package io.ryos.jfixed.core;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

/**
 * A class for cutting a string by a number of bytes.
 */
public class ByteSlicer {

    /**
     * cutting a string by a number of bytes
     * @param line string line
     * @param offset offset of value corresponding field
     * @param length length of value  corresponding field
     * @param charset charset of data
     * @return sliced string
     */
    public static String slice(String line, int offset, int length, Charset charset) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be non-negative : " + offset);
        }
        if (length < 0) {
            throw new IllegalArgumentException("length must be non-negative : " + length);
        }
        if (line == null) {
            throw new IllegalArgumentException("line must not be null");
        }

        // change string into byte-array
        byte[] bytes = line.getBytes(charset);

        // check range
        if (offset > bytes.length) {
            throw new IndexOutOfBoundsException(
                    String.format("offset %d exceeds line length %d : ",offset, bytes.length)
            );
        }
        if (offset + length > bytes.length) {
            throw new IndexOutOfBoundsException(
                    String.format("range [%d, %d] exceeds line length %d : ",offset, offset + length, bytes.length)
            );
        }

        // extracts the specified range of the byte array.
        byte[] slicedBytes = new byte[length];
        System.arraycopy(bytes, offset, slicedBytes, 0, length);

        // decode by CharsetDecoder
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);

        // change byte-array into string
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(slicedBytes);
            CharBuffer charBuffer = decoder.decode(byteBuffer);
            return charBuffer.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Failed to decode bytes at offset %d, length %d with charset %s",
                            offset, length, charset.name()), e
            );
        }
    }
}
