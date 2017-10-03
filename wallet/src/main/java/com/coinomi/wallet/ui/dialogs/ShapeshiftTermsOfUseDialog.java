package com.coinomi.wallet.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import com.coinomi.wallet.R;
import com.coinomi.wallet.ui.DialogBuilder;

public class ShapeshiftTermsOfUseDialog extends DialogFragment {
    private Listener listener;

    public interface Listener {
        void onTermsAgree();

        void onTermsDisagree();
    }

    public static ShapeshiftTermsOfUseDialog newInstance() {
        return new ShapeshiftTermsOfUseDialog();
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof Listener) {
            this.listener = (Listener) activity;
        }
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DialogBuilder builder = new DialogBuilder(getActivity());
        builder.setTitle((int) R.string.terms_of_service_title);
        builder.setMessage((int) R.string.shapeshift_terms_of_service);
        if (this.listener != null) {
            OnClickListener onClickListener = new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (ShapeshiftTermsOfUseDialog.this.listener != null) {
                        switch (which) {
                            case -2:
                                ShapeshiftTermsOfUseDialog.this.listener.onTermsDisagree();
                                break;
                            case -1:
                                ShapeshiftTermsOfUseDialog.this.listener.onTermsAgree();
                                break;
                        }
                    }
                    ShapeshiftTermsOfUseDialog.this.dismissAllowingStateLoss();
                }
            };
            builder.setNegativeButton((int) R.string.button_disagree, onClickListener);
            builder.setPositiveButton((int) R.string.button_agree, onClickListener);
        } else {
            builder.setPositiveButton((int) R.string.button_ok, null);
        }
        return builder.create();
    }
}
