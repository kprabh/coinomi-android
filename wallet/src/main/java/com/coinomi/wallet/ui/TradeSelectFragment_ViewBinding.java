package com.coinomi.wallet.ui;

import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;
import com.coinomi.wallet.ui.widget.AmountEditView;

public class TradeSelectFragment_ViewBinding<T extends TradeSelectFragment> implements Unbinder {
    protected T target;
    private View view2131689751;
    private View view2131689801;

    public TradeSelectFragment_ViewBinding(final T target, View source) {
        this.target = target;
        target.tradeInfo = (TextView) Utils.findRequiredViewAsType(source, R.id.trade_info_rates, "field 'tradeInfo'", TextView.class);
        target.poweredBy = (TextView) Utils.findRequiredViewAsType(source, R.id.powered_by, "field 'poweredBy'", TextView.class);
        target.sourceSpinner = (Spinner) Utils.findRequiredViewAsType(source, R.id.from_coin, "field 'sourceSpinner'", Spinner.class);
        target.destinationSpinner = (Spinner) Utils.findRequiredViewAsType(source, R.id.to_coin, "field 'destinationSpinner'", Spinner.class);
        target.sourceAmountView = (AmountEditView) Utils.findRequiredViewAsType(source, R.id.trade_coin_amount, "field 'sourceAmountView'", AmountEditView.class);
        target.destinationAmountView = (AmountEditView) Utils.findRequiredViewAsType(source, R.id.receive_coin_amount, "field 'destinationAmountView'", AmountEditView.class);
        target.amountError = (TextView) Utils.findRequiredViewAsType(source, R.id.amount_error_message, "field 'amountError'", TextView.class);
        target.amountWarning = (TextView) Utils.findRequiredViewAsType(source, R.id.amount_warning_message, "field 'amountWarning'", TextView.class);
        target.nextButton = (Button) Utils.findRequiredViewAsType(source, R.id.button_next, "field 'nextButton'", Button.class);
        View view = Utils.findRequiredView(source, R.id.use_all_funds, "method 'onEmptyWalletClick'");
        this.view2131689751 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onEmptyWalletClick();
            }
        });
        view = Utils.findRequiredView(source, R.id.precise, "method 'onPreciseClick'");
        this.view2131689801 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onPreciseClick();
            }
        });
    }

    public void unbind() {
        T target = this.target;
        if (target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        target.tradeInfo = null;
        target.poweredBy = null;
        target.sourceSpinner = null;
        target.destinationSpinner = null;
        target.sourceAmountView = null;
        target.destinationAmountView = null;
        target.amountError = null;
        target.amountWarning = null;
        target.nextButton = null;
        this.view2131689751.setOnClickListener(null);
        this.view2131689751 = null;
        this.view2131689801.setOnClickListener(null);
        this.view2131689801 = null;
        this.target = null;
    }
}
