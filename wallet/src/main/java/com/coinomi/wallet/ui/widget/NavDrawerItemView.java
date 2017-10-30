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

import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.wallet.Constants;
import com.coinomi.wallet.R;
import com.coinomi.wallet.util.WalletUtils;
import com.squareup.picasso.Picasso;

/**
 * @author John L. Jegutanis
 */
public class NavDrawerItemView extends LinearLayout implements Checkable {
    private int color = 0;
    private final TextView title;
    private final ImageView icon;
    private final View view;
    private final ImageView subIcon;
    private final View ident;

    private boolean isChecked = false;

    public NavDrawerItemView(Context context) {
        super(context);

        view = LayoutInflater.from(context).inflate(R.layout.nav_drawer_item, this, true);
        title = (TextView) findViewById(R.id.item_text);
        icon = (ImageView) findViewById(R.id.item_icon);
        subIcon = (ImageView) findViewById(R.id.item_sub_icon);
        ident = findViewById(R.id.item_indent);
    }

    public void setData(String titleStr, int iconRes) {
        title.setText(titleStr);
        icon.setImageResource(iconRes);
    }

    public void setData(String titleStr, String iconBase) {
        this.title.setText(titleStr);
        if (!(iconBase == null || iconBase.isEmpty())) {
            if (iconBase.startsWith("data:image")) {
                byte[] decodedString = Base64.decode(iconBase.replace("data:image/png;base64,", ""), 0);
                this.subIcon.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
            } else {
                Picasso.with(getContext()).load(iconBase).into(this.subIcon);
            }
        }
        this.icon.setVisibility(8);
        this.subIcon.setVisibility(0);
    }

    @Override
    public void setChecked(boolean checked) {
        isChecked = checked;

        if (isChecked) {
            view.setBackgroundResource(R.color.primary_100);
        } else {
            view.setBackgroundResource(this.color);
        }
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public void toggle() {
        setChecked(!isChecked);
    }

    public void setIdent() {
        this.ident.setVisibility(View.VISIBLE);
        this.color = R.color.bg_sub_type;
        this.view.setBackgroundResource(this.color);
    }
}
