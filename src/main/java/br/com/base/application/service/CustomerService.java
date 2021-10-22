package br.com.base.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import br.com.base.application.domain.Customer;
import br.com.base.application.domain.payload.CreateCustomerPayload;
import br.com.base.application.domain.payload.UpdateCustomerPayload;
import br.com.base.application.domain.response.CustomerResponse;
import br.com.base.application.domain.search.CustomerSearchParams;
import br.com.base.application.exception.CustomerAlreadyExistsException;
import br.com.base.application.exception.CustomerNotFoundException;
import br.com.base.application.repository.CustomerRepository;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository repository;

    public CustomerResponse create(@Valid CreateCustomerPayload payload) {
        log.info("Create customer - Payload: {}", kv("CreateCustomerPayload", payload));
        if (repository.existsByName(payload.getName())) {
            throw new CustomerAlreadyExistsException();
        }
        return new CustomerResponse(repository.save(createModel(payload)));
    }


    public CustomerResponse update(ObjectId id, @Valid UpdateCustomerPayload payload) {
        log.info("Update customer - Id: {} Payload: {}", kv("Id", id), kv("UpdateCustomerPayload", payload));

        return repository.findById(id).map(customer -> repository.save(updateModel(payload, customer))
                ).map(CustomerResponse::new)
                .orElseThrow(CustomerNotFoundException::new);
    }

    public CustomerResponse findById(ObjectId id) {
        return new CustomerResponse(getCustomerById(id));
    }

    public void delete(ObjectId id) {
        log.info("Delete customer -  Id: {}", kv("Id", id));
        final var customer = getCustomerById(id);

        repository.delete(customer);
    }

    private Customer getCustomerById(ObjectId id) {
        return repository.findById(id).orElseThrow(CustomerNotFoundException::new);
    }

    public Page<CustomerResponse> findAll(Pageable pageable, CustomerSearchParams search) {
        return repository.findAll(example(search), pageable).map(CustomerResponse::new);
    }

    private Customer createModel(CreateCustomerPayload payload) {
        return Customer.builder()
                .name(payload.getName())
                .build();
    }

    private Customer updateModel(UpdateCustomerPayload payload, Customer model) {
        model.setName(payload.getName());
        return model;
    }

    private Customer filters(final CustomerSearchParams search) {
        return Customer.builder().name(search.getName())
                .build();
    }

    private Example<Customer> example(final CustomerSearchParams search) {
        return Example.of(filters(search));
    }

}
