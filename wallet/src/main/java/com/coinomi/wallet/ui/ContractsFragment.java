package com.coinomi.wallet.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import butterknife.Unbinder;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.core.wallet.families.eth.EthContract;
import com.coinomi.core.wallet.families.eth.EthFamilyWallet;
import com.coinomi.wallet.R;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.ui.adaptors.ContractsListAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContractsFragment extends Fragment {
    private static final String[] CONTRACT_TYPES = new String[]{"ERC20"};
    private static final Logger log = LoggerFactory.getLogger(ContractsFragment.class);
    private EthFamilyWallet account;
    ContractsListAdapter adapter;
    private WalletApplication application;
    @BindView(2131689704)
    ListView contractRows;
    AutoCompleteTextView filter;
    Listener listener;
    private Unbinder unbinder;

    class C04051 implements TextWatcher {
        C04051() {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            System.out.println("Text [" + s + "]");
            ContractsFragment.this.adapter.getFilter().filter(s.toString());
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void afterTextChanged(Editable s) {
        }
    }

    public interface Listener {
        void showSubtype(String str, CoinType coinType);
    }

    public static ContractsFragment newInstance(String accountId) {
        ContractsFragment fragment = new ContractsFragment();
        Bundle args = new Bundle();
        args.putString("account_id", accountId);
        fragment.setArguments(args);
        return fragment;
    }

    public static ContractsFragment newInstance() {
        return new ContractsFragment();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        WalletAccount a = this.application.getAccount(getArguments().getString("account_id"));
        if (a == null || !(a instanceof EthFamilyWallet)) {
            Toast.makeText(getActivity(), R.string.no_such_pocket_error, Toast.LENGTH_LONG).show();
            getActivity().finish();
            return;
        }
        this.account = (EthFamilyWallet) a;
    }

    private void addHeaderAndFooterToList(LayoutInflater inflater, ViewGroup container, View view) {
        ListView list = (ListView) ButterKnife.findById(view, (int) R.id.contract_rows);
        View header = inflater.inflate(R.layout.fragment_contracts_header, null);
        list.addHeaderView(header, null, true);
        ArrayAdapter<String> typesAdapter = new ArrayAdapter(getContext(), 17367050, CONTRACT_TYPES);
        this.filter = (AutoCompleteTextView) header.findViewById(R.id.contracts_query);
        this.filter.setAdapter(typesAdapter);
        View listFooter = new View(inflater.getContext());
        listFooter.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin));
        list.addFooterView(listFooter);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_contract:
                Intent intent = new Intent(getActivity(), AddContractActivity.class);
                intent.putExtra("account_id", this.account.getId());
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contracts, container, false);
        addHeaderAndFooterToList(inflater, container, view);
        this.unbinder = ButterKnife.bind((Object) this, view);
        if (this.account != null) {
            setupAdapter(inflater);
            this.filter.addTextChangedListener(new C04051());
        }
        return view;
    }

    private void setupAdapter(LayoutInflater inflater) {
        this.adapter = new ContractsListAdapter(inflater.getContext(), this.account);
        this.adapter.sortByName(true);
        this.contractRows.setAdapter(this.adapter);
    }

    public void onDestroyView() {
        this.unbinder.unbind();
        super.onDestroyView();
    }

    public void onResume() {
        super.onResume();
        this.adapter.replace(this.account.getContracts());
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.listener = (Listener) context;
        this.application = (WalletApplication) context.getApplicationContext();
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement ContractsFragment Listener.");
        }
    }

    @OnItemClick({2131689704})
    public void onItemClick(int position) {
        if (position >= this.contractRows.getHeaderViewsCount()) {
            EthContract obj = (EthContract)this.contractRows.getItemAtPosition(position);
            if (obj == null || !(obj instanceof EthContract)) {
                Toast.makeText(getActivity(), getString(R.string.get_contract_info_error), Toast.LENGTH_LONG).show();
                return;
            }
            EthContract contract = obj;
            String contractId = obj.getContractAddress();
            if (contract.isContractType("erc20") || contract.getContractSuitName().equalsIgnoreCase("token-interface")) {
                this.listener.showSubtype(this.account.getId(), contract.getSubType());
                return;
            }
            Intent intent = new Intent(getActivity(), ContractDetailsActivity.class);
            intent.putExtra("account_id", this.account.getId());
            intent.putExtra("contract_id", ((EthContract) obj).getContractAddress());
            String str = "show_default";
            boolean z = ((EthContract) obj).getContractSuit() == null || ((EthContract) obj).getContractSuit().isEmpty();
            intent.putExtra(str, z);
            startActivity(intent);
        }
    }
}
