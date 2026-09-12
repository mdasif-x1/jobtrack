package com.asif.jobtrack.controller;

import com.asif.jobtrack.dto.request.JobApplicationRequest;
import com.asif.jobtrack.dto.request.UpdateStatusRequest;
import com.asif.jobtrack.dto.response.JobApplicationResponse;
import com.asif.jobtrack.dto.response.PagedResponse;
import com.asif.jobtrack.service.JobApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    public JobApplicationController(
            JobApplicationService jobApplicationService
    ) {
        this.jobApplicationService = jobApplicationService;
    }

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

    @GetMapping
    public ResponseEntity<PagedResponse<JobApplicationResponse>>
    getAllApplications(
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "applicationDate",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        PagedResponse<JobApplicationResponse> response =
                jobApplicationService.getAllApplications(pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> getApplicationById(
            @PathVariable Long id
    ) {
        JobApplicationResponse response =
                jobApplicationService.getApplicationById(id);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> updateApplication(
            @PathVariable Long id,
            @Valid @RequestBody JobApplicationRequest request
    ) {
        JobApplicationResponse response =
                jobApplicationService.updateApplication(id, request);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<JobApplicationResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        JobApplicationResponse response =
                jobApplicationService.updateStatus(
                        id,
                        request.getStatus()
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
            @PathVariable Long id
    ) {
        jobApplicationService.deleteApplication(id);

        return ResponseEntity.noContent().build();
    }
}