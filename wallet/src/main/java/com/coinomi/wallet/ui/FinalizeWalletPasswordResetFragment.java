package com.coinomi.wallet.ui;

import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.coinomi.core.CoreUtils;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.wallet.R;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.util.WeakHandler;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FinalizeWalletPasswordResetFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(FinalizeWalletPasswordResetFragment.class);
    private WalletApplication app;
    private final Handler handler = new MyHandler(this);
    private String newPassword;
    private String oldPassword;
    private ResetKeysTask restoreKeysTask;
    private String seed;
    private String seedPassword;

    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$android$os$AsyncTask$Status = new int[Status.values().length];

        static {
            try {
                $SwitchMap$android$os$AsyncTask$Status[Status.FINISHED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
        }
    }

    private static class MyHandler extends WeakHandler<FinalizeWalletPasswordResetFragment> {
        public MyHandler(FinalizeWalletPasswordResetFragment ref) {
            super(ref);
        }

        protected void weakHandleMessage(FinalizeWalletPasswordResetFragment ref, Message msg) {
            switch (msg.what) {
                case 0:
                    ref.resetFinished();
                    return;
                default:
                    return;
            }
        }
    }

    static class ResetKeysTask extends AsyncTask<Void, Void, Void> {
        Exception error = null;
        Handler handler;
        private final String newPassword;
        private final String oldPassword;
        private final List<String> seed;
        private final String seedPassword;
        private final Wallet wallet;

        ResetKeysTask(Handler handler, Wallet wallet, String oldPassword, String newPassword) {
            this.handler = handler;
            this.wallet = wallet;
            this.oldPassword = oldPassword;
            this.newPassword = newPassword;
            this.seed = null;
            this.seedPassword = null;
        }

        ResetKeysTask(Handler handler, Wallet wallet, String seedText, String seedPassword, String newPassword) {
            this.handler = handler;
            this.wallet = wallet;
            this.seed = CoreUtils.parseMnemonic(seedText);
            this.seedPassword = seedPassword;
            this.newPassword = newPassword;
            this.oldPassword = null;
        }

        protected Void doInBackground(Void... params) {
            try {
                if (this.oldPassword != null) {
                    this.wallet.resetEncryptionFromPassword(this.oldPassword, this.newPassword);
                } else {
                    this.wallet.resetEncryptionFromSeed(this.seed, this.seedPassword, this.newPassword);
                }
                this.wallet.saveNow();
            } catch (Throwable e) {
                FinalizeWalletPasswordResetFragment.log.error("Error resetting a wallet password", e);
                this.error = (Exception) e;
            }
            return null;
        }

        protected void onPostExecute(Void error) {
            this.handler.sendEmptyMessage(0);
        }
    }

    public static Fragment newInstance(Bundle args) {
        FinalizeWalletPasswordResetFragment fragment = new FinalizeWalletPasswordResetFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        this.app = getWalletApplication();
        if (getArguments() != null) {
            Bundle args = getArguments();
            this.seed = args.getString("seed");
            this.newPassword = args.getString("password");
            this.oldPassword = args.getString("old_password");
            this.seedPassword = args.getString("seed_password");
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_finalize_wallet_key_reset, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (this.restoreKeysTask == null) {
            if (this.oldPassword != null) {
                this.restoreKeysTask = new ResetKeysTask(this.handler, this.app.getWallet(), this.oldPassword, this.newPassword);
            } else {
                this.restoreKeysTask = new ResetKeysTask(this.handler, this.app.getWallet(), this.seed, this.seedPassword, this.newPassword);
            }
            this.restoreKeysTask.execute(new Void[0]);
            return;
        }
        switch (AnonymousClass1.$SwitchMap$android$os$AsyncTask$Status[this.restoreKeysTask.getStatus().ordinal()]) {
            case 1:
                this.handler.sendEmptyMessage(0);
                return;
            default:
                return;
        }
    }

    WalletApplication getWalletApplication() {
        return (WalletApplication) getActivity().getApplication();
    }

    private void resetFinished() {
        if (this.restoreKeysTask.error == null) {
            Toast.makeText(getActivity(), R.string.wallet_key_reset_ok, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getActivity(), getResources().getString(R.string.wallet_key_reset_error, new Object[]{this.restoreKeysTask.error.getLocalizedMessage()}), 1).show();
        }
        getActivity().finish();
    }
}
