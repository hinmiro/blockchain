package org.example.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class WalletDTO {
    private String username;

    public WalletDTO(String username) {
        this.username = username;
    }
}
