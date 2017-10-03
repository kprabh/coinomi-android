package com.coinomi.core.coins.families;

import com.coinomi.core.coins.ValueType;

/**
 * @author John L. Jegutanis
 */
public enum Families {
    ETHEREUM("ethereum"),
    NXT("nxt"),
    FIAT("fiat"),
    // same as in org.bitcoinj.params.Networks
    BITCOIN("bitcoin"),
    NUBITS("nubits"),
    PEERCOIN("peercoin"),
    REDDCOIN("reddcoin"),
    VPNCOIN("vpncoin"),
    CLAMS("clams"),
    SOLARCOIN("solarcoin"),
    GRIDCOIN("gridcoin"),
    ZCASH("zcash"),
    NAVCOIN("navcoin"),
    BITCOINCASH("btccash");

    public final String family;

    Families(String family) {
        this.family = family;
    }

    @Override
    public String toString() {
        return family;
    }
}
