package com.coinomi.wallet.ui.widget;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;

public class CoinListItem_ViewBinding<T extends CoinListItem> implements Unbinder {
    protected T target;

    public CoinListItem_ViewBinding(T target, View source) {
        this.target = target;
        target.icon = (ImageView) Utils.findRequiredViewAsType(source, R.id.item_icon, "field 'icon'", ImageView.class);
        target.title = (TextView) Utils.findRequiredViewAsType(source, R.id.item_text, "field 'title'", TextView.class);
        target.amount = (Amount) Utils.findRequiredViewAsType(source, R.id.amount, "field 'amount'", Amount.class);
    }

    public void unbind() {
        T target = this.target;
        if (target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        target.icon = null;
        target.title = null;
        target.amount = null;
        this.target = null;
    }
}
