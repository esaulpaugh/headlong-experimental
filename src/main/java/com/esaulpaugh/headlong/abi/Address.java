/*
   Copyright 2021 Evan Saulpaugh

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.util.FastHex;
import com.joemelsha.crypto.hash.Keccak;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Provides type safety by disambiguating address arguments from {@link String} and {@link BigInteger} while imposing 
 * certain format requirements such as <a href="https://eips.ethereum.org/EIPS/eip-55">EIP-55: Mixed-case checksum address encoding</a>.
 */
public final class Address {

    private static final int PREFIX_LEN = 2;
    private static final int ADDRESS_DATA_BYTES = TypeFactory.ADDRESS_BIT_LEN / Byte.SIZE;
    private static final int ADDRESS_LEN_CHARS = PREFIX_LEN + ADDRESS_DATA_BYTES * FastHex.CHARS_PER_BYTE;
    private static final int HEX_RADIX = 16;

    private final BigInteger value;

    Address(BigInteger value) {
        this.value = value;
    }

    public BigInteger value() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Address) {
            Address other = (Address) o;
            return value.equals(other.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return toChecksumAddress(value);
    }

    public static Address wrap(final String checksumAddress) {
        validateChecksumAddress(checksumAddress);
        return new Address(new BigInteger(checksumAddress.substring(PREFIX_LEN), HEX_RADIX));
    }

    public static void validateChecksumAddress(final String checksumAddress) {
        if(toChecksumAddress(checksumAddress).equals(checksumAddress)) {
            return;
        }
        throw new IllegalArgumentException("invalid checksum");
    }

    public static String toChecksumAddress(final BigInteger address) {
        final String minimalHex = address.toString(HEX_RADIX);
        final int start = ADDRESS_LEN_CHARS - minimalHex.length();
        if(start < PREFIX_LEN) {
            throw new IllegalArgumentException("invalid bit length: " + address.bitLength());
        }
        final byte[] addressBytes = "0x0000000000000000000000000000000000000000".getBytes(StandardCharsets.US_ASCII);
        int i = start;
        do {
            addressBytes[i] = (byte) minimalHex.charAt(i - start);
        } while (++i < addressBytes.length);
        return doChecksum(addressBytes);
    }

    /**
     * @see <a href="https://github.com/ethereum/EIPs/blob/master/EIPS/eip-55.md#implementation">EIP-55</a>
     * @param address   the hexadecimal Ethereum address
     * @return  the same address with the correct EIP-55 checksum casing
     */
    public static String toChecksumAddress(final String address) {
        if(address.length() != ADDRESS_LEN_CHARS) {
            throw new IllegalArgumentException("expected address length " + ADDRESS_LEN_CHARS + "; actual is " + address.length());
        }
        if(address.charAt(0) != '0' || address.charAt(1) != 'x') {
            throw new IllegalArgumentException("missing 0x prefix");
        }
        final byte[] addressBytes = "0x0000000000000000000000000000000000000000".getBytes(StandardCharsets.US_ASCII);
        for (int i = PREFIX_LEN; i < addressBytes.length; i++) {
            addressBytes[i] = (byte) getLowercaseHex(address, i);
        }
        return doChecksum(addressBytes);
    }

    @SuppressWarnings("deprecation")
    private static String doChecksum(final byte[] addressBytes) {
        final Keccak keccak256 = new Keccak(256);
        keccak256.update(addressBytes, PREFIX_LEN, ADDRESS_DATA_BYTES * FastHex.CHARS_PER_BYTE);
        final int offset = PREFIX_LEN / FastHex.CHARS_PER_BYTE; // offset by one byte so the indices of the hex-encoded hash and the address ascii line up
        final byte[] buffer = new byte[offset + ADDRESS_DATA_BYTES];
        keccak256.digest(ByteBuffer.wrap(buffer, offset, ADDRESS_DATA_BYTES));
        final byte[] hash = FastHex.encodeToBytes(buffer);
        for (int i = PREFIX_LEN; i < addressBytes.length; i++) {
            final int c = addressBytes[i];
            switch (hash[i]) {
            case '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' -> addressBytes[i] = (byte) Character.toUpperCase(c);
            }
        }
        return new String(addressBytes, 0, 0, addressBytes.length);
    }

    private static int getLowercaseHex(final String address, final int i) {
        final int c = address.charAt(i);
        return switch (c) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' -> c;
            case 'A', 'B', 'C', 'D', 'E', 'F' -> c + ('a' - 'A');
            default -> throw new IllegalArgumentException("illegal hex val @ " + i);
        };
    }
}
