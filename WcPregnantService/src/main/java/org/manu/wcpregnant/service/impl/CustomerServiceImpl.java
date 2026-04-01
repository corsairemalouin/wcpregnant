package org.manu.wcpregnant.service.impl;

import java.util.List;

import org.manu.wcpregnant.service.CustomerService;
import org.manu.wcpregnant.web.dto.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    @Override
    public List<Customer> getAllCustomers() {
        return List.of();
    }
}
