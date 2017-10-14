package com.coinomi.core.coins;

import com.coinomi.core.exceptions.AddressMalformedException;
import com.coinomi.core.util.GenericUtils;
import com.google.common.collect.ImmutableList;
import com.coinomi.core.coins.families.EthFamily;
import com.coinomi.core.exceptions.UnsupportedCoinTypeException;
import com.coinomi.core.wallet.families.eth.ERC20DefaultTokens;
import com.coinomi.core.wallet.families.eth.ERC20Token;
import com.coinomi.core.wallet.families.eth.EthContract;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.Networks;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * When adding new coin types the order affects which types will be chosen by default if they share
 * a URI scheme. For example BITCOIN_MAIN and BITCOIN_TEST share the bitcoin: scheme so BITCOIN_MAIN
 * will be chosen by default when we don't have any other information. The same applies to the other
 * testnets and NUBITS_MAIN and NUSHARES_MAIN that share the nu: URI scheme. For anything else the
 * order doesn't matter.
 *
 * @author John L. Jegutanis
 */
public enum CoinID {
    BITCOIN_MAIN(BitcoinMain.get()),
    BITCOIN_TEST(BitcoinTest.get()),
    LITECOIN_MAIN(LitecoinMain.get()),
    LITECOIN_TEST(LitecoinTest.get()),
    DOGECOIN_MAIN(DogecoinMain.get()),
    DOGECOIN_TEST(DogecoinTest.get()),
    ETHEREUM_MAIN(EthereumMain.get()),
    ETHCLASSIC_MAIN(EthClassicMain.get()),
    REDDCOIN_MAIN(ReddcoinMain.get()),
    PEERCOIN_MAIN(PeercoinMain.get()),
    DASH_MAIN(DashMain.get()),
    NUBITS_MAIN(NuBitsMain.get()),
    NUSHARES_MAIN(NuSharesMain.get()),
    NAMECOIN_MAIN(NamecoinMain.get()),
    BLACKCOIN_MAIN(BlackcoinMain.get()),
    MONACOIN_MAIN(MonacoinMain.get()),
    FEATHERCOIN_MAIN(FeathercoinMain.get()),
    RUBYCOIN_MAIN(RubycoinMain.get()),
    DIGITALCOIN_MAIN(DigitalcoinMain.get()),
    CANNACOIN_MAIN(CannacoinMain.get()),
    DIGIBYTE_MAIN(DigibyteMain.get()),
    NEOSCOIN_MAIN(NeoscoinMain.get()),
    VERTCOIN_MAIN(VertcoinMain.get()),
    NXT_MAIN(NxtMain.get()),
    BURST_MAIN(BurstMain.get()),
    JUMBUCKS_MAIN(JumbucksMain.get()),
    VPNCOIN_MAIN(VpncoinMain.get()),
    NOVACOIN_MAIN(NovacoinMain.get()),
    SHADOWCASH_MAIN(ShadowCashMain.get()),
    CANADAECOIN_MAIN(CanadaeCoinMain.get()),
    PARKBYTE_MAIN(ParkbyteMain.get()),
    VERGE_MAIN(VergeMain.get()),
    CLAMS_MAIN(ClamsMain.get()),
    GCR_MAIN(GcrMain.get()),
    POTCOIN_MAIN(PotcoinMain.get()),
    GULDEN_MAIN(GuldenMain.get()),
    AURORACOIN_MAIN(AuroracoinMain.get()),
    BATACOIN_MAIN(BatacoinMain.get()),
    OKCASH_MAIN(OKCashMain.get()),
    ASIACOIN_MAIN(AsiacoinMain.get()),
    EGULDEN_MAIN(EguldenMain.get()),
    CLUBCOIN_MAIN(ClubcoinMain.get()),
    RICHCOIN_MAIN(RichcoinMain.get()),
    IXCOIN_MAIN(IxcoinMain.get()),
    VOXELS_MAIN(VoxelsMain.get()),
    SILKCOIN_MAIN(SilkcoinMain.get()),
    ;

    private static List<CoinType> types;
    private static final Logger log = LoggerFactory.getLogger(CoinID.class);
    private static AbstractMap<String, CoinType> idLookup = new ConcurrentHashMap();
    private static AbstractMap<String, CoinType> symbolLookup = new ConcurrentHashMap();
    private static AbstractMap<String, List<CoinType>> uriLookup = new ConcurrentHashMap();

    static {
      //  Set<NetworkParameters> bitcoinjNetworks = Networks.get();
        for (NetworkParameters network : Networks.get()) {
            Networks.unregister(network);
        }

        List<CoinType> coinTypeBuilder = new CopyOnWriteArrayList();
        for (CoinID id : values()) {
            Networks.register(id.type);

            if (symbolLookup.containsKey(id.type.symbol)) {
                throw new IllegalStateException(
                        "Coin currency codes must be unique, double found: " + id.type.symbol);
            }
            symbolLookup.put(id.type.symbol, id.type);

            if (idLookup.containsKey(id.type.getId())) {
                throw new IllegalStateException(
                        "Coin IDs must be unique, double found: " + id.type.getId());
            }
            // Coin ids must end with main or test
            if (!id.type.getId().endsWith("main") && !id.type.getId().endsWith("test")) {
                throw new IllegalStateException(
                        "Coin IDs must end with 'main' or 'test': " + id.type.getId());
            }
            idLookup.put(id.type.getId(), id.type);

            if (!uriLookup.containsKey(id.type.uriScheme)) {
                uriLookup.put(id.type.uriScheme, new CopyOnWriteArrayList());
            }
            uriLookup.get(id.type.uriScheme).add(id.type);

            coinTypeBuilder.add(id.type);
        }
        types = coinTypeBuilder;
        addERC20Tokens();
    }
    private static void addERC20Tokens() {
        JSONArray array = ERC20DefaultTokens.getERC20DefaultTokens(ETHEREUM_MAIN.type);
        int i = 0;
        while (i < array.length()) {
            try {
                JSONObject obj = array.getJSONObject(i);
                if (obj.has("address")) {
                    EthContract contract = EthContract.fromJSON(ETHEREUM_MAIN.type, obj);
                    if (!contract.isContractType("erc20")) {
                        throw new IllegalStateException("Unexpected contract type " + contract.getContractAddress());
                    } else if (ERC20Token.getERCToken(contract) == null) {
                        throw new IllegalStateException("Could not add ERC20 contract " + contract.getContractAddress());
                    }
                }
                i++;
            } catch (JSONException e) {
                throw new IllegalStateException("Could not parse json", e);
            }
        }
    }

    private final CoinType type;

    CoinID(final CoinType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type.getId();
    }

    public CoinType getCoinType() {
        return type;
    }

    public static List<CoinType> getSupportedCoins() {
        return types;
    }

    public static CoinType typeFromId(String stringId) {
        if (idLookup.containsKey(stringId)) {
            return idLookup.get(stringId);
        } else {
            throw new UnsupportedCoinTypeException("Unsupported ID: " + stringId);
        }
    }

    public static List<CoinType> fromUri(String input) {
        String inputLowercase = input.toLowerCase();
        for (String uri : uriLookup.keySet()) {
            if (inputLowercase.startsWith(uri + "://") || inputLowercase.startsWith(uri + ":")) {
                return uriLookup.get(uri);
            }
        }
        throw new UnsupportedCoinTypeException("Unsupported URI: " + input);
    }

    public static List<CoinType> fromUriScheme(String scheme) {
        String schemeLowercase = scheme.toLowerCase();
        if (uriLookup.containsKey(schemeLowercase)) {
            return uriLookup.get(schemeLowercase);
        } else {
            throw new UnsupportedCoinTypeException("Unsupported URI scheme: " + scheme);
        }
    }

    public static List<CoinType> typesFromAddress(String address) throws AddressMalformedException {
        return GenericUtils.getPossibleTypes(address);
    }

    public static boolean isSymbolSupported(String symbol) {
        return symbolLookup.containsKey(symbol);
    }

    public static CoinType typeFromSymbol(String symbol) {
        if (symbolLookup.containsKey(symbol.toUpperCase())) {
            return symbolLookup.get(symbol.toUpperCase());
        } else {
            throw new UnsupportedCoinTypeException("Unsupported coin symbol: " + symbol);
        }
    }

    public static synchronized boolean addCoinType(CoinType type) {
        boolean z;
        synchronized (CoinID.class) {
            if (symbolLookup.containsKey(type.getSymbol())) {
                log.error("Type with symbol already exists: " + type.getSymbol());
                z = false;
            } else {
                symbolLookup.put(type.getSymbol(), type);
                types.add(type);
                idLookup.put(type.getId(), type);
                if (!uriLookup.containsKey(type.uriScheme.toLowerCase())) {
                    uriLookup.put(type.uriScheme.toLowerCase(), new CopyOnWriteArrayList());
                }
                ((List) uriLookup.get(type.uriScheme.toLowerCase())).add(type);
                z = true;
            }
        }
        return z;
    }

    public static boolean hasCoinType(String typeId) {
        return idLookup.containsKey(typeId);
    }

    public static CoinType subTypeFromId(String subTypeHash, CoinType type) {
        if (type instanceof EthFamily) {
            return typeFromId(CoinType.generateSubTypeId(subTypeHash, type));
        }
        throw new UnsupportedCoinTypeException("Sub types are not supported for type: " + type.getId());
    }
}
