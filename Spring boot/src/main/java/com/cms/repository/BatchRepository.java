package com.cms.repository;

import com.cms.entities.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {
    List<Batch> findByBatchNameContainingOrDepartmentContaining(String batchName, String department);

	List<Batch> findByBatchNameContainingIgnoreCaseAndDepartmentContainingIgnoreCase(String batchName,
			String department);
	Optional<Batch> findByBatchNameAndDepartmentAndSection(String batchName, String department, String section);
}
