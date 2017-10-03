package com.coinomi.wallet.ui;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.wallet.R;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.ui.Dialogs.ProgressDialogFragment;
import com.coinomi.wallet.util.Fonts;
import com.coinomi.wallet.util.Fonts.Font;
import com.coinomi.wallet.util.Keyboard;
import com.coinomi.wallet.util.UiUtils;
import com.coinomi.wallet.util.WeakHandler;

public class ChangePasswordIntroFragment extends Fragment {
    private WalletApplication app;
    private CheckPasswordTask checkPasswordTask;
    private final Handler handler = new MyHandler(this);
    private Listener listener;
    @BindView(2131689607)
    EditText password;
    @BindView(2131689683)
    TextView passwordError;
    private Unbinder unbinder;

    static class CheckPasswordTask extends AsyncTask<Void, Void, Boolean> {
        Handler handler;
        private final String password;
        private final Wallet wallet;

        CheckPasswordTask(Handler handler, Wallet wallet, String password) {
            this.handler = handler;
            this.wallet = wallet;
            this.password = password;
        }

        protected void onPreExecute() {
            this.handler.sendEmptyMessage(0);
        }

        protected Boolean doInBackground(Void... params) {
            try {
                this.wallet.getMasterKey().decrypt(this.wallet.getKeyCrypter().deriveKey(this.password));
                return Boolean.valueOf(true);
            } catch (Exception e) {
                return Boolean.valueOf(false);
            }
        }

        protected void onPostExecute(Boolean isPasswordOk) {
            this.handler.sendMessage(this.handler.obtainMessage(1, isPasswordOk));
        }
    }

    public interface Listener {
        void onCurrentPasswordVerified(Bundle bundle);

        void onForgotPassword();
    }

    private static class MyHandler extends WeakHandler<ChangePasswordIntroFragment> {
        public MyHandler(ChangePasswordIntroFragment ref) {
            super(ref);
        }

        protected void weakHandleMessage(ChangePasswordIntroFragment ref, Message msg) {
            switch (msg.what) {
                case 0:
                    ref.onPasswordTaskStarted();
                    return;
                case 1:
                    ref.ononPasswordTaskFinished(((Boolean) msg.obj).booleanValue());
                    return;
                default:
                    return;
            }
        }
    }

    static ChangePasswordIntroFragment newInstance() {
        return newInstance(null);
    }

    static ChangePasswordIntroFragment newInstance(Bundle args) {
        ChangePasswordIntroFragment fragment = new ChangePasswordIntroFragment();
        if (args == null) {
            args = new Bundle();
        }
        fragment.setArguments(args);
        return fragment;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_password, container, false);
        this.unbinder = ButterKnife.bind((Object) this, view);
        Fonts.setTypeface(ButterKnife.findById(view, (int) R.id.font_icon), Font.COINOMI_FONT_ICONS);
        return view;
    }

    public void onDestroyView() {
        this.unbinder.unbind();
        super.onDestroyView();
    }

    @OnClick({2131689685})
    public void OnNextClick() {
        Keyboard.hideKeyboard(getActivity());
        if (this.checkPasswordTask == null) {
            this.checkPasswordTask = new CheckPasswordTask(this.handler, this.app.getWallet(), this.password.getText().toString());
            this.checkPasswordTask.execute(new Void[0]);
        }
    }

    @OnClick({2131689684})
    public void OnForgotPasswordClick() {
        if (this.listener != null) {
            cancelTask();
            this.listener.onForgotPassword();
        }
    }

    private void cancelTask() {
        if (this.checkPasswordTask != null) {
            this.checkPasswordTask.cancel(true);
            this.checkPasswordTask = null;
        }
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

    public void onPasswordTaskStarted() {
        ProgressDialogFragment.show(getFragmentManager(), getString(R.string.restore_verifying_seed), "password_check_dialog_tag");
    }

    public void ononPasswordTaskFinished(boolean isPasswordOk) {
        if (!Dialogs.dismissAllowingStateLoss(getFragmentManager(), "password_check_dialog_tag")) {
            this.checkPasswordTask = null;
            if (!isPasswordOk) {
                UiUtils.setVisible(this.passwordError);
            } else if (this.listener != null) {
                Bundle args = getArguments() == null ? new Bundle() : getArguments();
                args.putString("old_password", this.password.getText().toString());
                this.listener.onCurrentPasswordVerified(args);
            }
        }
    }
}
