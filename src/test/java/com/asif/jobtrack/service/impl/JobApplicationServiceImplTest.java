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
import com.asif.jobtrack.mapper.CompanyMapper;
import com.asif.jobtrack.mapper.JobApplicationMapper;
import com.asif.jobtrack.repository.CompanyRepository;
import com.asif.jobtrack.repository.JobApplicationRepository;
import com.asif.jobtrack.repository.projection.StatusCountProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceImplTest {

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Spy
    private CompanyMapper companyMapper = new CompanyMapper();

    @Spy
    private JobApplicationMapper jobApplicationMapper =
            new JobApplicationMapper(companyMapper);

    @InjectMocks
    private JobApplicationServiceImpl jobApplicationService;

    private LocalDate today;
    private Company company;
    private JobApplication application;
    private JobApplicationRequest request;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

        company = new Company();
        company.setId(1L);
        company.setName("Atlassian");
        company.setWebsite("https://www.atlassian.com");
        company.setLocation("Bengaluru");
        company.setCreatedAt(LocalDateTime.now().minusDays(5));

        application = new JobApplication();
        application.setId(10L);
        application.setJobRole("Java Backend Intern");
        application.setJobType(JobType.INTERNSHIP);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setApplicationDate(today.minusDays(3));
        application.setJobLink("https://example.com/jobs/java-backend");
        application.setLocation("Bengaluru");
        application.setNotes("Initial application");
        application.setNextActionDate(today.plusDays(2));
        application.setCompany(company);
        application.setCreatedAt(LocalDateTime.now().minusDays(3));
        application.setUpdatedAt(LocalDateTime.now().minusDays(3));

        request = new JobApplicationRequest();
        request.setJobRole("Java Backend Intern");
        request.setJobType(JobType.INTERNSHIP);
        request.setStatus(ApplicationStatus.APPLIED);
        request.setApplicationDate(today.minusDays(3));
        request.setJobLink("https://example.com/jobs/java-backend");
        request.setLocation("Bengaluru");
        request.setNotes("Initial application");
        request.setNextActionDate(today.plusDays(2));
        request.setCompanyId(1L);
    }

    @Test
    void createApplication_shouldSaveAndReturnResponse() {
        when(jobApplicationRepository.existsByJobLink(
                request.getJobLink()
        )).thenReturn(false);

        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));

        when(jobApplicationRepository.save(any(JobApplication.class)))
                .thenAnswer(invocation -> {
                    JobApplication savedApplication =
                            invocation.getArgument(0);

                    savedApplication.setId(10L);
                    savedApplication.setCreatedAt(LocalDateTime.now());
                    savedApplication.setUpdatedAt(LocalDateTime.now());

                    return savedApplication;
                });

        JobApplicationResponse response =
                jobApplicationService.createApplication(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getJobRole())
                .isEqualTo("Java Backend Intern");
        assertThat(response.getCompany().getId()).isEqualTo(1L);

        verify(jobApplicationRepository)
                .save(any(JobApplication.class));
    }

    @Test
    void createApplication_shouldThrowWhenCompanyDoesNotExist() {
        when(jobApplicationRepository.existsByJobLink(
                request.getJobLink()
        )).thenReturn(false);

        when(companyRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> jobApplicationService.createApplication(request)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Company not found with id: 1");

        verify(jobApplicationRepository, never())
                .save(any(JobApplication.class));
    }

    @Test
    void createApplication_shouldThrowWhenJobLinkAlreadyExists() {
        when(jobApplicationRepository.existsByJobLink(
                request.getJobLink()
        )).thenReturn(true);

        assertThatThrownBy(
                () -> jobApplicationService.createApplication(request)
        )
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(
                        "An application with this job link already exists"
                );

        verifyNoInteractions(companyRepository);
        verify(jobApplicationRepository, never())
                .save(any(JobApplication.class));
    }

    @Test
    void createApplication_shouldThrowWhenApplicationDateIsInFuture() {
        request.setApplicationDate(today.plusDays(1));
        request.setNextActionDate(today.plusDays(2));

        assertThatThrownBy(
                () -> jobApplicationService.createApplication(request)
        )
                .isInstanceOf(InvalidApplicationException.class)
                .hasMessage("Application date cannot be in the future");

        verifyNoInteractions(
                jobApplicationRepository,
                companyRepository
        );
    }

    @Test
    void createApplication_shouldThrowWhenNextActionIsBeforeApplicationDate() {
        request.setApplicationDate(today.minusDays(1));
        request.setNextActionDate(today.minusDays(2));

        assertThatThrownBy(
                () -> jobApplicationService.createApplication(request)
        )
                .isInstanceOf(InvalidApplicationException.class)
                .hasMessage(
                        "Next action date cannot be before application date"
                );

        verifyNoInteractions(
                jobApplicationRepository,
                companyRepository
        );
    }

    @Test
    void updateApplication_shouldAllowSameJobLinkAndUpdateManagedEntity() {
        Company updatedCompany = new Company();
        updatedCompany.setId(2L);
        updatedCompany.setName("Microsoft");
        updatedCompany.setCreatedAt(LocalDateTime.now());

        JobApplicationRequest updateRequest =
                new JobApplicationRequest();
        updateRequest.setJobRole("Backend Engineer Updated");
        updateRequest.setJobType(JobType.FULL_TIME);
        updateRequest.setStatus(ApplicationStatus.INTERVIEW);
        updateRequest.setApplicationDate(today.minusDays(2));
        updateRequest.setJobLink(
                "https://example.com/jobs/java-backend"
        );
        updateRequest.setLocation("Noida");
        updateRequest.setNotes("Updated application");
        updateRequest.setNextActionDate(today.plusDays(4));
        updateRequest.setCompanyId(2L);

        LocalDateTime originalCreatedAt =
                application.getCreatedAt();

        when(jobApplicationRepository.findById(10L))
                .thenReturn(Optional.of(application));

        when(jobApplicationRepository.existsByJobLinkAndIdNot(
                updateRequest.getJobLink(),
                10L
        )).thenReturn(false);

        when(companyRepository.findById(2L))
                .thenReturn(Optional.of(updatedCompany));

        when(jobApplicationRepository.save(application))
                .thenReturn(application);

        JobApplicationResponse response =
                jobApplicationService.updateApplication(
                        10L,
                        updateRequest
                );

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getJobRole())
                .isEqualTo("Backend Engineer Updated");
        assertThat(response.getCompany().getId()).isEqualTo(2L);
        assertThat(application.getCreatedAt())
                .isEqualTo(originalCreatedAt);

        verify(jobApplicationRepository).save(application);
    }

    @Test
    void updateApplication_shouldThrowWhenAnotherApplicationOwnsJobLink() {
        when(jobApplicationRepository.findById(10L))
                .thenReturn(Optional.of(application));

        when(jobApplicationRepository.existsByJobLinkAndIdNot(
                request.getJobLink(),
                10L
        )).thenReturn(true);

        assertThatThrownBy(
                () -> jobApplicationService.updateApplication(
                        10L,
                        request
                )
        )
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(
                        "Another application already uses this job link"
                );

        verify(companyRepository, never()).findById(any());
        verify(jobApplicationRepository, never()).save(any());
    }

    @Test
    void updateStatus_shouldUpdateStatusAndReturnResponse() {
        when(jobApplicationRepository.findById(10L))
                .thenReturn(Optional.of(application));

        when(jobApplicationRepository.save(application))
                .thenReturn(application);

        JobApplicationResponse response =
                jobApplicationService.updateStatus(
                        10L,
                        ApplicationStatus.INTERVIEW
                );

        assertThat(response.getStatus())
                .isEqualTo(ApplicationStatus.INTERVIEW);

        verify(jobApplicationRepository).save(application);
    }

    @Test
    void updateStatus_shouldThrowWhenApplicationDoesNotExist() {
        when(jobApplicationRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> jobApplicationService.updateStatus(
                        99L,
                        ApplicationStatus.INTERVIEW
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job application not found with id: 99");

        verify(jobApplicationRepository, never()).save(any());
    }

    @Test
    void deleteApplication_shouldDeleteWhenApplicationExists() {
        when(jobApplicationRepository.findById(10L))
                .thenReturn(Optional.of(application));

        jobApplicationService.deleteApplication(10L);

        verify(jobApplicationRepository).delete(application);
    }

    @Test
    void searchApplications_shouldNormalizeKeywordAndMapPage() {
        Pageable pageable = PageRequest.of(0, 1);

        Page<JobApplication> applicationPage =
                new PageImpl<>(
                        List.of(application),
                        pageable,
                        1
                );

        when(jobApplicationRepository.searchByKeyword(
                "java",
                pageable
        )).thenReturn(applicationPage);

        PagedResponse<JobApplicationResponse> response =
                jobApplicationService.searchApplications(
                        "  JAVA  ",
                        pageable
                );

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getId())
                .isEqualTo(10L);
        assertThat(response.getTotalElements()).isEqualTo(1);

        verify(jobApplicationRepository).searchByKeyword(
                "java",
                pageable
        );
    }

    @Test
    void filterByStatus_shouldMapPage() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<JobApplication> applicationPage =
                new PageImpl<>(
                        List.of(application),
                        pageable,
                        1
                );

        when(jobApplicationRepository.findByStatus(
                ApplicationStatus.APPLIED,
                pageable
        )).thenReturn(applicationPage);

        PagedResponse<JobApplicationResponse> response =
                jobApplicationService.filterByStatus(
                        ApplicationStatus.APPLIED,
                        pageable
                );

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getStatus())
                .isEqualTo(ApplicationStatus.APPLIED);

        verify(jobApplicationRepository).findByStatus(
                ApplicationStatus.APPLIED,
                pageable
        );
    }

    @Test
    void filterByJobType_shouldMapPage() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<JobApplication> applicationPage =
                new PageImpl<>(
                        List.of(application),
                        pageable,
                        1
                );

        when(jobApplicationRepository.findByJobType(
                JobType.INTERNSHIP,
                pageable
        )).thenReturn(applicationPage);

        PagedResponse<JobApplicationResponse> response =
                jobApplicationService.filterByJobType(
                        JobType.INTERNSHIP,
                        pageable
                );

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getJobType())
                .isEqualTo(JobType.INTERNSHIP);

        verify(jobApplicationRepository).findByJobType(
                JobType.INTERNSHIP,
                pageable
        );
    }

    @Test
    void filterByDateRange_shouldMapInclusivePage() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate startDate = today.minusDays(3);
        LocalDate endDate = today.minusDays(1);

        Page<JobApplication> applicationPage =
                new PageImpl<>(
                        List.of(application),
                        pageable,
                        1
                );

        when(jobApplicationRepository.findByApplicationDateBetween(
                startDate,
                endDate,
                pageable
        )).thenReturn(applicationPage);

        PagedResponse<JobApplicationResponse> response =
                jobApplicationService.filterByDateRange(
                        startDate,
                        endDate,
                        pageable
                );

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getId())
                .isEqualTo(10L);

        verify(jobApplicationRepository)
                .findByApplicationDateBetween(
                        startDate,
                        endDate,
                        pageable
                );
    }

    @Test
    void filterByDateRange_shouldThrowWhenStartDateIsAfterEndDate() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(
                () -> jobApplicationService.filterByDateRange(
                        today,
                        today.minusDays(1),
                        pageable
                )
        )
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage(
                        "Start date must be before or equal to end date"
                );

        verify(jobApplicationRepository, never())
                .findByApplicationDateBetween(
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void getUpcomingApplications_shouldUseTodayAndMapResults() {
        JobApplication futureApplication = new JobApplication();
        futureApplication.setId(11L);
        futureApplication.setJobRole("Future Follow Up");
        futureApplication.setJobType(JobType.FULL_TIME);
        futureApplication.setStatus(ApplicationStatus.SAVED);
        futureApplication.setApplicationDate(today.minusDays(1));
        futureApplication.setJobLink(
                "https://example.com/jobs/future-follow-up"
        );
        futureApplication.setNextActionDate(today.plusDays(1));
        futureApplication.setCompany(company);

        when(jobApplicationRepository
                .findByNextActionDateIsNotNullAndNextActionDateGreaterThanEqualOrderByNextActionDateAsc(
                        any(LocalDate.class)
                ))
                .thenReturn(List.of(futureApplication));

        List<JobApplicationResponse> response =
                jobApplicationService.getUpcomingApplications();

        ArgumentCaptor<LocalDate> todayCaptor =
                ArgumentCaptor.forClass(LocalDate.class);

        verify(jobApplicationRepository)
                .findByNextActionDateIsNotNullAndNextActionDateGreaterThanEqualOrderByNextActionDateAsc(
                        todayCaptor.capture()
                );

        assertThat(todayCaptor.getValue()).isEqualTo(today);
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getId()).isEqualTo(11L);
    }

    @Test
    void getStatusStatistics_shouldIncludeZeroForMissingStatuses() {
        StatusCountProjection savedCount =
                mock(StatusCountProjection.class);

        when(savedCount.getStatus())
                .thenReturn(ApplicationStatus.SAVED);
        when(savedCount.getCount()).thenReturn(2L);

        StatusCountProjection interviewCount =
                mock(StatusCountProjection.class);

        when(interviewCount.getStatus())
                .thenReturn(ApplicationStatus.INTERVIEW);
        when(interviewCount.getCount()).thenReturn(1L);

        when(jobApplicationRepository.countApplicationsByStatus())
                .thenReturn(List.of(savedCount, interviewCount));

        Map<ApplicationStatus, Long> statistics =
                jobApplicationService.getStatusStatistics();

        assertThat(statistics)
                .containsKeys(ApplicationStatus.values());
        assertThat(statistics.get(ApplicationStatus.SAVED))
                .isEqualTo(2L);
        assertThat(statistics.get(ApplicationStatus.INTERVIEW))
                .isEqualTo(1L);
        assertThat(statistics.get(ApplicationStatus.OFFERED))
                .isEqualTo(0L);
        assertThat(statistics.values())
                .allMatch(count -> count >= 0L);
    }
}