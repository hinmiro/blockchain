package org.example.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigInteger;

@Getter
@Setter
@ToString
public class Transaction {
    private BigInteger id;
    private String sender;
    private String receiver;
    private double amount;


    public Transaction(BigInteger id, String sender, String receiver, double amount) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
    }
}
