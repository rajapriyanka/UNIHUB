package com.cms.service;

import com.cms.entities.Batch;
import com.cms.repository.AttendanceRepository;
import com.cms.repository.BatchRepository;
import com.cms.repository.FacultyCourseRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.TimetableEntryRepository;
import jakarta.transaction.Transactional;


import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BatchService {

    @Autowired
    private BatchRepository batchRepository;
    
    @Autowired
    private AttendanceRepository attendanceRepository;
    
    @Autowired
    private  FacultyCourseRepository facultyCourseRepository;
    
    @Autowired
    private  StudentRepository studentRepository;
    
    @Autowired
    private  TimetableEntryRepository timetableEntryRepository;


    public Batch registerBatch(Batch batch) {
        return batchRepository.save(batch);
    }

    public Batch updateBatch(Long id, Batch updatedBatch) {
        Batch existingBatch = batchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        if (updatedBatch.getBatchName() != null && !updatedBatch.getBatchName().trim().isEmpty()) {
            existingBatch.setBatchName(updatedBatch.getBatchName().trim());
        }
        if (updatedBatch.getDepartment() != null && !updatedBatch.getDepartment().trim().isEmpty()) {
            existingBatch.setDepartment(updatedBatch.getDepartment().trim());
        }
        if (updatedBatch.getSection() != null) {
            existingBatch.setSection(updatedBatch.getSection().trim());
        }

        return batchRepository.save(existingBatch);
    }

    @Transactional
    public void deleteBatch(Long id) {
        // Find batch details
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        // Delete related attendance records
        attendanceRepository.deleteByBatchName(batch.getBatchName());

        // Delete related faculty-course mappings
        facultyCourseRepository.deleteByBatchId(id);

        // Delete related timetable entries
        timetableEntryRepository.deleteByBatchId(id);

        // Delete related students
        studentRepository.deleteByBatchName(batch.getBatchName());

        // Finally, delete the batch
        batchRepository.deleteById(id);
    }
    public List<Batch> getAllBatches() {
        return batchRepository.findAll();
    }

    public List<Batch> searchBatches(String query) {
        return batchRepository.findByBatchNameContainingOrDepartmentContaining(query, query);
    }
    public List<Batch> searchBatchesByBoth(String batchName, String department) {
        return batchRepository.findByBatchNameContainingIgnoreCaseAndDepartmentContainingIgnoreCase(batchName, department);
    }
}
