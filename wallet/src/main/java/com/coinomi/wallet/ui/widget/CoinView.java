package com.coinomi.wallet.ui.widget;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.coinomi.wallet.R;
import com.squareup.picasso.Picasso;

public class CoinView extends LinearLayout implements Checkable {
    private int color = 0;
    private final ImageView icon;
    private final View ident;
    private boolean isChecked = false;
    private final ImageView subIcon;
    private final TextView title;
    private final View view;

    public CoinView(Context context) {
        super(context);
        this.view = LayoutInflater.from(context).inflate(R.layout.coin_view, this, true);
        this.title = (TextView) findViewById(R.id.item_text);
        this.icon = (ImageView) findViewById(R.id.item_icon);
        this.subIcon = (ImageView) findViewById(R.id.item_sub_icon);
        this.ident = findViewById(R.id.item_indent);
    }

    public void setData(String titleStr, int iconRes) {
        this.title.setText(titleStr);
        this.icon.setImageResource(iconRes);
    }

    public void setNormalData(String titleStr, String iconBase) {
        this.title.setText(titleStr);
        if (!(iconBase == null || iconBase.isEmpty())) {
            if (iconBase.startsWith("data:image")) {
                byte[] decodedString = Base64.decode(iconBase.replace("data:image/png;base64,", ""), 0);
                this.icon.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
            } else {
                Picasso.with(getContext()).load(iconBase).into(this.icon);
            }
        }
        this.icon.setVisibility(View.VISIBLE);
        this.subIcon.setVisibility(View.GONE);
    }

    public void setChecked(boolean checked) {
        this.isChecked = checked;
        if (this.isChecked) {
            this.view.setBackgroundResource(R.color.primary_100);
        } else {
            this.view.setBackgroundResource(this.color);
        }
    }

    public boolean isChecked() {
        return this.isChecked;
    }

    public void toggle() {
        setChecked(!this.isChecked);
    }
}
