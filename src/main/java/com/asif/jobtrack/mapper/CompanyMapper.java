package com.asif.jobtrack.mapper;

import com.asif.jobtrack.dto.request.CompanyRequest;
import com.asif.jobtrack.dto.response.CompanyResponse;
import com.asif.jobtrack.entity.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanyResponse toResponse(Company company) {

        if (company == null) {
            return null;
        }

        CompanyResponse response = new CompanyResponse();

        response.setId(company.getId());
        response.setName(company.getName());
        response.setWebsite(company.getWebsite());
        response.setLocation(company.getLocation());
        response.setCreatedAt(company.getCreatedAt());

        return response;
    }

    public Company toEntity(CompanyRequest request) {

        Company company = new Company();

        updateEntity(request, company);

        return company;
    }

    public void updateEntity(CompanyRequest request, Company company) {

        company.setName(normalizeRequiredText(request.getName()));
        company.setWebsite(normalizeOptionalText(request.getWebsite()));
        company.setLocation(normalizeOptionalText(request.getLocation()));
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