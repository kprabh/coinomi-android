package com.coinomi.wallet.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import com.coinomi.wallet.R;
import com.coinomi.wallet.ui.DialogBuilder;

public class WhatsNew extends DialogFragment {
    private Listener listener;

    public interface Listener {
        void onWhatsNewDismiss();
    }

    public static WhatsNew newInstance() {
        return new WhatsNew();
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            this.listener = (Listener) context;
        }
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DialogBuilder builder = new DialogBuilder(getActivity());
        builder.setTitle((int) R.string.whatsnew);
        builder.setMessage((int) R.string.whatsnewtext);
        if (this.listener != null) {
            builder.setPositiveButton((int) R.string.button_ok, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (WhatsNew.this.listener != null) {
                        switch (which) {
                            case -1:
                                WhatsNew.this.listener.onWhatsNewDismiss();
                                break;
                        }
                    }
                    WhatsNew.this.dismissAllowingStateLoss();
                }
            });
        } else {
            builder.setPositiveButton((int) R.string.button_ok, null);
        }
        return builder.create();
    }
}
