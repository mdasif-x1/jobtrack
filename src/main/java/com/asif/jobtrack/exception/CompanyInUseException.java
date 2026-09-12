package com.asif.jobtrack.exception;

public class CompanyInUseException extends RuntimeException {

    public CompanyInUseException(String message) {
        super(message);
    }
}