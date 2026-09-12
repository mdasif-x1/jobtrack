package com.asif.jobtrack.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyRequest {

    @NotBlank(message = "company name is required")
    @Size(max = 120, message = "company name must not exceed 120 character")
    private String name;

    @Size(max = 500 , message = "website must not exceed 500 character")
    @Pattern(
            regexp = "^$|^https?://.+$",
            message = "Website must be a valid http/https URL"
    )
    private String website;

    @Size(max = 120, message = "Location must not exceed 120 character")
    private String location;
}
