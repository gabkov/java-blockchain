package com.gabkov.blockchain.controller;


import com.gabkov.blockchain.Wallet;
import com.gabkov.blockchain.services.BlockChainBase;
import com.gabkov.blockchain.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


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

}
