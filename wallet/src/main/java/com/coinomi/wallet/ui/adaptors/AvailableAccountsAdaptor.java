package com.coinomi.wallet.ui.adaptors;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.coinomi.core.Preconditions;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.wallet.ui.widget.CoinView;
import com.coinomi.wallet.util.WalletUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author John L. Jegutanis
 */
public class AvailableAccountsAdaptor extends BaseAdapter {

    private final Context context;
    private List<Entry> entries;

    public static class Entry {
        final public int iconRes;
        final public String title;
        private WalletAccount account;
        private CoinType type;

        public Entry(WalletAccount account, CoinType type) {
            this.title = type.getName() + "(" + account.getDescriptionOrCoinName() + ")";
            this.account = (WalletAccount) Preconditions.checkNotNull(account);
            this.type = (CoinType) Preconditions.checkNotNull(type);
            this.iconRes = -1;
        }

        public Entry(WalletAccount account) {
            iconRes = WalletUtils.getIconRes(account);
            title = account.getDescriptionOrCoinName();
            this.account = (WalletAccount) Preconditions.checkNotNull(account);
            type = account.getCoinType();
        }

        public Entry(CoinType type) {
            iconRes = WalletUtils.getIconRes(type);
            title = type.getName();
            account = null;
            this.type = Preconditions.checkNotNull(type);
        }

        // Used for search
//        private Entry(Object accountOrCoinType) {
//            iconRes = -1;
//            title = null;
//            this.accountOrCoinType = accountOrCoinType;
//        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Entry)) {
                return false;
            }
            if (this.account != null) {
                return this.account.equals(((Entry) o).account);
            }
            return this.type.equals(((Entry) o).type);
        }

        public CoinType getType() {
            if (this.type != null) {
                return this.type;
            }
            if (this.account != null) {
                return this.account.getCoinType();
            }
                throw new IllegalStateException("No cointype available");
            }

        public WalletAccount getAccount() {
            return this.account;
        }

        public boolean isSubType() {
            return this.type.isSubType();
    }
        public void setAccount(WalletAccount account) {
            this.account = account;
        }
    }

    public AvailableAccountsAdaptor(final Context context) {
        this.context = context;
        //entries = ImmutableList.of();
    }

    public int getPosition(Entry entry) {
        return this.entries.indexOf(entry);
    }

    /**
     * Update the adaptor to include all accounts that are in the validTypes list.
     *
     * If includeTypes is true, it will also include any coin type that is in not in accounts but is
     * in the validTypes.
     */
    public void update(final List<WalletAccount> accounts, final List<CoinType> validTypes,
                       final boolean includeTypes) {
        entries = createEntries(accounts, validTypes, includeTypes);
        notifyDataSetChanged();
    }
    public void updateAccount(WalletAccount account) {
        for (Entry entry : this.entries) {
            if (account.getCoinType().equals(entry.getType())) {
                entry.setAccount(account);
                notifyDataSetChanged();
                return;
            }
        }
    }
    private static List<Entry> createEntries(final List<WalletAccount> accounts,
                                                      final List<CoinType> validTypes,
                                                      final boolean includeTypes) {
        final ArrayList<CoinType> typesToAdd = Lists.newArrayList(validTypes);

        final ImmutableList.Builder<Entry> listBuilder = ImmutableList.builder();
        for (WalletAccount account : accounts) {
            if (validTypes.contains(account.getCoinType())) {
                listBuilder.add(new Entry(account));
                // Don't add this type as we just added the account for this type
                typesToAdd.remove(account.getCoinType());
            }
            for (CoinType subType : (List<CoinType>)account.availableSubTypes()) {
                if (validTypes.contains(subType)) {
                    listBuilder.add(new Entry(account, subType));
                    typesToAdd.remove(subType);
        }
            }
        }
        if (includeTypes) {
            for (CoinType type : typesToAdd) {
                listBuilder.add(new Entry(type));
            }
        }

        return listBuilder.build();
    }

    public List<CoinType> getTypes() {
        Set<CoinType> types = new HashSet<>();
        for (AvailableAccountsAdaptor.Entry entry : entries) {
            types.add(entry.getType());
        }
        return ImmutableList.copyOf(types);
    }

    public List<Entry> getEntries() {
        return entries;
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public Entry getItem(int position) {
        return entries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Entry entry = getItem(position);
        if (convertView == null) {
            convertView = new CoinView(this.context);
        }
        if (entry.isSubType()) {
            ((CoinView) convertView).setNormalData(entry.title, entry.getType().getIcon());
        } else {
            ((CoinView) convertView).setData(entry.title, entry.iconRes);
        }
        return convertView;
    }
}
