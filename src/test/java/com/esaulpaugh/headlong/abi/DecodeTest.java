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
package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.abi.util.Formatter;
import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.List;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static com.esaulpaugh.headlong.TestUtils.decodeHex;
import static com.esaulpaugh.headlong.TestUtils.removeWhitespace;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DecodeTest {

    private static final Function FUNCTION = new Function("gogo((fixed[],int8)[1][][5])", "(ufixed,string)");

    private static final byte[] RETURN_BYTES = decodeHex("""
                    0000000000000000000000000000000000000000000000000000000000000045
                    0000000000000000000000000000000000000000000000000000000000000040
                    0000000000000000000000000000000000000000000000000000000000000004
                    7730307400000000000000000000000000000000000000000000000000000000
                    """
    );

    private static final byte[] BAD_PADDING_A = decodeHex("""
                    0000000000000000000000000000000000000000000000000000000000000045
                    0000000000000000000000000000000000000000000000000000000000000040
                    0000000000000000000000000000000000000000000000000000000000000004
                    7730307480000000000000000000000000000000000000000000000000000000
                    """
    );

    private static final byte[] BAD_PADDING_B = decodeHex("""
                    0000000000000000000000000000000000000000000000000000000000000045
                    0000000000000000000000000000000000000000000000000000000000000040
                    0000000000000000000000000000000000000000000000000000000000000004
                    7730307400000000000000000000000200000000000000000000000000000000
                    """
    );

    private static final byte[] BAD_PADDING_C = decodeHex("""
                    0000000000000000000000000000000000000000000000000000000000000045
                    0000000000000000000000000000000000000000000000000000000000000040
                    0000000000000000000000000000000000000000000000000000000000000004
                    7730307400000000000000000000000000000000000000000000000000000001
                    """
    );

    private static final Tuple RETURN_VALS = Tuple.of(new BigDecimal(BigInteger.valueOf(69L), 18), "w00t");

    @Test
    public void testLenient() throws Throwable {
        final byte[] lenientBytes = decodeHex("""
                        0000000000000000000000000000000000000000000000000000000000000045
                        00000000000000000000000000000000000000000000000000000000000000a3
                        0000000000000000000000000000000000000000000000000000000000000000
                        0000000000000000000000000000000000000000000000000000000000000000
                        0000000000000000000000000000000000000000000000000000000000000000000000
                        0000000000000000000000000000000000000000000000000000000000000004
                        7730307400000000000000000000000000000000000000000000000000000000
                        """);

        assertEquals(RETURN_VALS, FUNCTION.decodeReturn(lenientBytes));
        assertEquals(RETURN_VALS, FUNCTION.decodeReturn(ByteBuffer.wrap(lenientBytes)));

        final byte[] tooSmallOffset = decodeHex("""
                        0000000000000000000000000000000000000000000000000000000000000045
                        000000000000000000000000000000000000000000000000000000000000003f
                        0000000000000000000000000000000000000000000000000000000000000004
                        7730307400000000000000000000000000000000000000000000000000000000
                        """);

        assertThrown(IllegalArgumentException.class, "illegal backwards jump: (0+63=63)<64", () -> FUNCTION.decodeReturn(tooSmallOffset));
    }

    @Test
    public void testUint() {
        Function foo = new Function("foo()", "(uint8)");
        int val = (int) (Math.pow(2, 8)) - 1;
        byte[] _byte = Strings.decode("00000000000000000000000000000000000000000000000000000000000000FF");

        assertEquals(val, foo.decodeReturn(_byte).get(0));
        int i = foo.decodeSingletonReturn(_byte);
        assertEquals(val, i);

        byte[] _int_ = Strings.decode("000000000000000000000000000000000000000000000000000000000000FFFF");
        assertEquals((int) (Math.pow(2, 16)) - 1, (int) new Function("()", "(uint16)").decodeSingletonReturn(_int_));

        byte[] _long = Strings.decode("00000000000000000000000000000000000000000000000000000000FFFFFFFF");
        assertEquals((long) (Math.pow(2, 32)) - 1, (long) new Function("()", "(uint32)").decodeSingletonReturn(_long));

        byte[] _160_ = Strings.decode("000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        assertEquals(new Address(BigInteger.valueOf(2L).pow(160).subtract(BigInteger.ONE)), new Function("()", "(address)").decodeSingletonReturn(_160_));

        Function foo2 = new Function("()", "(uint)");
        byte[] _big_ = Strings.decode("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        BigInteger expected = BigInteger.valueOf(2L).pow(256).subtract(BigInteger.ONE);
        BigInteger bi = ((UnitType<?>) foo2.getOutputs().get(0)).maxValue();
        assertEquals(expected, bi);
        assertEquals(expected, foo2.decodeReturn(_big_).get(0));
        BigInteger n = foo2.decodeSingletonReturn(_big_);
        assertEquals(expected, n);
    }

    @Test
    public void testDoSLimit() throws Throwable {
        final long _2_17 = (long) Math.pow(2, 17);
        final String hex17 = Long.toHexString(_2_17);
        final String hex17min1 = Strings.encode(Integers.toBytes(_2_17 - 1));
        System.out.println(hex17 + " " + hex17min1);

        final long _2_31 = (long) Math.pow(2.0, 31.0);
        final String hex31 = Long.toHexString(_2_31);
        final long _2_31min1 = _2_31 - 1;
        final String hex31min1 = Long.toHexString(_2_31min1);
        System.out.println(hex31 + " " + hex31min1);

        final String bigLength = removeWhitespace("""
                        0000000000000000000000000000000000000000000000000000000000000020
                        000000000000000000000000000000000000000000000000000000000001ffff
                        aa00000000000000000000000000000000000000000000000000000000000000
                        """);
        assertThrown(
                BufferUnderflowException.class,
                () -> Function.parse("()", "(bytes)").decodeReturn(Strings.decode(bigLength))
        );
        final String tooBigLength = removeWhitespace("""
                0000000000000000000000000000000000000000000000000000000000000020
                0000000000000000000000000000000000000000000000000000000000020000
                aa00000000000000000000000000000000000000000000000000000000000000
                """);
        assertThrown(
                IllegalArgumentException.class,
                "exceeds bit limit: 18 > 17",
                () -> Function.parse("()", "(bytes)").decodeReturn(Strings.decode(tooBigLength))
        );

        final String bigOffset = removeWhitespace("""
                000000000000000000000000000000000000000000000000000000007FFFFFFF
                0000000000000000000000000000000000000000000000000000000000000001
                aa00000000000000000000000000000000000000000000000000000000000000
                """);
        assertThrown(
                IllegalArgumentException.class,
                "newPosition > limit: (2147483647 > 96)",
                () -> Function.parse("()", "(bytes)").decodeReturn(Strings.decode(bigOffset))
        );
        final String tooBigOffset = removeWhitespace("""
                0000000000000000000000000000000000000000000000000000000080000000
                0000000000000000000000000000000000000000000000000000000000000001
                aa00000000000000000000000000000000000000000000000000000000000000
                """);
        assertThrown(
                IllegalArgumentException.class,
                "exceeds bit limit: 32 > 31",
                () -> Function.parse("()", "(bytes)").decodeReturn(Strings.decode(tooBigOffset))
        );

        final String valid = removeWhitespace("""
                        0000000000000000000000000000000000000000000000000000000000000020
                        0000000000000000000000000000000000000000000000000000000000000001
                        aa00000000000000000000000000000000000000000000000000000000000000
                        """);
        Function.parse("()", "(bytes)").decodeReturn(Strings.decode(valid));
    }

    @Test
    public void testDecode() throws Throwable {

        assertArrayEquals(RETURN_BYTES, FUNCTION.getOutputs().encode(RETURN_VALS).array());

        Tuple decoded = FUNCTION.decodeReturn(RETURN_BYTES);
        assertEquals(RETURN_VALS, decoded);

        decoded = FUNCTION.getOutputs().decode(RETURN_BYTES);
        assertEquals(RETURN_VALS, decoded);

        decoded = TupleType.parse(FUNCTION.getOutputs().toString()).decode(ByteBuffer.wrap(RETURN_BYTES));
        assertEquals(RETURN_VALS, decoded);

        TupleType tt = TupleType.parseElements("ufixed,string,");
        assertEquals(TupleType.parseElements("ufixed,string"), tt);
        decoded = tt.decode(ByteBuffer.wrap(RETURN_BYTES));
        assertEquals(RETURN_VALS, decoded);

        assertThrown(IllegalArgumentException.class, "malformed array: non-zero padding byte", () -> FUNCTION.decodeReturn(BAD_PADDING_A));
        assertThrown(IllegalArgumentException.class, "malformed array: non-zero padding byte", () -> FUNCTION.decodeReturn(BAD_PADDING_B));
        assertThrown(IllegalArgumentException.class, "malformed array: non-zero padding byte", () -> FUNCTION.decodeReturn(BAD_PADDING_C));
    }

    @Test
    public void testDynamicArrayEmptyTuples() {
        Tuple decoded = new Function("foo()", "(()[])").decodeReturn(
                decodeHex("""
                          0000000000000000000000000000000000000000000000000000000000000020
                          0000000000000000000000000000000000000000000000000000000000000002
                          """)
        );
        assertEquals(Tuple.singleton(new Tuple[] { Tuple.EMPTY, Tuple.EMPTY }), decoded);
    }

    @Test
    public void testBoolean() throws Throwable {
        TupleType tt = TupleType.parse("(bool)");

        String[] tooBig = new String[] {
                "0000000000000000000000000000000000000000000000000000000000000002",
                "8000000000000000000000000000000000000000000000000000000000000000",
        };
        String[] tooSmall = new String[] {
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF",
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE"
        };
        String[] justRight = new String[] {
                "0000000000000000000000000000000000000000000000000000000000000000",
                "0000000000000000000000000000000000000000000000000000000000000001"
        };

        TestUtils.assertThrown(IllegalArgumentException.class, "exceeds bit limit: 2 > 1", () -> tt.decode(Strings.decode(tooBig[0])));
        TestUtils.assertThrown(IllegalArgumentException.class, "signed value given for unsigned type", () -> tt.decode(Strings.decode(tooBig[1])));
        for (String hex : tooSmall) {
            TestUtils.assertThrown(IllegalArgumentException.class, "signed value given for unsigned type", () -> tt.decode(Strings.decode(hex)));
        }
        for (String hex : justRight) {
            tt.decode(Strings.decode(hex));
        }
    }

    @Test
    public void testCorruptBoolean() throws Throwable {
        final Function f = new Function("baz(uint32,bool)");
        final Tuple argsTuple = new Tuple(69L, true);
        final byte[] array = f.encodeCall(argsTuple).array();

        System.out.println(Formatter.formatCall(array));

        array[array.length - 1] = 0;
        System.out.println(Formatter.formatCall(array));
        assertNotEquals(argsTuple, f.decodeCall(array));

        array[array.length - 32] = (byte) 0x80;
        System.out.println(Formatter.formatCall(array));
        assertThrown(IllegalArgumentException.class, "signed value given for unsigned type", () -> f.decodeCall(array));

        for (int i = array.length - 32; i < array.length; i++) {
            array[i] = (byte) 0xFF;
        }
        array[array.length - 1] = (byte) 0xFE;
        System.out.println(Formatter.formatCall(array));
        assertThrown(IllegalArgumentException.class, "signed value given for unsigned type", () -> f.decodeCall(array));
    }

    @Test
    public void testCorruptBooleanArray() throws Throwable {
        final Function f = new Function("baz(bool[])");
        final Tuple argsTuple = new Tuple((Object) new boolean[] { true });
        final byte[] array = f.encodeCall(argsTuple).array();

//        System.out.println(Function.formatCall(array));

        array[array.length - 1] = 0;
        assertNotEquals(argsTuple, f.decodeCall(array));

        array[array.length - 1] = 2;
        assertThrown(IllegalArgumentException.class, "illegal boolean value @ 68", () -> f.decodeCall(array));

        array[array.length - 1] = -1;
        assertThrown(IllegalArgumentException.class, "illegal boolean value @ 68", () -> f.decodeCall(array));

        array[array.length - 32] = (byte) 0x80;

        assertThrown(IllegalArgumentException.class, "illegal boolean value @ 68", () -> f.decodeCall(array));

        for (int i = array.length - 32; i < array.length; i++) {
            array[i] = (byte) 0xFF;
        }
        array[array.length - 1] = (byte) 0xFE;

        assertThrown(IllegalArgumentException.class, "illegal boolean value @ 68", () -> f.decodeCall(array));
    }

    @Test
    public void testNameOverwrite() {
        BooleanType t = TypeFactory.create("bool", "moo");
        assertEquals("moo", t.getName());

        BooleanType q = TypeFactory.create("bool");
        assertEquals("moo", t.getName());
        assertNull(q.getName());

        BooleanType z = TypeFactory.create("bool", "jumbo");
        assertEquals("moo", t.getName());
        assertNull(q.getName());
        assertEquals("jumbo", z.getName());
    }

    @Test
    public void testStringArray() throws Throwable {
        final ArrayType<ArrayType<ByteType, String>, String[]> type = TypeFactory.create("string[]", "nam");
        final String[] array = new String[] { "Hello, world!", "world! Hello," };
        final ByteBuffer abi = ByteBuffer.wrap(
                decodeHex("""
                          0000000000000000000000000000000000000000000000000000000000000002
                          0000000000000000000000000000000000000000000000000000000000000040
                          0000000000000000000000000000000000000000000000000000000000000080
                          000000000000000000000000000000000000000000000000000000000000000d
                          48656c6c6f2c20776f726c642100000000000000000000000000000000000000
                          000000000000000000000000000000000000000000000000000000000000000d
                          776f726c64212048656c6c6f2c00000000000000000000000000000000000000
                          """)
        );

        assertArrayEquals(abi.array(), type.encode(array).array());

        {
            final ABIType<?>                  a = TypeFactory.create("string[]");
            final ABIType<Object>             b = TypeFactory.create("string[]");
            final ABIType<String[]>           c = TypeFactory.create("string[]");
            final ABIType<Object[]>           c_ = TypeFactory.create("string[]");
            assertThrown(
                    IllegalArgumentException.class,
                    "class mismatch: [Ljava.lang.Object; != [Ljava.lang.String; (string[] requires String[] but found Object[])",
                    () -> c_.encode(new Object[] { "" })
            );
            final ABIType<String[]>           d = TypeFactory.create("string[]");
            final ABIType<?>                  e = TypeFactory.create("string[]");
            final ABIType<? extends String[]> f = TypeFactory.create("string[]");

            final ArrayType<?, ?>        g = TypeFactory.create("string[]");
            final ArrayType<?, ?>        h = TypeFactory.create("string[]");
            final ArrayType<?, String[]> i = TypeFactory.create("string[]");
            final ArrayType<ArrayType<ByteType, String>, String[]> j = TypeFactory.create("string[]");
            final ABIType<? extends String[]> k = TypeFactory.create("string[]");

            final IntType l = TypeFactory.create("int16");
            final ArrayType<?, BigInteger[]> m = TypeFactory.create("int[]");
            final TupleType n = TypeFactory.create("(bool)");
            m.encode(new BigInteger[] {});

            TupleType.wrap(List.of(TypeFactory.create("int"), TypeFactory.create("bytes[7]")));
        }

        assertEquals("nam", type.getName());

        Object decoded0 = Function.parse("()", "(string[])").getOutputs().get(0).decode(abi, new byte[32]);
        assertArrayEquals(array, (Object[]) decoded0);

        assertArrayEquals(array, type.decode(abi.array()));
    }
}
