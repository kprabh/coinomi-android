package com.coinomi.wallet.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.wallet.families.eth.EthFamilyWallet;
import com.coinomi.core.uri.CoinURI;
import com.coinomi.core.uri.CoinURIParseException;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.wallet.Constants;
import com.coinomi.core.wallet.families.eth.ERC20Token;
import com.coinomi.core.wallet.families.eth.EthFamilyWallet;
import com.coinomi.wallet.R;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.util.Keyboard;
import com.coinomi.wallet.util.WeakHandler;
import com.coinomi.core.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.coinomi.wallet.util.UiUtils.toastGenericError;

/**
 * @author John L. Jegutanis
 */
public class AccountFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(AccountFragment.class);

    private static final String ACCOUNT_CURRENT_SCREEN = "account_current_screen";
    private static final int NUM_OF_SCREENS = 3;
    // Set offscreen page limit to 2 because receive fragment draws a QR code and we don't
    // want to re-render that if we go to the SendFragment and back
    private static final int OFF_SCREEN_LIMIT = 2;

    // Screen ids
    private static final int RECEIVE = 0;
    private static final int BALANCE = 1;
    private static final int SEND = 2;
    private static final int CONTRACT = 3;
    // Handler ids
    private static final int SEND_TO_URI = 0;

    private int currentScreen;
    @BindView(R.id.pager) ViewPager viewPager;
    NavigationDrawerFragment mNavigationDrawerFragment;
    @Nullable private WalletAccount account;
    private Listener listener;
    private WalletApplication application;
    private final MyHandler handler = new MyHandler(this);
    private CoinType subType;
    private Unbinder unbinder;
    public static AccountFragment getInstance() {
        AccountFragment fragment = new AccountFragment();
        fragment.setArguments(new Bundle());
        return fragment;
    }

    public static AccountFragment getInstance(String accountId) {
        AccountFragment fragment = getInstance();
        fragment.setupArgs(accountId);
        return fragment;
    }

    private void setupArgs(String accountId) {
        getArguments().putString(Constants.ARG_ACCOUNT_ID, accountId);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        this.account = (WalletAccount) Preconditions.checkNotNull(this.application.getAccount(getArguments().getString("account_id")));
        this.subType = null;
        if (getArguments().containsKey("sub_coin_id")) {
            this.subType = this.account.getCoinType(getArguments().getString("sub_coin_id"));
            if (this.subType != null && (this.subType instanceof ERC20Token) && (this.account instanceof EthFamilyWallet)) {
                ((EthFamilyWallet) this.account).subscribeToContract(((ERC20Token) this.subType).getAddress());
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);
        unbinder = ButterKnife.bind(this, view);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);

        viewPager.setOffscreenPageLimit(OFF_SCREEN_LIMIT);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                currentScreen = position;
                if (position == BALANCE) Keyboard.hideKeyboard(getActivity());
                if (listener != null) {
                    switch (position) {
                        case RECEIVE:
                            listener.onReceiveSelected();
                            break;
                        case BALANCE:
                            listener.onBalanceSelected();
                            break;
                        case SEND:
                            listener.onSendSelected();
                            break;
                        case CONTRACT:
                            AccountFragment.this.listener.onContractsSelected();
                            return;
                        default:
                            throw new RuntimeException("Unknown screen item: " + position);
                    }
                }
            }

            @Override public void onPageScrolled(int pos, float posOffset, int posOffsetPixels) { }
            @Override public void onPageScrollStateChanged(int state) { }
        });

        viewPager.setAdapter(
                new AppSectionsPagerAdapter(getActivity(), getChildFragmentManager(), account, subType));

        return view;
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        mNavigationDrawerFragment = null;
        super.onDestroyView();
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        try {
            this.listener = (Listener) context;
            this.application = (WalletApplication) context.getApplicationContext();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + Listener.class);
        }
    }

    @Override
    public void onDetach() {
        listener = null;
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(ACCOUNT_CURRENT_SCREEN, currentScreen);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            currentScreen = savedInstanceState.getInt(ACCOUNT_CURRENT_SCREEN, BALANCE);
        } else {
            currentScreen = BALANCE;
        }
        updateView();
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mNavigationDrawerFragment != null && !mNavigationDrawerFragment.isDrawerOpen() &&
                isVisible() && account != null) {

            switch (viewPager.getCurrentItem()) {
                case RECEIVE:
                    inflater.inflate(R.menu.request, menu);
                    MenuItem newAddressItem = menu.findItem(R.id.action_new_address);
                    if (newAddressItem != null) {
                        newAddressItem.setVisible(account.canCreateNewAddresses());
                    }
                    break;
                case BALANCE:
                    if (this.subType == null || !(this.account instanceof EthFamilyWallet) || !(this.subType instanceof ERC20Token)) {
                        inflater.inflate(R.menu.balance, menu);
                        menu.findItem(R.id.action_sign_verify_message).setVisible(this.account.getCoinType().canSignVerifyMessages());
                        return;
                    } else if (((EthFamilyWallet) this.account).isFavorite(this.subType)) {
                        inflater.inflate(R.menu.token_remove_from_favorites, menu);
                        return;
                    } else {
                        inflater.inflate(R.menu.token_add_to_favorites, menu);
                        return;
                    }
                case SEND:
                    inflater.inflate(R.menu.send, menu);
                    break;case CONTRACT:
                    inflater.inflate(R.menu.contract_add_new, menu);
                    break;
            }
        }
    }

    private void updateView() {
        goToItem(currentScreen, true);
    }

    @Nullable
    public WalletAccount getAccount() {
        return account;
    }

    public void sendToUri(final CoinURI coinUri) {
        if (viewPager != null) {
            viewPager.setCurrentItem(SEND);
            handler.sendMessage(handler.obtainMessage(SEND_TO_URI, coinUri));
        } else {
            // Should not happen
            toastGenericError(getContext());
        }
    }

    private void setSendToUri(CoinURI coinURI) {
        if (viewPager != null) viewPager.setCurrentItem(SEND);
        SendFragment f = getSendFragment();
        if (f != null) {
            try {
                f.updateStateFrom(coinURI);
            } catch (CoinURIParseException e) {
                Toast.makeText(getContext(),
                        getString(R.string.scan_error, e.getMessage()),
                        Toast.LENGTH_LONG).show();
            }
        } else {
            log.warn("Expected fragment to be not null");
            toastGenericError(getContext());
        }
    }

    @Nullable
    private SendFragment getSendFragment() {
        return (SendFragment) getFragment(getChildFragmentManager(), SEND);
    }

    @Nullable
    private static Fragment getFragment(FragmentManager fm, int item) {
        if (fm.getFragments() == null) return null;

        for (Fragment f : fm.getFragments()) {
            switch (item) {
                case RECEIVE:
                    if (f instanceof AddressRequestFragment) return f;
                    break;
                case BALANCE:
                    if (f instanceof BalanceFragment) return f;
                    break;
                case SEND:
                    if (f instanceof SendFragment) return f;
                    break;
                case CONTRACT:
                    if (f instanceof ContractsFragment) return f;
                    break;
                default:
                    throw new RuntimeException("Cannot get fragment, unknown screen item: " + item);
            }
        }
        return null;
    }

    @SuppressWarnings({ "unchecked"})
    private static <T extends Fragment> T createFragment(WalletAccount account, CoinType subType, int item) {
        String accountId = account.getId();
        switch (item) {
            case RECEIVE:
                return (T) AddressRequestFragment.newInstance(accountId, subType);
            case BALANCE:
                return (T) BalanceFragment.newInstance(accountId, subType);
            case SEND:
                return (T) SendFragment.newInstance(accountId, subType);
            case CONTRACT:
                if (subType == null || !(subType instanceof ERC20Token)) {
                    return (T) ContractsFragment.newInstance(accountId);
                }
                return (T) ContractDetailsFragment.newInstance(accountId, (ERC20Token) subType);

            default:
                throw new RuntimeException("Cannot create fragment, unknown screen item: " + item);
        }
    }

    public boolean goToReceive(boolean smoothScroll) {
        return goToItem(RECEIVE, smoothScroll);
    }

    public boolean goToBalance(boolean smoothScroll) {
        return goToItem(BALANCE, smoothScroll);
    }

    public boolean goToSend(boolean smoothScroll) {
        return goToItem(SEND, smoothScroll);
    }

    private boolean goToItem(int item, boolean smoothScroll) {
        if (viewPager != null && viewPager.getCurrentItem() != item) {
            viewPager.setCurrentItem(item, smoothScroll);
            return true;
        }
        return false;
    }

    public boolean resetSend() {
        SendFragment f = getSendFragment();
        if (f != null) {
            f.reset();
            return true;
        }
        return false;
    }

    public boolean hasSubType() {
        return this.subType != null;
    }

    private static class AppSectionsPagerAdapter extends FragmentPagerAdapter {
        private final String receiveTitle;
        private final String sendTitle;
        private final String balanceTitle;
        private Fragment contracts;
        private final String contractsTitle;
        private AddressRequestFragment request;
        private SendFragment send;
        private BalanceFragment balance;
        private CoinType subType;
        private WalletAccount account;

        public AppSectionsPagerAdapter(Context context, FragmentManager fm, WalletAccount account, CoinType subType) {
            super(fm);
            receiveTitle = context.getString(R.string.wallet_title_request);
            sendTitle = context.getString(R.string.wallet_title_send);
            balanceTitle = context.getString(R.string.wallet_title_balance);
            this.account = account;
            this.subType = subType;
            if (subType == null) {
                this.contractsTitle = context.getString(R.string.wallet_title_contracts);
            } else {
                this.contractsTitle = context.getString(R.string.info);
            }
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case RECEIVE:
                    if (request == null) request = createFragment(account, subType, i);
                    return request;
                case SEND:
                    if (send == null) send = createFragment(account, subType, i);
                    return send;
                case BALANCE:
                    if (balance == null) balance = createFragment(account, subType, i);
                    return balance;
                case CONTRACT:
                    if (contracts == null) contracts = createFragment(account, subType, i);
                    return contracts;
                default:
                    throw new RuntimeException("Cannot get item, unknown screen item: " + i);
            }
        }


        @Override
        public int getCount() {
            return this.account instanceof EthFamilyWallet ? 4 : 3;
        }

        @Override
        public CharSequence getPageTitle(int i) {
            switch (i) {
                case RECEIVE: return receiveTitle;
                case SEND: return sendTitle;
                case BALANCE: return balanceTitle;
                case CONTRACT: return contractsTitle;
                default: throw new RuntimeException("Cannot get item, unknown screen item: " + i);
            }
        }
    }

    private static class MyHandler extends WeakHandler<AccountFragment> {
        public MyHandler(AccountFragment ref) { super(ref); }

        @Override
        protected void weakHandleMessage(AccountFragment ref, Message msg) {
            switch (msg.what) {
                case SEND_TO_URI:
                    ref.setSendToUri((CoinURI) msg.obj);
                    break;
            }
        }
    }

    public interface Listener extends BalanceFragment.Listener, SendFragment.Listener {
        // TODO make an external interface so that SendFragment and AddressRequestFragment can use.
        void registerActionMode(ActionMode actionMode);
        void onReceiveSelected();
        void onBalanceSelected();
        void onSendSelected();void onContractsSelected();
    }
}
