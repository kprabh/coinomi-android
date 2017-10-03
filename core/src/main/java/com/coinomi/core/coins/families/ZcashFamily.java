package com.coinomi.core.coins.families;

public abstract class ZcashFamily extends BitFamily {
    public ZcashFamily() {
        this.family = Families.ZCASH;
        this.addressVersionLength = 2;
    }
}
