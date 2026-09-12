package com.asif.jobtrack.mapper;

import com.asif.jobtrack.dto.request.JobApplicationRequest;
import com.asif.jobtrack.dto.response.JobApplicationResponse;
import com.asif.jobtrack.entity.Company;
import com.asif.jobtrack.entity.JobApplication;
import org.springframework.stereotype.Component;

@Component
public class JobApplicationMapper {

    private final CompanyMapper companyMapper;

    public JobApplicationMapper(CompanyMapper companyMapper) {
        this.companyMapper = companyMapper;
    }

    public JobApplicationResponse toResponse(JobApplication application) {

        if (application == null) {
            return null;
        }

        JobApplicationResponse response = new JobApplicationResponse();

        response.setId(application.getId());
        response.setJobRole(application.getJobRole());
        response.setJobType(application.getJobType());
        response.setStatus(application.getStatus());
        response.setApplicationDate(application.getApplicationDate());
        response.setJobLink(application.getJobLink());
        response.setLocation(application.getLocation());
        response.setNotes(application.getNotes());
        response.setNextActionDate(application.getNextActionDate());
        response.setCompany(companyMapper.toResponse(application.getCompany()));
        response.setCreatedAt(application.getCreatedAt());
        response.setUpdatedAt(application.getUpdatedAt());

        return response;
    }

    public JobApplication toEntity(
            JobApplicationRequest request,
            Company company
    ) {
        JobApplication application = new JobApplication();

        updateEntity(request, application, company);

        return application;
    }

    public void updateEntity(
            JobApplicationRequest request,
            JobApplication application,
            Company company
    ) {
        application.setJobRole(normalizeRequiredText(request.getJobRole()));
        application.setJobType(request.getJobType());
        application.setStatus(request.getStatus());
        application.setApplicationDate(request.getApplicationDate());
        application.setJobLink(request.getJobLink());
        application.setLocation(normalizeOptionalText(request.getLocation()));
        application.setNotes(normalizeOptionalText(request.getNotes()));
        application.setNextActionDate(request.getNextActionDate());
        application.setCompany(company);
    }

    private String normalizeRequiredText(String value) {

        if (value == null) {
            return null;
        }

        return value.trim();
    }

    private String normalizeOptionalText(String value) {

        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();

        if (normalizedValue.isEmpty()) {
            return null;
        }

        return normalizedValue;
    }
}