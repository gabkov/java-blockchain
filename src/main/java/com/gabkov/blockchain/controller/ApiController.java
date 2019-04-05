package com.gabkov.blockchain.controller;


import com.gabkov.blockchain.Wallet;
import com.gabkov.blockchain.services.BlockChainBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@RestController
@Slf4j
public class ApiController {

    private final BlockChainBase blockChainBase;

    public ApiController(BlockChainBase blockChainBase){
        this.blockChainBase = blockChainBase;
    }


    @RequestMapping(value = "/api/genesis")
    public String genesis(){
        blockChainBase.genesis();
        return blockChainBase.getWalletA().toString();
    }


    @RequestMapping(value = "/api/new-wallet")
    public String newWallet() {
        return blockChainBase.getNewWallet().toString();
    }


    @RequestMapping(value = "/api/new-transaction")
    public ResponseEntity<?> newTransaction(@RequestBody Map<String, String> transactionData){
        if(blockChainBase.newTransaction(transactionData)){
            return new ResponseEntity<>("Transaction done, waiting for confirmation", HttpStatus.OK);
        }else {
            return new ResponseEntity<>("Invalid transaction", HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/api/mine-next-block")
    public ResponseEntity<?> mineNextBlock(){
        blockChainBase.mineNextBlock();
        return new ResponseEntity<>("Next block mined successfully", HttpStatus.OK);
    }


    @RequestMapping(value = "/api/get-blockchain")
    public List<HashMap<String, HashMap<String, String>>> getCurrentBlockChain(){
        return BlockChainBase.getBlockchain();
    }


    @RequestMapping(value = "/api/get-balance")
    public ResponseEntity<?> getWalletAndBalance(@RequestBody Map<String, String> address){
        Wallet wallet = blockChainBase.getWalletAndBalance(address.get("address"));
        if(wallet != null) {
            return new ResponseEntity<>(wallet.toString(), HttpStatus.OK);
        }
        return new ResponseEntity<>("Invalid address", HttpStatus.BAD_REQUEST);
    }


    @RequestMapping(value = "/api/get-transaction-info/{id}")
    public ResponseEntity<?> getTransactionInfoByHash(@PathVariable (value = "id") String id){
        LinkedHashMap<String, String> transaction = BlockChainBase.getTransactionInfo(id);
        if(transaction != null){
            return new ResponseEntity<>(transaction, HttpStatus.OK);
        }
        return new ResponseEntity<>("There is no transaction with the provided id", HttpStatus.BAD_REQUEST);
    }

}
