package com.gabkov.blockchain.services;

import com.gabkov.blockchain.Block;
import com.gabkov.blockchain.Wallet;
import com.gabkov.blockchain.transaction.Transaction;
import com.gabkov.blockchain.transaction.TransactionInput;
import com.gabkov.blockchain.transaction.TransactionOutput;
import com.gabkov.blockchain.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

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

    private Block currentBlock;

    public static HashMap<String, TransactionOutput> getUTXOs() {
        return UTXOs;
    }

    public static float getMinimumTransaction() {
        return minimumTransaction;
    }

    public BlockChainBase(WalletCreator walletCreator) {
        this.walletCreator = walletCreator;
    }

    public static void addBlock(Block newBlock) {
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }

    public Wallet getNewWallet() {
        Wallet newWallet = walletCreator.createNewWallet();
        wallets.add(newWallet);
        return newWallet;
    }

    public Wallet getWalletA() {
        return walletA;
    }

    public static List<LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, String>>>> getBlockchain() {

        List<LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, String>>>> formattedBlockChain = new ArrayList<>();

        for (int i = 0; i < blockchain.size(); i++) {
            LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, String>>> formattedBlock = new LinkedHashMap<>();
            LinkedHashMap<String, LinkedHashMap<String, String>> formattedBlockData = new LinkedHashMap<>();

            Block block = blockchain.get(i);

            LinkedHashMap<String, String> blockHeader = new LinkedHashMap<>();
            blockHeader.put("hash", block.getHash());
            blockHeader.put("previous hash", block.getPreviousHash());
            blockHeader.put("merkle root", block.getMerkleRoot());
            blockHeader.put("timestamp", String.valueOf(block.getTimeStamp()));
            blockHeader.put("nonce", String.valueOf(block.getNonce()));
            blockHeader.put("number of transactions", String.valueOf(block.getTransactions().size()));

            formattedBlockData.put("block header", blockHeader);

            for (int j = 0; j < block.getTransactions().size(); j++) {
                LinkedHashMap<String, String> transaction = block.getTransactions().get(j).getTransactionInfo();
                formattedBlockData.put("transaction " + j, transaction);
            }

            formattedBlock.put("Block " + i, formattedBlockData);
            formattedBlockChain.add(formattedBlock);
        }
        return formattedBlockChain;
    }

    public static LinkedHashMap<String, String> getTransactionInfo(String transactionId) {
        for (Block block : blockchain) {
            for (Transaction transaction : block.getTransactions()) {
                if (transaction.getTransactionId().equals(transactionId)) {
                    LinkedHashMap<String, String> requiredTransaction = transaction.getTransactionInfo();
                    requiredTransaction.put("included in block", block.getHash());
                    return requiredTransaction;
                }
            }
        }
        return null;
    }

    public void genesis() {
        //Create the new wallets
        walletA = new Wallet();
        wallets.add(walletA);
        Wallet coinbase = new Wallet();

        //create genesis transaction, which sends 100 DumbCoin to walletA:
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
        log.info("Genesis merkle root: " + genesis.getMerkleRoot());
        currentBlock = new Block(genesis.getHash());

    }

    public boolean newTransaction(Map<String, String> transactionData) {
        // if the current block is already in the blockchain it means we need to start a new block to add the next transactions
        if (blockchain.contains(currentBlock)) {
            currentBlock = new Block(blockchain.get(blockchain.size() - 1).getHash());
        }
        Wallet from = getWalletByStringPK(transactionData.get("sender"));
        Wallet to = getWalletByStringPK(transactionData.get("recipient"));
        float value = Float.parseFloat(transactionData.get("value"));
        if (from == null || to == null) {
            log.error("Invalid addresses");
            return false;
        }
        // If sendFunds return null it means that there was not enough funds in the sender wallet for the transaction
        Transaction newTransaction = from.sendFunds(to.getPublicKey(), value);
        // Adding the new transaction to the currentBlock, more validation inside addTransaction <-- returns a boolean
        return currentBlock.addTransaction(newTransaction);
    }

    private Wallet getWalletByStringPK(String publicKey) {
        for (Wallet wallet : wallets) {
            if (StringUtil.getStringFromKey(wallet.getPublicKey()).equals(publicKey)) {
                return wallet;
            }
        }
        return null;
    }

    public Wallet getWalletAndBalance(String address) {
        Wallet wallet = getWalletByStringPK(address);
        return wallet;
    }

    public void mineNextBlock() {
        // if the current block is already in the blockchain it means we need to start a new block, this is only used when there are no transactions and just mine a new "empty" block
        if (blockchain.contains(currentBlock)) {
            currentBlock = new Block(blockchain.get(blockchain.size() - 1).getHash());
        }
        addBlock(currentBlock);
        isChainValid();
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
                log.error("#Current Hashes not equal");
                return false;
            }
            //compare previous hash and registered previous hash
            if (!previousBlock.getHash().equals(currentBlock.getPreviousHash())) {
                log.error("#Previous Hashes not equal");
                return false;
            }
            //check if hash is solved
            if (!currentBlock.getHash().substring(0, difficulty).equals(hashTarget)) {
                log.error("#This block hasn't been mined");
                return false;
            }

            //loop thru blockchains transactions:
            TransactionOutput tempOutput;
            for (int t = 0; t < currentBlock.getTransactions().size(); t++) {
                Transaction currentTransaction = currentBlock.getTransactions().get(t);

                if (!currentTransaction.verifiySignature()) {
                    log.error("#Signature on Transaction(" + t + ") is Invalid");
                    return false;
                }
                if (currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                    log.error("#Inputs are note equal to outputs on Transaction(" + t + ")");
                    return false;
                }

                for (TransactionInput input : currentTransaction.getInputs()) {
                    tempOutput = tempUTXOs.get(input.getTransactionOutputId());

                    if (tempOutput == null) {
                        log.error("#Referenced input on Transaction(" + t + ") is Missing");
                        return false;
                    }

                    if (input.getUTXO().getValue() != tempOutput.getValue()) {
                        log.error("#Referenced input Transaction(" + t + ") value is Invalid");
                        return false;
                    }

                    tempUTXOs.remove(input.getTransactionOutputId());
                }

                for (TransactionOutput output : currentTransaction.getOutputs()) {
                    tempUTXOs.put(output.getId(), output);
                }

                if (currentTransaction.getOutputs().get(0).getReciepient() != currentTransaction.getReciepient()) {
                    log.error("#Transaction(" + t + ") output reciepient is not who it should be");
                    return false;
                }
                if (currentTransaction.getOutputs().get(1).getReciepient() != currentTransaction.getSender()) {
                    log.error("#Transaction(" + t + ") output 'change' is not sender.");
                    return false;
                }

            }

        }
        log.info("Blockchain is valid");
        return true;
    }
}
