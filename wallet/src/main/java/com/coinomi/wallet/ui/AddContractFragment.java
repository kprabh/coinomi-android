package com.coinomi.wallet.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import com.coinomi.core.coins.eth.CallTransaction.Contract;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.core.wallet.families.eth.EthContract;
import com.coinomi.core.wallet.families.eth.EthContractSuits;
import com.coinomi.core.wallet.families.eth.EthFamilyWallet;
import com.coinomi.wallet.R;
import com.coinomi.wallet.WalletApplication;
import java.util.ArrayList;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddContractFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(AddContractFragment.class);
    private EthFamilyWallet account;
    private WalletApplication application;
    @BindView(2131689674)
    EditText contractAbi;
    @BindView(2131689672)
    EditText contractAddress;
    @BindView(2131689670)
    EditText contractDescription;
    @BindView(2131689669)
    EditText contractName;
    @BindView(2131689673)
    Spinner contractSuit;
    @BindView(2131689671)
    EditText contractWebsite;
    private Unbinder unbinder;

    public static AddContractFragment newInstance() {
        return new AddContractFragment();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WalletAccount a = this.application.getAccount(getArguments().getString("account_id"));
        if (a == null || !(a instanceof EthFamilyWallet)) {
            Toast.makeText(getActivity(), R.string.no_such_pocket_error, 1).show();
            getActivity().finish();
            return;
        }
        this.account = (EthFamilyWallet) a;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_contract, container, false);
        this.unbinder = ButterKnife.bind((Object) this, view);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter(getContext(), 17367048, new ArrayList(EthContractSuits.getTemplateNames()));
        dataAdapter.setDropDownViewResource(17367049);
        this.contractSuit.setAdapter(dataAdapter);
        return this.account == null ? view : view;
    }

    public void onDestroyView() {
        this.unbinder.unbind();
        super.onDestroyView();
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        this.application = (WalletApplication) context.getApplicationContext();
    }

    @OnClick({2131689675})
    public void onItemClick() {
        String name = this.contractName.getText().toString();
        String address = this.contractAddress.getText().toString();
        String website = this.contractWebsite.getText().toString();
        String abi = this.contractAbi.getText().toString();
        String description = this.contractDescription.getText().toString();
        String suit = this.contractSuit.getSelectedItem().toString();
        String icon = "";
        if (name.isEmpty()) {
            Toast.makeText(getActivity(), R.string.invalid_contract_name, Toast.LENGTH_LONG).show();
        } else if (address.isEmpty()) {
            Toast.makeText(getActivity(), R.string.invalid_contract_address, Toast.LENGTH_LONG).show();
        } else if (abi.isEmpty()) {
            Toast.makeText(getActivity(), R.string.invalid_contract_abi, Toast.LENGTH_LONG).show();
        } else {
            try {
                Contract.checkABI(abi);
                this.account.newContract(new EthContract(this.account.getCoinType(), name, description, website, address, abi, suit, icon, (long) abi.hashCode()));
                Toast.makeText(getActivity(), R.string.contract_added_successfully, Toast.LENGTH_LONG).show();
                getActivity().finish();
            } catch (JSONException e) {
                Toast.makeText(getActivity(), R.string.invalid_contract_abi, Toast.LENGTH_LONG).show();
            }
        }
    }
}
