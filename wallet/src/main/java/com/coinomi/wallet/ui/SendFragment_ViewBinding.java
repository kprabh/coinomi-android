package com.coinomi.wallet.ui;

import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;
import com.coinomi.wallet.ui.widget.AddressView;
import com.coinomi.wallet.ui.widget.AmountEditView;

public class SendFragment_ViewBinding<T extends SendFragment> implements Unbinder {
    protected T target;
    private View view2131689729;
    private View view2131689744;
    private View view2131689745;
    private View view2131689747;
    private View view2131689756;

    public SendFragment_ViewBinding(final T target, View source) {
        this.target = target;
        target.sendToAddressView = (AutoCompleteTextView) Utils.findRequiredViewAsType(source, R.id.send_to_address, "field 'sendToAddressView'", AutoCompleteTextView.class);
        View view = Utils.findRequiredView(source, R.id.send_to_address_static, "field 'sendToStaticAddressView' and method 'onStaticAddressClick'");
        target.sendToStaticAddressView = (AddressView) Utils.castView(view, R.id.send_to_address_static, "field 'sendToStaticAddressView'", AddressView.class);
        this.view2131689744 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onStaticAddressClick();
            }
        });
        target.sendCoinAmountView = (AmountEditView) Utils.findRequiredViewAsType(source, R.id.send_coin_amount, "field 'sendCoinAmountView'", AmountEditView.class);
        target.sendLocalAmountView = (AmountEditView) Utils.findRequiredViewAsType(source, R.id.send_local_amount, "field 'sendLocalAmountView'", AmountEditView.class);
        target.addressError = (TextView) Utils.findRequiredViewAsType(source, R.id.address_error_message, "field 'addressError'", TextView.class);
        target.amountError = (TextView) Utils.findRequiredViewAsType(source, R.id.amount_error_message, "field 'amountError'", TextView.class);
        target.amountWarning = (TextView) Utils.findRequiredViewAsType(source, R.id.amount_warning_message, "field 'amountWarning'", TextView.class);
        view = Utils.findRequiredView(source, R.id.scan_qr_code, "field 'scanQrCodeButton' and method 'handleScan'");
        target.scanQrCodeButton = (ImageButton) Utils.castView(view, R.id.scan_qr_code, "field 'scanQrCodeButton'", ImageButton.class);
        this.view2131689729 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.handleScan();
            }
        });
        view = Utils.findRequiredView(source, R.id.erase_address, "field 'eraseAddressButton' and method 'onAddressClearClick'");
        target.eraseAddressButton = (ImageButton) Utils.castView(view, R.id.erase_address, "field 'eraseAddressButton'", ImageButton.class);
        this.view2131689745 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onAddressClearClick();
            }
        });
        target.txMessageButton = (Button) Utils.findRequiredViewAsType(source, R.id.tx_message_add_remove, "field 'txMessageButton'", Button.class);
        target.txMessageLabel = (TextView) Utils.findRequiredViewAsType(source, R.id.tx_message_label, "field 'txMessageLabel'", TextView.class);
        target.txMessageView = (EditText) Utils.findRequiredViewAsType(source, R.id.tx_message, "field 'txMessageView'", EditText.class);
        target.txMessageCounter = (TextView) Utils.findRequiredViewAsType(source, R.id.tx_message_counter, "field 'txMessageCounter'", TextView.class);
        view = Utils.findRequiredView(source, R.id.send_confirm, "field 'sendConfirmButton' and method 'onSendClick'");
        target.sendConfirmButton = (Button) Utils.castView(view, R.id.send_confirm, "field 'sendConfirmButton'", Button.class);
        this.view2131689756 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onSendClick();
            }
        });
        view = Utils.findRequiredView(source, R.id.use_all_funds, "method 'onEmptyWalletClick'");
        this.view2131689747 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onEmptyWalletClick();
            }
        });
    }

    public void unbind() {
        T target = this.target;
        if (target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        target.sendToAddressView = null;
        target.sendToStaticAddressView = null;
        target.sendCoinAmountView = null;
        target.sendLocalAmountView = null;
        target.addressError = null;
        target.amountError = null;
        target.amountWarning = null;
        target.scanQrCodeButton = null;
        target.eraseAddressButton = null;
        target.txMessageButton = null;
        target.txMessageLabel = null;
        target.txMessageView = null;
        target.txMessageCounter = null;
        target.sendConfirmButton = null;
        this.view2131689744.setOnClickListener(null);
        this.view2131689744 = null;
        this.view2131689729.setOnClickListener(null);
        this.view2131689729 = null;
        this.view2131689745.setOnClickListener(null);
        this.view2131689745 = null;
        this.view2131689756.setOnClickListener(null);
        this.view2131689756 = null;
        this.view2131689747.setOnClickListener(null);
        this.view2131689747 = null;
        this.target = null;
    }
}
