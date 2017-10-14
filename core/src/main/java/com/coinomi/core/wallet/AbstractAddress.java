package com.coinomi.core.wallet;

import com.coinomi.core.coins.CoinType;

import java.io.Serializable;

/**
 * @author John L. Jegutanis
 */
public interface AbstractAddress extends Serializable {
    boolean equals(AbstractAddress abstractAddress);

    CoinType getType();
    String toString();
    long getId();
}
