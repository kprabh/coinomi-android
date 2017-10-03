package com.coinomi.wallet.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;

public class SweepWalletFragment_ViewBinding<T extends SweepWalletFragment> implements Unbinder {
    protected T target;
    private View view2131689685;
    private View view2131689729;
    private View view2131689778;
    private TextWatcher view2131689778TextWatcher;

    public SweepWalletFragment_ViewBinding(final T target, View source) {
        this.target = target;
        target.privateKeyInputView = Utils.findRequiredView(source, R.id.private_key_input, "field 'privateKeyInputView'");
        View view = Utils.findRequiredView(source, R.id.sweep_wallet_key, "field 'privateKeyText', method 'onPrivateKeyInputFocusChange', and method 'onPrivateKeyInputTextChange'");
        target.privateKeyText = (EditText) Utils.castView(view, R.id.sweep_wallet_key, "field 'privateKeyText'", EditText.class);
        this.view2131689778 = view;
        view.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View p0, boolean p1) {
                target.onPrivateKeyInputFocusChange(p1);
            }
        });
        this.view2131689778TextWatcher = new TextWatcher() {
            public void onTextChanged(CharSequence p0, int p1, int p2, int p3) {
            }

            public void beforeTextChanged(CharSequence p0, int p1, int p2, int p3) {
            }

            public void afterTextChanged(Editable p0) {
                target.onPrivateKeyInputTextChange();
            }
        };
        ((TextView) view).addTextChangedListener(this.view2131689778TextWatcher);
        target.passwordView = Utils.findRequiredView(source, R.id.passwordView, "field 'passwordView'");
        target.errorΜessage = (TextView) Utils.findRequiredViewAsType(source, R.id.sweep_error, "field 'errorΜessage'", TextView.class);
        target.password = (EditText) Utils.findRequiredViewAsType(source, R.id.passwordInput, "field 'password'", EditText.class);
        target.sweepLoadingView = Utils.findRequiredView(source, R.id.sweep_loading, "field 'sweepLoadingView'");
        target.sweepStatus = (TextView) Utils.findRequiredViewAsType(source, R.id.sweeping_status, "field 'sweepStatus'", TextView.class);
        view = Utils.findRequiredView(source, R.id.button_next, "field 'nextButton' and method 'verifyKeyAndProceed'");
        target.nextButton = (Button) Utils.castView(view, R.id.button_next, "field 'nextButton'", Button.class);
        this.view2131689685 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.verifyKeyAndProceed();
            }
        });
        view = Utils.findRequiredView(source, R.id.scan_qr_code, "method 'handleScan'");
        this.view2131689729 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.handleScan();
            }
        });
    }

    public void unbind() {
        T target = this.target;
        if (target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        target.privateKeyInputView = null;
        target.privateKeyText = null;
        target.passwordView = null;
        target.errorΜessage = null;
        target.password = null;
        target.sweepLoadingView = null;
        target.sweepStatus = null;
        target.nextButton = null;
        this.view2131689778.setOnFocusChangeListener(null);
        ((TextView) this.view2131689778).removeTextChangedListener(this.view2131689778TextWatcher);
        this.view2131689778TextWatcher = null;
        this.view2131689778 = null;
        this.view2131689685.setOnClickListener(null);
        this.view2131689685 = null;
        this.view2131689729.setOnClickListener(null);
        this.view2131689729 = null;
        this.target = null;
    }
}
