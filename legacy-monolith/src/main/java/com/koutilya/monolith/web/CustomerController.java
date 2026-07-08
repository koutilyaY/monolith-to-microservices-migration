package com.koutilya.monolith.web;

import com.koutilya.monolith.domain.Customer;
import com.koutilya.monolith.service.CustomerService;
import com.koutilya.monolith.web.dto.CreateCustomerRequest;
import com.koutilya.monolith.web.dto.CustomerResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Customers API. Customers is NOT extracted (partial strangle), so this stays in the monolith
 * and the gateway always routes {@code /api/customers/**} here.
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request,
                                                   UriComponentsBuilder uriBuilder) {
        Customer customer = customerService.create(request.name(), request.email());
        URI location = uriBuilder.path("/api/customers/{id}").buildAndExpand(customer.getId()).toUri();
        return ResponseEntity.created(location).body(CustomerResponse.from(customer));
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable Long id) {
        return CustomerResponse.from(customerService.get(id));
    }

    @GetMapping
    public List<CustomerResponse> list() {
        return customerService.list().stream().map(CustomerResponse::from).toList();
    }
}
