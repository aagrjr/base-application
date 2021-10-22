package br.com.base.application.domain.payload;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCustomerPayload {

    @ApiModelProperty(value = "Customer's name.", required = true)
    @Size(max = 120, message = "{Customer.name.size}")
    @NotBlank(message = "{Customer.name.notBlank}")
    private String name;

}
