package com.yowyob.erp.common.service.impl;

import com.yowyob.erp.common.service.TimeService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Default implementation of TimeService.
 * Uses the system clock.
 *
 * @author ALD
 * @date 30.12.2025
 */
@Service
@Primary
public class DefaultTimeService implements TimeService {

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now();
    }

    @Override
    public LocalDate today() {
        return LocalDate.now();
    }
}
