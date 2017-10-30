package com.coinomi.wallet.ui.adaptors;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.exceptions.ExecutionException;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.core.wallet.families.eth.ERC20Token;
import com.coinomi.core.wallet.families.eth.EthFamilyWallet;

import com.coinomi.wallet.Constants;
import com.coinomi.wallet.ExchangeRatesProvider;
import com.coinomi.wallet.R;
import com.coinomi.wallet.ui.widget.Amount;
import com.coinomi.wallet.ExchangeRatesProvider.ExchangeRate;
import com.coinomi.wallet.ui.widget.Amount;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * @author John L. Jegutanis
 */
public class AccountListAdapter extends BaseAdapter {
    private final List<AccountOrType> accounts = new ArrayList();
    private final Context context;
    private final LayoutInflater inflater;
    private final HashMap<String, ExchangeRate> rates;

    public class AccountOrType {
        public final WalletAccount account;
        public final CoinType subType;

        protected AccountOrType(WalletAccount account, CoinType subType) {
            this.account = account;
            this.subType = subType;
        }
    }

    public AccountListAdapter(Context context, @Nonnull final Wallet wallet) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        for (WalletAccount acc : wallet.getAllAccounts()) {
            this.accounts.add(new AccountOrType(acc, null));
            for (CoinType t : ( List<CoinType>)acc.favoriteSubTypes()) {
                this.accounts.add(new AccountOrType(acc, t));
            }
        }
        this.rates = new HashMap<>();
    }



    public void clear() {
        accounts.clear();

        notifyDataSetChanged();
    }

    public void replace(@Nonnull final Wallet wallet) {
        accounts.clear();
        for (WalletAccount acc : wallet.getAllAccounts()) {
            this.accounts.add(new AccountOrType(acc, null));
            for (CoinType t :  (List<CoinType>)acc.favoriteSubTypes()) {
                this.accounts.add(new AccountOrType(acc, t));
            }
        }

        notifyDataSetChanged();
    }

    @Override
    public boolean isEmpty() {
        return accounts.isEmpty();
    }

    @Override
    public int getCount() {
        return accounts.size();
    }

    @Override
    public AccountOrType getItem(final int position) {
        return accounts.get(position);
    }

    @Override
    public long getItemId(final int position) {
        if (position == accounts.size()){
            return 0;
        }
        if (((AccountOrType) this.accounts.get(position)).subType == null) {
            return (long) ((AccountOrType) this.accounts.get(position)).account.getId().hashCode();
        }
        return (long) ((AccountOrType) this.accounts.get(position)).subType.getId().hashCode();
    }

    public void setExchangeRates(@Nullable Map<String, ExchangeRatesProvider.ExchangeRate> newRates) {
        if (newRates != null) {
            this.rates.putAll(newRates);
        } else {
            rates.clear();
        }
        notifyDataSetChanged();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(final int position, View row, final ViewGroup parent) {
        if (row == null) row = inflater.inflate(R.layout.account_row, null);

        bindView(row, getItem(position));
        return row;
    }

    private void bindView(View row, AccountOrType account) {
        CoinType type;
        String nameOrDesc;
        Value value;
        if (account.subType == null) {
            type = account.account.getCoinType();
            nameOrDesc = account.account.getDescriptionOrCoinName();
            value = account.account.getBalance();
        } else if (account.subType instanceof ERC20Token) {
            type = account.subType;
            nameOrDesc = account.subType.getName();
            try {
                value = ((ERC20Token) type).getBalance((EthFamilyWallet) account.account);
            } catch (ExecutionException e) {
                value = type.zeroCoin();
            }
        } else {
            throw new RuntimeException("Unsupported subtype " + account.subType.getClass());
        }
        String symbol = type.getSymbol();
        final ImageView icon = (ImageView) row.findViewById(R.id.account_icon);
        if (account.subType == null) {
        icon.setImageResource(Constants.COINS_ICONS.get(type));
        } else if (account.subType.getIcon().startsWith("data:image")) {
            byte[] decodedString = Base64.decode(account.subType.getIcon().replace("data:image/png;base64,", ""), 0);
            icon.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
        } else if (!account.subType.getIcon().isEmpty()) {
            Picasso.with(this.context).load(account.subType.getIcon()).into(icon);
        }
        final TextView rowLabel = (TextView) row.findViewById(R.id.account_description);
        rowLabel.setText(nameOrDesc);

        final Amount rowValue = (Amount) row.findViewById(R.id.account_balance);
        rowValue.setAmount(GenericUtils.formatFiatValue(value, 4, 0));
        rowValue.setSymbol(symbol);

        ExchangeRatesProvider.ExchangeRate rate = rates.get(symbol);
        final Amount rowBalanceRateValue = (Amount) row.findViewById(R.id.account_balance_rate);
        if (rate != null) {
            Value localAmount = rate.rate.convert(value);
            GenericUtils.formatFiatValue(localAmount, 2,0);
            rowBalanceRateValue.setAmount(GenericUtils.formatFiatValue(localAmount, 2, 0));
            rowBalanceRateValue.setSymbol(localAmount.type.getSymbol());
            rowBalanceRateValue.setVisibility(View.VISIBLE);
        } else {
            rowBalanceRateValue.setVisibility(View.GONE);
        }

        final Amount rowRateValue = (Amount) row.findViewById(R.id.exchange_rate_row_rate);
        if (rate != null ) {
            Value localAmount = rate.rate.convert(type.oneCoin());
            GenericUtils.formatFiatValue(localAmount, 2, 0);
            rowRateValue.setAmount(GenericUtils.formatFiatValue(localAmount, 2, 0));
            rowRateValue.setSymbol(localAmount.type.getSymbol());
            rowRateValue.setVisibility(View.VISIBLE);
        } else {
            rowRateValue.setVisibility(View.GONE);
        }
    }

}
