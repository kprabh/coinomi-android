package com.coinomi.core.coins.families;

import com.coinomi.core.messages.GenericBitTxMessage.GenericBitTxMessageFactory;
import com.coinomi.core.messages.MessageFactory;

public class SolarFamily extends BitFamily {
    private static transient MessageFactory instance = new GenericBitTxMessageFactory(528);

    public SolarFamily() {
        this.family = Families.SOLARCOIN;
    }

    public MessageFactory getMessagesFactory() {
        return instance;
    }
}
