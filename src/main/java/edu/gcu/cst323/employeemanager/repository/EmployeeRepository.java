package edu.gcu.cst323.employeemanager.repository;

import edu.gcu.cst323.employeemanager.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /** Fetches each department in the same round trip so the list view avoids N+1 queries. */
    @Query("select e from Employee e join fetch e.department order by e.lastName asc, e.firstName asc")
    List<Employee> findAllWithDepartment();

    Optional<Employee> findByEmailIgnoreCase(String email);
}
