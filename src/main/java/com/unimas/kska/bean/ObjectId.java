package com.unimas.kska.bean;

import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;


public class ObjectId implements Comparable<ObjectId> {

    private static final AtomicInteger NEXT_COUNTER = new AtomicInteger(new SecureRandom().nextInt());

    private static final char[] HEX_CHARS = new char[]{
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private final int timestamp;
    private final int counter;

    public static ObjectId get() {
        return new ObjectId();
    }

    private ObjectId() {
        this.timestamp = (int) (System.currentTimeMillis() / 1000);
        this.counter = NEXT_COUNTER.getAndIncrement() & 0x00ffffff;
    }


    /**
     * Constructs a new instance from a hexadecimal string representation.
     *
     * @param hexString the string to convert
     * @throws IllegalArgumentException if the string is not a valid hex string representation of an ObjectId
     */
    public ObjectId(final String hexString) {
        if (!isValid(hexString))
            throw new IllegalArgumentException("invalid hexadecimal representation of an ObjectId: [" + hexString + "]");
        byte[] b = new byte[7];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) Integer.parseInt(hexString.substring(i * 2, i * 2 + 2), 16);
        }
        ByteBuffer buffer = ByteBuffer.wrap(b);
        // Note: Cannot use ByteBuffer.getInt because it depends on tbe buffer's byte order
        // and ObjectId's are always in big-endian order.
        timestamp = makeInt(buffer.get(), buffer.get(), buffer.get(), buffer.get());
        counter = makeInt((byte) 0, buffer.get(), buffer.get(), buffer.get());
    }

    private static boolean isValid(final String hexString) {
        if (hexString == null) {
            throw new IllegalArgumentException();
        }
        int len = hexString.length();
        if (len != 14) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            char c = hexString.charAt(i);
            if (c >= '0' && c <= '9') {
                continue;
            }
            if (c >= 'a' && c <= 'f') {
                continue;
            }
            if (c >= 'A' && c <= 'F') {
                continue;
            }
            return false;
        }
        return true;
    }

    public Date getCreateDate() {
        return new Date(timestamp * 1000L);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectId objectId = (ObjectId) o;
        if (counter != objectId.counter) return false;
        return timestamp == objectId.timestamp;
    }

    @Override
    public int hashCode() {
        int result = timestamp;
        result = 31 * result + counter;
        return result;
    }

    @Override
    public int compareTo(@NotNull final ObjectId other) {
        byte[] byteArray = toByteArray();
        byte[] otherByteArray = other.toByteArray();
        for (int i = 0; i < 12; i++) {
            if (byteArray[i] != otherByteArray[i]) {
                return ((byteArray[i] & 0xff) < (otherByteArray[i] & 0xff)) ? -1 : 1;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        char[] chars = new char[14];
        int i = 0;
        for (byte b : toByteArray()) {
            chars[i++] = HEX_CHARS[b >> 4 & 0xF];
            chars[i++] = HEX_CHARS[b & 0xF];
        }
        return new String(chars);
    }

    /**
     * Convert to a byte array.
     * Note that the numbers are stored in big-endian order.
     */
    private byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(7);
        buffer.put(int3(timestamp));
        buffer.put(int2(timestamp));
        buffer.put(int1(timestamp));
        buffer.put(int0(timestamp));
        buffer.put(int2(counter));
        buffer.put(int1(counter));
        buffer.put(int0(counter));
        return buffer.array();  // using .allocate ensures there is a backing array that can be returned
    }

    // Big-Endian helpers
    private static int makeInt(final byte b3, final byte b2, final byte b1, final byte b0) {
        return (((b3) << 24) |
                ((b2 & 0xff) << 16) |
                ((b1 & 0xff) << 8) |
                ((b0 & 0xff)));
    }

    private static byte int3(final int x) {
        return (byte) (x >> 24);
    }

    private static byte int2(final int x) {
        return (byte) (x >> 16);
    }

    private static byte int1(final int x) {
        return (byte) (x >> 8);
    }

    private static byte int0(final int x) {
        return (byte) (x);
    }

}
