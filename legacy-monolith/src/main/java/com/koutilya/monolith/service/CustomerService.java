package com.koutilya.monolith.service;

import com.koutilya.monolith.domain.Customer;
import com.koutilya.monolith.domain.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Customer create(String name, String email) {
        if (customerRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("email already registered: " + email);
        }
        return customerRepository.save(new Customer(name, email));
    }

    @Transactional(readOnly = true)
    public Customer get(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("customer not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Customer> list() {
        return customerRepository.findAll();
    }
}
