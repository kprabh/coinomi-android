package com.coinomi.wallet.ui;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;
import com.coinomi.wallet.ui.widget.Amount;
import com.coinomi.wallet.ui.widget.SponsorView;
import com.coinomi.wallet.ui.widget.SwipeRefreshLayout;

public class BalanceFragment_ViewBinding<T extends BalanceFragment> implements Unbinder {
    protected T target;
    private View view2131689595;
    private View view2131689677;
    private View view2131689678;

    public BalanceFragment_ViewBinding(final T target, View source) {
        this.target = target;
        View view = Utils.findRequiredView(source, R.id.transaction_rows, "field 'transactionRows' and method 'onItemClick'");
        target.transactionRows = (ListView) Utils.castView(view, R.id.transaction_rows, "field 'transactionRows'", ListView.class);
        this.view2131689677 = view;
        ((AdapterView) view).setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View p1, int p2, long p3) {
                target.onItemClick(p2);
            }
        });
        target.swipeContainer = (SwipeRefreshLayout) Utils.findRequiredViewAsType(source, R.id.swipeContainer, "field 'swipeContainer'", SwipeRefreshLayout.class);
        target.emptyPocketMessage = Utils.findRequiredView(source, R.id.history_empty, "field 'emptyPocketMessage'");
        view = Utils.findRequiredView(source, R.id.account_balance, "field 'accountBalance' and method 'onMainAmountClick'");
        target.accountBalance = (Amount) Utils.castView(view, R.id.account_balance, "field 'accountBalance'", Amount.class);
        this.view2131689595 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onMainAmountClick();
            }
        });
        view = Utils.findRequiredView(source, R.id.account_exchanged_balance, "field 'accountExchangedBalance' and method 'onLocalAmountClick'");
        target.accountExchangedBalance = (Amount) Utils.castView(view, R.id.account_exchanged_balance, "field 'accountExchangedBalance'", Amount.class);
        this.view2131689678 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onLocalAmountClick();
            }
        });
        target.connectionLabel = (TextView) Utils.findRequiredViewAsType(source, R.id.connection_label, "field 'connectionLabel'", TextView.class);
        target.blockHeight = (TextView) Utils.findRequiredViewAsType(source, R.id.block_height, "field 'blockHeight'", TextView.class);
        target.sponsorView = (SponsorView) Utils.findRequiredViewAsType(source, R.id.sponsor_view, "field 'sponsorView'", SponsorView.class);
    }

    public void unbind() {
        T target = this.target;
        if (target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        target.transactionRows = null;
        target.swipeContainer = null;
        target.emptyPocketMessage = null;
        target.accountBalance = null;
        target.accountExchangedBalance = null;
        target.connectionLabel = null;
        target.blockHeight = null;
        target.sponsorView = null;
        ((AdapterView) this.view2131689677).setOnItemClickListener(null);
        this.view2131689677 = null;
        this.view2131689595.setOnClickListener(null);
        this.view2131689595 = null;
        this.view2131689678.setOnClickListener(null);
        this.view2131689678 = null;
        this.target = null;
    }
}
