package com.coinomi.wallet.ui;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;
import com.coinomi.wallet.ui.widget.SendOutput;
import com.coinomi.wallet.ui.widget.TransactionAmountVisualizer;

public class MakeTransactionFragment_ViewBinding<T extends MakeTransactionFragment> implements Unbinder {
    protected T target;
    private View view2131689710;
    private View view2131689714;

    public MakeTransactionFragment_ViewBinding(final T target, View source) {
        this.target = target;
        target.changeFeesView = Utils.findRequiredView(source, R.id.change_fee_layout, "field 'changeFeesView'");
        View view = Utils.findRequiredView(source, R.id.fee_priority_spinner, "field 'feePriority' and method 'onFeePrioritySelected'");
        target.feePriority = (Spinner) Utils.castView(view, R.id.fee_priority_spinner, "field 'feePriority'", Spinner.class);
        this.view2131689710 = view;
        ((AdapterView) view).setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p0, View p1, int p2, long p3) {
                target.onFeePrioritySelected(p0, p1, p2, p3);
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        target.transactionInfo = (TextView) Utils.findRequiredViewAsType(source, R.id.transaction_info, "field 'transactionInfo'", TextView.class);
        target.passwordView = (EditText) Utils.findRequiredViewAsType(source, R.id.password, "field 'passwordView'", EditText.class);
        target.txVisualizer = (TransactionAmountVisualizer) Utils.findRequiredViewAsType(source, R.id.transaction_amount_visualizer, "field 'txVisualizer'", TransactionAmountVisualizer.class);
        target.tradeWithdrawSendOutput = (SendOutput) Utils.findRequiredViewAsType(source, R.id.transaction_trade_withdraw, "field 'tradeWithdrawSendOutput'", SendOutput.class);
        view = Utils.findRequiredView(source, R.id.button_confirm, "method 'onConfirmClick'");
        this.view2131689714 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onConfirmClick();
            }
        });
    }

    public void unbind() {
        T target = this.target;
        if (target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        target.changeFeesView = null;
        target.feePriority = null;
        target.transactionInfo = null;
        target.passwordView = null;
        target.txVisualizer = null;
        target.tradeWithdrawSendOutput = null;
        ((AdapterView) this.view2131689710).setOnItemSelectedListener(null);
        this.view2131689710 = null;
        this.view2131689714.setOnClickListener(null);
        this.view2131689714 = null;
        this.target = null;
    }
}
