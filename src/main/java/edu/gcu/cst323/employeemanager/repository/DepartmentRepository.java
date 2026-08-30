package edu.gcu.cst323.employeemanager.repository;

import edu.gcu.cst323.employeemanager.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Spring Data derives the implementation; no boilerplate DAO to port between clouds. */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findAllByOrderByNameAsc();
}
