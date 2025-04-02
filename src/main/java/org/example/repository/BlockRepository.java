package org.example.repository;

import org.example.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface BlockRepository extends JpaRepository<Block, String> {
    Block findTopByOrderByTimestampDesc();
    Optional<Block> findTopByOrderByTimestampAsc();
    List<Block> findAllByOrderByTimestampAsc();
}
