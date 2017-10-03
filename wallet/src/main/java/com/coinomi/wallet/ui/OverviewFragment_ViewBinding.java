package com.coinomi.wallet.ui;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;
import com.coinomi.wallet.ui.widget.Amount;
import com.coinomi.wallet.ui.widget.SponsorView;
import com.coinomi.wallet.ui.widget.SwipeRefreshLayout;

public class OverviewFragment_ViewBinding<T extends OverviewFragment> implements Unbinder {
    protected T target;
    private View view2131689595;
    private View view2131689716;

    public OverviewFragment_ViewBinding(final T target, View source) {
        this.target = target;
        target.swipeContainer = (SwipeRefreshLayout) Utils.findRequiredViewAsType(source, R.id.swipeContainer, "field 'swipeContainer'", SwipeRefreshLayout.class);
        View view = Utils.findRequiredView(source, R.id.account_rows, "field 'accountRows', method 'onAccountClick', and method 'onAccountLongClick'");
        target.accountRows = (ListView) Utils.castView(view, R.id.account_rows, "field 'accountRows'", ListView.class);
        this.view2131689716 = view;
        ((AdapterView) view).setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View p1, int p2, long p3) {
                target.onAccountClick(p2);
            }
        });
        ((AdapterView) view).setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> adapterView, View p1, int p2, long p3) {
                return target.onAccountLongClick(p2);
            }
        });
        view = Utils.findRequiredView(source, R.id.account_balance, "field 'mainAmount' and method 'onMainAmountClick'");
        target.mainAmount = (Amount) Utils.castView(view, R.id.account_balance, "field 'mainAmount'", Amount.class);
        this.view2131689595 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onMainAmountClick(p0);
            }
        });
        target.sponsorView = (SponsorView) Utils.findRequiredViewAsType(source, R.id.sponsor_view, "field 'sponsorView'", SponsorView.class);
    }

    public void unbind() {
        T target = this.target;
        if (target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        target.swipeContainer = null;
        target.accountRows = null;
        target.mainAmount = null;
        target.sponsorView = null;
        ((AdapterView) this.view2131689716).setOnItemClickListener(null);
        ((AdapterView) this.view2131689716).setOnItemLongClickListener(null);
        this.view2131689716 = null;
        this.view2131689595.setOnClickListener(null);
        this.view2131689595 = null;
        this.target = null;
    }
}
