package org.example.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TransactionRequest {
    private String recipientId;
    private double amount;
    private String signature;
}
