package com.coinomi.wallet.ui.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.coinomi.sponsor.Sponsors;
import com.coinomi.sponsor.Sponsors.Sponsor;
import com.coinomi.wallet.R;
import com.coinomi.wallet.util.NetworkUtils;
import com.squareup.picasso.Picasso;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Locale;
import okhttp3.OkHttpClient;
import okhttp3.Request.Builder;
import okhttp3.Response;

public class SponsorView extends LinearLayout {
    private TextView aboutText = ((TextView) findViewById(R.id.sponsor_about));
    private final int defaultBg;
    private final int defaultFg;
    private ImageView imageView = ((ImageView) findViewById(R.id.sponsor_image));
    private TextView primaryText = ((TextView) findViewById(R.id.sponsor_primary));
    private TextView secondaryText = ((TextView) findViewById(R.id.sponsor_secondary));
    private Sponsors sponsors;
    private GetSponsorTask task;

    static class GetSponsorTask extends AsyncTask<Void, Void, Sponsors> {
        private final OkHttpClient client;
        private final String id;
        private WeakReference<SponsorView> viewRef;

        GetSponsorTask(SponsorView view, String id, OkHttpClient client) {
            this.viewRef = new WeakReference(view);
            this.id = id;
            this.client = client;
        }

        protected Sponsors doInBackground(Void... params) {
            Builder reqBuilder = new Builder().url("https://configuration.coinomi.com/sponsors/wallet.json");
            String language = Locale.getDefault().getLanguage();
            if (!language.isEmpty()) {
                reqBuilder = reqBuilder.header("Accept-Language", language);
            }
            try {
                Response response = this.client.newCall(reqBuilder.build()).execute();
                if (response.isSuccessful()) {
                    return Sponsors.parse(response.body().string());
                }
                throw new IOException("Unexpected code " + response);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public void onPostExecute(Sponsors sponsors) {
            SponsorView view = (SponsorView) this.viewRef.get();
            if (view != null) {
                view.sponsors = sponsors;
                view.updateView(this.id);
                view.task = null;
            }
        }
    }

    public SponsorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = getContext().getResources();
        this.defaultFg = res.getColor(R.color.sponsor_fg);
        this.defaultBg = res.getColor(R.color.sponsor_bg);
        LayoutInflater.from(context).inflate(R.layout.sponsor, this, true);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Sponsor, 0, 0);
        try {
            if (a.getBoolean(0, false)) {
                int size = (int) res.getDimension(R.dimen.row_font_icon_bg_size);
                this.imageView.getLayoutParams().height = size;
                this.imageView.getLayoutParams().width = size;
                if (this.imageView.getLayoutParams() instanceof MarginLayoutParams) {
                    int margin = (int) res.getDimension(R.dimen.row_font_icon_margin);
                    MarginLayoutParams params = (MarginLayoutParams) this.imageView.getLayoutParams();
                    params.leftMargin = margin;
                    params.rightMargin = margin;
                    if (VERSION.SDK_INT >= 17) {
                        params.setMarginStart(margin);
                        params.setMarginEnd(margin);
                    }
                }
            }
            a.recycle();
        } catch (Throwable th) {
            a.recycle();
        }
    }

    public void onAttachedToWindow() {
        if (this.task != null) {
            this.task.execute(new Void[0]);
        }
        super.onAttachedToWindow();
    }

    public void onDetachedFromWindow() {
        if (this.task != null) {
            this.task.cancel(true);
            this.task = null;
        }
        super.onDetachedFromWindow();
    }

    public void setup(String id) {
        if (this.task == null) {
            this.task = new GetSponsorTask(this, id, NetworkUtils.getHttpClient(getContext()));
        }
    }

    private void updateView(String id) {
        if (this.sponsors != null) {
            try {
                Sponsor sponsor = this.sponsors.getRandomSponsor(id);
                if (sponsor != null) {
                    setSponsor(sponsor);
                    setVisibility(VISIBLE);
                    return;
                }
                setVisibility(GONE);
            } catch (Exception e) {
                setVisibility(GONE);
            }
        }
    }

    private void setSponsor(final Sponsor sponsor) {
        if (!sponsor.isSponsor) {
            this.aboutText.setVisibility(GONE);
        }
        this.primaryText.setText(sponsor.primary);
        if (sponsor.secondary != null) {
            this.secondaryText.setText(sponsor.secondary);
            this.secondaryText.setVisibility(VISIBLE);
        } else {
            this.secondaryText.setVisibility(GONE);
        }
        if (sponsor.link != null) {
            setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    SponsorView.this.getContext().startActivity(new Intent("android.intent.action.VIEW", Uri.parse(sponsor.link.toString())));
                }
            });
        } else {
            setOnClickListener(null);
        }
        if (sponsor.image != null) {
            Picasso.with(getContext()).load(sponsor.image.toString()).into(this.imageView);
            this.imageView.setVisibility(VISIBLE);
        } else {
            this.imageView.setVisibility(INVISIBLE);
        }
        if (sponsor.colorFg != null) {
            this.aboutText.setTextColor(sponsor.colorFg.intValue());
            this.primaryText.setTextColor(sponsor.colorFg.intValue());
            this.secondaryText.setTextColor(sponsor.colorFg.intValue());
        } else {
            this.aboutText.setTextColor(this.defaultFg);
            this.primaryText.setTextColor(this.defaultFg);
            this.secondaryText.setTextColor(this.defaultFg);
        }
        if (sponsor.colorBg != null) {
            setBackgroundColor(sponsor.colorBg.intValue());
        } else {
            setBackgroundColor(this.defaultBg);
        }
    }
}
