package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.infrastructure.web.dto.invoice.CustomerInvoiceDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.invoice.SupplierInvoiceDto;
import com.yowyob.erp.accounting.application.service.InvoiceAccountingService;
import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;

import jakarta.validation.Valid;

/**
 * Controller for invoice accounting integration.
 */
@RestController
@RequestMapping("/api/accounting/invoices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Invoice Accounting", description = "Endpoints for accounting invoices from other modules")
public class InvoiceAccountingController {

        private final InvoiceAccountingService invoice_accounting_service;

        @PostMapping("/purchase")
        @Operation(summary = "Account a supplier invoice (purchase)")
        public Mono<ResponseEntity<ApiResponseWrapper<Object>>> accountSupplierInvoice(
                        @Valid @RequestBody SupplierInvoiceDto supplier_invoice_dto) {

                log.info("Request to account supplier invoice: {}", supplier_invoice_dto.getNumeroFacture());
                return invoice_accounting_service.accountSupplierInvoice(supplier_invoice_dto)
                                .map(result -> ResponseEntity
                                                .ok(ApiResponseWrapper.success(result,
                                                                "Supplier invoice processed successfully")))
                                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
        }

        @PostMapping("/sale")
        @Operation(summary = "Account a customer invoice (sale)")
        public Mono<ResponseEntity<ApiResponseWrapper<Object>>> accountCustomerInvoice(
                        @Valid @RequestBody CustomerInvoiceDto customer_invoice_dto) {

                log.info("Request to account customer invoice: {}", customer_invoice_dto.getNumeroFacture());
                return invoice_accounting_service.accountCustomerInvoice(customer_invoice_dto)
                                .map(result -> ResponseEntity
                                                .ok(ApiResponseWrapper.success(result,
                                                                "Customer invoice processed successfully")))
                                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
        }
}
