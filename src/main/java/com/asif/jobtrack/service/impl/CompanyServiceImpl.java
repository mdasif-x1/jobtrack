package com.asif.jobtrack.service.impl;

import com.asif.jobtrack.dto.request.CompanyRequest;
import com.asif.jobtrack.dto.response.CompanyResponse;
import com.asif.jobtrack.entity.Company;
import com.asif.jobtrack.exception.CompanyInUseException;
import com.asif.jobtrack.exception.ResourceNotFoundException;
import com.asif.jobtrack.mapper.CompanyMapper;
import com.asif.jobtrack.repository.CompanyRepository;
import com.asif.jobtrack.repository.JobApplicationRepository;
import com.asif.jobtrack.service.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final CompanyMapper companyMapper;

    public CompanyServiceImpl(
            CompanyRepository companyRepository,
            JobApplicationRepository jobApplicationRepository,
            CompanyMapper companyMapper
    ) {
        this.companyRepository = companyRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.companyMapper = companyMapper;
    }

    @Override
    @Transactional
    public CompanyResponse createCompany(CompanyRequest request) {

        Company company = companyMapper.toEntity(request);

        Company savedCompany = companyRepository.save(company);

        return companyMapper.toResponse(savedCompany);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(Long id) {

        Company company = getCompanyOrThrow(id);

        return companyMapper.toResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyResponse> getAllCompanies() {

        return companyRepository.findAll()
                .stream()
                .map(companyMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CompanyResponse updateCompany(Long id, CompanyRequest request) {

        Company existingCompany = getCompanyOrThrow(id);

        companyMapper.updateEntity(request, existingCompany);

        Company updatedCompany = companyRepository.save(existingCompany);

        return companyMapper.toResponse(updatedCompany);
    }

    @Override
    @Transactional
    public void deleteCompany(Long id) {

        Company company = getCompanyOrThrow(id);

        boolean companyHasApplications =
                jobApplicationRepository.existsByCompanyId(id);

        if (companyHasApplications) {
            throw new CompanyInUseException(
                    "Company cannot be deleted because it has job applications"
            );
        }

        companyRepository.delete(company);
    }

    private Company getCompanyOrThrow(Long id) {

        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Company not found with id: " + id
                ));
    }
}