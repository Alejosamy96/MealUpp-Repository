package com.example.mealupp.repository;

import com.example.mealupp.model.StaffMember;
import com.example.mealupp.model.StaffRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffMemberRepository extends JpaRepository<StaffMember, Long> {

  Optional<StaffMember> findByUsername(String username);

  boolean existsByUsername(String username);

  long countByRoleAndActiveTrue(StaffRole role);
}
