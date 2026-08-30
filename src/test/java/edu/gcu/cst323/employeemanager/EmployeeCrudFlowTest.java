package edu.gcu.cst323.employeemanager;

import edu.gcu.cst323.employeemanager.model.Department;
import edu.gcu.cst323.employeemanager.model.Employee;
import edu.gcu.cst323.employeemanager.repository.DepartmentRepository;
import edu.gcu.cst323.employeemanager.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Walks the four pages and all four CRUD operations the way a browser would,
 * which also renders every Thymeleaf template for real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeCrudFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private Department engineering;

    @BeforeEach
    void seed() {
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
        engineering = departmentRepository.save(new Department("Engineering"));
        departmentRepository.save(new Department("Finance"));
    }

    @Test
    void homePageRenders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("Employee Manager")));
    }

    @Test
    void healthEndpointsRespond() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("UP")));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void emptyListPageRenders() throws Exception {
        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/list"))
                .andExpect(content().string(containsString("No employees yet")));
    }

    @Test
    void unknownEmployeeReturnsNotFound() throws Exception {
        mockMvc.perform(get("/employees/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReadUpdateDeleteThroughTheUi() throws Exception {
        // CREATE - the blank form, then a valid submission.
        mockMvc.perform(get("/employees/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/form"))
                .andExpect(model().attributeExists("employee", "departments"));

        mockMvc.perform(post("/employees")
                        .param("firstName", "Ada")
                        .param("lastName", "Lovelace")
                        .param("email", "ada.lovelace@example.com")
                        .param("hireDate", "2019-03-11")
                        .param("department", String.valueOf(engineering.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees"));

        Employee created = employeeRepository.findByEmailIgnoreCase("ada.lovelace@example.com").orElseThrow();
        assertThat(created.getFullName()).isEqualTo("Ada Lovelace");
        assertThat(created.getDepartment().getName()).isEqualTo("Engineering");
        assertThat(created.getHireDate()).isEqualTo(LocalDate.of(2019, 3, 11));

        // READ - list and detail.
        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ada Lovelace")));

        mockMvc.perform(get("/employees/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/detail"))
                .andExpect(content().string(containsString("Engineering")));

        // UPDATE - the edit form preselects the current department, then a valid submission.
        mockMvc.perform(get("/employees/" + created.getId() + "/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/form"))
                .andExpect(content().string(containsString("selected=\"selected\"")));

        mockMvc.perform(post("/employees/" + created.getId())
                        .param("firstName", "Ada")
                        .param("lastName", "Byron")
                        .param("email", "ada.byron@example.com")
                        .param("hireDate", "2019-03-11")
                        .param("department", String.valueOf(engineering.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees"));

        Employee updated = employeeRepository.findById(created.getId()).orElseThrow();
        assertThat(updated.getLastName()).isEqualTo("Byron");
        assertThat(updated.getEmail()).isEqualTo("ada.byron@example.com");

        // DELETE - the button on the list and detail pages posts here.
        mockMvc.perform(post("/employees/" + created.getId() + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees"));

        assertThat(employeeRepository.findById(created.getId())).isEmpty();
    }

    @Test
    void invalidSubmissionRedisplaysFormWithErrors() throws Exception {
        mockMvc.perform(post("/employees")
                        .param("firstName", "")
                        .param("lastName", "Hopper")
                        .param("email", "not-an-email")
                        .param("hireDate", "2020-07-01")
                        .param("department", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/form"))
                .andExpect(model().attributeHasFieldErrors("employee", "firstName", "email", "department"))
                .andExpect(content().string(containsString("First name is required")));

        assertThat(employeeRepository.count()).isZero();
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        Employee existing = new Employee();
        existing.setFirstName("Grace");
        existing.setLastName("Hopper");
        existing.setEmail("grace.hopper@example.com");
        existing.setHireDate(LocalDate.of(2020, 7, 1));
        existing.setDepartment(engineering);
        employeeRepository.save(existing);

        mockMvc.perform(post("/employees")
                        .param("firstName", "Another")
                        .param("lastName", "Person")
                        .param("email", "grace.hopper@example.com")
                        .param("hireDate", "2021-01-01")
                        .param("department", String.valueOf(engineering.getId())))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("employee", "email"));

        assertThat(employeeRepository.count()).isEqualTo(1);
    }
}
