package com.coinomi.wallet.ui;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;

public class SetPasswordFragment_ViewBinding<T extends SetPasswordFragment> implements Unbinder {
    protected T target;

    public SetPasswordFragment_ViewBinding(T target, View source) {
        this.target = target;
        target.password1 = (EditText) Utils.findRequiredViewAsType(source, R.id.password1, "field 'password1'", EditText.class);
        target.password2 = (EditText) Utils.findRequiredViewAsType(source, R.id.password2, "field 'password2'", EditText.class);
        target.errorPassword = (TextView) Utils.findRequiredViewAsType(source, R.id.password_error, "field 'errorPassword'", TextView.class);
        target.errorPasswordsMismatch = (TextView) Utils.findRequiredViewAsType(source, R.id.passwords_mismatch, "field 'errorPasswordsMismatch'", TextView.class);
    }

    public void unbind() {
        T target = this.target;
        if (target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        target.password1 = null;
        target.password2 = null;
        target.errorPassword = null;
        target.errorPasswordsMismatch = null;
        this.target = null;
    }
}
