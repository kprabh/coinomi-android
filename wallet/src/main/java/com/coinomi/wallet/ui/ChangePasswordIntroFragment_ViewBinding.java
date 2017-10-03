package com.coinomi.wallet.ui;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;

public class ChangePasswordIntroFragment_ViewBinding<T extends ChangePasswordIntroFragment> implements Unbinder {
    protected T target;
    private View view2131689684;
    private View view2131689685;

    public ChangePasswordIntroFragment_ViewBinding(final T target, View source) {
        this.target = target;
        target.password = (EditText) Utils.findRequiredViewAsType(source, R.id.password, "field 'password'", EditText.class);
        target.passwordError = (TextView) Utils.findRequiredViewAsType(source, R.id.password_error, "field 'passwordError'", TextView.class);
        View view = Utils.findRequiredView(source, R.id.button_next, "method 'OnNextClick'");
        this.view2131689685 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.OnNextClick();
            }
        });
        view = Utils.findRequiredView(source, R.id.forgot_password, "method 'OnForgotPasswordClick'");
        this.view2131689684 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.OnForgotPasswordClick();
            }
        });
    }

    public void unbind() {
        T target = this.target;
        if (target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        target.password = null;
        target.passwordError = null;
        this.view2131689685.setOnClickListener(null);
        this.view2131689685 = null;
        this.view2131689684.setOnClickListener(null);
        this.view2131689684 = null;
        this.target = null;
    }
}
