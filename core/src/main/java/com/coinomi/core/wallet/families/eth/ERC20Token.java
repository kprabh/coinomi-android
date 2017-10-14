package com.coinomi.core.wallet.families.eth;

import com.coinomi.core.coins.CoinID;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.FeePolicy;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.ValueType;
import com.coinomi.core.coins.families.EthFamily;
import com.coinomi.core.exceptions.AddressMalformedException;
import com.coinomi.core.exceptions.ExecutionException;
import com.coinomi.core.wallet.AbstractAddress;
import java.math.BigDecimal;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ERC20Token extends CoinType {
    private static final Logger log = LoggerFactory.getLogger(ERC20Token.class);
    private String contractAddress;
    boolean isFavorite;

    public static ERC20Token getERCToken(EthContract contract) {
        if (contract.isContractType("erc20")) {
            try {
                String subTypeId = EthFamily.generateSubTypeId(contract);
                if (!CoinID.hasCoinType(subTypeId)) {
                    ERC20Token token = new ERC20Token(contract);
                    if (!CoinID.addCoinType(token)) {
                        log.error("Could not add type with id " + token.getId());
                        return null;
                    }
                }
                return (ERC20Token) CoinID.typeFromId(subTypeId);
            } catch (Exception e) {
                log.debug(e.getMessage());
            }
        }
        return null;
    }

    private ERC20Token(EthContract contract) throws JSONException {
        update(contract);
    }

    public AbstractAddress newAddress(String addressStr) throws AddressMalformedException {
        return new EthAddress(this, addressStr);
    }

    public boolean isSubType() {
        return true;
    }

    public boolean isFavorite() {
        return this.isFavorite;
    }

    public void setFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public Value getBalance(EthFamilyWallet wallet) throws ExecutionException {
        if (wallet.getAllContracts().containsKey(this.contractAddress)) {
            EthContract contract = (EthContract) wallet.getAllContracts().get(this.contractAddress);
            if (contract.getHistory("balanceOf").isEmpty()) {
                getFreshBalance(wallet);
            } else {
                try {
                    if (((JSONObject) contract.getHistory("balanceOf").get(0)).getJSONObject("outputs").has("balance")) {
                        return Value.valueOf((ValueType) this, new BigDecimal(((JSONObject) contract.getHistory("balanceOf").get(0)).getJSONObject("outputs").getString("balance")).toBigInteger());
                    }
                    return Value.valueOf((ValueType) this, new BigDecimal(((JSONObject) contract.getHistory("balanceOf").get(0)).getJSONObject("outputs").getString("")).toBigInteger());
                } catch (Exception e) {
                }
            }
        }
        return zeroCoin();
    }

    public void getFreshBalance(EthFamilyWallet wallet) throws ExecutionException {
        EthContract.executeFunction(wallet, getAddress(), "balanceOf", "0", new String[]{wallet.getAddress().toString()});
    }

    public String getAddress() {
        return this.contractAddress;
    }

    public synchronized void update(EthContract contract) throws JSONException {
        this.contractAddress = contract.getContractAddress();
        this.unitExponent = Integer.valueOf(contract.getExtras().getInt("decimals"));
        this.symbol = contract.getExtras().getString("symbol");
        this.parentType = contract.getCoinType();
        this.id = EthFamily.generateSubTypeId(contract);
        this.name = contract.getName();
        this.uriScheme = this.parentType.getUriScheme();
        this.minNonDust = value(1);
        this.feePolicy = FeePolicy.FEE_GAS_PRICE;
        this.icon = contract.getIcon();
        if (this.icon.isEmpty()) {
            this.icon = "https://github.com/Coinomi/crypto-icons/raw/master/png/token.png";
        }
    }

    public String transfer(EthFamilyWallet wallet, AbstractAddress address, Value sendAmount) {
        try {
            return EthContract.executeFunction(wallet, getAddress(), "transfer", "0.00", new String[]{address.toString(), sendAmount.getBigInt().toString()});
        } catch (ExecutionException e) {
            return null;
        }
    }
}
