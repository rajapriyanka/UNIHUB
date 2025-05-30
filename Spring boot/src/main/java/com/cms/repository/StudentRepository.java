package com.cms.repository;

import com.cms.entities.Student;
import com.cms.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    @Query("SELECT s FROM Student s JOIN FETCH s.user WHERE s.user.email = :email")
    Optional<Student> findByUserEmail(String email);

    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.user")
    List<Student> findAllWithUsers();

    List<Student> findByNameContainingIgnoreCase(String name);

    Optional<Student> findByUser(User user);
    List<Student> findByBatchName(String batchName);
    
    List<Student> findByBatchNameAndDepartment(String batchName, String department);
    
    List<Student> findByBatchNameAndSection(String batchName, String section);
    
    // Find students by batch name, department, and section
    List<Student> findByBatchNameAndDepartmentAndSection(String batchName, String department, String section);
    
    
    
    @Modifying
    @Query("DELETE FROM Student s WHERE s.batchName = :batchName")
    void deleteByBatchName(@Param("batchName") String batchName);
    


}
