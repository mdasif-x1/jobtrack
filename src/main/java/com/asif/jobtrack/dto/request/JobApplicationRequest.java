package com.asif.jobtrack.dto.request;

import com.asif.jobtrack.enums.ApplicationStatus;
import com.asif.jobtrack.enums.JobType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationRequest {

    @NotBlank(message = "Job role is required")
    @Size(max = 150, message = "Job role must not exceed 150 characters")
    private String jobRole;

    @NotNull(message = "Job type is required")
    private JobType jobType;

    @NotNull(message = "Application status is required")
    private ApplicationStatus status;

    @NotNull(message = "Application date is required")
    @PastOrPresent(message = "Application date cannot be in the future")
    private LocalDate applicationDate;

    @NotBlank(message = "Job link is required")
    @Size(max = 700, message = "Job link must not exceed 700 characters")
    @Pattern(
            regexp = "^https?://.+$",
            message = "Job link must be a valid http/https URL"
    )
    private String jobLink;

    @Size(max = 120, message = "Location must not exceed 120 characters")
    private String location;

    @Size(max = 3000, message = "Notes must not exceed 3000 characters")
    private String notes;

    private LocalDate nextActionDate;

    @NotNull(message = "Company ID is required")
    @Positive(message = "Company ID must be positive")
    private Long companyId;
}