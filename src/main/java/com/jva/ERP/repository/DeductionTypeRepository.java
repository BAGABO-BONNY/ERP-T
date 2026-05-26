package com.jva.ERP.repository;

import com.jva.ERP.entity.DeductionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for system-wide DeductionType records.
 */
@Repository
public interface DeductionTypeRepository extends JpaRepository<DeductionType, Long> {

    Optional<DeductionType> findByTypeName(String typeName);

    boolean existsByTypeName(String typeName);

    /** All active deduction types (used during payroll processing). */
    List<DeductionType> findByIsActiveTrue();

    /** Active allowance types (House, Transport). */
    List<DeductionType> findByIsActiveTrueAndIsAllowanceTrue();

    /** Active deduction types only (not allowances). */
    List<DeductionType> findByIsActiveTrueAndIsAllowanceFalse();
}
