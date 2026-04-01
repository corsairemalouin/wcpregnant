package org.manu.wcpregnant.web;

import java.util.List;

import org.manu.wcpregnant.service.CustomerService;
import org.manu.wcpregnant.web.dto.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.manu.wcpregnant.web.api.CustomerApi;

//@RestController
//@RequestMapping("/wcpregnant/customers")
public class CustomerApiImpl implements  CustomerApi {
    private static final Logger log = LoggerFactory.getLogger(CustomerApiImpl.class);

    @Autowired
    CustomerService service;

    @GetMapping("/")
    @Override
    public ResponseEntity<List<Customer>> getAllCustomers() {
        return new ResponseEntity<>(service.getAllCustomers(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Customer> getCustomerByLogin(String login) {
        // On récupère l'Optional depuis le service
        return service.getCustomerByLogin(login)
                // Si le client existe, on renvoie 200 OK avec le client
                .map(customer -> new ResponseEntity<>(customer, HttpStatus.OK))
                // Si l'Optional est vide, on renvoie 404 Not Found
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
