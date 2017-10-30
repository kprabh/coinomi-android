package com.coinomi.wallet.ui.adaptors;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
//import android.widget.Filter.FilterResults;
import android.widget.Filterable;
import android.widget.TextView;
import com.coinomi.core.wallet.families.eth.EthContract;
import com.coinomi.core.wallet.families.eth.EthFamilyWallet;
import com.coinomi.wallet.R;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.json.JSONException;

public class ContractsListAdapter extends BaseAdapter implements Filterable {
    private final Context context;
    private final List<EthContract> contracts = new ArrayList();
    private List<EthContract> filteredContracts = new ArrayList();
    private final LayoutInflater inflater;
    private ItemFilter mFilter = new ItemFilter();

    private class ItemFilter extends Filter {
        private ItemFilter() {
        }

        protected FilterResults performFiltering(CharSequence constraint) {
            String filterString = constraint.toString().toLowerCase();
            FilterResults results = new FilterResults();
            List<EthContract> list = ContractsListAdapter.this.contracts;
            int count = list.size();
            ArrayList<EthContract> nlist = new ArrayList(count);
            if (filterString.isEmpty()) {
                nlist.addAll(ContractsListAdapter.this.contracts);
            } else {
                int i = 0;
                while (i < count) {
                    EthContract filterableString = (EthContract) list.get(i);
                    try {
                        if (filterableString.isContractType(filterString) || filterableString.toJSON().toString().toLowerCase().contains(filterString.toLowerCase())) {
                            nlist.add(filterableString);
                            i++;
                        } else {
                            i++;
                        }
                    } catch (JSONException e) {
                        nlist.addAll(ContractsListAdapter.this.contracts);
                    }
                }
            }
            results.values = nlist;
            results.count = nlist.size();
            return results;
        }

        protected void publishResults(CharSequence constraint, FilterResults results) {
            ContractsListAdapter.this.filteredContracts = (ArrayList) results.values;
            ContractsListAdapter.this.sortByName(true);
            ContractsListAdapter.this.notifyDataSetChanged();
        }
    }

    public ContractsListAdapter(Context context, EthFamilyWallet account) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.contracts.addAll(account.getContracts());
        this.filteredContracts.addAll(account.getContracts());
    }

    public void sortByName(final boolean ascending) {
        Collections.sort(this.filteredContracts, new Comparator<EthContract>() {
            public int compare(EthContract contract, EthContract t1) {
                return ascending ? contract.getName().toLowerCase().compareTo(t1.getName().toLowerCase()) : contract.getName().toLowerCase().compareTo(t1.getName().toLowerCase()) * -1;
            }
        });
    }

    public boolean isEmpty() {
        return this.filteredContracts.isEmpty();
    }

    public int getCount() {
        return this.filteredContracts.size();
    }

    public EthContract getItem(int position) {
        return (EthContract) this.filteredContracts.get(position);
    }

    public long getItemId(int position) {
        if (position == this.filteredContracts.size()) {
            return 0;
        }
        return (long) ((EthContract) this.filteredContracts.get(position)).getName().hashCode();
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

    public Filter getFilter() {
        return this.mFilter;
    }

    public void replace(Collection<? extends EthContract> contracts) {
        this.contracts.clear();
        this.contracts.addAll(contracts);
        notifyDataSetChanged();
    }
}
