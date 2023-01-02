package com.lovansom.carbookingapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lovansom.carbookingapp.model.ERole;
import com.lovansom.carbookingapp.model.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
	  Optional<Role> findByName(ERole name);
}
