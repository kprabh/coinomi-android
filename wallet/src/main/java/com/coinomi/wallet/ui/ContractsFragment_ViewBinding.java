package com.coinomi.wallet.ui;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;

public class ContractsFragment_ViewBinding<T extends ContractsFragment> implements Unbinder {
    protected T target;
    private View view2131689704;

    public ContractsFragment_ViewBinding(final T target, View source) {
        this.target = target;
        View view = Utils.findRequiredView(source, R.id.contract_rows, "field 'contractRows' and method 'onItemClick'");
        target.contractRows = (ListView) Utils.castView(view, R.id.contract_rows, "field 'contractRows'", ListView.class);
        this.view2131689704 = view;
        ((AdapterView) view).setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View p1, int p2, long p3) {
                target.onItemClick(p2);
            }
        });
    }

    public void unbind() {
        T target = this.target;
        if (target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        target.contractRows = null;
        ((AdapterView) this.view2131689704).setOnItemClickListener(null);
        this.view2131689704 = null;
        this.target = null;
    }
}
