package org.example;

import com.google.gson.GsonBuilder;
import org.example.model.Block;
import org.example.model.Data;
import org.example.model.Transaction;

import java.math.BigInteger;
import java.util.ArrayList;

public class Main {

    public static ArrayList<Block> blockchain = new ArrayList<>();
    public static BigInteger i = new BigInteger("0");

    private static BigInteger increment() {
        return i.add(BigInteger.ONE);
    }


    public static void main(String[] args) {
        blockchain.add(new Block("0", new Data(new Transaction(i.add(BigInteger.ONE), "Mizard", "Sinzuu", 2.54))));
        blockchain.add(new Block(blockchain.getLast().getHash(), new Data(new Transaction(increment(), "Sinzuu", "Roope Ankka", 4.56))));

        String blockchainJson = new GsonBuilder().setPrettyPrinting().create().toJson(blockchain);
        System.out.println(blockchainJson);


    }
}