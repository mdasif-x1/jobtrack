package com.asif.jobtrack.service;

import com.asif.jobtrack.dto.request.JobApplicationRequest;
import com.asif.jobtrack.dto.response.JobApplicationResponse;
import com.asif.jobtrack.dto.response.PagedResponse;
import com.asif.jobtrack.enums.ApplicationStatus;
import com.asif.jobtrack.enums.JobType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface JobApplicationService {

    JobApplicationResponse createApplication(JobApplicationRequest request);

    JobApplicationResponse getApplicationById(Long id);

    PagedResponse<JobApplicationResponse> getAllApplications(Pageable pageable);

    JobApplicationResponse updateApplication(
            Long id,
            JobApplicationRequest request
    );

    JobApplicationResponse updateStatus(
            Long id,
            ApplicationStatus status
    );

    void deleteApplication(Long id);

    PagedResponse<JobApplicationResponse> searchApplications(
            String keyword,
            Pageable pageable
    );

    PagedResponse<JobApplicationResponse> filterByStatus(
            ApplicationStatus status,
            Pageable pageable
    );

    PagedResponse<JobApplicationResponse> filterByJobType(
            JobType jobType,
            Pageable pageable
    );

    PagedResponse<JobApplicationResponse> filterByDateRange(
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    List<JobApplicationResponse> getUpcomingApplications();

    Map<ApplicationStatus, Long> getStatusStatistics();
}