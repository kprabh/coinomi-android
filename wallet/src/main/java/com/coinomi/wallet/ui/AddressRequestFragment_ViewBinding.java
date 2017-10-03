package com.coinomi.wallet.ui;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;
import com.coinomi.wallet.ui.widget.AmountEditView;

public class AddressRequestFragment_ViewBinding<T extends AddressRequestFragment> implements Unbinder {
    protected T target;
    private View view2131689719;
    private View view2131689722;
    private View view2131689728;

    public AddressRequestFragment_ViewBinding(final T target, View source) {
        this.target = target;
        target.addressLabelView = (TextView) Utils.findRequiredViewAsType(source, R.id.request_address_label, "field 'addressLabelView'", TextView.class);
        target.addressView = (TextView) Utils.findRequiredViewAsType(source, R.id.request_address, "field 'addressView'", TextView.class);
        View view = Utils.findRequiredView(source, R.id.convert_to_btc_address, "field 'convertButtonView' and method 'onConvertClick'");
        target.convertButtonView = (Button) Utils.castView(view, R.id.convert_to_btc_address, "field 'convertButtonView'", Button.class);
        this.view2131689722 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onConvertClick();
            }
        });
        target.sendCoinAmountView = (AmountEditView) Utils.findRequiredViewAsType(source, R.id.request_coin_amount, "field 'sendCoinAmountView'", AmountEditView.class);
        view = Utils.findRequiredView(source, R.id.view_previous_addresses, "field 'previousAddressesLink' and method 'onPreviousAddressesClick'");
        target.previousAddressesLink = view;
        this.view2131689728 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onPreviousAddressesClick();
            }
        });
        target.qrView = (ImageView) Utils.findRequiredViewAsType(source, R.id.qr_code, "field 'qrView'", ImageView.class);
        view = Utils.findRequiredView(source, R.id.request_address_view, "method 'onAddressClick'");
        this.view2131689719 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onAddressClick();
            }
        });
    }

    public void unbind() {
        T target = this.target;
        if (target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        target.addressLabelView = null;
        target.addressView = null;
        target.convertButtonView = null;
        target.sendCoinAmountView = null;
        target.previousAddressesLink = null;
        target.qrView = null;
        this.view2131689722.setOnClickListener(null);
        this.view2131689722 = null;
        this.view2131689728.setOnClickListener(null);
        this.view2131689728 = null;
        this.view2131689719.setOnClickListener(null);
        this.view2131689719 = null;
        this.target = null;
    }
}
