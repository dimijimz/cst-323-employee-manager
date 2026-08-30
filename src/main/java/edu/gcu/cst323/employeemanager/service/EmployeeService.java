package edu.gcu.cst323.employeemanager.service;

import edu.gcu.cst323.employeemanager.model.Employee;
import edu.gcu.cst323.employeemanager.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business layer for employee CRUD.
 *
 * <p>Every create, read, update and delete emits an INFO-level SLF4J record. Those
 * lines go to stdout in every environment, which is what the cloud platforms
 * (Azure App Service log stream, container logs, and so on) actually collect.
 */
@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /** READ (all) - backs the /employees table. */
    public List<Employee> findAll() {
        List<Employee> employees = employeeRepository.findAllWithDepartment();
        log.info("READ   employee: retrieved {} employee(s)", employees.size());
        return employees;
    }

    /** READ (one) - backs the detail and edit views. */
    public Employee findById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("READ   employee: id {} not found", id);
                    return new EmployeeNotFoundException(id);
                });
        log.info("READ   employee: id={} name={}", employee.getId(), employee.getFullName());
        return employee;
    }

    /** CREATE - persists a new employee. */
    @Transactional
    public Employee create(Employee employee) {
        employee.setId(null); // Defensive: a submitted id must never overwrite another row.
        Employee saved = employeeRepository.save(employee);
        log.info("CREATE employee: id={} name={} email={} department={}",
                saved.getId(), saved.getFullName(), saved.getEmail(), saved.getDepartment().getName());
        return saved;
    }

    /** UPDATE - copies submitted fields onto the managed entity. */
    @Transactional
    public Employee update(Long id, Employee submitted) {
        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        existing.setFirstName(submitted.getFirstName());
        existing.setLastName(submitted.getLastName());
        existing.setEmail(submitted.getEmail());
        existing.setHireDate(submitted.getHireDate());
        existing.setDepartment(submitted.getDepartment());

        Employee saved = employeeRepository.save(existing);
        log.info("UPDATE employee: id={} name={} email={} department={}",
                saved.getId(), saved.getFullName(), saved.getEmail(), saved.getDepartment().getName());
        return saved;
    }

    /** DELETE - removes an employee by id. */
    @Transactional
    public void delete(Long id) {
        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
        employeeRepository.delete(existing);
        log.info("DELETE employee: id={} name={}", id, existing.getFullName());
    }

    /** Guards the unique constraint on email so the user sees a field error, not a 500. */
    public boolean emailTakenByAnother(String email, Long currentId) {
        return employeeRepository.findByEmailIgnoreCase(email)
                .filter(found -> !found.getId().equals(currentId))
                .isPresent();
    }
}
