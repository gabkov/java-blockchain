package com.gabkov.blockchain.transaction;

import com.gabkov.blockchain.utils.StringUtil;

import java.security.PublicKey;

/**
 * Transaction outputs will show the final amount sent to each party from the transaction.
 * These, when referenced as inputs in new transactions, act as proof that you have coins to send.
 */

public class TransactionOutput {
    private String id;
    private PublicKey reciepient; //also known as the new owner of these coins.
    private float value; //the amount of coins they own
    private String parentTransactionId; //the id of the transaction this output was created in

    //Constructor
    public TransactionOutput(PublicKey reciepient, float value, String parentTransactionId) {
        this.reciepient = reciepient;
        this.value = value;
        this.parentTransactionId = parentTransactionId;
        this.id = StringUtil.applySha256(StringUtil.getStringFromKey(reciepient) + value + parentTransactionId);
    }

    //Check if coin belongs to you
    public boolean isMine(PublicKey publicKey) {
        return (publicKey == reciepient);
    }

    public String getId() {
        return id;
    }

    public PublicKey getReciepient() {
        return reciepient;
    }

    public float getValue() {
        return value;
    }

}