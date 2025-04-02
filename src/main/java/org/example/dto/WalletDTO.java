package org.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class WalletDTO {
    private String id;
    private String username;
    @JsonIgnore
    private String publicKeyEncoded;
    private Double value;

    public WalletDTO(String username) {
        this.username = username;
    }
}
