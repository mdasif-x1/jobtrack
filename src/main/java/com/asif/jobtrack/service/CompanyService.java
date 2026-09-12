package com.asif.jobtrack.service;

import com.asif.jobtrack.dto.request.CompanyRequest;
import com.asif.jobtrack.dto.response.CompanyResponse;

import java.util.List;

public interface CompanyService {

    CompanyResponse createCompany(CompanyRequest request);

    CompanyResponse getCompanyById(Long id);

    List<CompanyResponse> getAllCompanies();

    CompanyResponse updateCompany(Long id, CompanyRequest request);

    void deleteCompany(Long id);
}