package org.example.repository;

import org.example.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface BlockRepository extends JpaRepository<Block, String> {
    Optional<Block> findTopByOrderByTimestampDesc();
    Optional<Block> findTopByOrderByTimestampAsc();
    List<Block> findAllByOrderByTimestampAsc();

    @Query("SELECT b FROM Block b LEFT JOIN FETCH b.transactions ORDER BY b.timestamp ASC")
    List<Block> findAllBlocksWithTransactions();
}
