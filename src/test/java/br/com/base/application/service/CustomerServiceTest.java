package br.com.base.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.base.application.domain.payload.CreateCustomerPayload;
import br.com.base.application.domain.response.CustomerResponse;
import br.com.base.application.domain.search.CustomerSearchParams;
import br.com.base.application.helper.MockGenerator;
import br.com.base.application.repository.CustomerRepository;
import br.com.base.application.domain.Customer;
import br.com.base.application.domain.payload.UpdateCustomerPayload;
import br.com.base.application.exception.CustomerAlreadyExistsException;
import br.com.base.application.exception.CustomerNotFoundException;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@RunWith(MockitoJUnitRunner.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomerServiceTest {

    @RegisterExtension
    static MockGenerator mockGenerator = MockGenerator.instance();
    private final ObjectId id = new ObjectId("61701da494bdec3eec35d8ff");
    @Mock
    CustomerRepository repository;
    @InjectMocks
    private CustomerService service;
    private CreateCustomerPayload createCustomerPayload;
    private UpdateCustomerPayload updateCustomerPayload;
    private Customer customer;

    @BeforeEach
    public void beforeEach() {
        createCustomerPayload = mockGenerator.generateFromJson("createPayload").as(CreateCustomerPayload.class);
        updateCustomerPayload = mockGenerator.generateFromJson("updatePayload").as(UpdateCustomerPayload.class);

        customer = mockGenerator.generateFromJson("customer").as(Customer.class);

        reset(repository);
    }

    @Test
    void createWithSuccess() {
        when(repository.existsByName(any())).thenReturn(false);
        when(repository.save(any())).thenReturn(customer);

        assertResult(service.create(createCustomerPayload));

        verify(repository).existsByName(any());
        verify(repository).save(any());
    }

    @Test
    void createWithCustomerAlreadyExistsException() {
        when(repository.existsByName(any())).thenReturn(true);

        assertThrows(CustomerAlreadyExistsException.class, () -> service.create(createCustomerPayload));

        verify(repository).existsByName(any());
        verify(repository, never()).save(any());
    }

    @Test
    void updateWithSuccess() {
        when(repository.findById(id)).thenReturn(Optional.of(customer));
        when(repository.save(any())).thenReturn(customer);

        assertResult(service.update(id, updateCustomerPayload));
        verify(repository).findById(id);
        verify(repository).save(any());
    }

    @Test
    void updateWithCustomerNotFoundException() {
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> service.update(id, updateCustomerPayload));

        verify(repository).findById(id);
        verify(repository, never()).save(any());
    }

    @Test
    void findByIdWithSuccess() {
        when(repository.findById(id)).thenReturn(Optional.of(customer));

        assertResult(service.findById(id));

        verify(repository).findById(id);
    }

    @Test
    void FindByIdWithCustomerNotFoundException() {
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> service.findById(id));

        verify(repository).findById(id);
    }

    @Test
    void deleteWithSuccess() {
        when(repository.findById(id)).thenReturn(Optional.of(customer));

        final var captor = ArgumentCaptor.forClass(Customer.class);

        service.delete(id);

        verify(repository).findById(id);
        verify(repository).delete(captor.capture());

        assertEquals(captor.getValue().getId(), id);
    }

    @Test
    void deleteWithCustomerNotFoundException() {
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> service.delete(id));

        verify(repository).findById(id);
        verify(repository, never()).save(any());
    }

    @Test
    void findAllWithSuccess() {
        Pageable page = PageRequest.of(0, 1);
        CustomerSearchParams params = new CustomerSearchParams();
        params.setName(customer.getName());

        Page<Customer> customerPage = new PageImpl<>(List.of(customer), page,
                Integer.MAX_VALUE);

        when(repository.findAll(any(), any(Pageable.class))).thenReturn(customerPage);
        var response = service.findAll(page, params);

        assertNotNull(response);
        assertResult(response.getContent().get(0));
        assertEquals(response.getContent().size(), page.getPageSize());
    }


    public void assertResult(CustomerResponse result) {
        assertNotNull(result);
        assertEquals(result.getId(), id.toHexString());
        assertEquals(result.getName(), customer.getName());
    }
}