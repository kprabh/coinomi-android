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

public class ContractDetailsFragment_ViewBinding<T extends ContractDetailsFragment> implements Unbinder {
    protected T target;
    private View view2131689693;
    private View view2131689698;

    public ContractDetailsFragment_ViewBinding(final T target, View source) {
        this.target = target;
        target.exchangedBalance = (Amount) Utils.findRequiredViewAsType(source, R.id.contract_exchanged_balance, "field 'exchangedBalance'", Amount.class);
        target.contractBalance = (Amount) Utils.findRequiredViewAsType(source, R.id.contract_balance, "field 'contractBalance'", Amount.class);
        View view = Utils.findRequiredView(source, R.id.output_rows, "field 'contractFunctions' and method 'onItemClick'");
        target.contractFunctions = (ListView) Utils.castView(view, R.id.output_rows, "field 'contractFunctions'", ListView.class);
        this.view2131689693 = view;
        ((AdapterView) view).setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View p1, int p2, long p3) {
                target.onItemClick(p2);
            }
        });
        target.contractDescription = (TextView) Utils.findRequiredViewAsType(source, R.id.contract_description, "field 'contractDescription'", TextView.class);
        target.contractUrl = (TextView) Utils.findRequiredViewAsType(source, R.id.contract_url, "field 'contractUrl'", TextView.class);
        view = Utils.findRequiredView(source, R.id.contract_row_address, "field 'contractAddress' and method 'onContractAddressClick'");
        target.contractAddress = (TextView) Utils.castView(view, R.id.contract_row_address, "field 'contractAddress'", TextView.class);
        this.view2131689698 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onContractAddressClick();
            }
        });
    }

    public void unbind() {
        T target = this.target;
        if (target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        target.exchangedBalance = null;
        target.contractBalance = null;
        target.contractFunctions = null;
        target.contractDescription = null;
        target.contractUrl = null;
        target.contractAddress = null;
        ((AdapterView) this.view2131689693).setOnItemClickListener(null);
        this.view2131689693 = null;
        this.target = null;
    }
}
