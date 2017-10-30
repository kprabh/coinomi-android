package com.coinomi.wallet.util;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import com.coinomi.wallet.R;

public class PoweredByUtil {
    public static String exchangeName(Context context, String id) {
        switch (id.hashCode()) {
            case 750107009:
                if (id.equals("shapeshift")) {
                    return context.getString(R.string.shapeshift);
                }
                break;
            case 1455272265:
                if (id.equals("changelly")) {
                    return context.getString(R.string.changelly);
                }
                break;
        }
        return context.getString(R.string.default_exchange);
    }

    public static void setup(Context context, String id, TextView poweredBy) {
        setup(context, id, poweredBy, true);
    }

    public static void setup(final Context context, String id, TextView poweredBy, boolean setOnClick) {
        int i = -1;
        switch (id.hashCode()) {
            case 750107009:
                if (id.equals("shapeshift")) {
                    i = 0;
                    break;
                }
                break;
            case 1455272265:
                if (id.equals("changelly")) {
                    i = 1;
                    break;
                }
                break;
        }
        switch (i) {
            case 0:
                poweredBy.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.shapeshift, 0);
                if (setOnClick) {
                    poweredBy.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            new Builder(context).setTitle(R.string.about_shapeshift_title).setMessage(R.string.about_shapeshift_message).setPositiveButton(R.string.button_ok, null).create().show();
                        }
                    });
                    return;
                }
                return;
            case 1:
                poweredBy.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.changelly, 0);
                if (setOnClick) {
                    poweredBy.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            new Builder(context).setTitle(R.string.about_changelly_title).setMessage(R.string.about_changelly_message).setPositiveButton(R.string.button_ok, null).create().show();
                        }
                    });
                    return;
                }
                return;
            default:
                return;
        }
    }

    public static void setExchangeName(Context context, String id, TextView textView) {
        textView.setText(exchangeName(context, id));
    }
}
