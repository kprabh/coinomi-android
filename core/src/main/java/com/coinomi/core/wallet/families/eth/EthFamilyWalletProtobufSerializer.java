package com.coinomi.core.wallet.families.eth;

import com.coinomi.core.coins.CoinID;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.protos.Protos.WalletPocket;
import com.coinomi.core.protos.Protos.WalletPocket.Builder;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Map;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EthFamilyWalletProtobufSerializer {
    private static final Logger log = LoggerFactory.getLogger(EthFamilyWalletProtobufSerializer.class);
    protected Map<ByteString, EthTransaction> txMap = new HashMap();

    public static WalletPocket toProtobuf(EthFamilyWallet pocket) {
        Builder walletBuilder = WalletPocket.newBuilder();
        walletBuilder.setNetworkIdentifier(pocket.getCoinType().getId());
        if (pocket.getDescription() != null) {
            walletBuilder.setDescription(pocket.getDescription());
        }
        if (pocket.getId() != null) {
            walletBuilder.setId(pocket.getId());
        }
        walletBuilder.addAllKey(pocket.serializeKeychainToProtobuf());
        return walletBuilder.build();
    }

    public EthFamilyWallet readWallet(WalletPocket walletProto, KeyCrypter keyCrypter) throws UnreadableWalletException {
        try {
            EthFamilyKey rootKey;
            EthFamilyWallet pocket;
            CoinType coinType = CoinID.typeFromId(walletProto.getNetworkIdentifier());
            if (keyCrypter != null) {
                rootKey = EthFamilyKey.fromProtobuf(walletProto.getKeyList(), keyCrypter);
            } else {
                rootKey = EthFamilyKey.fromProtobuf(walletProto.getKeyList());
            }
            if (walletProto.hasId()) {
                pocket = new EthFamilyWallet(walletProto.getId(), rootKey, coinType);
            } else {
                pocket = new EthFamilyWallet(rootKey, coinType);
            }
            if (walletProto.hasDescription()) {
                pocket.setDescription(walletProto.getDescription());
            }
            if (walletProto.hasLastSeenBlockHeight()) {
                pocket.setLastBlockSeenHeight(walletProto.getLastSeenBlockHeight());
            } else {
                pocket.setLastBlockSeenHeight(-1);
            }
            pocket.setLastBlockSeenTimeSecs(walletProto.getLastSeenBlockTimeSecs());
            return pocket;
        } catch (IllegalArgumentException e) {
            throw new UnreadableWalletException("Unknown network parameters ID " + walletProto.getNetworkIdentifier());
        }
    }
}
