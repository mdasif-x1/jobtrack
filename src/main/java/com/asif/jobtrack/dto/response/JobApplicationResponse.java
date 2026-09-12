package com.asif.jobtrack.dto.response;

import com.asif.jobtrack.enums.ApplicationStatus;
import com.asif.jobtrack.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationResponse {

    private Long id;
    private String jobRole;
    private JobType jobType;
    private ApplicationStatus status;
    private LocalDate applicationDate;
    private String jobLink;
    private String location;
    private String notes;
    private LocalDate nextActionDate;
    private CompanyResponse company;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}