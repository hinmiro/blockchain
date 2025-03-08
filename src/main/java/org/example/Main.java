package org.example;

import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Block;
import org.example.model.Data;
import org.example.model.Transaction;
import org.example.util.BlockchainSave;

import java.math.BigInteger;
import java.util.List;

@Slf4j
public class Main {

    public static List<Block> blockchain;
    public static BigInteger i = new BigInteger("0");
    public static Integer difficulty = 5;
    public static BigInteger blockInt = new BigInteger("1");
    public static BlockchainSave options = new BlockchainSave();

    private static BigInteger increment() {
        i = i.add(BigInteger.ONE);
        return i;
    }

    private static boolean isChainValid() {
        Block currentBlock;
        Block previousBlock;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');

        for (int j = 1; j < blockchain.size(); j++) {
            currentBlock = blockchain.get(j);
            previousBlock = blockchain.get(j-1);

            if (!currentBlock.getHash().equals(currentBlock.applyHash())) {
                log.warn("Current Hashes not equal");
                return false;
            }

            if (!previousBlock.getHash().equals(previousBlock.applyHash())) {
                log.warn("Invalid previous hashes");
            }

            if (!currentBlock.getHash().substring(0, difficulty).equals(hashTarget)) {
                log.warn("This block has been mined");
                return false;
            }
        }
        return true;
    }


    public static void main(String[] args) {
        blockchain = options.loadBlockchainFromFile();

        blockchain.add(new Block("0", new Data(new Transaction(increment(), "Mizard", "Sinzuu", 2.54))));
        log.info("Trying to mine a block number {}", blockInt.toString());
        blockInt = blockInt.add(BigInteger.ONE);
        blockchain.get(0).mineBlock(difficulty);

        blockchain.add(new Block(blockchain.getLast().getHash(), new Data(new Transaction(increment(), "Sinzuu", "Donald", 4.56))));
        log.info("Trying to mine a block number {}", blockInt.toString());
        blockInt = blockInt.add(BigInteger.ONE);
        blockchain.get(1).mineBlock(difficulty);

        log.info("Blockchain is valid: {}", isChainValid());
        options.saveBlockchainToFile(blockchain);


        String blockchainJson = new GsonBuilder().setPrettyPrinting().create().toJson(blockchain);
        System.out.println(blockchainJson);


    }
}