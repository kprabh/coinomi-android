package com.coinomi.wallet.ui;

import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;

public class AddContractFragment_ViewBinding<T extends AddContractFragment> implements Unbinder {
    protected T target;
    private View view2131689675;

    public AddContractFragment_ViewBinding(final T target, View source) {
        this.target = target;
        target.contractAbi = (EditText) Utils.findRequiredViewAsType(source, R.id.add_contract_abi, "field 'contractAbi'", EditText.class);
        target.contractSuit = (Spinner) Utils.findRequiredViewAsType(source, R.id.add_contract_suit, "field 'contractSuit'", Spinner.class);
        target.contractName = (EditText) Utils.findRequiredViewAsType(source, R.id.add_contract_name, "field 'contractName'", EditText.class);
        target.contractAddress = (EditText) Utils.findRequiredViewAsType(source, R.id.add_contract_address, "field 'contractAddress'", EditText.class);
        target.contractWebsite = (EditText) Utils.findRequiredViewAsType(source, R.id.add_contract_website, "field 'contractWebsite'", EditText.class);
        target.contractDescription = (EditText) Utils.findRequiredViewAsType(source, R.id.add_contract_description, "field 'contractDescription'", EditText.class);
        View view = Utils.findRequiredView(source, R.id.add_contract_button, "method 'onItemClick'");
        this.view2131689675 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onItemClick();
            }
        });
    }

    public void unbind() {
        T target = this.target;
        if (target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        target.contractAbi = null;
        target.contractSuit = null;
        target.contractName = null;
        target.contractAddress = null;
        target.contractWebsite = null;
        target.contractDescription = null;
        this.view2131689675.setOnClickListener(null);
        this.view2131689675 = null;
        this.target = null;
    }
}
