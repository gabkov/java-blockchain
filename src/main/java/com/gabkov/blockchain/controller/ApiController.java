package com.gabkov.blockchain.controller;


import com.gabkov.blockchain.Wallet;
import com.gabkov.blockchain.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Slf4j
public class ApiController {

    private final Wallet wallet;

    public ApiController(Wallet wallet){
        this.wallet = wallet;
    }

    @RequestMapping(value = "/api/new-wallet", headers = "Accept=application/json")
    public String newWallet() {
        return StringUtil.getStringFromKey(wallet.getPublicKey());
    }

}
