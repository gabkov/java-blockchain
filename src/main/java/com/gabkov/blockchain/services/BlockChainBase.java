package com.gabkov.blockchain.services;

import com.gabkov.blockchain.Block;
import com.gabkov.blockchain.Wallet;
import com.gabkov.blockchain.transaction.Transaction;
import com.gabkov.blockchain.transaction.TransactionInput;
import com.gabkov.blockchain.transaction.TransactionOutput;
import com.gabkov.blockchain.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class BlockChainBase {

    private WalletCreator walletCreator;
    private List<Wallet> wallets = new ArrayList<>();

    // Initial wallet for starting transactions
    private Wallet walletA;

    private static ArrayList<Block> blockchain = new ArrayList<Block>();
    private static HashMap<String, TransactionOutput> UTXOs = new HashMap<>(); //list of all unspent transactions.

    private static int difficulty = 3;
    private static float minimumTransaction = 0.1f;

    private static Transaction genesisTransaction;

    public static HashMap<String, TransactionOutput> getUTXOs() {
        return UTXOs;
    }

    public List<Transaction> currentTransactions = new ArrayList<>();

    public static float getMinimumTransaction() {
        return minimumTransaction;
    }

    public BlockChainBase(WalletCreator walletCreator){
        this.walletCreator = walletCreator;
    }


    public static void addBlock(Block newBlock) {
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }

    public Wallet getNewWallet(){
        Wallet newWallet = walletCreator.createNewWallet();
        wallets.add(newWallet);
        return newWallet;
    }

    public Wallet getWalletA() {
        return walletA;
    }

    public void genesis(){
        //Create the new wallets
        walletA = new Wallet();
        wallets.add(walletA);
        Wallet coinbase = new Wallet();

        //create genesis transaction, which sends 100 NoobCoin to walletA:
        genesisTransaction = new Transaction(coinbase.getPublicKey(), walletA.getPublicKey(), 100f, null);
        genesisTransaction.generateSignature(coinbase.getPrivateKey());     //manually sign the genesis transaction
        genesisTransaction.setTransactionId("0"); //manually set the transaction id
        genesisTransaction.getOutputs().add(new TransactionOutput(genesisTransaction.getReciepient(), genesisTransaction.getValue(), genesisTransaction.getTransactionId())); //manually add the Transactions Output
        UTXOs.put(genesisTransaction.getOutputs().get(0).getId(), genesisTransaction.getOutputs().get(0)); //its important to store our first transaction in the UTXOs list.

        //System.out.println("Creating and Mining Genesis block... ");
        log.info("Creating and Mining Genesis block... ");
        Block genesis = new Block("0");
        genesis.addTransaction(genesisTransaction);
        addBlock(genesis);
        System.out.println("Genesis merkle root: " + genesis.getMerkleRoot());

    }

    // block1.addTransaction(walletA.sendFunds(walletB.getPublicKey(), 40f));

    public boolean newTransaction(Map<String, String> transactionData){
        Wallet from = getWalletByStringPK(transactionData.get("sender"));
        Wallet to = getWalletByStringPK(transactionData.get("recipient"));
        if (from == null || to == null){
            log.error("Invalid addresses");
            return false;
        }
        Transaction newTransaction = from.sendFunds(to.getPublicKey(), Float.valueOf(transactionData.get("value")));
        if(newTransaction == null) return false;
        currentTransactions.add(newTransaction);
        return true;
    }

    private Wallet getWalletByStringPK(String publicKey){
        for(Wallet wallet : wallets){
            if(StringUtil.getStringFromKey(wallet.getPublicKey()).equals(publicKey)){
                return wallet;
            }
        }
        return null;
    }

    public static Boolean isChainValid() {
        Block currentBlock;
        Block previousBlock;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        HashMap<String, TransactionOutput> tempUTXOs = new HashMap<String, TransactionOutput>(); //a temporary working list of unspent transactions at a given block state.
        tempUTXOs.put(genesisTransaction.getOutputs().get(0).getId(), genesisTransaction.getOutputs().get(0));

        //loop through blockchain to check hashes:
        for (int i = 1; i < blockchain.size(); i++) {

            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i - 1);
            //compare registered hash and calculated hash:
            if (!currentBlock.getHash().equals(currentBlock.calculateHash())) {
                System.out.println("#Current Hashes not equal");
                return false;
            }
            //compare previous hash and registered previous hash
            if (!previousBlock.getHash().equals(currentBlock.getPreviousHash())) {
                System.out.println("#Previous Hashes not equal");
                return false;
            }
            //check if hash is solved
            if (!currentBlock.getHash().substring(0, difficulty).equals(hashTarget)) {
                System.out.println("#This block hasn't been mined");
                return false;
            }

            //loop thru blockchains transactions:
            TransactionOutput tempOutput;
            for (int t = 0; t < currentBlock.getTransactions().size(); t++) {
                Transaction currentTransaction = currentBlock.getTransactions().get(t);

                if (!currentTransaction.verifiySignature()) {
                    System.out.println("#Signature on Transaction(" + t + ") is Invalid");
                    return false;
                }
                if (currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                    System.out.println("#Inputs are note equal to outputs on Transaction(" + t + ")");
                    return false;
                }

                for (TransactionInput input : currentTransaction.getInputs()) {
                    tempOutput = tempUTXOs.get(input.getTransactionOutputId());

                    if (tempOutput == null) {
                        System.out.println("#Referenced input on Transaction(" + t + ") is Missing");
                        return false;
                    }

                    if (input.getUTXO().getValue() != tempOutput.getValue()) {
                        System.out.println("#Referenced input Transaction(" + t + ") value is Invalid");
                        return false;
                    }

                    tempUTXOs.remove(input.getTransactionOutputId());
                }

                for (TransactionOutput output : currentTransaction.getOutputs()) {
                    tempUTXOs.put(output.getId(), output);
                }

                if (currentTransaction.getOutputs().get(0).getReciepient() != currentTransaction.getReciepient()) {
                    System.out.println("#Transaction(" + t + ") output reciepient is not who it should be");
                    return false;
                }
                if (currentTransaction.getOutputs().get(1).getReciepient() != currentTransaction.getSender()) {
                    System.out.println("#Transaction(" + t + ") output 'change' is not sender.");
                    return false;
                }

            }

        }
        System.out.println("Blockchain is valid");
        return true;
    }
}
