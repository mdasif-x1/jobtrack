package com.asif.jobtrack.controller;

import com.asif.jobtrack.dto.request.JobApplicationRequest;
import com.asif.jobtrack.dto.request.UpdateStatusRequest;
import com.asif.jobtrack.dto.response.JobApplicationResponse;
import com.asif.jobtrack.dto.response.PagedResponse;
import com.asif.jobtrack.exception.InvalidRequestException;
import com.asif.jobtrack.service.JobApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Set;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.asif.jobtrack.enums.ApplicationStatus;
import com.asif.jobtrack.enums.JobType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/applications")
@Tag(
        name = "Job Applications",
        description = "Manage applications, search, filters, upcoming actions, and statistics"
)
public class JobApplicationController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "applicationDate",
            "nextActionDate",
            "jobRole",
            "status",
            "createdAt"
    );

    private final JobApplicationService jobApplicationService;

    public JobApplicationController(
            JobApplicationService jobApplicationService
    ) {
        this.jobApplicationService = jobApplicationService;
    }

    @Operation(summary = "Create a job application")
    @PostMapping
    public ResponseEntity<JobApplicationResponse> createApplication(
            @Valid @RequestBody JobApplicationRequest request
    ) {
        JobApplicationResponse response =
                jobApplicationService.createApplication(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    // API page numbers are zero-based; frontend will display page + 1.
    @Operation(summary = "Get paginated job applications")
    @GetMapping
    public PagedResponse<JobApplicationResponse> getAllApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "applicationDate,desc") String sort
    ) {
        Pageable pageable = createPageable(page, size, sort);

        return jobApplicationService.getAllApplications(pageable);
    }

    @Operation(summary = "Search applications by job role or company")
    @GetMapping("/search")
    public PagedResponse<JobApplicationResponse> searchApplications(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "applicationDate,desc") String sort
    ) {
        Pageable pageable = createPageable(page, size, sort);

        return jobApplicationService.searchApplications(
                keyword,
                pageable
        );
    }

    @Operation(summary = "Filter applications by status")
    @GetMapping("/filter/status")
    public PagedResponse<JobApplicationResponse> filterByStatus(
            @RequestParam ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "applicationDate,desc") String sort
    ) {
        Pageable pageable = createPageable(page, size, sort);

        return jobApplicationService.filterByStatus(
                status,
                pageable
        );
    }

    @Operation(summary = "Filter applications by job type")
    @GetMapping("/filter/job-type")
    public PagedResponse<JobApplicationResponse> filterByJobType(
            @RequestParam JobType jobType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "applicationDate,desc") String sort
    ) {
        Pageable pageable = createPageable(page, size, sort);

        return jobApplicationService.filterByJobType(
                jobType,
                pageable
        );
    }

    @Operation(summary = "Filter applications by application date range")
    @GetMapping("/filter/date")
    public PagedResponse<JobApplicationResponse> filterByDateRange(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "applicationDate,desc") String sort
    ) {
        Pageable pageable = createPageable(page, size, sort);

        return jobApplicationService.filterByDateRange(
                startDate,
                endDate,
                pageable
        );
    }

    @Operation(summary = "Get upcoming actions")
    @GetMapping("/upcoming")
    public List<JobApplicationResponse> getUpcomingApplications() {
        return jobApplicationService.getUpcomingApplications();
    }

    @Operation(summary = "Get application counts grouped by status")
    @GetMapping("/statistics/status")
    public Map<ApplicationStatus, Long> getStatusStatistics() {
        return jobApplicationService.getStatusStatistics();
    }

    @Operation(summary = "Get a job application by ID")
    @GetMapping("/{id}")
    public JobApplicationResponse getApplicationById(
            @PathVariable Long id
    ) {
        return jobApplicationService.getApplicationById(id);
    }

    @Operation(summary = "Update a job application")
    @PutMapping("/{id}")
    public JobApplicationResponse updateApplication(
            @PathVariable Long id,
            @Valid @RequestBody JobApplicationRequest request
    ) {
        return jobApplicationService.updateApplication(id, request);
    }

    @Operation(summary = "Update only application status")
    @PatchMapping("/{id}/status")
    public JobApplicationResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        return jobApplicationService.updateStatus(
                id,
                request.getStatus()
        );
    }

    @Operation(summary = "Delete a job application")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
            @PathVariable Long id
    ) {
        jobApplicationService.deleteApplication(id);

        return ResponseEntity.noContent().build();
    }

    private Pageable createPageable(
            int page,
            int size,
            String sort
    ) {
        if (page < 0) {
            throw new InvalidRequestException(
                    "Page must be 0 or greater"
            );
        }

        if (size < 1 || size > 100) {
            throw new InvalidRequestException(
                    "Size must be between 1 and 100"
            );
        }

        String[] sortParts = sort.split(",", -1);

        if (sortParts.length > 2 || sortParts[0].isBlank()) {
            throw new InvalidRequestException(
                    "Sort must use the format: field,asc or field,desc"
            );
        }

        String sortField = sortParts[0].trim();

        if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
            throw new InvalidRequestException(
                    "Unsupported sort field: " + sortField
                            + ". Allowed fields: "
                            + ALLOWED_SORT_FIELDS
            );
        }

        Sort.Direction direction = Sort.Direction.ASC;

        if (sortParts.length == 2) {
            String directionValue = sortParts[1].trim();

            if ("asc".equalsIgnoreCase(directionValue)) {
                direction = Sort.Direction.ASC;
            } else if ("desc".equalsIgnoreCase(directionValue)) {
                direction = Sort.Direction.DESC;
            } else {
                throw new InvalidRequestException(
                        "Sort direction must be asc or desc"
                );
            }
        }

        return PageRequest.of(
                page,
                size,
                Sort.by(direction, sortField)
        );
    }
}