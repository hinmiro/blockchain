package org.example.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.example.util.StringUtil;

import java.util.Date;

@Getter
@Setter
@ToString
public class Block {

    private String hash;
    private String previousHash;
    private Data data;
    private long timeStamp;

    public Block(String previousHash, Data data) {
        this.previousHash = previousHash;
        this.data = data;
        this.timeStamp = new Date().getTime();
        this.hash = applyHash();
    }

    public String applyHash() {
        return StringUtil.apply(previousHash+Long.toString(timeStamp)+data.toString());
    }
}
