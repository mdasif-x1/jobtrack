package com.asif.jobtrack.repository;

import com.asif.jobtrack.entity.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.asif.jobtrack.enums.ApplicationStatus;
import com.asif.jobtrack.enums.JobType;

import java.time.LocalDate;

import com.asif.jobtrack.repository.projection.StatusCountProjection;

import java.time.LocalDate;
import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication,Long> {

    boolean existsByJobLink(String jobLink);

    boolean existsByJobLinkAndIdNot(String jobLink,Long id);

    boolean existsByCompanyId(Long companyId);

    @Query("""
            SELECT application
            FROM JobApplication application
            JOIN application.company company
            WHERE LOWER(application.jobRole)
                    LIKE CONCAT('%', :keyword, '%')
               OR LOWER(company.name)
                    LIKE CONCAT('%', :keyword, '%')
            """)
    Page<JobApplication> searchByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    Page<JobApplication> findByStatus(
            ApplicationStatus status,
            Pageable pageable
    );

    Page<JobApplication> findByJobType(
            JobType jobType,
            Pageable pageable
    );

    Page<JobApplication> findByApplicationDateBetween(
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    List<JobApplication>
    findByNextActionDateIsNotNullAndNextActionDateGreaterThanEqualOrderByNextActionDateAsc(
            LocalDate today
    );

    @Query("""
        SELECT application.status AS status,
               COUNT(application) AS count
        FROM JobApplication application
        GROUP BY application.status
        """)
    List<StatusCountProjection> countApplicationsByStatus();
}
