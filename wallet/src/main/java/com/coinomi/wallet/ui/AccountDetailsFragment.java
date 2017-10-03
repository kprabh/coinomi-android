package com.coinomi.wallet.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.wallet.Constants;
import com.coinomi.wallet.R;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.util.QrUtils;
import com.coinomi.wallet.util.UiUtils;
import com.google.common.collect.ImmutableList;

import org.bitcoinj.crypto.HDUtils;

import static com.coinomi.core.Preconditions.checkNotNull;

/**
 * @author John L. Jegutanis
 */
public class AccountDetailsFragment extends Fragment {
    private String derivationPath;
    private String publicKeySerialized;

    public static AccountDetailsFragment newInstance(WalletAccount account) {
        AccountDetailsFragment fragment = new AccountDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(Constants.ARG_ACCOUNT_ID, account.getId());
        fragment.setArguments(args);
        return fragment;
    }

    public AccountDetailsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkNotNull(getArguments(), "Must provide arguments with an account id.");

        WalletApplication application = (WalletApplication) getActivity().getApplication();
        WalletAccount account =
                application.getAccount(getArguments().getString(Constants.ARG_ACCOUNT_ID));
        if (account == null) {
            Toast.makeText(getActivity(), R.string.no_such_pocket_error, Toast.LENGTH_LONG).show();
            getActivity().finish();
            return;
        }

        publicKeySerialized = account.getPublicKeySerialized();ImmutableList derivationPath = account.getDeterministicRootKeyPath();
        if (!derivationPath.isEmpty()) {
            this.derivationPath = HDUtils.formatPath(derivationPath);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_details, container, false);

        TextView publicKey = (TextView) view.findViewById(R.id.public_key);
        publicKey.setOnClickListener(getPubKeyOnClickListener());
        publicKey.setText(publicKeySerialized);
        TextView derivationPathView = (TextView) view.findViewById(R.id.derivation_path);
        if (this.derivationPath != null) {
            derivationPathView.setText(this.derivationPath);
        } else {
            derivationPathView.setVisibility(View.GONE);
            view.findViewById(R.id.derivation_path_label).setVisibility(View.GONE);
        }
        ImageView qrView = (ImageView) view.findViewById(R.id.qr_code_public_key);
        QrUtils.setQr(qrView, getResources(), publicKeySerialized);

        return view;
    }

    private View.OnClickListener getPubKeyOnClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity activity = getActivity();
                UiUtils.startCopyShareActionMode(publicKeySerialized, activity);
            }
        };
    }
}
