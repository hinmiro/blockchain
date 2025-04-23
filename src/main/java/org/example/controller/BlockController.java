package org.example.controller;

import org.example.service.BlockService;
import org.example.service.MempoolService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/block")
public class BlockController {
    private final BlockService blockService;
    private final MempoolService mempoolService;

    public BlockController(BlockService blockService, MempoolService mempoolService) {
        this.blockService = blockService;
        this.mempoolService = mempoolService;
    }

//    @GetMapping("/status")
//    public ResponseEntity<BlockchainStatus> getStatus() {
//        return ResponseEntity.ok(BlockchainStatus.builder()
//                .blockHeight(blockService.getBlockHeight())
//                .mempoolSize(mempoolService.getPendingTrasactionCount())
//                .lastBlockHash(blockService.getLastBlockHash())
//                .build());
//    }
//
//    public ResponseEntity<BlockDTO> mineBlock() {
//        return ResponseEntity.ok(blockService.mineNewBlockManually());
//    }

}
