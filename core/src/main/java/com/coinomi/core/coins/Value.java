package com.coinomi.core.coins;

/**
 * Copyright 2014 Andreas Schildbach
 * Copyright 2015 John L. Jegutanis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.bitcoinj.core.Coin;

import com.coinomi.core.util.MonetaryFormat;
import com.google.common.math.LongMath;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Represents a monetary value. This class is immutable.
 */
public class Value implements MonetaryFormat.Monetary, Comparable<Value>, Serializable {
    /**
     * The type of this value
     */
    public final ValueType type;
    /**
     * The number of units of this monetary value.
     */
    public final BigInteger value;


    @Nullable
    public static Value valueOf(final ValueType type, @Nullable final Coin coin) {
        if (coin != null) return new Value(type, coin.value);
        return null;
    }

    public static Value valueOf(ValueType type, byte[] units) {
        return new Value(type, units);
    }

    public static Value valueOf(ValueType type, long units) {
        return new Value(type, units);
    }

    public static Value valueOf(final ValueType type, BigInteger units) {
        return new Value(type, units);
    }

    public static Value valueOf(final ValueType type, String unitsStr) {
        BigInteger v;
        if (unitsStr.startsWith("0x")) {
            v = new BigInteger(unitsStr.replace("0x", ""), 16);
        } else {
            v = new BigInteger(unitsStr);
        }
        return valueOf(type, v);
    }

    public Value(ValueType type, long units) {
        this.type = checkNotNull(type);
        this.value = BigInteger.valueOf(units);
    }

    public Value(ValueType type, byte[] units) {
        this(type, new BigInteger(units));
    }

    public Value(ValueType type, BigInteger units) {
        this.type = checkNotNull(type);
        this.value = units;
    }

    @Override

    public int smallestUnitExponent() {
        return type.getUnitExponent();
    }

    public BigInteger getBigInt() {
        return this.value;
    }

    /**
     * Returns the number of units of this monetary value.
     */
    @Deprecated
    public long getValue() {
        return this.value.longValue();
    }
    @Deprecated
    public Coin toCoin() {
        return Coin.valueOf(getValue());
    }

    /**
     * Convert an amount expressed in the way humans are used to into units.
     */
    public static Value valueOf(final ValueType type, final int coins, final int cents) {
        checkArgument(cents < 100);
        checkArgument(cents >= 0);
        checkArgument(coins >= 0);
        return type.oneCoin().multiply(coins).add(type.oneCoin().divide(100).multiply(cents));
    }

    /**
     * Parses an amount expressed in the way humans are used to.
     *
     * This takes string in a format understood by {@link BigDecimal#BigDecimal(String)},
     * for example "0", "1", "0.10", "1.23E3", "1234.5E-5".
     *
     * @throws IllegalArgumentException if you try to specify fractional units, or a value out of
     * range.
     */
    public static Value parse(final ValueType type, final String str) {
        return parse(type, new BigDecimal(str));
    }

    /**
     * Parses a {@link BigDecimal} amount expressed in the way humans are used to.
     *
     * @throws IllegalArgumentException if you try to specify fractional units, or a value out of
     * range.
     */
    public static Value parse(final ValueType type, final BigDecimal decimal) {
        return Value.valueOf(type, decimal.movePointRight(type.getUnitExponent())
                .toBigIntegerExact());
    }

    public static Value parse(ValueType type, double value) {
        return valueOf(type, new BigDecimal(value).setScale(type.getUnitExponent(), 4).unscaledValue());
    }

    public Value add(final Value value) {
        checkArgument(type.equals(value.type), "Cannot add a different type");
        return new Value(this.type, this.value.add(value.getBigInt()));
    }

    public Value add(final Coin value) {
        return new Value(this.type, LongMath.checkedAdd(getValue(), value.value));
    }

    /*public Value add(final BigInteger value) {
        return new Value(this.type, LongMath.checkedAdd(getValue(), value));
    }*/

    public Value subtract(final Value value) {
        checkArgument(type.equals(value.type), "Cannot subtract a different type");
        return subtract(value.getBigInt());
    }

    public Value subtract(final Coin value) {
        return new Value(this.type, LongMath.checkedSubtract(getValue(), value.getValue()));
    }

    public Value subtract(String str) {
        return subtract(type.value(str));
    }

    public Value subtract(BigInteger value) {
        return new Value(this.type, this.value.subtract(value));
    }

    public Value multiply(final long factor) {
        return new Value(this.type, this.value.multiply(BigInteger.valueOf(factor)));
    }

    public Value multiply(final BigInteger factor) {
        return new Value(this.type, this.value.multiply(factor));
    }

    public Value divide(final long divisor) {
        return new Value(this.type, this.value.divide(BigInteger.valueOf(divisor)));
    }

/*    public Value[] divideAndRemainder(final long divisor) {
        return new Value[] { new Value(this.type, this.value / divisor),
                             new Value(this.type, this.value % divisor) };
    }*/

    /*public long divide(final Value divisor) {
        checkArgument(type.equals(divisor.type), "Cannot divide with a different type");
        return this.value / divisor.value;
    }*/

    /**
     * Returns true if and only if this instance represents a monetary value greater than zero,
     * otherwise false.
     */
    public boolean isPositive() {
        return signum() == 1;
    }

    /**
     * Returns true if and only if this instance represents a monetary value less than zero,
     * otherwise false.
     */
    public boolean isNegative() {
        return signum() == -1;
    }

    /**
     * Returns true if and only if this instance represents zero monetary value,
     * otherwise false.
     */
    public boolean isZero() {
        return signum() == 0;
    }

    /**
     * Returns true if the monetary value represented by this instance is greater than that
     * of the given other Value, otherwise false.
     */
    public boolean isGreaterThan(Value other) {
        return compareTo(other) > 0;
    }

    /**
     * Returns true if the monetary value represented by this instance is less than that
     * of the given other Value, otherwise false.
     */
    public boolean isLessThan(Value other) {
        return compareTo(other) < 0;
    }

//    public Value shiftLeft(final int n) {
//        return new Value(this.type, this.value << n);
//    }
//
//    public Value shiftRight(final int n) {
//        return new Value(this.type, this.value >> n);
//    }

    @Override
    public int signum() {
        return this.value.signum();
    }

    public Value negate() {
        return new Value(this.type, this.value.negate());
    }

    /**
     * Returns the value as a 0.12 type string. More digits after the decimal place will be used
     * if necessary, but two will always be present.
     */
    public String toFriendlyString() {
        return type.getMoneyFormat().format(this).toString();
    }

    /**
     * <p>
     * Returns the value as a plain string denominated in BTC.
     * The result is unformatted with no trailing zeroes.
     * For instance, a value of 150000 satoshis gives an output string of "0.0015" BTC
     * </p>
     */
    public String toPlainString() {
        return type.getPlainFormat().format(this).toString();
    }

    @Override
    public String toString() {
        return toPlainString() + type.getSymbol();
    }

    /**
     * Returns the value expressed as string
     */
    public String toUnitsString() {
        return value.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (o == null || o.getClass() != getClass())
            return false;
        final Value other = (Value) o;
        if (this.value != other.value || !this.type.equals(other.type))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return value.intValue();
    }

    @Override
    public int compareTo(@Nonnull final Value other) {
        checkArgument(type.equals(other.type), "Cannot compare different types");
        return getBigInt().compareTo(other.getBigInt());
    }

    public boolean isDust() {
        return compareTo(type.getMinNonDust()) < 0;
    }

    public boolean isOfType(ValueType otherType) {
        return type.equals(otherType);
    }

    public boolean isOfType(Value otherValue) {
        return type.equals(otherValue.type);
    }

    /**
     * Check if the value is within the [min, max] range
     */
    public boolean within(Value min, Value max) {
        return compareTo(min) >=0 && compareTo(max) <= 0;
    }

    public static Value max(Value value1, Value value2) {
        return value1.compareTo(value2) >= 0 ? value1 : value2;
    }

    public static Value min(Value value1, Value value2) {
        return value1.compareTo(value2) <= 0 ? value1 : value2;
    }

    public boolean canCompare(Value other) {
        return canCompare(this, other);
    }

    public static boolean canCompare(@Nullable Value amount1, @Nullable Value amount2) {
        return amount1 != null && amount2 != null && amount1.isOfType(amount2);
    }
}
