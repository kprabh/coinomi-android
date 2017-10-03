package com.coinomi.wallet.ui.dialogs;

import android.view.View;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;
import com.coinomi.wallet.ui.widget.AmountEditView;

public class EditFeeDialog_ViewBinding<T extends EditFeeDialog> implements Unbinder {
    protected T target;

    public EditFeeDialog_ViewBinding(T target, View source) {
        this.target = target;
        target.description = (TextView) Utils.findRequiredViewAsType(source, R.id.fee_description, "field 'description'", TextView.class);
        target.feeAmount = (AmountEditView) Utils.findRequiredViewAsType(source, R.id.fee_amount, "field 'feeAmount'", AmountEditView.class);
    }

    public void unbind() {
        T target = this.target;
        if (target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        target.description = null;
        target.feeAmount = null;
        this.target = null;
    }
}
