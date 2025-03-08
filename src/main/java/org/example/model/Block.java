package org.example.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.example.util.StringUtil;

import java.util.Date;

@Slf4j
@Getter
@Setter
@ToString
public class Block {

    private String hash;
    private String previousHash;
    private Data data;
    private long timeStamp;
    private int nonce;

    public Block(String previousHash, Data data) {
        this.previousHash = previousHash;
        this.data = data;
        this.timeStamp = new Date().getTime();
        this.hash = applyHash();
    }

    public String applyHash() {
        return StringUtil.apply(previousHash + Long.toString(timeStamp) + Integer.toString(nonce) + data.toString());
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
