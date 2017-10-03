package com.coinomi.core.coins.eth.util;

/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
 * (derived from ethereumJ library, Copyright (c) 2016 <ether.camp>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */



        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;
        import org.spongycastle.util.BigIntegers;
        import org.spongycastle.util.encoders.Hex;

        import javax.annotation.CheckForNull;
        import javax.annotation.Nonnull;
        import javax.annotation.Nullable;
        import java.math.BigInteger;
        import java.nio.ByteBuffer;
        import java.nio.charset.StandardCharsets;
        import java.util.*;

        import static org.spongycastle.util.Arrays.concatenate;
        import static org.spongycastle.util.BigIntegers.asUnsignedByteArray;

/**
 * Recursive Length Prefix (RLP) encoding.
 * <p>
 * The purpose of RLP is to encode arbitrarily nested arrays of binary data, and
 * RLP is the main encoding method used to serialize objects in Ethereum. The
 * only purpose of RLP is to encode structure; encoding specific atomic data
 * types (eg. strings, integers, floats) is left up to higher-order protocols; in
 * Ethereum the standard is that integers are represented in big endian binary
 * form. If one wishes to use RLP to encode a dictionary, the two suggested
 * canonical forms are to either use [[k1,v1],[k2,v2]...] with keys in
 * lexicographic order or to use the higher-level Patricia Tree encoding as
 * Ethereum does.
 * <p>
 * The RLP encoding function takes in an item. An item is defined as follows:
 * <p>
 * - A string (ie. byte array) is an item - A list of items is an item
 * <p>
 * For example, an empty string is an item, as is the string containing the word
 * "cat", a list containing any number of strings, as well as more complex data
 * structures like ["cat",["puppy","cow"],"horse",[[]],"pig",[""],"sheep"]. Note
 * that in the context of the rest of this article, "string" will be used as a
 * synonym for "a certain number of bytes of binary data"; no special encodings
 * are used and no knowledge about the content of the strings is implied.
 * <p>
 * See: https://github.com/ethereum/wiki/wiki/%5BEnglish%5D-RLP
 *
 * @author Roman Mandeleil
 * @since 01.04.2014
 */
public class RLP {

    private static final Logger logger = LoggerFactory.getLogger("rlp");


    /**
     * Allow for content up to size of 2^64 bytes *
     */
    private static final double MAX_ITEM_LENGTH = Math.pow(256, 8);

    /**
     * Reason for threshold according to Vitalik Buterin:
     * - 56 bytes maximizes the benefit of both options
     * - if we went with 60 then we would have only had 4 slots for long strings
     * so RLP would not have been able to store objects above 4gb
     * - if we went with 48 then RLP would be fine for 2^128 space, but that's way too much
     * - so 56 and 2^64 space seems like the right place to put the cutoff
     * - also, that's where Bitcoin's varint does the cutof
     */
    private static final int SIZE_THRESHOLD = 56;

    /** RLP encoding rules are defined as follows: */

    /*
     * For a single byte whose value is in the [0x00, 0x7f] range, that byte is
     * its own RLP encoding.
     */

    /**
     * [0x80]
     * If a string is 0-55 bytes long, the RLP encoding consists of a single
     * byte with value 0x80 plus the length of the string followed by the
     * string. The range of the first byte is thus [0x80, 0xb7].
     */
    private static final int OFFSET_SHORT_ITEM = 0x80;

    /**
     * [0xb7]
     * If a string is more than 55 bytes long, the RLP encoding consists of a
     * single byte with value 0xb7 plus the length of the length of the string
     * in binary form, followed by the length of the string, followed by the
     * string. For example, a length-1024 string would be encoded as
     * \xb9\x04\x00 followed by the string. The range of the first byte is thus
     * [0xb8, 0xbf].
     */
    private static final int OFFSET_LONG_ITEM = 0xb7;

    /**
     * [0xc0]
     * If the total payload of a list (i.e. the combined length of all its
     * items) is 0-55 bytes long, the RLP encoding consists of a single byte
     * with value 0xc0 plus the length of the list followed by the concatenation
     * of the RLP encodings of the items. The range of the first byte is thus
     * [0xc0, 0xf7].
     */
    private static final int OFFSET_SHORT_LIST = 0xc0;

    /**
     * [0xf7]
     * If the total payload of a list is more than 55 bytes long, the RLP
     * encoding consists of a single byte with value 0xf7 plus the length of the
     * length of the list in binary form, followed by the length of the list,
     * followed by the concatenation of the RLP encodings of the items. The
     * range of the first byte is thus [0xf8, 0xff].
     */
    private static final int OFFSET_LONG_LIST = 0xf7;


    /* ******************************************************
     *                      DECODING                        *
     * ******************************************************/

    private static byte decodeOneByteItem(byte[] data, int index) {
        // null item
        if ((data[index] & 0xFF) == OFFSET_SHORT_ITEM) {
            return (byte) (data[index] - OFFSET_SHORT_ITEM);
        }
        // single byte item
        if ((data[index] & 0xFF) < OFFSET_SHORT_ITEM) {
            return data[index];
        }
        // single byte item
        if ((data[index] & 0xFF) == OFFSET_SHORT_ITEM + 1) {
            return data[index + 1];
        }
        return 0;
    }

    public static int decodeInt(byte[] data, int index) {
        int value = 0;
        // NOTE: there are two ways zero can be encoded - 0x00 and OFFSET_SHORT_ITEM

        if ((data[index] & 0xFF) < OFFSET_SHORT_ITEM) {
            return data[index];
        } else if ((data[index] & 0xFF) >= OFFSET_SHORT_ITEM
                && (data[index] & 0xFF) < OFFSET_LONG_ITEM) {

            byte length = (byte) (data[index] - OFFSET_SHORT_ITEM);
            byte pow = (byte) (length - 1);
            for (int i = 1; i <= length; ++i) {
                value += (data[index + i] & 0xFF) << (8 * pow);
                pow--;
            }
        } else {
            throw new RuntimeException("wrong decode attempt");
        }
        return value;
    }


    public static byte[] decodeIP4Bytes(byte[] data, int index) {

        int offset = 1;

        final byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            result[i] = decodeOneByteItem(data, index + offset);
            if ((data[index + offset] & 0xFF) > OFFSET_SHORT_ITEM)
                offset += 2;
            else
                offset += 1;
        }

        // return IP address
        return result;
    }

    public static int getFirstListElement(byte[] payload, int pos) {

        if (pos >= payload.length)
            return -1;

        if ((payload[pos] & 0xFF) >= OFFSET_LONG_LIST) {
            byte lengthOfLength = (byte) (payload[pos] - OFFSET_LONG_LIST);
            return pos + lengthOfLength + 1;
        }
        if ((payload[pos] & 0xFF) >= OFFSET_SHORT_LIST
                && (payload[pos] & 0xFF) < OFFSET_LONG_LIST) {
            return pos + 1;
        }
        if ((payload[pos] & 0xFF) >= OFFSET_LONG_ITEM
                && (payload[pos] & 0xFF) < OFFSET_SHORT_LIST) {
            byte lengthOfLength = (byte) (payload[pos] - OFFSET_LONG_ITEM);
            return pos + lengthOfLength + 1;
        }
        return -1;
    }

    public static int getNextElementIndex(byte[] payload, int pos) {

        if (pos >= payload.length)
            return -1;

        if ((payload[pos] & 0xFF) >= OFFSET_LONG_LIST) {
            byte lengthOfLength = (byte) (payload[pos] - OFFSET_LONG_LIST);
            int length = calcLength(lengthOfLength, payload, pos);
            return pos + lengthOfLength + length + 1;
        }
        if ((payload[pos] & 0xFF) >= OFFSET_SHORT_LIST
                && (payload[pos] & 0xFF) < OFFSET_LONG_LIST) {

            byte length = (byte) ((payload[pos] & 0xFF) - OFFSET_SHORT_LIST);
            return pos + 1 + length;
        }
        if ((payload[pos] & 0xFF) >= OFFSET_LONG_ITEM
                && (payload[pos] & 0xFF) < OFFSET_SHORT_LIST) {

            byte lengthOfLength = (byte) (payload[pos] - OFFSET_LONG_ITEM);
            int length = calcLength(lengthOfLength, payload, pos);
            return pos + lengthOfLength + length + 1;
        }
        if ((payload[pos] & 0xFF) > OFFSET_SHORT_ITEM
                && (payload[pos] & 0xFF) < OFFSET_LONG_ITEM) {

            byte length = (byte) ((payload[pos] & 0xFF) - OFFSET_SHORT_ITEM);
            return pos + 1 + length;
        }
        if ((payload[pos] & 0xFF) == OFFSET_SHORT_ITEM) {
            return pos + 1;
        }
        if ((payload[pos] & 0xFF) < OFFSET_SHORT_ITEM) {
            return pos + 1;
        }
        return -1;
    }

    /**
     * Get exactly one message payload
     */
    public static void fullTraverse(byte[] msgData, int level, int startPos,
                                    int endPos, int levelToIndex, Queue<Integer> index) {

        try {

            if (msgData == null || msgData.length == 0)
                return;
            int pos = startPos;

            while (pos < endPos) {

                if (level == levelToIndex)
                    index.add(pos);

                // It's a list with a payload more than 55 bytes
                // data[0] - 0xF7 = how many next bytes allocated
                // for the length of the list
                if ((msgData[pos] & 0xFF) >= OFFSET_LONG_LIST) {

                    byte lengthOfLength = (byte) (msgData[pos] - OFFSET_LONG_LIST);
                    int length = calcLength(lengthOfLength, msgData, pos);

                    // now we can parse an item for data[1]..data[length]
                    System.out.println("-- level: [" + level
                            + "] Found big list length: " + length);

                    fullTraverse(msgData, level + 1, pos + lengthOfLength + 1,
                            pos + lengthOfLength + length, levelToIndex, index);

                    pos += lengthOfLength + length + 1;
                    continue;
                }
                // It's a list with a payload less than 55 bytes
                if ((msgData[pos] & 0xFF) >= OFFSET_SHORT_LIST
                        && (msgData[pos] & 0xFF) < OFFSET_LONG_LIST) {

                    byte length = (byte) ((msgData[pos] & 0xFF) - OFFSET_SHORT_LIST);

                    System.out.println("-- level: [" + level
                            + "] Found small list length: " + length);

                    fullTraverse(msgData, level + 1, pos + 1, pos + length + 1,
                            levelToIndex, index);

                    pos += 1 + length;
                    continue;
                }
                // It's an item with a payload more than 55 bytes
                // data[0] - 0xB7 = how much next bytes allocated for
                // the length of the string
                if ((msgData[pos] & 0xFF) >= OFFSET_LONG_ITEM
                        && (msgData[pos] & 0xFF) < OFFSET_SHORT_LIST) {

                    byte lengthOfLength = (byte) (msgData[pos] - OFFSET_LONG_ITEM);
                    int length = calcLength(lengthOfLength, msgData, pos);

                    // now we can parse an item for data[1]..data[length]
                    System.out.println("-- level: [" + level
                            + "] Found big item length: " + length);
                    pos += lengthOfLength + length + 1;

                    continue;
                }
                // It's an item less than 55 bytes long,
                // data[0] - 0x80 == length of the item
                if ((msgData[pos] & 0xFF) > OFFSET_SHORT_ITEM
                        && (msgData[pos] & 0xFF) < OFFSET_LONG_ITEM) {

                    byte length = (byte) ((msgData[pos] & 0xFF) - OFFSET_SHORT_ITEM);

                    System.out.println("-- level: [" + level
                            + "] Found small item length: " + length);
                    pos += 1 + length;
                    continue;
                }
                // null item
                if ((msgData[pos] & 0xFF) == OFFSET_SHORT_ITEM) {
                    System.out.println("-- level: [" + level
                            + "] Found null item: ");
                    pos += 1;
                    continue;
                }
                // single byte item
                if ((msgData[pos] & 0xFF) < OFFSET_SHORT_ITEM) {
                    System.out.println("-- level: [" + level
                            + "] Found single item: ");
                    pos += 1;
                    continue;
                }
            }
        } catch (Throwable th) {
            throw new RuntimeException("RLP wrong encoding",
                    th.fillInStackTrace());
        }
    }

    private static int calcLength(int lengthOfLength, byte[] msgData, int pos) {
        byte pow = (byte) (lengthOfLength - 1);
        int length = 0;
        for (int i = 1; i <= lengthOfLength; ++i) {
            length += (msgData[pos + i] & 0xFF) << (8 * pow);
            pow--;
        }
        return length;
    }

    private static int calcLengthRaw(int lengthOfLength, byte[] msgData, int index) {
        byte pow = (byte) (lengthOfLength - 1);
        int length = 0;
        for (int i = 1; i <= lengthOfLength; ++i) {
            length += msgData[index + i] << (8 * pow);
            pow--;
        }
        return length;
    }

    public static byte getCommandCode(byte[] data) {
        int index = getFirstListElement(data, 0);
        final byte command = data[index];
        return ((command & 0xFF) == OFFSET_SHORT_ITEM) ? 0 : command;
    }




    public static RLPList decode2(byte[] msgData) {
        RLPList rlpList = new RLPList();
        fullTraverse(msgData, 0, 0, msgData.length, 1, rlpList);
        return rlpList;
    }
    /**
     * Get exactly one message payload
     */
    private static void fullTraverse(byte[] msgData, int level, int startPos, int endPos, int levelToIndex, RLPList rlpList) {
        if (msgData != null) {
            try {
                if (msgData.length != 0) {
                    int pos = startPos;
                    while (pos < endPos) {
                        logger.debug("fullTraverse: level: " + level + " startPos: " + pos + " endPos: " + endPos);
                        byte lengthOfLength;
                        int length;
                        byte[] rlpData;
                        byte[] item;
                        RLPList newLevelList;
                        byte[] bArr;
                        if ((msgData[pos] & 255) > 247) {
                            lengthOfLength = (byte) (msgData[pos] - 247);
                            length = calcLength(lengthOfLength, msgData, pos);
                            rlpData = new byte[((lengthOfLength + length) + 1)];
                            System.arraycopy(msgData, pos, rlpData, 0, (lengthOfLength + length) + 1);
                            newLevelList = new RLPList();
                            newLevelList.setRLPData(rlpData);
                            bArr = msgData;
                            fullTraverse(bArr, level + 1, (pos + lengthOfLength) + 1, ((pos + lengthOfLength) + length) + 1, levelToIndex, newLevelList);
                            rlpList.add(newLevelList);
                            pos += (lengthOfLength + length) + 1;
                        } else if ((msgData[pos] & 255) >= 192 && (msgData[pos] & 255) <= 247) {
                            length = (byte) ((msgData[pos] & 255) - 192);
                            rlpData = new byte[(length + 1)];
                            System.arraycopy(msgData, pos, rlpData, 0, length + 1);
                            newLevelList = new RLPList();
                            newLevelList.setRLPData(rlpData);
                            if (length > (byte) 0) {
                                bArr = msgData;
                                fullTraverse(bArr, level + 1, pos + 1, (pos + length) + 1, levelToIndex, newLevelList);
                            }
                            rlpList.add(newLevelList);
                            pos += length + 1;
                        } else if ((msgData[pos] & 255) > 183 && (msgData[pos] & 255) < 192) {
                            lengthOfLength = (byte) (msgData[pos] - 183);
                            length = calcLength(lengthOfLength, msgData, pos);
                            item = new byte[length];
                            System.arraycopy(msgData, (pos + lengthOfLength) + 1, item, 0, length);
                            System.arraycopy(msgData, pos, new byte[(lengthOfLength + 1)], 0, lengthOfLength + 1);
                            rlpList.add(new RLPItem(item));
                            pos += (lengthOfLength + length) + 1;
                        } else if ((msgData[pos] & 255) > 128 && (msgData[pos] & 255) <= 183) {
                            length = (byte) ((msgData[pos] & 255) - 128);
                            item = new byte[length];
                            System.arraycopy(msgData, pos + 1, item, 0, length);
                            System.arraycopy(msgData, pos, new byte[2], 0, 2);
                            rlpList.add(new RLPItem(item));
                            pos += length + 1;
                        } else if ((msgData[pos] & 255) == 128) {
                            rlpList.add(new RLPItem(ByteUtil.EMPTY_BYTE_ARRAY));
                            pos++;
                        } else if ((msgData[pos] & 255) < 128) {
                            RLPList rLPList = rlpList;
                            rLPList.add(new RLPItem(new byte[]{(byte) (msgData[pos] & 255)}));
                            pos++;
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("RLP wrong encoding (" + Hex.toHexString(msgData, startPos, endPos - startPos) + ")", e);
            } catch (OutOfMemoryError e2) {
                throw new RuntimeException("Invalid RLP (excessive mem allocation while parsing) (" + Hex.toHexString(msgData, startPos, endPos - startPos) + ")", e2);
            }
        }
    }

    /* ******************************************************
     *                      ENCODING                        *
     * ******************************************************/



    public static byte[] encodeByte(byte singleByte) {
        if ((singleByte & 0xFF) == 0) {
            return new byte[]{(byte) OFFSET_SHORT_ITEM};
        } else if ((singleByte & 0xFF) <= 0x7F) {
            return new byte[]{singleByte};
        } else {
            return new byte[]{(byte) (OFFSET_SHORT_ITEM + 1), singleByte};
        }
    }

    public static byte[] encodeShort(short singleShort) {
        if ((singleShort & 0xFF) == singleShort)
            return encodeByte((byte) singleShort);
        else {
            return new byte[]{(byte) (OFFSET_SHORT_ITEM + 2),
                    (byte) (singleShort >> 8 & 0xFF),
                    (byte) (singleShort >> 0 & 0xFF)};
        }
    }

    public static byte[] encodeInt(int singleInt) {
        if ((singleInt & 0xFF) == singleInt)
            return encodeByte((byte) singleInt);
        else if ((singleInt & 0xFFFF) == singleInt)
            return encodeShort((short) singleInt);
        else if ((singleInt & 0xFFFFFF) == singleInt)
            return new byte[]{(byte) (OFFSET_SHORT_ITEM + 3),
                    (byte) (singleInt >>> 16),
                    (byte) (singleInt >>> 8),
                    (byte) singleInt};
        else {
            return new byte[]{(byte) (OFFSET_SHORT_ITEM + 4),
                    (byte) (singleInt >>> 24),
                    (byte) (singleInt >>> 16),
                    (byte) (singleInt >>> 8),
                    (byte) singleInt};
        }
    }


    public static byte[] encodeBigInteger(BigInteger srcBigInteger) {
        if (srcBigInteger.equals(BigInteger.ZERO))
            return encodeByte((byte) 0);
        else
            return encodeElement(asUnsignedByteArray(srcBigInteger));
    }

    public static byte[] encodeElement(byte[] srcData) {

        if (ByteUtil.isNullOrZeroArray(srcData))
            return new byte[]{(byte) OFFSET_SHORT_ITEM};
        else if (ByteUtil.isSingleZero(srcData))
            return srcData;
        else if (srcData.length == 1 && (srcData[0] & 0xFF) < OFFSET_SHORT_ITEM) {
            return srcData;
        } else if (srcData.length < SIZE_THRESHOLD) {
            // length = 8X
            byte length = (byte) (OFFSET_SHORT_ITEM + srcData.length);
            byte[] data = Arrays.copyOf(srcData, srcData.length + 1);
            System.arraycopy(data, 0, data, 1, srcData.length);
            data[0] = length;

            return data;
        } else {
            // length of length = BX
            // prefix = [BX, [length]]
            int tmpLength = srcData.length;
            byte byteNum = 0;
            while (tmpLength != 0) {
                ++byteNum;
                tmpLength = tmpLength >> 8;
            }
            byte[] lenBytes = new byte[byteNum];
            for (int i = 0; i < byteNum; ++i) {
                lenBytes[byteNum - 1 - i] = (byte) ((srcData.length >> (8 * i)) & 0xFF);
            }
            // first byte = F7 + bytes.length
            byte[] data = Arrays.copyOf(srcData, srcData.length + 1 + byteNum);
            System.arraycopy(data, 0, data, 1 + byteNum, srcData.length);
            data[0] = (byte) (OFFSET_LONG_ITEM + byteNum);
            System.arraycopy(lenBytes, 0, data, 1, lenBytes.length);

            return data;
        }
    }




    public static byte[] encodeListHeader(int size) {

        if (size == 0) {
            return new byte[]{(byte) OFFSET_SHORT_LIST};
        }

        int totalLength = size;

        byte[] header;
        if (totalLength < SIZE_THRESHOLD) {

            header = new byte[1];
            header[0] = (byte) (OFFSET_SHORT_LIST + totalLength);
        } else {
            // length of length = BX
            // prefix = [BX, [length]]
            int tmpLength = totalLength;
            byte byteNum = 0;
            while (tmpLength != 0) {
                ++byteNum;
                tmpLength = tmpLength >> 8;
            }
            tmpLength = totalLength;

            byte[] lenBytes = new byte[byteNum];
            for (int i = 0; i < byteNum; ++i) {
                lenBytes[byteNum - 1 - i] = (byte) ((tmpLength >> (8 * i)) & 0xFF);
            }
            // first byte = F7 + bytes.length
            header = new byte[1 + lenBytes.length];
            header[0] = (byte) (OFFSET_LONG_LIST + byteNum);
            System.arraycopy(lenBytes, 0, header, 1, lenBytes.length);

        }

        return header;
    }


    public static byte[] encodeLongElementHeader(int length) {

        if (length < SIZE_THRESHOLD) {

            if (length == 0)
                return new byte[]{(byte) 0x80};
            else
                return new byte[]{(byte) (0x80 + length)};

        } else {

            // length of length = BX
            // prefix = [BX, [length]]
            int tmpLength = length;
            byte byteNum = 0;
            while (tmpLength != 0) {
                ++byteNum;
                tmpLength = tmpLength >> 8;
            }

            byte[] lenBytes = new byte[byteNum];
            for (int i = 0; i < byteNum; ++i) {
                lenBytes[byteNum - 1 - i] = (byte) ((length >> (8 * i)) & 0xFF);
            }

            // first byte = F7 + bytes.length
            byte[] header = new byte[1 + lenBytes.length];
            header[0] = (byte) (OFFSET_LONG_ITEM + byteNum);
            System.arraycopy(lenBytes, 0, header, 1, lenBytes.length);

            return header;
        }
    }


    public static byte[] encodeList(byte[]... elements) {

        if (elements == null) {
            return new byte[]{(byte) OFFSET_SHORT_LIST};
        }

        int totalLength = 0;
        for (byte[] element1 : elements) {
            totalLength += element1.length;
        }

        byte[] data;
        int copyPos;
        if (totalLength < SIZE_THRESHOLD) {

            data = new byte[1 + totalLength];
            data[0] = (byte) (OFFSET_SHORT_LIST + totalLength);
            copyPos = 1;
        } else {
            // length of length = BX
            // prefix = [BX, [length]]
            int tmpLength = totalLength;
            byte byteNum = 0;
            while (tmpLength != 0) {
                ++byteNum;
                tmpLength = tmpLength >> 8;
            }
            tmpLength = totalLength;
            byte[] lenBytes = new byte[byteNum];
            for (int i = 0; i < byteNum; ++i) {
                lenBytes[byteNum - 1 - i] = (byte) ((tmpLength >> (8 * i)) & 0xFF);
            }
            // first byte = F7 + bytes.length
            data = new byte[1 + lenBytes.length + totalLength];
            data[0] = (byte) (OFFSET_LONG_LIST + byteNum);
            System.arraycopy(lenBytes, 0, data, 1, lenBytes.length);

            copyPos = lenBytes.length + 1;
        }
        for (byte[] element : elements) {
            System.arraycopy(element, 0, data, copyPos, element.length);
            copyPos += element.length;
        }
        return data;
    }



    private static int calculateLength(byte[] data, int index) {
        if ((data[index] & 0xFF) < OFFSET_SHORT_ITEM) {
            return 1;
        } else if ((data[index] & 0xFF) >= OFFSET_LONG_ITEM
                && (data[index] & 0xFF) < OFFSET_SHORT_LIST) {

            byte lengthOfLength = (byte) (data[index] - OFFSET_LONG_ITEM);
            return calcLengthRaw(lengthOfLength, data, index);

        } else if ((data[index] & 0xFF) >= OFFSET_SHORT_ITEM
                && (data[index] & 0xFF) < OFFSET_LONG_ITEM) {

            return (byte) (data[index] - OFFSET_SHORT_ITEM);

        } else {
            throw new RuntimeException("wrong decode attempt");
        }
    }


}