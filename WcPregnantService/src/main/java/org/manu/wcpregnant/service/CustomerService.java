package org.manu.wcpregnant.service;

import java.util.List;
import java.util.Optional;

import org.manu.wcpregnant.web.dto.Customer;

public interface CustomerService {
    List<Customer> getAllCustomers();

    Optional<Customer> getCustomerByLogin(String login);
}
