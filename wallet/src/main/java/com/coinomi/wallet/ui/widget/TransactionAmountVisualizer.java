package com.coinomi.wallet.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.messages.TxMessage;
import com.coinomi.core.util.ExchangeRate;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.util.TransactionUtils;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.AbstractTransaction;
import com.coinomi.core.wallet.AbstractTransaction.AbstractOutput;
import com.coinomi.core.wallet.AbstractWallet;
import com.coinomi.core.wallet.families.eth.EthFamilyWallet;
import com.coinomi.core.wallet.families.eth.EthTransaction;
import com.coinomi.wallet.R;
import com.google.common.collect.ImmutableList;

import java.util.List;

import javax.annotation.Nullable;

/**
 * @author John L. Jegutanis
 *
 * TODO TransactionAmountVisualizerAdapter does a similar function, keep only one
 */
public class TransactionAmountVisualizer extends LinearLayout {

    private final SendOutput output;
    private final SendOutput fee;
    private final TextView txMessageLabel;
    private final TextView txMessage;
    private Value outputAmount;
    private Value feeAmount;
    private boolean isSending;
    private AbstractAddress address;
    private CoinType type;

    public TransactionAmountVisualizer(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.transaction_amount_visualizer, this, true);

        output = (SendOutput) findViewById(R.id.transaction_output);
        output.setVisibility(View.GONE);
        fee = (SendOutput) findViewById(R.id.transaction_fee);
        fee.setVisibility(View.GONE);
        txMessageLabel = (TextView) findViewById(R.id.tx_message_label);
        txMessage = (TextView) findViewById(R.id.tx_message);

        if (isInEditMode()) {
            output.setVisibility(View.VISIBLE);
            fee.setVisibility(View.VISIBLE);
        }
    }

    public void setContractTransaction(AbstractWallet pocket, AbstractTransaction tx, Value tradeDepositAmount) {
        if (tradeDepositAmount != null) {
            setTransaction(pocket, tx);
            this.outputAmount = tradeDepositAmount;
            this.output.setAmount(GenericUtils.formatCoinValue(tradeDepositAmount.type, this.outputAmount));
            this.output.setSymbol(tradeDepositAmount.type.getSymbol());
            return;
        }
        setTransaction(pocket, tx);
    }

    public void setTransaction(@Nullable AbstractWallet pocket, AbstractTransaction tx) {
        type = tx.getType();
        String symbol = type.getSymbol();

        final Value value = pocket != null ? tx.getValue(pocket) : type.value(0);
        isSending = TransactionUtils.isSending(value);
        // if sending and all the outputs point inside the current pocket. If received
        boolean isInternalTransfer = isSending;
        output.setVisibility(View.VISIBLE);
        List<AbstractOutput> outputs = tx.getSentTo();
        for (AbstractOutput txo : outputs) {
            if (isSending) {
                if (pocket != null && pocket.isAddressMine(txo.getAddress())) continue;
                isInternalTransfer = false;
            } else {
                if (pocket != null && !pocket.isAddressMine(txo.getAddress())) continue;
            }

            // TODO support more than one output
            outputAmount = txo.getValue();
            output.setAmount(GenericUtils.formatCoinValue(type, outputAmount));
            output.setSymbol(symbol);
            address = txo.getAddress();
            output.setLabelAndAddress(address);
            break; // TODO remove when supporting more than one output
        }

        if (isInternalTransfer) {
            output.setLabel(getResources().getString(R.string.internal_transfer));
        }

        output.setSending(isSending);

        feeAmount = tx.getFee();
        if (isSending && feeAmount != null && !feeAmount.isZero()) {
            fee.setVisibility(View.VISIBLE);
            fee.setAmount(GenericUtils.formatCoinValue(type, feeAmount));
            fee.setSymbol(symbol);
        }

        if (type.canHandleMessages()) {
            setMessage(type.getMessagesFactory().extractPublicMessage(tx));
        }
        if ((tx instanceof EthTransaction) && ((EthTransaction) tx).getData() != null && ((EthFamilyWallet) pocket).getAllContracts().containsKey(((AbstractOutput) tx.getSentTo().get(0)).getAddress().toString())) {
            String message = ((EthTransaction) tx).getData();
            if (message != null) {
                this.txMessageLabel.setText(R.string.contract_data);
                this.txMessageLabel.setVisibility(View.VISIBLE);
                this.txMessage.setText(message);
                this.txMessage.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setMessage(TxMessage message) {
        if (message != null) {
            switch (message.getType()) {
                case PRIVATE:
                    txMessageLabel.setText(R.string.tx_message_private);
                    break;
                case PUBLIC:
                    txMessageLabel.setText(R.string.tx_message_public);
                    break;
            }
            txMessageLabel.setVisibility(VISIBLE);

            txMessage.setText(message.toString());
            txMessage.setVisibility(VISIBLE);
        }
    }

    public void setExchangeRate(ExchangeRate rate, ExchangeRate baseRate) {
        if (outputAmount != null) {
            Value fiatAmount = rate.convert(type, outputAmount.toCoin());
            output.setAmountLocal(GenericUtils.formatFiatValue(fiatAmount));
            output.setSymbolLocal(fiatAmount.type.getSymbol());
        }

        if (isSending && feeAmount != null) {
            if (baseRate == null) {
                baseRate = rate;
            }
            Value fiatAmount = baseRate.convert(type, feeAmount.toCoin());
            fee.setAmountLocal(GenericUtils.formatFiatValue(fiatAmount));
            fee.setSymbolLocal(fiatAmount.type.getSymbol());
        }
    }

    /**
     * Hide the output address and label. Useful when we are exchanging, where the send address is
     * not important to the user.
     */
    public void hideAddresses() {
        output.hideLabelAndAddress();
    }

    public void resetLabels() {
        output.setLabelAndAddress(address);
    }

    public List<SendOutput> getOutputs() {
        return ImmutableList.of(output);
    }
}
