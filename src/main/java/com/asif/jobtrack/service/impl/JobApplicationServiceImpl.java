package com.asif.jobtrack.service.impl;

import com.asif.jobtrack.dto.request.JobApplicationRequest;
import com.asif.jobtrack.dto.response.JobApplicationResponse;
import com.asif.jobtrack.dto.response.PagedResponse;
import com.asif.jobtrack.entity.Company;
import com.asif.jobtrack.entity.JobApplication;
import com.asif.jobtrack.enums.ApplicationStatus;
import com.asif.jobtrack.enums.JobType;
import com.asif.jobtrack.exception.DuplicateResourceException;
import com.asif.jobtrack.exception.InvalidApplicationException;
import com.asif.jobtrack.exception.InvalidRequestException;
import com.asif.jobtrack.exception.ResourceNotFoundException;
import com.asif.jobtrack.mapper.JobApplicationMapper;
import com.asif.jobtrack.repository.CompanyRepository;
import com.asif.jobtrack.repository.JobApplicationRepository;
import com.asif.jobtrack.service.JobApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.asif.jobtrack.repository.projection.StatusCountProjection;

import java.util.EnumMap;

@Service
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final CompanyRepository companyRepository;
    private final JobApplicationMapper jobApplicationMapper;

    public JobApplicationServiceImpl(
            JobApplicationRepository jobApplicationRepository,
            CompanyRepository companyRepository,
            JobApplicationMapper jobApplicationMapper
    ) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.companyRepository = companyRepository;
        this.jobApplicationMapper = jobApplicationMapper;
    }

    @Override
    @Transactional
    public JobApplicationResponse createApplication(
            JobApplicationRequest request
    ) {
        validateApplicationDates(
                request.getApplicationDate(),
                request.getNextActionDate()
        );

        boolean jobLinkAlreadyExists =
                jobApplicationRepository.existsByJobLink(
                        request.getJobLink()
                );

        if (jobLinkAlreadyExists) {
            throw new DuplicateResourceException(
                    "An application with this job link already exists"
            );
        }

        Company company = companyRepository
                .findById(request.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Company not found with id: " + request.getCompanyId()
                ));

        JobApplication application =
                jobApplicationMapper.toEntity(request, company);

        JobApplication savedApplication =
                jobApplicationRepository.save(application);

        return jobApplicationMapper.toResponse(savedApplication);
    }

    @Override
    @Transactional(readOnly = true)
    public JobApplicationResponse getApplicationById(Long id) {
        JobApplication application = getApplicationOrThrow(id);

        return jobApplicationMapper.toResponse(application);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<JobApplicationResponse> getAllApplications(
            Pageable pageable
    ) {
        Page<JobApplication> applicationPage =
                jobApplicationRepository.findAll(pageable);

        return toPagedResponse(applicationPage);
    }

    @Override
    @Transactional
    public JobApplicationResponse updateApplication(
            Long id,
            JobApplicationRequest request
    ) {
        JobApplication existingApplication = getApplicationOrThrow(id);

        validateApplicationDates(
                request.getApplicationDate(),
                request.getNextActionDate()
        );

        boolean jobLinkUsedByAnotherApplication =
                jobApplicationRepository.existsByJobLinkAndIdNot(
                        request.getJobLink(),
                        id
                );

        if (jobLinkUsedByAnotherApplication) {
            throw new DuplicateResourceException(
                    "Another application already uses this job link"
            );
        }

        Company company = companyRepository
                .findById(request.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Company not found with id: " + request.getCompanyId()
                ));

        jobApplicationMapper.updateEntity(
                request,
                existingApplication,
                company
        );

        JobApplication updatedApplication =
                jobApplicationRepository.save(existingApplication);

        return jobApplicationMapper.toResponse(updatedApplication);
    }

    @Override
    @Transactional
    public JobApplicationResponse updateStatus(
            Long id,
            ApplicationStatus status
    ) {
        JobApplication existingApplication = getApplicationOrThrow(id);

        existingApplication.setStatus(status);

        JobApplication updatedApplication =
                jobApplicationRepository.save(existingApplication);

        return jobApplicationMapper.toResponse(updatedApplication);
    }

    @Override
    @Transactional
    public void deleteApplication(Long id) {
        JobApplication application = getApplicationOrThrow(id);

        jobApplicationRepository.delete(application);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<JobApplicationResponse> searchApplications(
            String keyword,
            Pageable pageable
    ) {
        String normalizedKeyword = keyword == null
                ? ""
                : keyword.trim().toLowerCase(Locale.ROOT);

        if (normalizedKeyword.isBlank()) {
            throw new InvalidRequestException(
                    "Search keyword must not be blank"
            );
        }

        Page<JobApplication> applicationPage =
                jobApplicationRepository.searchByKeyword(
                        normalizedKeyword,
                        pageable
                );

        return toPagedResponse(applicationPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<JobApplicationResponse> filterByStatus(
            ApplicationStatus status,
            Pageable pageable
    ) {
        Page<JobApplication> applicationPage =
                jobApplicationRepository.findByStatus(
                        status,
                        pageable
                );

        return toPagedResponse(applicationPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<JobApplicationResponse> filterByJobType(
            JobType jobType,
            Pageable pageable
    ) {
        Page<JobApplication> applicationPage =
                jobApplicationRepository.findByJobType(
                        jobType,
                        pageable
                );

        return toPagedResponse(applicationPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<JobApplicationResponse> filterByDateRange(
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        if (startDate == null || endDate == null) {
            throw new InvalidRequestException(
                    "Both startDate and endDate are required"
            );
        }

        if (startDate.isAfter(endDate)) {
            throw new InvalidRequestException(
                    "Start date must be before or equal to end date"
            );
        }

        Page<JobApplication> applicationPage =
                jobApplicationRepository.findByApplicationDateBetween(
                        startDate,
                        endDate,
                        pageable
                );

        return toPagedResponse(applicationPage);
    }

    private JobApplication getApplicationOrThrow(Long id) {
        return jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job application not found with id: " + id
                ));
    }

    private void validateApplicationDates(
            LocalDate applicationDate,
            LocalDate nextActionDate
    ) {
        if (applicationDate != null
                && applicationDate.isAfter(LocalDate.now())) {

            throw new InvalidApplicationException(
                    "Application date cannot be in the future"
            );
        }

        if (applicationDate != null
                && nextActionDate != null
                && nextActionDate.isBefore(applicationDate)) {

            throw new InvalidApplicationException(
                    "Next action date cannot be before application date"
            );
        }
    }

    private PagedResponse<JobApplicationResponse> toPagedResponse(
            Page<JobApplication> applicationPage
    ) {
        PagedResponse<JobApplicationResponse> response =
                new PagedResponse<>();

        List<JobApplicationResponse> applications =
                applicationPage.getContent()
                        .stream()
                        .map(jobApplicationMapper::toResponse)
                        .toList();

        response.setContent(applications);
        response.setPage(applicationPage.getNumber());
        response.setSize(applicationPage.getSize());
        response.setTotalElements(applicationPage.getTotalElements());
        response.setTotalPages(applicationPage.getTotalPages());
        response.setFirst(applicationPage.isFirst());
        response.setLast(applicationPage.isLast());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getUpcomingApplications() {
        LocalDate today = LocalDate.now();

        return jobApplicationRepository
                .findByNextActionDateIsNotNullAndNextActionDateGreaterThanEqualOrderByNextActionDateAsc(
                        today
                )
                .stream()
                .map(jobApplicationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ApplicationStatus, Long> getStatusStatistics() {
        Map<ApplicationStatus, Long> statistics =
                new EnumMap<>(ApplicationStatus.class);

        for (ApplicationStatus status : ApplicationStatus.values()) {
            statistics.put(status, 0L);
        }

        List<StatusCountProjection> statusCounts =
                jobApplicationRepository.countApplicationsByStatus();

        for (StatusCountProjection statusCount : statusCounts) {
            statistics.put(
                    statusCount.getStatus(),
                    statusCount.getCount()
            );
        }

        return statistics;
    }
}