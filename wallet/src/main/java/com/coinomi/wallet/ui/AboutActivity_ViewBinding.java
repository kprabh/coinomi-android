package com.coinomi.wallet.ui;

import android.view.View;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;

public class AboutActivity_ViewBinding<T extends AboutActivity> implements Unbinder {
    protected T target;
    private View view2131689599;
    private View view2131689600;

    public AboutActivity_ViewBinding(final T target, View source) {
        this.target = target;
        View view = Utils.findRequiredView(source, R.id.terms_of_service_button, "method 'onTermsOfUseClick'");
        this.view2131689599 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onTermsOfUseClick();
            }
        });
        view = Utils.findRequiredView(source, R.id.whats_new_button, "method 'onWhatsNewClick'");
        this.view2131689600 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onWhatsNewClick();
            }
        });
    }

    public void unbind() {
        if (this.target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        this.view2131689599.setOnClickListener(null);
        this.view2131689599 = null;
        this.view2131689600.setOnClickListener(null);
        this.view2131689600 = null;
        this.target = null;
    }
}
