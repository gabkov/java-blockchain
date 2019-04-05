package com.gabkov.blockchain.transaction;

import com.gabkov.blockchain.services.BlockChainBase;
import com.gabkov.blockchain.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.security.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;


@Slf4j
public class Transaction {

    private String transactionId; // this is also the hash of the transaction.
    private PublicKey sender; // senders address/public key.
    private PublicKey reciepient; // Recipients address/public key.
    private float value;
    private byte[] signature; // this is to prevent anybody else from spending funds in our wallet.

    private ArrayList<TransactionInput> inputs;
    private ArrayList<TransactionOutput> outputs = new ArrayList<>();

    private static int sequence = 0; // a rough count of how many transactions have been generated.

    public Transaction(PublicKey from, PublicKey to, float value, ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.reciepient = to;
        this.value = value;
        this.inputs = inputs;
    }

    // This Calculates the transaction hash (which will be used as its Id)
    private String calulateHash() {
        sequence++; //increase the sequence to avoid 2 identical transactions having the same hash
        return StringUtil.applySha256(StringUtil.getStringFromKey(sender) +
                StringUtil.getStringFromKey(reciepient) +
                value + sequence
        );
    }

    //Signs all the data we dont wish to be tampered with.
    public void generateSignature(PrivateKey privateKey) {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepient) + value;
        signature = StringUtil.applyECDSASig(privateKey, data);
    }

    //Verifies the data we signed hasnt been tampered with
    public boolean verifiySignature() {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepient) + value;
        return StringUtil.verifyECDSASig(sender, data, signature);
    }

    //Returns true if new transaction could be created.
    public boolean processTransaction() {
        if (verifiySignature() == false) {
            log.error("#Transaction Signature failed to verify");
            return false;
        }

        //gather transaction inputs (Make sure they are unspent):
        for (TransactionInput i : inputs) {
            i.setUTXO(BlockChainBase.getUTXOs().get(i.getTransactionOutputId()));
        }

        //check if transaction is valid:
        if (value < BlockChainBase.getMinimumTransaction()) {
            log.error("#Transaction Inputs to small: " + value);
            return false;
        }

        //generate transaction outputs:
        float leftOver = getInputsValue() - value; //get value of inputs then the left over change:
        transactionId = calulateHash();
        outputs.add(new TransactionOutput(this.reciepient, value, transactionId)); //send value to recipient
        outputs.add(new TransactionOutput(this.sender, leftOver, transactionId)); //send the left over 'change' back to sender

        //add outputs to Unspent list
        for (TransactionOutput o : outputs) {
            BlockChainBase.getUTXOs().put(o.getId(), o);
        }

        //remove transaction inputs from UTXO lists as spent:
        for (TransactionInput i : inputs) {
            if (i.getUTXO() == null) continue; //if Transaction can't be found skip it
            BlockChainBase.getUTXOs().remove(i.getUTXO().getId());
        }

        return true;
    }

    public LinkedHashMap<String, String> getTransactionInfo(){
        LinkedHashMap<String, String> transaction = new LinkedHashMap<>();
        transaction.put("transaction id", transactionId);
        transaction.put("sender", StringUtil.getStringFromKey(sender));
        transaction.put("recipient", StringUtil.getStringFromKey(reciepient));
        transaction.put("amount", String.valueOf(value));
        return transaction;
    }

    //returns sum of inputs(UTXOs) values
    public float getInputsValue() {
        float total = 0;
        for (TransactionInput i : inputs) {
            if (i.getUTXO() == null) continue; //if Transaction can't be found skip it
            total += i.getUTXO().getValue();
        }
        return total;
    }

    //returns sum of outputs:
    public float getOutputsValue() {
        float total = 0;
        for (TransactionOutput o : outputs) {
            total += o.getValue();
        }
        return total;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public PublicKey getSender() {
        return sender;
    }


    public PublicKey getReciepient() {
        return reciepient;
    }


    public float getValue() {
        return value;
    }

    public ArrayList<TransactionInput> getInputs() {
        return inputs;
    }


    public ArrayList<TransactionOutput> getOutputs() {
        return outputs;
    }
}
