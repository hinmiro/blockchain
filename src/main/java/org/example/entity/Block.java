package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.example.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "blocks")
@Slf4j
@Getter
@Setter
@ToString(exclude = "transactions")
@NoArgsConstructor
public class Block {
    @Id
    private String hash;
    private String previousHash;

    private long timestamp;
    private int nonce;

    @OneToMany(mappedBy = "block", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    public Block(String previousHash) {
        this.previousHash = previousHash;
        this.timestamp = System.currentTimeMillis();
        this.hash = applyHash();
    }

    public void addTransaction(Transaction transaction) {
        transaction.setBlock(this);
        transactions.add(transaction);
    }

    public String applyHash() {
        StringBuilder transactionData = new StringBuilder();
        for (Transaction tx : transactions) {
            transactionData.append(tx.getSender())
                    .append(tx.getReceiver())
                    .append(tx.getAmount())
                    .append(tx.getTimestamp());
        }

        return StringUtil.apply(previousHash +
                Long.toString(timestamp) +
                Integer.toString(nonce) +
                transactionData.toString());
    }

    public void mineBlock(int difficulty) {
        String target = new String(new char[difficulty]).replace('\0', '0');

        while (!hash.substring(0, difficulty).equals(target)) {
            nonce++;
            hash = applyHash();
        }
        log.info("Block mined: {}", hash);
    }
}
