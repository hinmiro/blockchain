package org.example.repository;

import org.example.entity.TransactionOutput;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionOutputRepository extends JpaRepository<TransactionOutput, String> {
}
