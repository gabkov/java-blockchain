package com.gabkov.blockchain;

import com.gabkov.blockchain.transaction.Transaction;
import com.gabkov.blockchain.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;

@Slf4j
public class Block {

    private String hash;
    private String previousHash;
    private String merkleRoot;
    private ArrayList<Transaction> transactions = new ArrayList<>();
    private long timeStamp; //as number of milliseconds since 1/1/1970.
    private int nonce;

    //Block Constructor.
    public Block(String previousHash) {
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();
        this.hash = calculateHash();
    }
    public String calculateHash() {
        String calculatedhash = StringUtil.applySha256(previousHash + timeStamp + nonce + merkleRoot);

        return calculatedhash;
    }
    public void mineBlock(int difficulty) {
        merkleRoot = StringUtil.getMerkleRoot(transactions); // calculating merkleRoot before mining
        String target = new String(new char[difficulty]).replace('\0', '0'); //Create a string with difficulty * "0"
        while (!hash.substring(0, difficulty).equals(target)) {
            nonce++;
            hash = calculateHash();
        }
        log.info("Block Mined : " + hash);
    }


    //Add transactions to this block
    public boolean addTransaction(Transaction transaction) {
        //process transaction and check if valid, unless block is genesis block then ignore.
        if (transaction == null) return false;
        // since coinbase transaction don't have inputs it is set to null so ignore it
        if ((!previousHash.equals("0")) && transaction.getInputs() != null) {
            if ((transaction.processTransaction() != true)) {
                log.error("Transaction failed to process. Discarded.");
                return false;
            }
        }
        // if it is a coinbase transaction put to the beginning of transactions since that is the first transaction at a block
        if(transaction.getInputs() == null){
            transactions.add(0, transaction);
        } else {
            transactions.add(transaction);
        }
        log.info("Transaction Successfully added to Block");
        log.info("Transaction hash: " + transaction.getTransactionId());
        return true;
    }

    public String getHash() {
        return hash;
    }
    public String getPreviousHash() {
        return previousHash;
    }

    public ArrayList<Transaction> getTransactions() {
        return transactions;
    }

    public String getMerkleRoot() {
        return merkleRoot;
    }


    public long getTimeStamp() {
        return timeStamp;
    }

    public int getNonce() {
        return nonce;
    }
}