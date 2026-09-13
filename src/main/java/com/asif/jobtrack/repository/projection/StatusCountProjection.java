package com.asif.jobtrack.repository.projection;

import com.asif.jobtrack.enums.ApplicationStatus;

public interface StatusCountProjection {

    ApplicationStatus getStatus();

    Long getCount();
}