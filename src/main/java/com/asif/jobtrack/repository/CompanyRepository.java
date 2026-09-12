package com.asif.jobtrack.repository;

import com.asif.jobtrack.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company,Long> {
}
