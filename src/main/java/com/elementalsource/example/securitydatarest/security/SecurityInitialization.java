package com.elementalsource.example.securitydatarest.security;

import com.elementalsource.example.securitydatarest.model.Employee;
import com.elementalsource.example.securitydatarest.model.Item;
import com.elementalsource.example.securitydatarest.repository.EmployeeRepository;
import com.elementalsource.example.securitydatarest.repository.ItemRepository;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class SecurityInitialization {

    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * Pre-load the system with employees and items.
     */
    @PostConstruct
    public void init() {

        employeeRepository.save(new Employee("Bilbo", "Baggins", "thief"));
        employeeRepository.save(new Employee("Frodo", "Baggins", "ring bearer"));
        employeeRepository.save(new Employee("Gandalf", "the Wizard", "servant of the Secret Fire"));

        /**
         * Due to method-level protections on {@link example.company.ItemRepository}, the security context must be loaded
         * with an authentication token containing the necessary privileges.
         */
        SecurityUtils.runAs("system", "system", "ROLE_ADMIN");

        itemRepository.save(new Item("Sting"));
        itemRepository.save(new Item("the one ring"));

        SecurityContextHolder.clearContext();
    }
}
