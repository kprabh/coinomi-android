package com.coinomi.wallet.ui;

import android.view.View;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;

public class DebuggingFragment_ViewBinding<T extends DebuggingFragment> implements Unbinder {
    protected T target;
    private View view2131689705;

    public DebuggingFragment_ViewBinding(final T target, View source) {
        this.target = target;
        View view = Utils.findRequiredView(source, R.id.button_execute_password_test, "method 'onExecutePasswordTest'");
        this.view2131689705 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onExecutePasswordTest();
            }
        });
    }

    public void unbind() {
        if (this.target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        this.view2131689705.setOnClickListener(null);
        this.view2131689705 = null;
        this.target = null;
    }
}
