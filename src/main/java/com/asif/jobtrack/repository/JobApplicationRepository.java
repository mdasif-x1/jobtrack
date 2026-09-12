package com.asif.jobtrack.repository;

import com.asif.jobtrack.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository extends JpaRepository<JobApplication,Long> {

    boolean existsByJobLink(String jobLink);

    boolean existsByJobLinkAndIdNot(String jobLink,Long id);

    boolean existsByCompanyId(Long companyId);
}
