package org.example.repository;

import org.example.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface BlockRepository extends JpaRepository<Block, String> {
    Block findTopByOrderByTimestampDesc();
    List<Block> findAllByOrderByTimestampAsc();
}
