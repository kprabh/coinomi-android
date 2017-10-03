package com.coinomi.wallet.ui.adaptors;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.coinomi.core.wallet.families.eth.EthContract;
import com.coinomi.core.wallet.families.eth.EthFamilyWallet;
import com.coinomi.wallet.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ContractsListAdapter extends BaseAdapter {
    private final Context context;
    private final List<EthContract> contracts = new ArrayList();
    private final LayoutInflater inflater;

    public ContractsListAdapter(Context context, EthFamilyWallet account) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.contracts.addAll(account.getContracts());
    }

    public void sortByName(final boolean ascending) {
        Collections.sort(this.contracts, new Comparator<EthContract>() {
            public int compare(EthContract contract, EthContract t1) {
                return ascending ? contract.getName().compareTo(t1.getName()) : contract.getName().compareTo(t1.getName()) * -1;
            }
        });
    }

    public boolean isEmpty() {
        return this.contracts.isEmpty();
    }

    public int getCount() {
        return this.contracts.size();
    }

    public EthContract getItem(int position) {
        return (EthContract) this.contracts.get(position);
    }

    public long getItemId(int position) {
        if (position == this.contracts.size()) {
            return 0;
        }
        return (long) ((EthContract) this.contracts.get(position)).getName().hashCode();
    }

    public boolean hasStableIds() {
        return true;
    }

    public View getView(int position, View row, ViewGroup parent) {
        if (row == null) {
            row = this.inflater.inflate(R.layout.contract_row, null);
        }
        bindView(row, getItem(position));
        return row;
    }

    private void bindView(View row, EthContract contract) {
        boolean shortenText;
        int i = 120;
        ((TextView) row.findViewById(R.id.contract_row_name)).setText(contract.getName());
        if (contract.getDescription().length() > 120) {
            shortenText = true;
        } else {
            shortenText = false;
        }
        String description = contract.getDescription();
        if (!shortenText) {
            i = contract.getDescription().length();
        }
        String descriptionText = description.substring(0, i);
        if (shortenText) {
            descriptionText = descriptionText + this.context.getString(R.string.ellipsis);
        }
        ((TextView) row.findViewById(R.id.contract_row_description)).setText(descriptionText);
    }
}
