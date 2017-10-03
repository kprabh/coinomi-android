package com.coinomi.wallet.ui;

import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;
import com.coinomi.wallet.ui.widget.AmountEditView;

public class ContractFunctionFragment_ViewBinding<T extends ContractFunctionFragment> implements Unbinder {
    protected T target;
    private View view2131689699;

    public ContractFunctionFragment_ViewBinding(final T target, View source) {
        this.target = target;
        target.functionInputs = (ListView) Utils.findRequiredViewAsType(source, R.id.input_rows, "field 'functionInputs'", ListView.class);
        target.outputHistory = (TextView) Utils.findRequiredViewAsType(source, R.id.function_output_history, "field 'outputHistory'", TextView.class);
        target.description = (TextView) Utils.findRequiredViewAsType(source, R.id.function_description, "field 'description'", TextView.class);
        target.noInputsDescription = (TextView) Utils.findRequiredViewAsType(source, R.id.function_no_inputs_description, "field 'noInputsDescription'", TextView.class);
        target.sendCoinAmountView = (AmountEditView) Utils.findRequiredViewAsType(source, R.id.send_coin_amount, "field 'sendCoinAmountView'", AmountEditView.class);
        View view = Utils.findRequiredView(source, R.id.contract_function_execute, "method 'onExecuteClick'");
        this.view2131689699 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onExecuteClick();
            }
        });
    }

    public void unbind() {
        T target = this.target;
        if (target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        target.functionInputs = null;
        target.outputHistory = null;
        target.description = null;
        target.noInputsDescription = null;
        target.sendCoinAmountView = null;
        this.view2131689699.setOnClickListener(null);
        this.view2131689699 = null;
        this.target = null;
    }
}
