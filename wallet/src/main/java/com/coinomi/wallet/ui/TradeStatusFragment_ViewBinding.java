package com.coinomi.wallet.ui;

import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;
import com.coinomi.wallet.ui.widget.AddressView;

public class TradeStatusFragment_ViewBinding<T extends TradeStatusFragment> implements Unbinder {
    protected T target;

    public TradeStatusFragment_ViewBinding(T target, View source) {
        this.target = target;
        target.exchangeInfo = (TextView) Utils.findRequiredViewAsType(source, R.id.exchange_status_info, "field 'exchangeInfo'", TextView.class);
        target.depositIcon = (TextView) Utils.findRequiredViewAsType(source, R.id.trade_deposit_status_icon, "field 'depositIcon'", TextView.class);
        target.depositProgress = (ProgressBar) Utils.findRequiredViewAsType(source, R.id.trade_deposit_status_progress, "field 'depositProgress'", ProgressBar.class);
        target.depositText = (TextView) Utils.findRequiredViewAsType(source, R.id.trade_deposit_status_text, "field 'depositText'", TextView.class);
        target.depositAddress = (AddressView) Utils.findRequiredViewAsType(source, R.id.trade_deposit_address, "field 'depositAddress'", AddressView.class);
        target.exchangeWithdrawView = Utils.findRequiredView(source, R.id.trade_exchange_withdraw_view, "field 'exchangeWithdrawView'");
        target.exchangeIcon = (TextView) Utils.findRequiredViewAsType(source, R.id.trade_exchange_status_icon, "field 'exchangeIcon'", TextView.class);
        target.exchangeProgress = (ProgressBar) Utils.findRequiredViewAsType(source, R.id.trade_exchange_status_progress, "field 'exchangeProgress'", ProgressBar.class);
        target.exchangeText = (TextView) Utils.findRequiredViewAsType(source, R.id.trade_exchange_status_text, "field 'exchangeText'", TextView.class);
        target.exchangeAddress = (AddressView) Utils.findRequiredViewAsType(source, R.id.trade_exchange_address, "field 'exchangeAddress'", AddressView.class);
        target.errorIcon = (TextView) Utils.findRequiredViewAsType(source, R.id.trade_error_status_icon, "field 'errorIcon'", TextView.class);
        target.errorText = (TextView) Utils.findRequiredViewAsType(source, R.id.trade_error_status_text, "field 'errorText'", TextView.class);
        target.viewTransaction = (Button) Utils.findRequiredViewAsType(source, R.id.trade_view_transaction, "field 'viewTransaction'", Button.class);
        target.emailReceipt = (Button) Utils.findRequiredViewAsType(source, R.id.trade_email_receipt, "field 'emailReceipt'", Button.class);
    }

    public void unbind() {
        T target = this.target;
        if (target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        target.exchangeInfo = null;
        target.depositIcon = null;
        target.depositProgress = null;
        target.depositText = null;
        target.depositAddress = null;
        target.exchangeWithdrawView = null;
        target.exchangeIcon = null;
        target.exchangeProgress = null;
        target.exchangeText = null;
        target.exchangeAddress = null;
        target.errorIcon = null;
        target.errorText = null;
        target.viewTransaction = null;
        target.emailReceipt = null;
        this.target = null;
    }
}
