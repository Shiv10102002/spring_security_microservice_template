// -- UserRepository.java --
package com.harinitech.springboot_security_jwt_rbac_app1.repo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.harinitech.springboot_security_jwt_rbac_app1.entity.User;
import com.harinitech.springboot_security_jwt_rbac_app1.model.Status;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	List<User> findByStatus(Status status);

	Page<User> findByStatus(Status status, Pageable pageable);

	Page<User> findByStatusAndRequestedRoleIn(Status status, Collection<String> requestedRoles, Pageable pageable);

	// ✅ FIXED: Optimized count methods for statistics (replaces N+1 queries)
	@Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName")
	long countByRoleNameIgnoreCase(String roleName);

	@Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true")
	long countByEnabledTrue();

	@Query("SELECT COUNT(u) FROM User u WHERE u.accountLocked = true")
	long countByAccountLockedTrue();
}
