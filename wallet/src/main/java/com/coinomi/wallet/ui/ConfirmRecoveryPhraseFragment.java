package com.coinomi.wallet.ui;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.MultiAutoCompleteTextView;
import android.widget.MultiAutoCompleteTextView.Tokenizer;
import android.widget.TextView;
import butterknife.ButterKnife;
import com.coinomi.core.CoreUtils;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.wallet.R;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.ui.Dialogs.ProgressDialogFragment;
import com.coinomi.wallet.util.Fonts;
import com.coinomi.wallet.util.Fonts.Font;
import com.coinomi.wallet.util.Keyboard;
import com.coinomi.wallet.util.UiUtils;
import com.coinomi.wallet.util.WeakHandler;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.crypto.MnemonicException.MnemonicChecksumException;
import org.bitcoinj.crypto.MnemonicException.MnemonicWordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfirmRecoveryPhraseFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(ConfirmRecoveryPhraseFragment.class);
    private WalletApplication app;
    private TextView errorMnemonicΜessage;
    private final Handler handler = new MyHandler(this);
    private boolean isSeedProtected = false;
    private Listener listener;
    private ArrayAdapter<String> mnemonicAdapter;
    private GridView mnemonicListView;
    private MultiAutoCompleteTextView mnemonicTextView;
    private String seed;
    private boolean showIcon;
    private Button skipButton;
    private VerifySeedTask verifySeedTask;

    private static abstract class SpaceTokenizer implements Tokenizer {
        public abstract void onToken();

        private SpaceTokenizer() {
        }

        public int findTokenStart(CharSequence text, int cursor) {
            int i = cursor;
            while (i > 0 && text.charAt(i - 1) != ' ') {
                i--;
            }
            return i;
        }

        public int findTokenEnd(CharSequence text, int cursor) {
            int len = text.length();
            for (int i = cursor; i < len; i++) {
                if (text.charAt(i) == ' ') {
                    return i;
                }
            }
            return len;
        }

        public CharSequence terminateToken(CharSequence text) {
            onToken();
            return text + " ";
        }
    }

    public interface Listener {
        void onSeedConfirmed(Bundle bundle);
    }

    private static class MyHandler extends WeakHandler<ConfirmRecoveryPhraseFragment> {
        public MyHandler(ConfirmRecoveryPhraseFragment ref) {
            super(ref);
        }

        protected void weakHandleMessage(ConfirmRecoveryPhraseFragment ref, Message msg) {
            switch (msg.what) {
                case 0:
                    ref.onPasswordTaskStarted();
                    return;
                case 1:
                    ref.onPasswordTaskFinished(((Boolean) msg.obj).booleanValue());
                    return;
                default:
                    return;
            }
        }
    }

    public static class SkipDialogFragment extends DialogFragment {
        private com.coinomi.wallet.ui.WelcomeFragment.Listener mListener;

        public static SkipDialogFragment newInstance(String seed) {
            SkipDialogFragment newDialog = new SkipDialogFragment();
            Bundle args = new Bundle();
            args.putString("seed", seed);
            newDialog.setArguments(args);
            return newDialog;
        }

        public void onAttach(Activity activity) {
            super.onAttach(activity);
            try {
                this.mListener = (com.coinomi.wallet.ui.WelcomeFragment.Listener) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString() + " must implement WelcomeFragment.OnFragmentInteractionListener");
            }
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String dialogMessage = getResources().getString(R.string.restore_skip_info1) + "\n\n" + getArguments().getString("seed") + "\n\n" + getResources().getString(R.string.restore_skip_info2);
            Builder builder = new Builder(getActivity());
            builder.setTitle(R.string.restore_skip_title).setMessage(dialogMessage).setPositiveButton(R.string.button_skip, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (SkipDialogFragment.this.mListener != null) {
                        SkipDialogFragment.this.mListener.onSeedVerified(SkipDialogFragment.this.getArguments());
                    }
                }
            }).setNegativeButton(R.string.button_cancel, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    SkipDialogFragment.this.dismiss();
                }
            });
            return builder.create();
        }
    }

    static class VerifySeedTask extends AsyncTask<Void, Void, Boolean> {
        Handler handler;
        private final String seed = null;
        private final String seedPassword = null;
        private final Wallet wallet = null;

        protected void onPreExecute() {
            this.handler.sendEmptyMessage(0);
        }

        protected Boolean doInBackground(Void... params) {
            ArrayList<String> seedWords = new ArrayList();
            for (String word : this.seed.trim().split(" ")) {
                if (!word.isEmpty()) {
                    seedWords.add(word);
                }
            }
            return this.wallet.isOwnSeed(seedWords, this.seedPassword);
        }

        protected void onPostExecute(Boolean isOwnSeed) {
            this.handler.sendMessage(this.handler.obtainMessage(1, isOwnSeed));
        }
    }

    public static ConfirmRecoveryPhraseFragment newInstance(String seed) {
        ConfirmRecoveryPhraseFragment fragment = new ConfirmRecoveryPhraseFragment();
        Bundle args = new Bundle();
        args.putBoolean("show_icon", true);
        if (seed != null) {
            args.putString("seed", seed.trim());
        }
        fragment.setArguments(args);
        return fragment;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (getArguments() != null) {
            this.seed = getArguments().getString("seed");
            this.showIcon = getArguments().getBoolean("show_icon", false);
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_confirm_recovery_phrase, container, false);
        if (this.showIcon) {
            Fonts.setTypeface(ButterKnife.findById(view, (int) R.id.coins_icon), Font.COINOMI_FONT_ICONS);
        } else {
            UiUtils.setGone(ButterKnife.findById(view, (int) R.id.coins_icon));
        }
        ((ImageButton) view.findViewById(R.id.erase_last_word)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ConfirmRecoveryPhraseFragment.this.handleErase();
            }
        });
        ArrayAdapter<String> adapter = new ArrayAdapter(getActivity(), R.layout.item_simple, MnemonicCode.INSTANCE.getWordList());
        this.mnemonicTextView = (MultiAutoCompleteTextView) view.findViewById(R.id.seed);
        this.mnemonicTextView.setEnabled(false);
        this.mnemonicTextView.setAdapter(adapter);
        this.mnemonicTextView.setTokenizer(new SpaceTokenizer() {
            public void onToken() {
                UiUtils.setGone(ConfirmRecoveryPhraseFragment.this.errorMnemonicΜessage);
            }
        });
        List<String> mnemonicList = Lists.newArrayList(this.seed.split(" "));
        Collections.shuffle(mnemonicList);
        this.mnemonicAdapter = new ArrayAdapter(getActivity(), R.layout.item_mnemonic_word, mnemonicList);
        this.mnemonicListView = (GridView) view.findViewById(R.id.mnemonic_words);
        this.mnemonicListView.setAdapter(this.mnemonicAdapter);
        this.mnemonicListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String entry = (String) parent.getAdapter().getItem(position);
                ConfirmRecoveryPhraseFragment.this.mnemonicTextView.setText(ConfirmRecoveryPhraseFragment.this.mnemonicTextView.getText() + " " + entry);
                ConfirmRecoveryPhraseFragment.this.mnemonicAdapter.remove(entry);
            }
        });
        this.errorMnemonicΜessage = (TextView) view.findViewById(R.id.restore_message);
        this.errorMnemonicΜessage.setVisibility(View.GONE);
        this.skipButton = (Button) view.findViewById(R.id.seed_entry_skip);
        this.skipButton.setOnClickListener(getOnSkipListener());
        this.skipButton.setVisibility(View.VISIBLE);
        view.findViewById(R.id.button_next).setOnClickListener(getOnNextListener());
        return view;
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.listener = (Listener) context;
            this.app = (WalletApplication) context.getApplicationContext();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + Listener.class);
        }
    }

    public void onDetach() {
        super.onDetach();
        this.listener = null;
    }

    private View.OnClickListener getOnNextListener() {
        return new View.OnClickListener() {
            public void onClick(View v) {
                ConfirmRecoveryPhraseFragment.this.verifyMnemonicAndProceed();
            }
        };
    }

    private View.OnClickListener getOnSkipListener() {
        return new View.OnClickListener() {
            public void onClick(View v) {
                ConfirmRecoveryPhraseFragment.log.info("Skipping seed verification.");
                ConfirmRecoveryPhraseFragment.this.mnemonicTextView.setText("");
                SkipDialogFragment.newInstance(ConfirmRecoveryPhraseFragment.this.seed).show(ConfirmRecoveryPhraseFragment.this.getFragmentManager(), null);
            }
        };
    }

    private void verifyMnemonicAndProceed() {
        Keyboard.hideKeyboard(getActivity());
        if (verifyMnemonic()) {
            finallyNotifyListener();
        }
    }

    private void finallyNotifyListener() {
        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
        }
        args.putString("seed", this.mnemonicTextView.getText().toString().trim());
        if (this.listener != null) {
            this.listener.onSeedConfirmed(args);
        }
    }

    private boolean verifyMnemonic() {
        log.info("Verifying seed");
        String seedText = this.mnemonicTextView.getText().toString().trim();
        boolean isSeedValid = false;
        try {
            MnemonicCode.INSTANCE.check(CoreUtils.parseMnemonic(seedText));
            UiUtils.setGone(this.errorMnemonicΜessage);
            isSeedValid = true;
        } catch (MnemonicChecksumException e) {
            log.info("Checksum error in seed: {}", e.getMessage());
            setError(this.errorMnemonicΜessage, R.string.restore_error_checksum, new Object[0]);
        } catch (MnemonicWordException e2) {
            log.info("Unknown words in seed: {}", e2.getMessage());
            setError(this.errorMnemonicΜessage, R.string.restore_error_words, new Object[0]);
        } catch (MnemonicException e3) {
            log.info("Error verifying seed: {}", e3.getMessage());
            setError(this.errorMnemonicΜessage, R.string.restore_error, e3.getMessage());
        }
        if (!(!isSeedValid || this.seed == null || seedText.equals(this.seed))) {
            log.info("Typed seed does not match the generated one.");
            setError(this.errorMnemonicΜessage, R.string.restore_error_mismatch, new Object[0]);
            isSeedValid = false;
        }
        if (!isSeedValid) {
            resetMnemonic();
        }
        return isSeedValid;
    }

    private void resetMnemonic() {
        this.mnemonicTextView.setText("");
        this.mnemonicAdapter.clear();
        List<String> mnemonicList = Lists.newArrayList(this.seed.split(" "));
        Collections.shuffle(mnemonicList);
        for (String s : mnemonicList) {
            this.mnemonicAdapter.add(s);
        }
    }

    private void setError(TextView errorView, int messageId, Object... formatArgs) {
        UiUtils.setTextAndVisible(errorView, getResources().getString(messageId, formatArgs));
    }

    private void handleErase() {
        if (!this.mnemonicTextView.getText().toString().trim().isEmpty()) {
            String[] words = this.mnemonicTextView.getText().toString().split(" ");
            String removeThis = words[words.length - 1];
            this.mnemonicTextView.setText(this.mnemonicTextView.getText().toString().replace(removeThis, ""));
            this.mnemonicAdapter.add(removeThis);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0 && resultCode == -1) {
            this.mnemonicTextView.setText(intent.getStringExtra("result"));
            verifyMnemonic();
        }
    }

    public void onPasswordTaskStarted() {
        ProgressDialogFragment.show(getFragmentManager(), getString(R.string.restore_verifying_seed), "seed_verification_dialog_tag");
    }

    public void onPasswordTaskFinished(boolean isOwnSeed) {
        if (!Dialogs.dismissAllowingStateLoss(getFragmentManager(), "seed_verification_dialog_tag")) {
            this.verifySeedTask = null;
            if (isOwnSeed) {
                finallyNotifyListener();
            } else {
                setError(this.errorMnemonicΜessage, R.string.restore_error_wrong_seed, new Object[0]);
            }
        }
    }
}
