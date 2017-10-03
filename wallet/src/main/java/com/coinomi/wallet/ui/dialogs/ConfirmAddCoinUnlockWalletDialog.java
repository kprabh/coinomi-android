package com.coinomi.wallet.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.coinomi.core.coins.CoinType;
import com.coinomi.wallet.R;
import com.coinomi.wallet.ui.DialogBuilder;

import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.HDUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;

/**
 * @author John L. Jegutanis
 */
public class ConfirmAddCoinUnlockWalletDialog extends DialogFragment {
    private static final String ADD_COIN = "add_coin";
    private static final String ASK_PASSWORD = "ask_password";
    private Listener listener;

    public static DialogFragment getInstance(CoinType type, boolean askPassword) {
        DialogFragment dialog = new ConfirmAddCoinUnlockWalletDialog();
        dialog.setArguments(new Bundle());
        dialog.getArguments().putSerializable(ADD_COIN, type);
        dialog.getArguments().putBoolean(ASK_PASSWORD, askPassword);
        return dialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (Listener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.getClass() + " must implement " + Listener.class);
        }
    }

    @Override
    public void onDetach() {
        listener = null;
        super.onDetach();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final CoinType type = (CoinType) getArguments().getSerializable(ADD_COIN);
        final boolean askPassword = getArguments().getBoolean(ASK_PASSWORD);
        final LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View view = inflater.inflate(R.layout.add_account_dialog, null);
        final TextView passwordMessage = ButterKnife.findById(view, R.id.password_message);
        final EditText password = ButterKnife.findById(view, R.id.password);
        final EditText description = ButterKnife.findById(view, R.id.edit_account_description);
        Button advancedSetting = (Button) ButterKnife.findById(view, (int) R.id.advanced_settings);
        final TextView customPathLabel = (TextView) ButterKnife.findById(view, (int) R.id.custom_path_label);
        final EditText customPath = (EditText) ButterKnife.findById(view, (int) R.id.custom_path);
        customPath.setHint(HDUtils.formatPath(type.getBip44Path(0)));
        advancedSetting.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (customPath.getVisibility() == View.VISIBLE) {
                    customPath.setVisibility(View.GONE);
                    customPathLabel.setVisibility(View.GONE);
                    return;
                }
                customPath.setVisibility(View.VISIBLE);
                customPathLabel.setVisibility(View.VISIBLE);
            }
        });
        customPath.setVisibility(View.GONE);
        customPathLabel.setVisibility(View.GONE);
        if (!askPassword) {
            passwordMessage.setVisibility(View.GONE);
            password.setVisibility(View.GONE);
        }

        return new DialogBuilder(getActivity())
                .setTitle(getString(R.string.adding_coin_confirmation_title, type.getName()))
                .setView(view)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {List<ChildNumber> parsedPath = null;
                        if (customPath.getVisibility() == View.VISIBLE) {
                            try {
                                parsedPath = ConfirmAddCoinUnlockWalletDialog.parsePath(customPath.getText().toString());
                            } catch (Exception e) {
                                Toast.makeText(ConfirmAddCoinUnlockWalletDialog.this.getContext(), ConfirmAddCoinUnlockWalletDialog.this.getResources().getString(R.string.add_coin_error, new Object[]{type.getName(), e.getMessage()}), Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                        if (listener != null) {
                            listener.addCoin(type, description.getText().toString(), password.getText(), parsedPath);
                        }
                    }
                }).create();
    }
    static List<ChildNumber> parsePath(String path) {
        String[] parsedNodes = path.replace("M", "").replace("'", "H").split("/");
        List<ChildNumber> nodes = new ArrayList();
        for (String n : parsedNodes) {
            String n2 = n.replaceAll(" ", "");
            if (n2.length() != 0) {
                boolean isHard = n2.endsWith("H");
                if (isHard) {
                    n2 = n2.substring(0, n2.length() - 1);
                }
                nodes.add(new ChildNumber(Integer.parseInt(n2), isHard));
            }
        }
        return nodes;
    }
    public interface Listener {
        void addCoin(CoinType type, String description, CharSequence password, List<ChildNumber> list);
    }
}