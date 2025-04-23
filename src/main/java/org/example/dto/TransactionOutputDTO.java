package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.entity.TransactionOutput;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class TransactionOutputDTO {
    private String id;
    private String recipientEncoded;
    private double value;
    private String parentTransactionId;

    public static TransactionOutputDTO fromEntity(TransactionOutput output) {
        return TransactionOutputDTO.builder()
                .id(output.getId())
                .recipientEncoded(output.getRecipientEncoded())
                .value(output.getValue())
                .parentTransactionId(output.getParentTransactionId())
                .build();
    }

}
