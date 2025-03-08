package org.example;

import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Block;
import org.example.model.Data;
import org.example.model.Transaction;

import java.math.BigInteger;
import java.util.ArrayList;

@Slf4j
public class Main {

    public static ArrayList<Block> blockchain = new ArrayList<>();
    public static BigInteger i = new BigInteger("0");

    private static BigInteger increment() {
        i = i.add(BigInteger.ONE);
        return i;
    }

    private static boolean isChainValid() {
        Block currentBlock;
        Block previousBlock;

        for (int j = 1; j < blockchain.size(); j++) {
            currentBlock = blockchain.get(j);
            previousBlock = blockchain.get(j-1);

            if (!currentBlock.getHash().equals(currentBlock.applyHash())) {
                log.warn("Current Hashes not equal: " + currentBlock.toString());
                return false;
            }

            if (!previousBlock.getHash().equals(previousBlock.applyHash())) {
                log.warn("Invalid previous hashes: " + previousBlock.toString());
            }
        }
        return true;
    }


    public static void main(String[] args) {
        blockchain.add(new Block("0", new Data(new Transaction(increment(), "Mizard", "Sinzuu", 2.54))));
        isChainValid();
        blockchain.add(new Block(blockchain.getLast().getHash(), new Data(new Transaction(increment(), "Sinzuu", "Roope Ankka", 4.56))));
        isChainValid();


        String blockchainJson = new GsonBuilder().setPrettyPrinting().create().toJson(blockchain);
        System.out.println(blockchainJson);


    }
}