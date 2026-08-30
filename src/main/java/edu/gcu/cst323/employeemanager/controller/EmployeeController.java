package edu.gcu.cst323.employeemanager.controller;

import edu.gcu.cst323.employeemanager.model.Department;
import edu.gcu.cst323.employeemanager.model.Employee;
import edu.gcu.cst323.employeemanager.service.DepartmentService;
import edu.gcu.cst323.employeemanager.service.EmployeeService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * All four CRUD operations, each reachable from the UI:
 * list (READ many), detail (READ one), new/edit form (CREATE and UPDATE),
 * and a POST-only delete button (DELETE).
 */
@Controller
@RequestMapping("/employees")
public class EmployeeController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;

    public EmployeeController(EmployeeService employeeService, DepartmentService departmentService) {
        this.employeeService = employeeService;
        this.departmentService = departmentService;
    }

    /** Available to every view in this controller so the form dropdown is always populated. */
    @ModelAttribute("departments")
    public List<Department> departments() {
        return departmentService.findAll();
    }

    @GetMapping
    public String list(Model model) {
        log.info("GET /employees - listing employees");
        model.addAttribute("employees", employeeService.findAll());
        return "employees/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        log.info("GET /employees/new - rendering create form");
        model.addAttribute("employee", new Employee());
        model.addAttribute("pageTitle", "Add Employee");
        model.addAttribute("formAction", "/employees");
        return "employees/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("employee") Employee employee,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        rejectDuplicateEmail(employee, null, binding);

        if (binding.hasErrors()) {
            log.warn("POST /employees - create rejected with {} validation error(s)", binding.getErrorCount());
            model.addAttribute("pageTitle", "Add Employee");
            model.addAttribute("formAction", "/employees");
            return "employees/form";
        }

        Employee saved = employeeService.create(employee);
        redirectAttributes.addFlashAttribute("message", "Created " + saved.getFullName() + ".");
        return "redirect:/employees";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        log.info("GET /employees/{} - rendering detail view", id);
        model.addAttribute("employee", employeeService.findById(id));
        return "employees/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        log.info("GET /employees/{}/edit - rendering edit form", id);
        model.addAttribute("employee", employeeService.findById(id));
        model.addAttribute("pageTitle", "Edit Employee");
        model.addAttribute("formAction", "/employees/" + id);
        return "employees/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("employee") Employee employee,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        rejectDuplicateEmail(employee, id, binding);

        if (binding.hasErrors()) {
            log.warn("POST /employees/{} - update rejected with {} validation error(s)", id, binding.getErrorCount());
            model.addAttribute("pageTitle", "Edit Employee");
            model.addAttribute("formAction", "/employees/" + id);
            return "employees/form";
        }

        Employee saved = employeeService.update(id, employee);
        redirectAttributes.addFlashAttribute("message", "Updated " + saved.getFullName() + ".");
        return "redirect:/employees";
    }

    /** POST rather than GET so the browser cannot delete a row by prefetching a link. */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("POST /employees/{}/delete - deleting employee", id);
        Employee employee = employeeService.findById(id);
        employeeService.delete(id);
        redirectAttributes.addFlashAttribute("message", "Deleted " + employee.getFullName() + ".");
        return "redirect:/employees";
    }

    private void rejectDuplicateEmail(Employee employee, Long currentId, BindingResult binding) {
        if (binding.hasFieldErrors("email") || employee.getEmail() == null) {
            return;
        }
        if (employeeService.emailTakenByAnother(employee.getEmail(), currentId)) {
            binding.rejectValue("email", "email.duplicate", "That email is already assigned to another employee");
        }
    }
}
