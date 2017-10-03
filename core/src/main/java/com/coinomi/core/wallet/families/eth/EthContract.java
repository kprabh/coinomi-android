package com.coinomi.core.wallet.families.eth;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.ValueType;
import com.coinomi.core.coins.eth.CallTransaction.Contract;
import com.coinomi.core.coins.eth.CallTransaction.Function;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EthContract {
    private Value balance;
    private Contract contract;
    private String contractABI;
    private String contractAddress;
    private String contractSuit;
    private String description;
    private HashMap<String, List<JSONObject>> history = new HashMap();
    private String icon;
    private long id;
    private String name;
    private String officialSite;

    public static EthContract fromJSON(CoinType coinType, JSONObject contract) throws JSONException {
        if (contract.has("address")) {
            if (contract.has("abi")) {
                String abi = contract.getString("abi");
                String contractAddress = contract.getString("address");
                String url = contract.has("url") ? contract.getString("url") : "";
                String name = contract.has("name") ? contract.getString("name") : "";
                JSONObject contractSuit = contract.has("suit") ? contract.getJSONObject("suit") : new JSONObject();
                String desc = contract.has("description") ? contract.getString("description") : "";
                String icon = contract.has("icon") ? contract.getString("icon") : "";
                long contractNumber = contract.has("contractNumber") ? contract.getLong("contractNumber") : (long) contract.hashCode();
                String suitName = contractSuit.has("name") ? contractSuit.getString("name") : "";
                if (contractSuit.has("template") && !contractSuit.getString("template").isEmpty()) {
                    EthContractSuits.parseTemplate(contractSuit);
                }
                EthContract parsed = new EthContract(coinType, name, desc, url, contractAddress, abi, suitName, icon, contractNumber);
                if (contract.has("history")) {
                    JSONArray array = contract.getJSONArray("history");
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject historyItem = array.getJSONObject(i);
                        String funcName = historyItem.getString("funcName");
                        JSONArray histArray = historyItem.getJSONArray("funcHist");
                        int j = 0;
                        while (j < histArray.length()) {
                            int j2 = j + 1;
                            parsed.putHistory(funcName, histArray.getJSONObject(j));
                            j = j2;
                        }
                    }
                }
                return parsed;
            }
        }
        throw new JSONException("should contain abi and address");
    }

    public EthContract(CoinType type, String name, String description, String officialSite, String contractAddress, String contractABI, String contractSuit, String icon, long contractNumber) {
        this.name = name;
        this.icon = icon;
        this.id = contractNumber;
        this.description = description;
        this.contractABI = contractABI;
        this.officialSite = officialSite;
        this.contractSuit = contractSuit;
        this.contractAddress = contractAddress;
        this.contract = new Contract(contractABI);
        this.balance = Value.valueOf((ValueType) type, BigInteger.ZERO);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("name", this.name).put("icon", this.icon).put("abi", this.contractABI).put("url", this.officialSite).put("contractNumber", this.id).put("suit", new JSONObject().put("name", this.contractSuit).put("template", EthContractSuits.getTemplate(this.contractSuit))).put("description", this.description).put("address", this.contractAddress);
        JSONArray historyArray = new JSONArray();
        for (String funcName : this.history.keySet()) {
            JSONObject histItem = new JSONObject();
            histItem.put("funcName", funcName);
            JSONArray histArray = new JSONArray();
            for (JSONObject item : this.history.get(funcName)) {
                histArray.put(item);
            }
            histItem.put("funcHist", histArray);
            historyArray.put(histItem);
        }
        obj.put("history", historyArray);
        return obj;
    }

    public void putHistory(String function, JSONObject message) {
        List<JSONObject> hist;
        if (this.history.containsKey(function)) {
            hist = (List) this.history.get(function);
        } else {
            hist = new ArrayList();
            this.history.put(function, new ArrayList());
        }
        hist.add(0, message);
        if (hist.size() > 10) {
            hist.remove(hist.size() - 1);
        }
    }

    public List<JSONObject> getHistory(String function) {
        if (this.history.containsKey(function)) {
            return (List) this.history.get(function);
        }
        return new ArrayList();
    }

    public String getName() {
        return this.name;
    }

    public String getIcon() {
        return this.icon;
    }

    public Value getBalance() {
        return this.balance;
    }

    public String getDescription() {
        return this.description;
    }

    public String getContractSuit() {
        return EthContractSuits.getTemplate(this.contractSuit);
    }

    public String getOfficialSite() {
        return this.officialSite;
    }

    public String getContractAddress() {
        return this.contractAddress;
    }

    public Contract getContract() {
        return this.contract;
    }

    public void setBalance(String balance) {
        this.balance = Value.valueOf(this.balance.type, balance);
    }

    public void parseResult(JSONObject message) throws JSONException {
        int i = 8;
        JSONObject histItem;
        String data;
        Function f;
        if (message.has("eth_call")) {
            histItem = new JSONObject();
            data = message.getJSONObject("eth_call").getString("data");
            f = getContract().getByName(message.getString("function"));
            if (message.getString("function").length() <= 0) {
                i = 0;
            }
            histItem.put("inputs", f.resultToJSON(data.substring(i), f.inputs));
            if (!message.getString("result").equalsIgnoreCase("0x")) {
                histItem.put("outputs", f.resultToJSON(message.getString("result"), f.outputs));
            }
            putHistory(message.getString("function"), histItem);
        } else if (message.has("eth_estimateGas")) {
            histItem = new JSONObject();
            data = message.getJSONObject("eth_estimateGas").getString("data");
            f = getContract().getByName(message.getString("function"));
            if (message.getString("function").length() <= 0) {
                i = 0;
            }
            histItem.put("inputs", f.resultToJSON(data.substring(i), f.inputs));
            histItem.put("outputs", new JSONObject().put("estimatedGas", message.getString("result")));
            putHistory(message.getString("function"), histItem);
        }
    }

    public HashMap<String, List<JSONObject>> getAllHistory() {
        return this.history;
    }

    public void setHistory(HashMap<String, List<JSONObject>> history) {
        this.history = history;
    }
}
