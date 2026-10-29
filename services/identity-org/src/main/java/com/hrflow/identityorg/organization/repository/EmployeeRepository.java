package com.hrflow.identityorg.organization.repository;

import com.hrflow.identityorg.organization.entity.Employee;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByTenantIdAndId(UUID tenantId, UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select employee from Employee employee where employee.tenantId = :tenantId and employee.id = :id")
    Optional<Employee> lockByTenantIdAndId(@Param("tenantId") UUID tenantId, @Param("id") UUID id);
    Page<Employee> findByTenantId(UUID tenantId, Pageable page);
    @Query("""
            select employee from Employee employee
            where employee.tenantId = :tenantId and
                (lower(employee.name) like concat('%', :search, '%')
                or lower(employee.email) like concat('%', :search, '%')
                or lower(employee.employeeCode) like concat('%', :search, '%'))
            """)
    Page<Employee> search(@Param("tenantId") UUID tenantId,
                          @Param("search") String search, Pageable page);
}
