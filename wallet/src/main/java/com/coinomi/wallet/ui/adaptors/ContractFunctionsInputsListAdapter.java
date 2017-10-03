package com.coinomi.wallet.ui.adaptors;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;
import com.coinomi.core.coins.eth.CallTransaction.Param;
import com.coinomi.wallet.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContractFunctionsInputsListAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final List<Param> inputs = new ArrayList();

    public ContractFunctionsInputsListAdapter(Context context, Param[] inputs) {
        this.inflater = LayoutInflater.from(context);
        setInputs(inputs);
    }

    public boolean isEmpty() {
        return this.inputs.isEmpty();
    }

    public int getCount() {
        return this.inputs.size();
    }

    public Param getItem(int position) {
        return (Param) this.inputs.get(position);
    }

    public long getItemId(int position) {
        return (long) position;
    }

    public View getView(int position, View row, ViewGroup parent) {
        if (row == null) {
            row = this.inflater.inflate(R.layout.contract_function_input_item, null);
        }
        bindView(row, getItem(position));
        return row;
    }

    private void bindView(View row, Param param) {
        ((TextView) row.findViewById(R.id.function_input_name)).setText(param.name);
        ((EditText) row.findViewById(R.id.function_input_edit)).setHint(param.type.getName());
    }

    public void setInputs(Param[] inputs) {
        this.inputs.clear();
        this.inputs.addAll(Arrays.asList(inputs));
    }
}
