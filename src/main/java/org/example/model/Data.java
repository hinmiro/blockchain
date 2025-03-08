package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Data {
    private Transaction transaction;

    public Data(Transaction transaction) {
        this.transaction = transaction;
    }
}
