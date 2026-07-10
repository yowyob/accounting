package com.yowyob.erp.shared.application.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdempotentCreateSupportTest {

    @Test
    void blankToNull_trimsAndNullifies() {
        assertNull(IdempotentCreateSupport.blankToNull(null));
        assertNull(IdempotentCreateSupport.blankToNull(""));
        assertNull(IdempotentCreateSupport.blankToNull("   "));
        assertEquals("abc", IdempotentCreateSupport.blankToNull("  abc  "));
    }
}
