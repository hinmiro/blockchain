package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.util.StringUtil;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class TransactionInput {
    @Id
    @Column(name = "input_id")
    private String id;

    private String transactionOutputId;

    @Transient
    private TransactionOutput UTXO;

    private String signature;

    @ManyToOne
    @JoinColumn(name="transaction_id")
    private Transaction transaction;

    public TransactionInput(String transactionOutputId) {
        this.transactionOutputId = transactionOutputId;
        this.id = StringUtil.apply(transactionOutputId + System.currentTimeMillis());
    }
}
