package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.CashRegisterAccountingResponse;
import com.yowyob.erp.accounting.dto.CashRegisterMovementDto;
import com.yowyob.erp.accounting.service.CashRegisterAccountingService;
import com.yowyob.erp.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/accounting/cash-movements")
@RequiredArgsConstructor
@Tag(name = "Cash Register Accounting", description = "Endpoints for accounting cash register movements")
public class CashRegisterAccountingController {

        private final CashRegisterAccountingService accountingService;

        @PostMapping
        @Operation(summary = "Account a cash register movement", description = "Generates an accounting entry for a cash register movement")
        public Mono<ResponseEntity<ApiResponse<CashRegisterAccountingResponse>>> accountMovement(
                        @RequestBody CashRegisterMovementDto movement) {
                return accountingService.accountMovement(movement)
                                .map(response -> ResponseEntity.ok(
                                                ApiResponse.success(response, "Movement accounted successfully")));
        }
}
