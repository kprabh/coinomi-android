package com.coinomi.wallet.ui;

import android.support.v4.view.ViewPager;
import android.view.View;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;

public class AccountFragment_ViewBinding<T extends AccountFragment> implements Unbinder {
    protected T target;

    public AccountFragment_ViewBinding(T target, View source) {
        this.target = target;
        target.viewPager = (ViewPager) Utils.findRequiredViewAsType(source, R.id.pager, "field 'viewPager'", ViewPager.class);
    }

    public void unbind() {
        T target = this.target;
        if (target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        target.viewPager = null;
        this.target = null;
    }
}
