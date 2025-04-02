package org.example.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionRequest {
    private String senderId;
    private String recipientId;
    private double amount;
}
