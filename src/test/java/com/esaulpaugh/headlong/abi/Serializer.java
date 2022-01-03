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

import com.esaulpaugh.headlong.util.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.math.BigInteger;

public class Serializer {

    public static JsonPrimitive serializeTypes(TupleType tupleType, Gson gson) {
        JsonArray array = new JsonArray();

        for(ABIType<?> type : tupleType.elementTypes) {
            array.add(new JsonPrimitive(type.canonicalType.replace("(", "tuple(")));
        }
        return new JsonPrimitive(gson.toJson(array));
    }

    public static JsonPrimitive serializeValues(Tuple tuple, Gson gson) {
        JsonArray valuesArray = new JsonArray();
        for(Object val : tuple) {
            valuesArray.add(toJsonElement(val));
        }
        return new JsonPrimitive(gson.toJson(valuesArray));
    }

    private static JsonElement toJsonElement(Object val) {
        if(val instanceof Boolean) {
            JsonObject object = new JsonObject();
            object.add("type", new JsonPrimitive("bool"));
            object.add("value", new JsonPrimitive(val.toString()));
            return object;
        } else if(val instanceof Integer || val instanceof Long) {
            JsonObject object = new JsonObject();
            object.add("type", new JsonPrimitive("number"));
            object.add("value", new JsonPrimitive(val.toString()));
            return object;
        } else if(val instanceof BigInteger integer) {
            JsonObject object = new JsonObject();
            object.add("type", new JsonPrimitive("string"));
            object.add("value", new JsonPrimitive("0x" + Strings.encode(integer.toByteArray())));
            return object;
        } else if(val instanceof BigDecimal decimal) {
            JsonObject object = new JsonObject();
            object.add("type", new JsonPrimitive("number"));
            object.add("value", new JsonPrimitive(decimal.unscaledValue().toString()));
            return object;
        } else if(val instanceof byte[] bytes) {
            JsonObject object = new JsonObject();
            object.add("type", new JsonPrimitive("buffer"));
            object.add("value", new JsonPrimitive("0x" + Strings.encode(bytes)));
            return object;
        } else if(val instanceof String str) {
            JsonObject object = new JsonObject();
            object.add("type", new JsonPrimitive("buffer"));
            object.add("value", new JsonPrimitive(str));
            return object;
        } else if(val instanceof Address) {
            JsonObject object = new JsonObject();
            object.add("type", new JsonPrimitive("string"));
            object.add("value", new JsonPrimitive(val.toString()));
            return object;
        } else if(val instanceof boolean[] bools) {
            JsonArray array = new JsonArray();
            for(boolean e : bools) {
                array.add(toJsonElement(e));
            }
            return array;
        } else if(val instanceof int[] ints) {
            JsonArray array = new JsonArray();
            for(int e : ints) {
                array.add(toJsonElement(e));
            }
            return array;
        } else if(val instanceof long[] longs) {
            JsonArray array = new JsonArray();
            for(long e : longs) {
                array.add(toJsonElement(e));
            }
            return array;
        } else if(val instanceof Object[] arr) {
            JsonArray array = new JsonArray();
            for(Object e : arr) {
                array.add(toJsonElement(e));
            }
            return array;
        } else if(val instanceof Tuple tuple) {
            JsonObject object = new JsonObject();
            object.add("type", new JsonPrimitive("tuple"));
            JsonArray array = new JsonArray();
            for(Object e : tuple) {
                array.add(toJsonElement(e));
            }
            object.add("value", array);
            return object;
        }
        throw new Error();
    }
}
