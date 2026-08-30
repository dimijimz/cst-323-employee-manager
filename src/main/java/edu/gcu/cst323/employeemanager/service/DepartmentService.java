package edu.gcu.cst323.employeemanager.service;

import edu.gcu.cst323.employeemanager.model.Department;
import edu.gcu.cst323.employeemanager.repository.DepartmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only access to departments. The employee form uses this to populate its
 * department dropdown; departments themselves are seeded by migration.
 */
@Service
@Transactional(readOnly = true)
public class DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<Department> findAll() {
        List<Department> departments = departmentRepository.findAllByOrderByNameAsc();
        log.info("READ  department: retrieved {} department(s)", departments.size());
        return departments;
    }
}
