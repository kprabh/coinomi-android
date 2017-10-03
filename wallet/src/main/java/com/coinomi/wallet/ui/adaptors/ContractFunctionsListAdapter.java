package com.coinomi.wallet.ui.adaptors;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import butterknife.ButterKnife;
import com.coinomi.core.coins.eth.CallTransaction.Function;
import com.coinomi.core.wallet.families.eth.EthContract;
import com.coinomi.wallet.R;
import com.coinomi.wallet.util.Fonts;
import com.coinomi.wallet.util.Fonts.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContractFunctionsListAdapter extends BaseAdapter {
    private final Context context;
    private final List<Function> contractsFunctions = new ArrayList();
    private final LayoutInflater inflater;

    public ContractFunctionsListAdapter(Context context, EthContract contract) {
        this.inflater = LayoutInflater.from(context);
        this.context = context;
        setContract(contract);
    }

    public boolean isEmpty() {
        return this.contractsFunctions.isEmpty();
    }

    public int getCount() {
        return this.contractsFunctions.size();
    }

    public Function getItem(int position) {
        return (Function) this.contractsFunctions.get(position);
    }

    public long getItemId(int position) {
        return (long) position;
    }

    public View getView(int position, View row, ViewGroup parent) {
        if (row == null) {
            row = this.inflater.inflate(R.layout.contract_function_spinner_item, null);
        }
        bindView(row, getItem(position));
        return row;
    }

    private void bindView(View row, Function function) {
        ((TextView) row.findViewById(R.id.function_name)).setText(function.name.isEmpty() ? this.context.getString(R.string.function_default) : function.name);
        TextView costFontIcon = (TextView) ButterKnife.findById(row, (int) R.id.function_cost_money);
        Fonts.setTypeface(costFontIcon, Font.COINOMI_FONT_ICONS);
        costFontIcon.setVisibility(function.constant ? 8 : 0);
    }

    public void setContract(EthContract contract) {
        this.contractsFunctions.clear();
        this.contractsFunctions.addAll(Arrays.asList(contract.getContract().functions));
    }
}
