// -- RoleRepository.java --
package com.shiv.springboot_security_jwt_rbac_app1.repo;

import com.shiv.springboot_security_jwt_rbac_app1.entity.Role;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

	// Find a role by name, returns an Optional to avoid null pointer exceptions
	Optional<Role> findByName(String name);
}
