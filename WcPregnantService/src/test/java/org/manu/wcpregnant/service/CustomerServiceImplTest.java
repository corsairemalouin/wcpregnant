package org.manu.wcpregnant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.service.impl.CustomerServiceImpl;
import org.manu.wcpregnant.web.dto.Customer;

class CustomerServiceImplTest {

    private final CustomerServiceImpl service = new CustomerServiceImpl();

    @Test
    void getAllCustomers_shouldReturnEmptyList() {
        List<Customer> all = service.getAllCustomers();
        assertTrue(all.isEmpty());
    }

    @Test
    void getCustomerByLogin_shouldReturnEmpty() {
        Optional<Customer> c = service.getCustomerByLogin("any");
        assertEquals(Optional.empty(), c);
    }
}

