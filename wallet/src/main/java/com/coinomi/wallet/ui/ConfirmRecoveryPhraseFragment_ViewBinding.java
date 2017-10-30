package com.coinomi.wallet.ui;

import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import com.coinomi.wallet.R;

public class ConfirmRecoveryPhraseFragment_ViewBinding<T extends ConfirmRecoveryPhraseFragment> implements Unbinder {
    protected T target;
    private View view2131689687;
    private View view2131689691;
    private View view2131689694;

    public ConfirmRecoveryPhraseFragment_ViewBinding(final T target, View source) {
        this.target = target;
        target.f37errorMnemonicessage = (TextView) Utils.findRequiredViewAsType(source, R.id.restore_message, "field 'errorMnemonicΜessage'", TextView.class);
        target.mnemonicListView = (GridView) Utils.findRequiredViewAsType(source, R.id.mnemonic_words, "field 'mnemonicListView'", GridView.class);
        target.mnemonicTextView = (EditText) Utils.findRequiredViewAsType(source, R.id.seed, "field 'mnemonicTextView'", EditText.class);
        View view = Utils.findRequiredView(source, R.id.erase_last_word, "method 'handleErase'");
        this.view2131689691 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.handleErase();
            }
        });
        view = Utils.findRequiredView(source, R.id.seed_entry_skip, "method 'onSkipClick'");
        this.view2131689694 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onSkipClick();
            }
        });
        view = Utils.findRequiredView(source, R.id.button_next, "method 'verifyMnemonicAndProceed'");
        this.view2131689687 = view;
        view.setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.verifyMnemonicAndProceed();
            }
        });
    }

    public void unbind() {
        T target = this.target;
        if (target == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        target.f37errorMnemonicessage = null;
        target.mnemonicListView = null;
        target.mnemonicTextView = null;
        this.view2131689691.setOnClickListener(null);
        this.view2131689691 = null;
        this.view2131689694.setOnClickListener(null);
        this.view2131689694 = null;
        this.view2131689687.setOnClickListener(null);
        this.view2131689687 = null;
        this.target = null;
    }
}
