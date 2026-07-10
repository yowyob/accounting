package com.yowyob.erp.shared.domain.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConflictExceptionTest {

    @Test
    void carriesConflictErrorCode() {
        ConflictException ex = new ConflictException("conflit offline");
        assertEquals("CONFLICT", ex.getErrorCode());
        assertEquals("conflit offline", ex.getMessage());
    }
}
