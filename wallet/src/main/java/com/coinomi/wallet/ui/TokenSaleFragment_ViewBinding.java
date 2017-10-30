package com.coinomi.wallet.ui;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;
import com.coinomi.wallet.ui.widget.AmountEditView;

public class TokenSaleFragment_ViewBinding<T extends TokenSaleFragment> implements Unbinder {
    protected T target;
    private View view2131689751;
    private View view2131689789;
    private View view2131689790;
    private View view2131689792;
    private View view2131689794;
    private View view2131689797;
    private View view2131689798;

    public TokenSaleFragment_ViewBinding(final T target, View source) {
        this.target = target;
        View view = Utils.findRequiredView(source, R.id.from_coin, "field 'sourceSpinner' and method 'onFromSelected'");
        target.sourceSpinner = (Spinner) Utils.castView(view, R.id.from_coin, "field 'sourceSpinner'", Spinner.class);
        this.view2131689792 = view;
        ((AdapterView) view).setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p0, View p1, int p2, long p3) {
                target.onFromSelected(p0);
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        view = Utils.findRequiredView(source, R.id.to_coin, "field 'destinationSpinner' and method 'onToSelected'");
        target.destinationSpinner = (Spinner) Utils.castView(view, R.id.to_coin, "field 'destinationSpinner'", Spinner.class);
        this.view2131689794 = view;
        ((AdapterView) view).setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p0, View p1, int p2, long p3) {
                target.onToSelected(p0);
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        target.destinationLayout = Utils.findRequiredView(source, R.id.to_coin_layout, "field 'destinationLayout'");
        view = Utils.findRequiredView(source, R.id.terms_agree, "field 'agreeTerms' and method 'onAgreeChecked'");
        target.agreeTerms = (CheckBox) Utils.castView(view, R.id.terms_agree, "field 'agreeTerms'", CheckBox.class);
        this.view2131689797 = view;
        ((CompoundButton) view).setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton p0, boolean p1) {
                target.onAgreeChecked(p1);
            }
        });
        view = Utils.findRequiredView(source, R.id.visit_site, "field 'visitSite' and method 'onVisitSiteClick'");
        target.visitSite = (Button) Utils.castView(view, R.id.visit_site, "field 'visitSite'", Button.class);
        this.view2131689789 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onVisitSiteClick();
            }
        });
        view = Utils.findRequiredView(source, R.id.show_terms, "field 'viewTems' and method 'onViewTermsClick'");
        target.viewTems = (Button) Utils.castView(view, R.id.show_terms, "field 'viewTems'", Button.class);
        this.view2131689790 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onViewTermsClick();
            }
        });
        view = Utils.findRequiredView(source, R.id.button_participate, "field 'buttonParticipate' and method 'onParticipateClick'");
        target.buttonParticipate = (Button) Utils.castView(view, R.id.button_participate, "field 'buttonParticipate'", Button.class);
        this.view2131689798 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onParticipateClick();
            }
        });
        target.details = (TextView) Utils.findRequiredViewAsType(source, R.id.details, "field 'details'", TextView.class);
        target.sendCoinAmountView = (AmountEditView) Utils.findRequiredViewAsType(source, R.id.coin_amount, "field 'sendCoinAmountView'", AmountEditView.class);
        target.amountError = (TextView) Utils.findRequiredViewAsType(source, R.id.amount_error_message, "field 'amountError'", TextView.class);
        target.amountWarning = (TextView) Utils.findRequiredViewAsType(source, R.id.amount_warning_message, "field 'amountWarning'", TextView.class);
        view = Utils.findRequiredView(source, R.id.use_all_funds, "method 'onEmptyWalletClick'");
        this.view2131689751 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onEmptyWalletClick();
            }
        });
    }

    public void unbind() {
        T target = this.target;
        if (target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        target.sourceSpinner = null;
        target.destinationSpinner = null;
        target.destinationLayout = null;
        target.agreeTerms = null;
        target.visitSite = null;
        target.viewTems = null;
        target.buttonParticipate = null;
        target.details = null;
        target.sendCoinAmountView = null;
        target.amountError = null;
        target.amountWarning = null;
        ((AdapterView) this.view2131689792).setOnItemSelectedListener(null);
        this.view2131689792 = null;
        ((AdapterView) this.view2131689794).setOnItemSelectedListener(null);
        this.view2131689794 = null;
        ((CompoundButton) this.view2131689797).setOnCheckedChangeListener(null);
        this.view2131689797 = null;
        this.view2131689789.setOnClickListener(null);
        this.view2131689789 = null;
        this.view2131689790.setOnClickListener(null);
        this.view2131689790 = null;
        this.view2131689798.setOnClickListener(null);
        this.view2131689798 = null;
        this.view2131689751.setOnClickListener(null);
        this.view2131689751 = null;
        this.target = null;
    }
}
