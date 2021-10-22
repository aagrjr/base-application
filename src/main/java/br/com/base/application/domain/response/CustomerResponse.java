package br.com.base.application.domain.response;

import br.com.base.application.domain.Customer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomerResponse {

    @JsonIgnore
    private final Customer customer;

    @ApiModelProperty("Customer autogenerated id")
    public String getId() {
        return customer.getId().toHexString();
    }

    @ApiModelProperty("Customer's name")
    public String getName() {
        return customer.getName();
    }
}