package edu.gcu.cst323.employeemanager.config;

import edu.gcu.cst323.employeemanager.model.Department;
import edu.gcu.cst323.employeemanager.repository.DepartmentRepository;
import org.springframework.format.Formatter;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Locale;

/**
 * Bridges the department dropdown and the Department entity in both directions.
 *
 * <p>parse() turns the submitted id into an entity, so the form can bind straight to
 * Employee.department and let the @NotNull constraint report a missing selection.
 * print() renders the bound entity back as its id, which is what lets the edit form
 * mark the employee's current department as selected.
 */
@Component
public class DepartmentFormatter implements Formatter<Department> {

    private final DepartmentRepository departmentRepository;

    public DepartmentFormatter(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public Department parse(String text, Locale locale) throws ParseException {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return departmentRepository.findById(Long.valueOf(text)).orElse(null);
        } catch (NumberFormatException ex) {
            throw new ParseException("Not a valid department id: " + text, 0);
        }
    }

    @Override
    public String print(Department department, Locale locale) {
        return (department == null || department.getId() == null) ? "" : String.valueOf(department.getId());
    }
}
