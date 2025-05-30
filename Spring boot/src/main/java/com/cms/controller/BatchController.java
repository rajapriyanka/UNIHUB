package com.cms.controller;

import com.cms.entities.Batch;
import com.cms.service.BatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    @Autowired
    private BatchService batchService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Batch> registerBatch(@RequestBody Batch batch) {
        return ResponseEntity.ok(batchService.registerBatch(batch));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Batch> updateBatch(@PathVariable Long id, @RequestBody Batch batch) {
        return ResponseEntity.ok(batchService.updateBatch(id, batch));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBatch(@PathVariable Long id) {
        batchService.deleteBatch(id);
        return ResponseEntity.ok().build();
    }
    
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN') or hasRole('STUDENT')")
    @GetMapping
    public ResponseEntity<List<Batch>> getAllBatches() {
        return ResponseEntity.ok(batchService.getAllBatches());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<List<Batch>> searchBatches(@RequestParam String query) {
        return ResponseEntity.ok(batchService.searchBatches(query));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search-by-both")
    public ResponseEntity<List<Batch>> searchBatchesByBoth(
            @RequestParam String batchName,
            @RequestParam String department
    ) {
        return ResponseEntity.ok(batchService.searchBatchesByBoth(batchName, department));
    }
}

