/*
   Copyright 2019 Evan Saulpaugh

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
package com.esaulpaugh.headlong.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/** Utility for encoding and decoding hexadecimal, Base64, and UTF-8-encoded {@link String}s. */
public final class Strings {

    private Strings() {}

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    public static final int HEX = 0; // 16
    public static final int UTF_8 = 1; // 256
    public static final int BASE_64_URL_SAFE = 2; // 64
    public static final int ASCII = 3; // 128

    public static final int URL_SAFE_FLAGS = FastBase64.URL_SAFE_CHARS | FastBase64.NO_LINE_SEP | FastBase64.NO_PADDING;

    public static String encode(byte b) {
        return encode(new byte[] { b });
    }

    public static String encode(ByteBuffer buffer) {
        return encode(buffer.array());
    }

    public static String encode(byte[] bytes) {
        return encode(bytes, HEX);
    }

    public static String encode(byte[] bytes, int encoding) {
        return encode(bytes, 0, bytes.length, encoding);
    }

    public static String encode(byte[] buffer, int from, int len, int encoding) {
        return switch (encoding) {
            case HEX -> FastHex.encodeToString(buffer, from, len);
            case UTF_8 -> new String(buffer, from, len, StandardCharsets.UTF_8);
            case BASE_64_URL_SAFE -> FastBase64.encodeToString(buffer, from, len, URL_SAFE_FLAGS);
            case ASCII -> new String(buffer, from, len, StandardCharsets.US_ASCII);
            default -> throw new UnsupportedOperationException();
        };
    }

    public static byte[] decode(String encoded) {
        return decode(encoded, HEX);
    }

    public static byte[] decode(String string, int encoding) {
        if(string.isEmpty()) {
            return EMPTY_BYTE_ARRAY;
        }
        return switch (encoding) {
            case HEX -> FastHex.decode(string, 0, string.length());
            case UTF_8 -> string.getBytes(StandardCharsets.UTF_8);
            case BASE_64_URL_SAFE -> java.util.Base64.getUrlDecoder().decode(string);
            case ASCII -> string.getBytes(StandardCharsets.US_ASCII);
            default -> throw new UnsupportedOperationException();
        };
    }
}
