package com.asif.jobtrack.service.impl;

import com.asif.jobtrack.dto.request.CompanyRequest;
import com.asif.jobtrack.dto.response.CompanyResponse;
import com.asif.jobtrack.entity.Company;
import com.asif.jobtrack.exception.CompanyInUseException;
import com.asif.jobtrack.exception.ResourceNotFoundException;
import com.asif.jobtrack.mapper.CompanyMapper;
import com.asif.jobtrack.repository.CompanyRepository;
import com.asif.jobtrack.repository.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Spy
    private CompanyMapper companyMapper = new CompanyMapper();

    @InjectMocks
    private CompanyServiceImpl companyService;

    private Company company;
    private CompanyRequest companyRequest;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setName("Atlassian");
        company.setWebsite("https://www.atlassian.com");
        company.setLocation("Bengaluru");
        company.setCreatedAt(
                LocalDateTime.of(2026, 9, 13, 10, 0)
        );

        companyRequest = new CompanyRequest();
        companyRequest.setName("Atlassian");
        companyRequest.setWebsite("https://www.atlassian.com");
        companyRequest.setLocation("Bengaluru");
    }

    @Test
    void createCompany_shouldSaveAndReturnResponse() {
        when(companyRepository.save(any(Company.class)))
                .thenReturn(company);

        CompanyResponse response =
                companyService.createCompany(companyRequest);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Atlassian");
        assertThat(response.getLocation()).isEqualTo("Bengaluru");

        verify(companyRepository).save(any(Company.class));
    }

    @Test
    void getCompanyById_shouldReturnResponseWhenCompanyExists() {
        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));

        CompanyResponse response =
                companyService.getCompanyById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Atlassian");
        assertThat(response.getWebsite())
                .isEqualTo("https://www.atlassian.com");
    }

    @Test
    void getCompanyById_shouldThrowWhenCompanyDoesNotExist() {
        when(companyRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> companyService.getCompanyById(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Company not found with id: 99");
    }

    @Test
    void updateCompany_shouldUpdateManagedCompanyAndReturnResponse() {
        CompanyRequest updateRequest = new CompanyRequest();
        updateRequest.setName("Atlassian Updated");
        updateRequest.setWebsite("https://www.atlassian.com/careers");
        updateRequest.setLocation("Noida");

        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));
        when(companyRepository.save(company))
                .thenReturn(company);

        CompanyResponse response =
                companyService.updateCompany(1L, updateRequest);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Atlassian Updated");
        assertThat(response.getLocation()).isEqualTo("Noida");
        assertThat(company.getCreatedAt())
                .isEqualTo(LocalDateTime.of(2026, 9, 13, 10, 0));

        verify(companyRepository).save(company);
    }

    @Test
    void deleteCompany_shouldDeleteWhenCompanyIsNotInUse() {
        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));
        when(jobApplicationRepository.existsByCompanyId(1L))
                .thenReturn(false);

        companyService.deleteCompany(1L);

        verify(companyRepository).delete(company);
    }

    @Test
    void deleteCompany_shouldThrowAndNeverDeleteWhenCompanyIsInUse() {
        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));
        when(jobApplicationRepository.existsByCompanyId(1L))
                .thenReturn(true);

        assertThatThrownBy(
                () -> companyService.deleteCompany(1L)
        )
                .isInstanceOf(CompanyInUseException.class)
                .hasMessage(
                        "Company cannot be deleted because it has job applications"
                );

        verify(companyRepository, never()).delete(any(Company.class));
    }
}