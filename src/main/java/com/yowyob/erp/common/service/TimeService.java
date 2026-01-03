package com.yowyob.erp.common.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Service to provide standard time across the application.
 * Allows for easy mocking and testing of time-dependent logic.
 *
 * @author ALD
 * @date 30.12.2025
 */
public interface TimeService {

    /**
     * Returns the current date and time.
     * 
     * @return LocalDateTime usually now()
     */
    LocalDateTime now();

    /**
     * Returns the current date.
     * 
     * @return LocalDate usually today()
     */
    LocalDate today();
}
