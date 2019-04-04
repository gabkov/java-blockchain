package com.gabkov.blockchain.controller;


import com.gabkov.blockchain.Block;
import com.gabkov.blockchain.services.BlockChainBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
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
            return new ResponseEntity<>("Invalid transaction", HttpStatus.UNPROCESSABLE_ENTITY);
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

}
