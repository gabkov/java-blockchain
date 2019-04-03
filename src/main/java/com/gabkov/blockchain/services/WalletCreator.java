package com.gabkov.blockchain.services;

import com.gabkov.blockchain.Wallet;
import org.springframework.stereotype.Component;

@Component
public class WalletCreator {

    public Wallet createNewWallet(){
        return new Wallet();
    }
}
