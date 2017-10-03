package com.coinomi.wallet.ui;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;

public class FeesSettingsFragment_ViewBinding<T extends FeesSettingsFragment> implements Unbinder {
    protected T target;
    private View view2131689706;

    public FeesSettingsFragment_ViewBinding(final T target, View source) {
        this.target = target;
        View view = Utils.findRequiredView(source, R.id.coins_list, "field 'coinList' and method 'editFee'");
        target.coinList = (ListView) Utils.castView(view, R.id.coins_list, "field 'coinList'", ListView.class);
        this.view2131689706 = view;
        ((AdapterView) view).setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View p1, int p2, long p3) {
                target.editFee(p2);
            }
        });
    }

    public void unbind() {
        T target = this.target;
        if (target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        target.coinList = null;
        ((AdapterView) this.view2131689706).setOnItemClickListener(null);
        this.view2131689706 = null;
        this.target = null;
    }
}
