package com.coinomi.core.coins.families;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.exceptions.AddressMalformedException;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.families.eth.EthAddress;

public class EthFamily extends CoinType {
    public EthFamily() {
        this.family = Families.ETHEREUM;
        this.hasDynamicFees = true;
    }

    public AbstractAddress newAddress(String addressStr) throws AddressMalformedException {
        return new EthAddress(this, addressStr);
    }
}
