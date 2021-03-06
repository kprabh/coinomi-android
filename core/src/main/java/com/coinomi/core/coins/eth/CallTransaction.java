/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package com.coinomi.core.coins.eth;

import static java.lang.String.format;


import com.coinomi.core.coins.eth.crypto.SHA3Helper;
import com.coinomi.core.coins.eth.util.ByteUtil;
//import static org.ethereum.solidity.SolidityType.IntType;
//import static org.ethereum.util.ByteUtil.longToBytesNoLeadZeroes;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.charset.Charset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

/**
 * Creates a contract function call transaction.
 * Serializes arguments according to the function ABI .
 *
 * Created by Anton Nashatyrev on 25.08.2015.
 */
public class CallTransaction {

    /*private final static ObjectMapper DEFAULT_MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);*/
    public static abstract class SolidityType {
        protected String name;

        public SolidityType(String name) {
            this.name = name;
        }

        /**
         * The type name as it was specified in the interface description
         */
        public String getName() {
            return name;
        }

        /**
         * The canonical type name (used for the method signature creation)
         * E.g. 'int' - canonical 'int256'
         */

        public String getCanonicalName() {
            return getName();
        }

        public static SolidityType getType(String typeName) {
            if (typeName.contains("[")) {
                return ArrayType.getType(typeName);
            }
            if ("bool".equals(typeName)) {
                return new BoolType();
            }
            if (typeName.startsWith("int") || typeName.startsWith("uint")) {
                return new IntType(typeName);
            }
            if ("address".equals(typeName)) {
                return new AddressType();
            }
            if ("string".equals(typeName)) {
                return new StringType();
            }
            if ("bytes".equals(typeName)) {
                return new BytesType();
            }
            if (typeName.startsWith("bytes")) {
                return new Bytes32Type(typeName);
            }
            throw new RuntimeException("Unknown type: " + typeName);
        }

        /**
         * Encodes the value according to specific type rules
         *
         * @param value
         */
        public abstract byte[] encode(Object value);

        public abstract Object decode(byte[] encoded, int offset);

        public Object decode(byte[] encoded) {
            return decode(encoded, 0);
        }

        /**
         * @return fixed size in bytes. For the dynamic types returns IntType.getFixedSize()
         * which is effectively the int offset to dynamic data
         */
        public int getFixedSize() {
            return 32;
        }

        public boolean isDynamicType() {
            return false;
        }

        @Override
        public String toString() {
            return getName();
        }


        public abstract static class ArrayType extends SolidityType {
            public static ArrayType getType(String typeName) {
                int idx1 = typeName.indexOf("[");
                int idx2 = typeName.indexOf("]", idx1);
                if (idx1 + 1 == idx2) {
                    return new DynamicArrayType(typeName);
                } else {
                    return new StaticArrayType(typeName);
                }
            }

            SolidityType elementType;

            public ArrayType(String name) {
                super(name);
                int idx = name.indexOf("[");
                String st = name.substring(0, idx);
                int idx2 = name.indexOf("]", idx);
                String subDim = idx2 + 1 == name.length() ? "" : name.substring(idx2 + 1);
                elementType = SolidityType.getType(st + subDim);
            }

            @Override
            public byte[] encode(Object value) {
                if (value.getClass().isArray()) {
                    List<Object> elems = new ArrayList<>();
                    for (int i = 0; i < Array.getLength(value); i++) {
                        elems.add(Array.get(value, i));
                    }
                    return encodeList(elems);
                } else if (value instanceof List) {
                    return encodeList((List) value);
                } else {
                    throw new RuntimeException("List value expected for type " + getName());
                }
            }

            public abstract byte[] encodeList(List l);
        }

        public static class StaticArrayType extends ArrayType {
            int size;

            public StaticArrayType(String name) {
                super(name);
                int idx1 = name.indexOf("[");
                int idx2 = name.indexOf("]", idx1);
                String dim = name.substring(idx1 + 1, idx2);
                size = Integer.parseInt(dim);
            }

            @Override
            public String getCanonicalName() {
                return elementType.getCanonicalName() + "[" + size + "]";
            }

            @Override
            public byte[] encodeList(List l) {
                if (l.size() != size)
                    throw new RuntimeException("List size (" + l.size() + ") != " + size + " for type " + getName());
                byte[][] elems = new byte[size][];
                for (int i = 0; i < l.size(); i++) {
                    elems[i] = elementType.encode(l.get(i));
                }
                return ByteUtil.merge(elems);
            }

            @Override
            public Object[] decode(byte[] encoded, int offset) {
                Object[] result = new Object[size];
                for (int i = 0; i < size; i++) {
                    result[i] = elementType.decode(encoded, offset + i * elementType.getFixedSize());
                }

                return result;
            }

            @Override
            public int getFixedSize() {
                // return negative if elementType is dynamic
                return elementType.getFixedSize() * size;
            }
        }

        public static class DynamicArrayType extends ArrayType {
            public DynamicArrayType(String name) {
                super(name);
            }

            @Override
            public String getCanonicalName() {
                return elementType.getCanonicalName() + "[]";
            }

            @Override
            public byte[] encodeList(List l) {
                byte[][] elems;
                if (elementType.isDynamicType()) {
                    elems = new byte[l.size() * 2 + 1][];
                    elems[0] = IntType.encodeInt(l.size());
                    int offset = l.size() * 32;
                    for (int i = 0; i < l.size(); i++) {
                        elems[i + 1] = IntType.encodeInt(offset);
                        byte[] encoded = elementType.encode(l.get(i));
                        elems[l.size() + i + 1] = encoded;
                        offset += 32 * ((encoded.length - 1) / 32 + 1);
                    }
                } else {
                    elems = new byte[l.size() + 1][];
                    elems[0] = IntType.encodeInt(l.size());

                    for (int i = 0; i < l.size(); i++) {
                        elems[i + 1] = elementType.encode(l.get(i));
                    }
                }
                return ByteUtil.merge(elems);
            }

            @Override
            public Object decode(byte[] encoded, int origOffset) {
                int len = IntType.decodeInt(encoded, origOffset).intValue();
                origOffset += 32;
                int offset = origOffset;
                Object[] ret = new Object[len];

                for (int i = 0; i < len; i++) {
                    if (elementType.isDynamicType()) {
                        ret[i] = elementType.decode(encoded, origOffset + IntType.decodeInt(encoded, offset).intValue());
                    } else {
                        ret[i] = elementType.decode(encoded, offset);
                    }
                    offset += elementType.getFixedSize();
                }
                return ret;
            }

            @Override
            public boolean isDynamicType() {
                return true;
            }
        }

        public static class BytesType extends SolidityType {
            protected BytesType(String name) {
                super(name);
            }

            public BytesType() {
                super("bytes");
            }

            @Override
            public byte[] encode(Object value) {
                if (!(value instanceof byte[])) {
                    throw new RuntimeException("byte[] value expected for type 'bytes'");
                }
                byte[] bb = (byte[]) value;
                byte[] ret = new byte[((bb.length - 1) / 32 + 1) * 32]; // padding 32 bytes
                System.arraycopy(bb, 0, ret, 0, bb.length);

                return ByteUtil.merge(IntType.encodeInt(bb.length), ret);
            }

            @Override
            public Object decode(byte[] encoded, int offset) {
                int len = IntType.decodeInt(encoded, offset).intValue();
                offset += 32;
                return Arrays.copyOfRange(encoded, offset, offset + len);
            }

            @Override
            public boolean isDynamicType() {
                return true;
            }
        }

        public static class StringType extends BytesType {
            public StringType() {
                super("string");
            }

            @Override
            public byte[] encode(Object value) {
                if (!(value instanceof String)) {
                    throw new RuntimeException("String value expected for type 'string'");
                }
                return super.encode(((String) value).getBytes(Charset.forName("UTF-8")));
            }

            @Override
            public Object decode(byte[] encoded, int offset) {
                return new String((byte[]) super.decode(encoded, offset), Charset.forName("UTF-8"));
            }
        }

        public static class Bytes32Type extends SolidityType {
            public Bytes32Type(String s) {
                super(s);
            }

            @Override
            public byte[] encode(Object value) {
                if (value instanceof Number) {
                    BigInteger bigInt = new BigInteger(value.toString());
                    return IntType.encodeInt(bigInt);
                } else if (value instanceof String) {
                    byte[] ret = new byte[32];
                    byte[] bytes = ((String) value).getBytes(Charset.forName("utf-8"));
                    System.arraycopy(bytes, 0, ret, 0, bytes.length);
                    return ret;
                }

                return new byte[0];
            }

            @Override
            public Object decode(byte[] encoded, int offset) {
                return Arrays.copyOfRange(encoded, offset, getFixedSize());
            }
        }

        public static class AddressType extends IntType {
            public AddressType() {
                super("address");
            }

            @Override
            public byte[] encode(Object value) {
                if (value instanceof String && !((String) value).startsWith("0x")) {
                    // address is supposed to be always in hex
                    value = "0x" + value;
                }
                byte[] addr = super.encode(value);
                for (int i = 0; i < 12; i++) {
                    if (addr[i] != 0) {
                        throw new RuntimeException("Invalid address (should be 20 bytes length): " + Hex.toHexString(addr));
                    }
                }
                return addr;
            }

        }

        public static class IntType extends SolidityType {
            public IntType(String name) {
                super(name);
            }

            @Override
            public String getCanonicalName() {
                if (getName().equals("int")) {
                    return "int256";
                }
                if (getName().equals("uint")) {
                    return "uint256";
                }
                return super.getCanonicalName();
            }

            @Override
            public byte[] encode(Object value) {
                BigInteger bigInt;

                if (value instanceof String) {
                    String s = ((String) value).toLowerCase().trim();
                    int radix = 10;
                    if (s.startsWith("0x")) {
                        s = s.substring(2);
                        radix = 16;
                    } else if (s.contains("a") || s.contains("b") || s.contains("c") ||
                            s.contains("d") || s.contains("e") || s.contains("f")) {
                        radix = 16;
                    }
                    bigInt = new BigInteger(s, radix);
                } else if (value instanceof BigInteger) {
                    bigInt = (BigInteger) value;
                } else if (value instanceof Number) {
                    bigInt = new BigInteger(value.toString());
                } else {
                    throw new RuntimeException("Invalid value for type '" + this + "': " + value + " (" + value.getClass() + ")");
                }
                return encodeInt(bigInt);
            }

            @Override
            public Object decode(byte[] encoded, int offset) {
                return decodeInt(encoded, offset);
            }

            public static BigInteger decodeInt(byte[] encoded, int offset) {
                return new BigInteger(Arrays.copyOfRange(encoded, offset, offset + 32));
            }

            public static byte[] encodeInt(int i) {
                return encodeInt(new BigInteger("" + i));
            }

            public static byte[] encodeInt(BigInteger bigInt) {
                byte[] ret = new byte[32];
                Arrays.fill(ret, bigInt.signum() < 0 ? (byte) 0xFF : 0);
                byte[] bytes = bigInt.toByteArray();
                System.arraycopy(bytes, 0, ret, 32 - bytes.length, bytes.length);
                return ret;
            }
        }

        public static class BoolType extends IntType {
            public BoolType() {
                super("bool");
            }

            @Override
            public byte[] encode(Object value) {
                if (!(value instanceof Boolean)) {
                    throw new RuntimeException("Wrong value for bool type: " + value);
                }
                    if (((String) value).equalsIgnoreCase("true") || ((String) value).equalsIgnoreCase("1")) {
                        return super.encode(Integer.valueOf(1));
                    }
                    if (((String) value).equalsIgnoreCase("false") || ((String) value).equalsIgnoreCase("0")) {
                        return super.encode(Integer.valueOf(0));
                    }
                return super.encode(value == Boolean.TRUE ? 1 : 0);
            }

            @Override
            public Object decode(byte[] encoded, int offset) {
                return Boolean.valueOf(((Number) super.decode(encoded, offset)).intValue() != 0);
            }
        }

    }
//TODO
    public static  Transaction createRawTransaction(long nonce, long gasPrice, long gasLimit, String toAddress,
                                                    long value, byte[] data) {
        Transaction tx = new Transaction(ByteUtil.longToBytesNoLeadZeroes(nonce),
                ByteUtil.longToBytesNoLeadZeroes(gasPrice),
                ByteUtil.longToBytesNoLeadZeroes(gasLimit),
                toAddress == null ? null : Hex.decode(toAddress),
                ByteUtil.longToBytesNoLeadZeroes(value),
                data);
        return tx;
    }


    public static  Transaction createCallTransaction(long nonce, long gasPrice, long gasLimit, String toAddress,
                                                     long value, Function callFunc, Object ... funcArgs) {

        byte[] callData = callFunc.encode(funcArgs);
        return createRawTransaction(nonce, gasPrice, gasLimit, toAddress, value, callData);
    }



    public static class Param {
        public Boolean indexed;
        public String name;
        public SolidityType type;


        public  String getType() {
            return type.getName();
        }
    }

    public enum FunctionType {
        constructor,
        function,
        event,
        fallback
    }

    public static class Function {
        public boolean anonymous;
        public boolean constant;
        public boolean payable;
        public String name = "";
        public Param[] inputs = new Param[0];
        public Param[] outputs = new Param[0];
        public FunctionType type;

        private Function() {}

        public  byte[] encode(Object ... args) {
            return ByteUtil.merge(encodeSignature(), encodeArguments(args));
        }
        public  byte[] encodeArguments(Object ... args) {
            if (args.length > inputs.length) throw new RuntimeException("Too many arguments: " + args.length + " > " + inputs.length);

            int staticSize = 0;
            int dynamicCnt = 0;
            // calculating static size and number of dynamic params
            for (int i = 0; i < args.length; i++) {
                Param param = inputs[i];
                if (param.type.isDynamicType()) {
                    dynamicCnt++;
                }
                staticSize += param.type.getFixedSize();
            }

            byte[][] bb = new byte[args.length + dynamicCnt][];

            int curDynamicPtr = staticSize;
            int curDynamicCnt = 0;
            for (int i = 0; i < args.length; i++) {
                if (inputs[i].type.isDynamicType()) {
                    byte[] dynBB = inputs[i].type.encode(args[i]);
                    bb[i] = SolidityType.IntType.encodeInt(curDynamicPtr);
                    bb[args.length + curDynamicCnt] = dynBB;
                    curDynamicCnt++;
                    curDynamicPtr += dynBB.length;
                } else {
                    bb[i] = inputs[i].type.encode(args[i]);
                }
            }
            return ByteUtil.merge(bb);
        }

        public JSONObject resultToJSON(String result, Param[] params) {
            byte[] encoded = Hex.decode(result.replace("0x", ""));
            JSONObject obj = new JSONObject();
            int off = 0;
            for (int i = 0; i < params.length; i++) {
                try {
                    if (params[i].type.isDynamicType()) {
                        obj.put(params[i].name, params[i].type.decode(encoded, SolidityType.IntType.decodeInt(encoded, off).intValue()));
                    } else if (params[i].type.getName().equalsIgnoreCase("address")) {
                        obj.put(params[i].name, Hex.toHexString(((BigInteger) params[i].type.decode(encoded, off)).toByteArray()));
                    } else if (params[i].type.getName().equalsIgnoreCase("bytes")) {
                        obj.put(params[i].name, Hex.toHexString((byte[]) params[i].type.decode(encoded, off)));
                    } else {
                        obj.put(params[i].name, params[i].type.decode(encoded, off));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                off += params[i].type.getFixedSize();
            }
            return obj;
        }
        public  String formatSignature() {
            StringBuilder paramsTypes = new StringBuilder();
            for (Param param : inputs) {
                paramsTypes.append(param.type.getCanonicalName()).append(",");
            }

            return format("%s(%s)", name, stripEnd(paramsTypes.toString(), ","));
        }

        public static String stripEnd(String str, String stripChars) {
            if (str == null) {
                return str;
            }
            int end = str.length();
            if (end == 0) {
                return str;
            }
            if (stripChars == null) {
                while (end != 0 && Character.isWhitespace(str.charAt(end - 1))) {
                    end--;
                }
            } else if (stripChars.isEmpty()) {
                return str;
            } else {
                while (end != 0 && stripChars.indexOf(str.charAt(end - 1)) != -1) {
                    end--;
                }
            }
            return str.substring(0, end);
        }

        public  byte[] encodeSignatureLong() {
            String signature = formatSignature();
            byte[] sha3Fingerprint = SHA3Helper.sha3(signature.getBytes());
            return sha3Fingerprint;
        }

        public  byte[] encodeSignature() {
            return Arrays.copyOfRange(encodeSignatureLong(), 0, 4);
        }

        @Override
        public   String toString() {
            return formatSignature();
        }
    }

    public static class Contract {
        public Function[] functions;
        public Contract(String jsonInterface) {
            try {
                functions = fromJSON(jsonInterface);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        public static void checkABI(String jsonABI) throws JSONException {
            JSONArray array = new JSONArray(jsonABI);
            Function[] funArray = new Function[array.length()];
            for (int i = 0; i < array.length(); i++) {
                boolean z;
                JSONObject obj = array.getJSONObject(i);
                Function fun = new Function();
                fun.name = obj.has("name") ? obj.getString("name") : "";
                if (obj.has("constant") && obj.getBoolean("constant")) {
                    z = true;
                } else {
                    z = false;
                }
                fun.constant = z;
                if (obj.has("anonymous")) {
                    z = false;
                } else {
                    z = true;
                }
                fun.anonymous = z;
                fun.type = obj.has("type") ? FunctionType.valueOf(obj.getString("type")) : FunctionType.function;
                fun.inputs = checkParams(obj.has("inputs") ? obj.getJSONArray("inputs") : new JSONArray());
                fun.outputs = checkParams(obj.has("outputs") ? obj.getJSONArray("outputs") : new JSONArray());
                funArray[i] = fun;
            }
        }

        private static Param[] checkParams(JSONArray array) throws JSONException {
            Param[] paramInputs = new Param[array.length()];
            for (int j = 0; j < array.length(); j++) {
                JSONObject input = array.getJSONObject(j);
                Param param = new Param();
                param.indexed = input.has("indexed");
                param.name = input.getString("name");
                param.type = SolidityType.getType(input.getString("type"));
                paramInputs[j] = param;
            }
            return paramInputs;
        }

        private Param[] paramsFromJSON(JSONArray array) throws JSONException {
            Param[] paramInputs = new Param[array.length()];
            for (int j = 0; j < array.length(); j++) {
                JSONObject input = array.getJSONObject(j);
                Param param = new Param();
                param.indexed = input.has("indexed");
                param.name = input.getString("name");
                param.type = SolidityType.getType(input.getString("type"));
                paramInputs[j] = param;
            }
            return paramInputs;
        }

        private Function[] fromJSON(String jsonInterface) throws JSONException {
            JSONArray array = new JSONArray(jsonInterface);
            Function[] funArray = new Function[array.length()];
            for (int i = 0; i < array.length(); i++) {
                boolean z;
                JSONObject obj = array.getJSONObject(i);
                Function fun = new Function();
                fun.name = obj.has("name") ? obj.getString("name") : "";
                if (obj.has("constant") && obj.getBoolean("constant")) {
                    z = true;
                } else {
                    z = false;
                }
                fun.constant = z;
                if (obj.has("anonymous")) {
                    z = false;
                } else {
                    z = true;
                }
                fun.anonymous = z;
                fun.type = obj.has("type") ? FunctionType.valueOf(obj.getString("type")) : FunctionType.function;
                fun.inputs = paramsFromJSON(obj.has("inputs") ? obj.getJSONArray("inputs") : new JSONArray());
                fun.outputs = paramsFromJSON(obj.has("outputs") ? obj.getJSONArray("outputs") : new JSONArray());
                funArray[i] = fun;
            }
            return funArray;
        }

        public  Function getByName(String name) {
            for (Function function : functions) {
                if (name.equals(function.name)) {
                    return function;
                }
            }
            return null;
        }




        public Function getByTopic(String topic) {
            for (Function function : this.functions) {
                if (topic.startsWith(Hex.toHexString(function.encodeSignature()))) {
                    return function;
                }
            }
            return null;
        }
    }

}