package com.shiv.springboot_security_jwt_rbac_app1.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shiv.springboot_security_jwt_rbac_app1.entity.Permission;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
	Optional<Permission> findByName(String name);
}