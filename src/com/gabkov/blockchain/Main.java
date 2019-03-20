package com.gabkov.blockchain;

public class Main {

    public static void main(String[] args) {

        Block genesisBlock = new Block("Hi im the first block", "0");
        System.out.println("Hash for block 1 : " + genesisBlock.hash);

    }
}
