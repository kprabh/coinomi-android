package com.coinomi.wallet.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

import com.coinomi.core.CoreUtils;
import com.coinomi.core.Preconditions;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.wallet.Constants;
import com.coinomi.wallet.R;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.util.Fonts;
import com.coinomi.wallet.util.Keyboard;
import com.coinomi.wallet.util.UiUtils;
import com.coinomi.wallet.util.WeakHandler;

import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import javax.annotation.Nullable;

import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class RestoreFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(RestoreFragment.class);
    private static final int REQUEST_CODE_SCAN = 0;
    private WalletApplication app;
    private MultiAutoCompleteTextView mnemonicTextView;
    @Nullable private String seed;
    private boolean isNewSeed;
    private TextView errorMnemonicΜessage;
    private WelcomeFragment.Listener listener;
    private boolean isSeedProtected = false;
    private EditText bip39Passphrase;
    private Button skipButton;
    private boolean showIcon;    private boolean validateWallet;
    private VerifySeedTask verifySeedTask;   private final Handler handler = new MyHandler(this);
    public interface Listener {
        void onSeedVerified(Bundle bundle);
    }
    public static RestoreFragment newInstance() {
        return newInstance(null);
    }
    public static RestoreFragment newInstanceForResettingEncryption() {
        RestoreFragment f = newInstance(null);
        Bundle args = new Bundle();
        args.putBoolean("validate_wallet", true);
        args.putBoolean("show_icon", false);
        f.setArguments(args);
        return f;
    }
    public static RestoreFragment newInstance(@Nullable String seed) {
        RestoreFragment fragment = new RestoreFragment();
        if (seed != null) {
            Bundle args = new Bundle();args.putBoolean("show_icon", true);
            args.putString(Constants.ARG_SEED, seed);
            fragment.setArguments(args);
        }
        return fragment;
    }

    public RestoreFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {this.validateWallet = getArguments().getBoolean("validate_wallet", false);
            if (this.validateWallet) {
                Preconditions.checkState(this.app.getWallet() != null, "Asked to validate a null wallet");
            }
            seed = getArguments().getString(Constants.ARG_SEED);
            isNewSeed = seed != null;this.showIcon = getArguments().getBoolean("show_icon", false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_restore, container, false);
        if (this.showIcon) {
        Fonts.setTypeface(view.findViewById(R.id.coins_icon), Fonts.Font.COINOMI_FONT_ICONS);
        } else {
            UiUtils.setGone(ButterKnife.findById(view, (int) R.id.coins_icon));
        }
        ImageButton scanQrButton = (ImageButton) view.findViewById(R.id.scan_qr_code);
        scanQrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleScan();
            }
        });

        // Setup auto complete the mnemonic words
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this.getActivity(),
                R.layout.item_simple, MnemonicCode.INSTANCE.getWordList());
        mnemonicTextView = (MultiAutoCompleteTextView) view.findViewById(R.id.seed);
        mnemonicTextView.setAdapter(adapter);
        mnemonicTextView.setTokenizer(new SpaceTokenizer() {
            @Override
            public void onToken() {
                clearError(errorMnemonicΜessage);
            }
        });

        // Restore message
        errorMnemonicΜessage = (TextView) view.findViewById(R.id.restore_message);
        errorMnemonicΜessage.setVisibility(View.GONE);

        bip39Passphrase = (EditText) view.findViewById(R.id.bip39_passphrase);
        final View bip39PassphraseTitle = view.findViewById(R.id.bip39_passphrase_title);

        bip39Passphrase.setVisibility(View.GONE);
        bip39PassphraseTitle.setVisibility(View.GONE);

        // For existing seed
        final View bip39Info = view.findViewById(R.id.bip39_info);
        bip39Info.setVisibility(View.GONE);
        final CheckBox useBip39Checkbox = (CheckBox) view.findViewById(R.id.use_bip39);
        if (isNewSeed) useBip39Checkbox.setVisibility(View.GONE);

        useBip39Checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isSeedProtected = isChecked;
                if (isChecked) {
                    if (isNewSeed) skipButton.setVisibility(View.GONE);
                    bip39Info.setVisibility(View.VISIBLE);
                    bip39PassphraseTitle.setVisibility(View.VISIBLE);
                    bip39Passphrase.setVisibility(View.VISIBLE);
                } else {
                    if (isNewSeed) skipButton.setVisibility(View.VISIBLE);
                    bip39Info.setVisibility(View.GONE);
                    bip39PassphraseTitle.setVisibility(View.GONE);
                    bip39Passphrase.setVisibility(View.GONE);
                    bip39Passphrase.setText(null);
                }
            }
        });

        // Skip link
        skipButton = (Button) view.findViewById(R.id.seed_entry_skip);
        if (isNewSeed) {
            skipButton.setOnClickListener(getOnSkipListener());
            skipButton.setVisibility(View.VISIBLE);
        } else {
            skipButton.setVisibility(View.GONE);
        }

        // Next button
        view.findViewById(R.id.button_next).setOnClickListener(getOnNextListener());

        return view;
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        try {
            listener = (WelcomeFragment.Listener) context; this.app = (WalletApplication) context.getApplicationContext();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement " + WelcomeFragment.Listener.class);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    private View.OnClickListener getOnNextListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyMnemonicAndProceed();
            }
        };
    }

    private View.OnClickListener getOnSkipListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log.info("Skipping seed verification.");
                mnemonicTextView.setText("");
                SkipDialogFragment skipDialog = SkipDialogFragment.newInstance(seed);
                skipDialog.show(getFragmentManager(), null);
            }
        };
    }

    private void verifyMnemonicAndProceed() {
        Keyboard.hideKeyboard(getActivity());
        if (!verifyMnemonic()) {
            return;
        }
        if (!this.validateWallet) {
            finallyNotifyListener();
        } else if (this.verifySeedTask == null) {
            this.verifySeedTask = new VerifySeedTask(this.handler, this.app.getWallet(), this.mnemonicTextView.getText().toString(), this.bip39Passphrase.getText().toString());
            this.verifySeedTask.execute(new Void[0]);
        }
    }

    private void finallyNotifyListener() {
            Bundle args = getArguments();
            if (args == null) args = new Bundle();

            // Do not set a BIP39 passphrase on new recovery phrases
            if (!isNewSeed && isSeedProtected) {
                args.putString(Constants.ARG_SEED_PASSWORD, bip39Passphrase.getText().toString());
            }
            args.putString(Constants.ARG_SEED, mnemonicTextView.getText().toString().trim());
            if (listener != null) listener.onSeedVerified(args);
        }


    private boolean verifyMnemonic() {
        log.info("Verifying seed");
        String seedText = mnemonicTextView.getText().toString().trim();
        ArrayList<String> seedWords = CoreUtils.parseMnemonic(seedText);
        boolean isSeedValid = false;
        try {
            MnemonicCode.INSTANCE.check(seedWords);
            UiUtils.setGone(this.errorMnemonicΜessage);
            isSeedValid = true;
        } catch (MnemonicException.MnemonicChecksumException e) {
            log.info("Checksum error in seed: {}", e.getMessage());
            setError(errorMnemonicΜessage, R.string.restore_error_checksum);
        } catch (MnemonicException.MnemonicWordException e) {
            log.info("Unknown words in seed: {}", e.getMessage());
            setError(errorMnemonicΜessage, R.string.restore_error_words);
        } catch (MnemonicException e) {
            log.info("Error verifying seed: {}", e.getMessage());
            setError(errorMnemonicΜessage, R.string.restore_error, e.getMessage());
        }

        if (isSeedValid && seed != null && !seedText.equals(seed.trim())) {
            log.info("Typed seed does not match the generated one.");
            setError(errorMnemonicΜessage, R.string.restore_error_mismatch);
            isSeedValid = false;
        }
        return isSeedValid;
    }

    public static class SkipDialogFragment extends DialogFragment {

        private WelcomeFragment.Listener mListener;

        public static SkipDialogFragment newInstance(String seed) {
            SkipDialogFragment newDialog = new SkipDialogFragment();
            Bundle args = new Bundle();
            args.putString(Constants.ARG_SEED, seed);
            newDialog.setArguments(args);
            return newDialog;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            try {
                mListener = (WelcomeFragment.Listener) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString()
                        + " must implement WelcomeFragment.OnFragmentInteractionListener");
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String seed = getArguments().getString(Constants.ARG_SEED);
            // FIXME does not look good with custom dialogs in older Samsungs
//            View dialogView = getActivity().getLayoutInflater().inflate(R.layout.skip_seed_dialog, null);
//            TextView seedView = (TextView) dialogView.findViewById(R.id.seed);
//            seedView.setText(seed);

            String dialogMessage = getResources().getString(R.string.restore_skip_info1) + "\n\n" +
                    seed + "\n\n" + getResources().getString(R.string.restore_skip_info2);
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.restore_skip_title)
//                   .setView(dialogView) FIXME
                   .setMessage(dialogMessage)
                   .setPositiveButton(R.string.button_skip, new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int which) {
                           if (mListener != null) mListener.onSeedVerified(getArguments());
                       }
                   })
                   .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int which) {
                           dismiss();
                       }
                   });

            return builder.create();
        }
    }

    private void setError(TextView errorView, int messageId, Object... formatArgs) {
        UiUtils.setTextAndVisible(errorView, getResources().getString(messageId, formatArgs));
    }

    private void setError(TextView errorView, String message) {
        errorView.setText(message);
        showError(errorView);
    }

    private void showError(TextView errorView) {
        errorView.setVisibility(View.VISIBLE);
    }

    private void clearError(TextView errorView) {
        errorView.setVisibility(View.GONE);
    }

    private void handleScan() {
        startActivityForResult(new Intent(getActivity(), ScanActivity.class), REQUEST_CODE_SCAN);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        if (requestCode == REQUEST_CODE_SCAN) {
            if (resultCode == Activity.RESULT_OK) {
                mnemonicTextView.setText(intent.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT));
                verifyMnemonic();
            }
        }
    }
    static class VerifySeedTask extends AsyncTask<Void, Void, Boolean> {
        Handler handler;
        private final String seed;
        private final String seedPassword;
        private final Wallet wallet;

        VerifySeedTask(Handler handler, Wallet wallet, String seed, String seedPassword) {
            this.handler = handler;
            this.wallet = wallet;
            this.seed = seed;
            this.seedPassword = seedPassword;
        }

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
    private abstract static class SpaceTokenizer implements MultiAutoCompleteTextView.Tokenizer {
        public int findTokenStart(CharSequence text, int cursor) {
            int i = cursor;

            while (i > 0 && text.charAt(i - 1) != ' ') {
                i--;
            }

            return i;
        }

        public int findTokenEnd(CharSequence text, int cursor) {
            int i = cursor;
            int len = text.length();

            while (i < len) {
                if (text.charAt(i) == ' ') {
                    return i;
                } else {
                    i++;
                }
            }

            return len;
        }

        public CharSequence terminateToken(CharSequence text) {
            onToken();
            return text + " ";
        }

        abstract public void onToken();
    }   public void onPasswordTaskStarted() {
        Dialogs.ProgressDialogFragment.show(getFragmentManager(), getString(R.string.restore_verifying_seed), "seed_verification_dialog_tag");
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
    }   private static class MyHandler extends WeakHandler<RestoreFragment> {
        public MyHandler(RestoreFragment ref) {
            super(ref);
        }

        protected void weakHandleMessage(RestoreFragment ref, Message msg) {
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
}
