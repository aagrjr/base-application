package br.com.base.application.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.base.application.domain.payload.CreateCustomerPayload;
import br.com.base.application.domain.Customer;
import br.com.base.application.domain.payload.UpdateCustomerPayload;
import br.com.base.application.domain.response.CustomerResponse;
import br.com.base.application.domain.search.CustomerSearchParams;
import br.com.base.application.exception.CustomerAlreadyExistsException;
import br.com.base.application.exception.CustomerNotFoundException;
import br.com.base.application.exception.handler.ExceptionHandlerController;
import br.com.base.application.helper.MockGenerator;
import br.com.base.application.helper.TestMessageSource;
import br.com.base.application.service.CustomerService;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@TestInstance(Lifecycle.PER_CLASS)
@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    private static final String BASE_URL = "/customers/";
    @RegisterExtension
    static MockGenerator mockGenerator = MockGenerator.instance();
    private final ObjectId id = new ObjectId();
    @MockBean
    private CustomerService service;
    private CreateCustomerPayload createCustomerPayload;
    private UpdateCustomerPayload updateCustomerPayload;
    private MockMvc mockMvc;
    private Customer customer;
    private CustomerResponse customerResponse;

    @BeforeEach
    private void beforeEach() {
        createCustomerPayload = mockGenerator.generateFromJson("createPayload").as(CreateCustomerPayload.class);
        updateCustomerPayload = mockGenerator.generateFromJson("updatePayload").as(UpdateCustomerPayload.class);

        customer = mockGenerator.generateFromJson("customer").as(Customer.class);
        customerResponse = new CustomerResponse(customer);

        if (mockMvc == null) {
            mockMvc = MockMvcBuilders.standaloneSetup(new CustomerController(service))
                    .setControllerAdvice(new ExceptionHandlerController(new TestMessageSource("Product Test")))
                    .setMessageConverters(mockGenerator.getHttpMessageConverter())
                    .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                    .build();
        }

        reset(service);
    }

    @Test
    void createWithSuccessStatusCode201() throws Exception {
        when(service.create(createCustomerPayload)).thenReturn(customerResponse);

        assertResult(mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(mockGenerator.asString(createCustomerPayload)))
                .andExpect(status().isCreated()));

        verify(service).create(any());
    }

    @Test
    void createWithErrorCustomerAlreadyExistsExceptionStatusCode409() throws Exception {
        when(service.create(createCustomerPayload)).thenThrow(new CustomerAlreadyExistsException());

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(mockGenerator.asString(createCustomerPayload)))
                .andExpect(status().isConflict());

        verify(service).create(any());
    }

    @Test
    void testCreateWithInvalidPayloadStatusCode400() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content("{aa:bb}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any());
    }

    @Test
    void testUpdateWithSuccessStatusCode202() throws Exception {
        when(service.update(id, updateCustomerPayload)).thenReturn(customerResponse);

        assertResult(
                mockMvc.perform(
                                put(BASE_URL.concat(id.toString())).contentType(MediaType.APPLICATION_JSON).content(mockGenerator.asString(updateCustomerPayload)))
                        .andExpect(status().isAccepted()));

        verify(service).update(id, updateCustomerPayload);
    }

    @Test
    void testUpdateWithErrorCustomerNotFoundExceptionStatusCode404() throws Exception {
        when(service.update(id, updateCustomerPayload)).thenThrow(new CustomerNotFoundException());

        mockMvc.perform(
                        put(BASE_URL.concat(id.toString())).contentType(MediaType.APPLICATION_JSON).content(mockGenerator.asString(updateCustomerPayload)))
                .andExpect(status().isNotFound());

        verify(service).update(id, updateCustomerPayload);
    }

    @Test
    void findByIdWithSuccessStatusCode200() throws Exception {
        when(service.findById(id)).thenReturn(customerResponse);

        assertResult(
                mockMvc.perform(
                                get(BASE_URL.concat(id.toString())).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk()));

        verify(service).findById(id);
    }

    @Test
    void findByIdWithErrorCustomerNotFoundExceptionStatusCode404() throws Exception {
        when(service.findById(id)).thenThrow(new CustomerNotFoundException());

        mockMvc.perform(
                        get(BASE_URL.concat(id.toString())).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(service).findById(id);
    }

    @Test
    void findByIdWithErrorInvalidIdStatusCode400() throws Exception {
        mockMvc.perform(
                        get(BASE_URL.concat("123456")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(service, never()).findById(id);
    }

    @Test
    void deleteWithSuccessStatusCode202() throws Exception {
        doNothing().when(service).delete(id);

        mockMvc.perform(
                        delete(BASE_URL.concat(id.toString())).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        verify(service).delete(id);
    }

    @Test
    void deleteWithErrorCustomerNotFoundExceptionStatusCode404() throws Exception {
        doThrow(new CustomerNotFoundException()).when(service).delete(id);

        mockMvc.perform(
                        delete(BASE_URL.concat(id.toString())).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(service).delete(id);
    }

    @Test
    void deleteWithErrorInvalidIdStatusCode400() throws Exception {
        mockMvc.perform(delete(BASE_URL.concat("wrongId")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(service, never()).delete(any());
    }

    @Test
    void findAllWithSuccessStatusCode200() throws Exception {
        final Pageable pageable = PageRequest.of(0, 10);
        final Page<CustomerResponse> page = new PageImpl<>(List.of(new CustomerResponse(customer)), pageable, Integer.MAX_VALUE);

        given(service.findAll(any(Pageable.class), any(CustomerSearchParams.class))).willReturn(page);

        final CustomerSearchParams params = CustomerSearchParams.builder()
                .name("Test")
                .documentNumber("38372550000")
                .build();

        mockMvc.perform(get(BASE_URL)
                        .queryParam("name", params.getName())
                        .queryParam("documentNumber", params.getDocumentNumber())
                        .queryParam("page", String.valueOf(page.getNumber()))
                        .queryParam("size", String.valueOf(page.getSize())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id", is(customerResponse.getId())))
                .andExpect(jsonPath("$.content[0].name", is(customerResponse.getName())))
                .andExpect(jsonPath("$.numberOfElements", is(1)));

        final ArgumentCaptor<CustomerSearchParams> searchParamsCaptor = ArgumentCaptor.forClass(CustomerSearchParams.class);
        final ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(service, Mockito.times(1)).findAll(pageableCaptor.capture(), searchParamsCaptor.capture());

        final CustomerSearchParams searchParam = searchParamsCaptor.getValue();
        assertEquals(params.getName(), searchParam.getName());
        assertEquals(params.getDocumentNumber(), searchParam.getDocumentNumber());

        final Pageable pageableParam = pageableCaptor.getValue();
        assertEquals(pageable.getPageNumber(), pageableParam.getPageNumber());
        assertEquals(pageable.getPageSize(), pageableParam.getPageSize());
    }


    private void assertResult(ResultActions resultActions) throws Exception {
        resultActions.andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(customerResponse.getId())))
                .andExpect(jsonPath("$.name", is(customerResponse.getName())));
    }
}