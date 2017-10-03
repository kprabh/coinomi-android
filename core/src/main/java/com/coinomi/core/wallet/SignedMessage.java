package com.coinomi.core.wallet;

import javax.annotation.Nullable;

import static com.coinomi.core.Preconditions.checkNotNull;

/**
 * @author John L. Jegutanis
 */
final public class SignedMessage {
    public enum Status {
        SignedOK, VerifiedOK, Unknown, AddressMalformed, KeyIsEncrypted, MissingPrivateKey,
        InvalidSigningAddress, InvalidMessageSignature
    }

    public final String message;
    public final String address;
    String signature;
    Status status = Status.Unknown;

    public SignedMessage(String address, String message, String signature) {
        this.address = checkNotNull(address);
        this.message = checkNotNull(message);
        this.signature = signature;
    }

    public SignedMessage(String address, String message) {
        this(address, message, null);
    }

    public SignedMessage(SignedMessage otherMessage, Status newStatus) {
        message = otherMessage.message;
        address = otherMessage.address;
        signature = otherMessage.signature;
        status = newStatus;
    }
    public void setStatus(Status status) {
        this.status = status;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
    public String getAddress() {
        return address;
    }

    public String getMessage() {
        return message;
    }

    @Nullable
    public String getSignature() {
        return signature;
    }

    public Status getStatus() {
        return status;
    }
}
