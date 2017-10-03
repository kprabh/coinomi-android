package com.coinomi.wallet.ui.adaptors;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.wallet.Configuration;
import com.coinomi.wallet.Constants;
import com.coinomi.wallet.ui.widget.CoinListItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author John L. Jegutanis
 */
public class FeesListAdapter extends BaseAdapter {
    private final Context context;
    private final Configuration config;
    private List<Value> fees;
    private final Wallet wallet;

    public FeesListAdapter(final Context context, Configuration config, Wallet wallet) {
        this.context = context;
        this.config = config;
        this.fees = new ArrayList<>();
        this.wallet = wallet;
        update();
    }

    public void update() {
        Set<CoinType> types = new HashSet<CoinType>(this.wallet != null ? this.wallet.getAccountTypes() : Constants.SUPPORTED_COINS);
        fees.clear();
        fees.addAll(config.getFeeValues(types).values());
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return fees.size();
    }

    @Override
    public Value getItem(int position) {
        return fees.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = new CoinListItem(context);
        }

        CoinListItem row = (CoinListItem) view;

        Value fee = getItem(position);
        CoinType type = (CoinType) fee.type;
        row.setCoin(type);
        row.setAmount(fee);
        return row;
    }
}