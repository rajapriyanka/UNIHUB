package com.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.entities.User;
import com.cms.enums.UserRole;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find a user by their role. This can return multiple users, so use List<User>.
    User findByUserRole(UserRole userRole);
    

  
	Optional<User> findFirstByEmail(String email);

}
