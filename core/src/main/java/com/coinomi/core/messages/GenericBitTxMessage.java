package com.coinomi.core.messages;

import com.coinomi.core.Preconditions;
import com.coinomi.core.messages.TxMessage.Type;
import com.coinomi.core.wallet.AbstractTransaction;
import com.coinomi.core.wallet.families.bitcoin.BitTransaction;
import com.google.common.base.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericBitTxMessage implements TxMessage {
    private static final Logger log = LoggerFactory.getLogger(GenericBitTxMessage.class);
    private final int maxMessageBytes;
    private String message;

    public static class GenericBitTxMessageFactory implements MessageFactory {
        private final int maxMessageBytes;

        public GenericBitTxMessageFactory(int maxMessageBytes) {
            this.maxMessageBytes = maxMessageBytes;
        }

        public int maxMessageSizeBytes() {
            return this.maxMessageBytes;
        }

        @Override
        public boolean canHandlePublicMessages() {
            return false;
        }

        @Override
        public boolean canHandlePrivateMessages() {
            return false;
        }

        public TxMessage createPublicMessage(String message) {
            return GenericBitTxMessage.create(this.maxMessageBytes, message);
        }

        public TxMessage extractPublicMessage(AbstractTransaction transaction) {
            return GenericBitTxMessage.parse(this.maxMessageBytes, transaction);
        }
    }

    public GenericBitTxMessage(int maxMessageBytes) {
        this.maxMessageBytes = maxMessageBytes;
    }

    GenericBitTxMessage(int maxMessageBytes, String message) {
        this(maxMessageBytes);
        setMessage(message);
    }

    public void setMessage(String message) {
        Preconditions.checkArgument(serialize(message).length <= this.maxMessageBytes, "Message is too big");
        this.message = message;
    }

    private static GenericBitTxMessage create(int maxMessageBytes, String message) throws IllegalArgumentException {
        return new GenericBitTxMessage(maxMessageBytes, message);
    }

    private static GenericBitTxMessage parse(int maxMessageBytes, AbstractTransaction tx) {
        GenericBitTxMessage genericBitTxMessage = null;
        try {
            byte[] bytes = ((BitTransaction) tx).getRawTransaction().getExtraBytes();
            if (!(bytes == null || bytes.length == 0)) {
                Preconditions.checkArgument(bytes.length <= maxMessageBytes, "Maximum data size exceeded");
                genericBitTxMessage = create(maxMessageBytes, new String(bytes, Charsets.UTF_8));
            }
        } catch (Exception e) {
            log.info("Could not parse message: {}", e.getMessage());
        }
        return genericBitTxMessage;
    }

    public Type getType() {
        return Type.PUBLIC;
    }

    public String toString() {
        return this.message;
    }

    public void serializeTo(AbstractTransaction transaction) {
        if (transaction instanceof BitTransaction) {
            ((BitTransaction) transaction).getRawTransaction().setExtraBytes(serialize(this.message));
        }
    }

    static byte[] serialize(String message) {
        return message.getBytes(Charsets.UTF_8);
    }
}
