/*
   Copyright 2022 Evan Saulpaugh

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
package com.esaulpaugh.headlong.abi.util;

import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;

import java.util.function.IntFunction;

import static com.esaulpaugh.headlong.abi.Function.SELECTOR_LEN;
import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;

public class Formatter {

    private Formatter() {}

    private static final int LABEL_LEN = 6;
    private static final int LABEL_PADDED_LEN = LABEL_LEN + 3;

    public static final IntFunction<String> LABELS_NONE = (int row) -> "";
    public static final IntFunction<String> LABELS_ROW_NUMBERS = (int row) -> pad(0, Integer.toString(row));
    public static final IntFunction<String> LABELS_OFFSETS = (int row) -> {
        String unpadded = Integer.toHexString(row * UNIT_LENGTH_BYTES);
        return pad(LABEL_LEN - unpadded.length(), unpadded);
    };

    public static String formatCall(byte[] abiCall) {
        return formatCall(abiCall, LABELS_ROW_NUMBERS);
    }

    /**
     * Returns a human-friendly representation for a given ABI-encoded function call.
     *
     * @param buffer the buffer containing the ABI call
     * @param labeler code to generate the row label
     * @return the formatted string
     * @throws IllegalArgumentException if the input length mod 32 != 4
     */
    public static String formatCall(byte[] buffer, IntFunction<String> labeler) {
        Integers.checkIsMultiple(buffer.length - SELECTOR_LEN, UNIT_LENGTH_BYTES);
        return finishFormat(
                buffer,
                SELECTOR_LEN,
                buffer.length,
                labeler,
                new StringBuilder(labeler == LABELS_NONE ? "" : pad(0, "ID"))
                        .append(Strings.encode(buffer, 0, SELECTOR_LEN, Strings.HEX))
        );
    }

    public static String format(byte[] abi) {
        return format(abi, LABELS_OFFSETS);
    }

    public static String format(byte[] abi, IntFunction<String> labeler) {
        Integers.checkIsMultiple(abi.length, UNIT_LENGTH_BYTES);
        return finishFormat(abi, 0, abi.length, labeler, new StringBuilder());
    }

    private static String finishFormat(byte[] buffer, int offset, int end, IntFunction<String> labeler, StringBuilder sb) {
        int row = 0;
        while(offset < end) {
            if(offset > 0) {
                sb.append('\n');
            }
            sb.append(labeler.apply(row++))
                    .append(Strings.encode(buffer, offset, UNIT_LENGTH_BYTES, Strings.HEX));
            offset += UNIT_LENGTH_BYTES;
        }
        return sb.toString();
    }

    private static String pad(int leftPadding, String unpadded) {
        return " ".repeat(leftPadding) + unpadded +
                " ".repeat(LABEL_PADDED_LEN - (leftPadding + unpadded.length()));
    }
}
