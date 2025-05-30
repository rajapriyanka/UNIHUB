package com.cms.repository;

import com.cms.entities.Faculty;
import com.cms.entities.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    List<Faculty> findByNameContainingIgnoreCase(String name);

	Object findByUser_Email(String email);
	
	
	 // Add this method to find Faculty by userId
    @Query("SELECT f FROM Faculty f WHERE f.user.id = :userId")
    Optional<Faculty> findByUserId(@Param("userId") Long userId);
	
	Optional<Faculty> findByUser(User user);
	
	 // Add this method to find faculty by department
    List<Faculty> findByDepartment(String department);
}
