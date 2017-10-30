package com.coinomi.wallet.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import com.coinomi.wallet.R;
import com.coinomi.wallet.ui.DialogBuilder;

public class ExchangeTermsOfUseDialog extends DialogFragment {
    private Listener listener;

    public interface Listener {
        void onTermsAgree(String str);

        void onTermsDisagree(String str);
    }

    public static ExchangeTermsOfUseDialog newInstance(String exchage) {
        Bundle args = new Bundle();
        args.putString("exchange_id", exchage);
        ExchangeTermsOfUseDialog dialog = new ExchangeTermsOfUseDialog();
        dialog.setArguments(args);
        return dialog;
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof Listener) {
            this.listener = (Listener) activity;
        }
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DialogBuilder builder = new DialogBuilder(getActivity());
        Bundle args = getArguments();
        if (args == null || !args.containsKey("exchange_id")) {
            return builder.create();
        }
        final String exchange = args.getString("exchange_id");
        builder.setTitle((int) R.string.terms_of_service_title);
        Object obj = -1;
        switch (exchange.hashCode()) {
            case 750107009:
                if (exchange.equals("shapeshift")) {
                    builder.setMessage((int) R.string.shapeshift_terms_of_service);
                    break;
                }
                break;
            case 1455272265:
                if (exchange.equals("changelly")) {
                    builder.setMessage((int) R.string.changelly_terms_of_service);
                    break;
                }
                break;
        }

        if (this.listener != null) {
            OnClickListener onClickListener = new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (ExchangeTermsOfUseDialog.this.listener != null) {
                        switch (which) {
                            case -2:
                                ExchangeTermsOfUseDialog.this.listener.onTermsDisagree(exchange);
                                break;
                            case -1:
                                ExchangeTermsOfUseDialog.this.listener.onTermsAgree(exchange);
                                break;
                        }
                    }
                    ExchangeTermsOfUseDialog.this.dismissAllowingStateLoss();
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
