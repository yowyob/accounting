import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.service.JournalAuditService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Controller for retrieving audit logs.
 * Provides endpoints for filtering audit trails by tenant, period, user, and
 * action.
 * 
 * @author ALD
 * @date 30.09.25
 */
@RestController
@RequestMapping("/api/accounting/audit")
@RequiredArgsConstructor
@Tag(name = "Accounting Audit", description = "Endpoints for retrieving audit logs")
public class JournalAuditController {

    private final JournalAuditService audit_service;

    /**
     * Retrieves all audits for a tenant (last N actions).
     * 
     * @param tenant_id the tenant ID
     * @param limit     maximum results (default 100)
     * @return list of audit DTOs
     */
    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Get all audits for a tenant")
    public ResponseEntity<ApiResponseWrapper<List<JournalAuditDto>>> getAllByTenant(
            @PathVariable(name = "tenantId") UUID tenant_id,
            @RequestParam(defaultValue = "100") int limit) {

        List<JournalAuditDto> list = audit_service.getAllByTenant(tenant_id, limit);
        return ResponseEntity.ok(ApiResponseWrapper.success(list, "Audit logs retrieved successfully"));
    }

    /**
     * Retrieves audits for a tenant within a specific time period.
     * 
     * @param tenant_id the tenant ID
     * @param debut     start date-time
     * @param fin       end date-time
     * @return list of audit DTOs
     */
    @GetMapping("/tenant/{tenantId}/periode")
    @Operation(summary = "Get audits by time period")
    public ResponseEntity<ApiResponseWrapper<List<JournalAuditDto>>> getByPeriode(
            @PathVariable(name = "tenantId") UUID tenant_id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {

        List<JournalAuditDto> list = audit_service.getByPeriode(tenant_id, debut, fin);
        return ResponseEntity.ok(ApiResponseWrapper.success(list, "Audit logs for period retrieved successfully"));
    }

    /**
     * Retrieves audits for a tenant filtered by user.
     * 
     * @param tenant_id   the tenant ID
     * @param utilisateur the username
     * @return list of audit DTOs
     */
    @GetMapping("/tenant/{tenantId}/utilisateur/{utilisateur}")
    @Operation(summary = "Get audits by user")
    public ResponseEntity<ApiResponseWrapper<List<JournalAuditDto>>> getByUtilisateur(
            @PathVariable(name = "tenantId") UUID tenant_id,
            @PathVariable String utilisateur) {

        List<JournalAuditDto> list = audit_service.getByUtilisateur(tenant_id, utilisateur);
        return ResponseEntity
                .ok(ApiResponseWrapper.success(list, "Audit logs for user " + utilisateur + " retrieved successfully"));
    }

    /**
     * Retrieves audits for a tenant filtered by action type.
     * 
     * @param tenant_id the tenant ID
     * @param action    the action (e.g., CREATE, VALIDATE)
     * @return list of audit DTOs
     */
    @GetMapping("/tenant/{tenantId}/action/{action}")
    @Operation(summary = "Get audits by action type")
    public ResponseEntity<ApiResponseWrapper<List<JournalAuditDto>>> getByAction(
            @PathVariable(name = "tenantId") UUID tenant_id,
            @PathVariable String action) {

        List<JournalAuditDto> list = audit_service.getByAction(tenant_id, action);
        return ResponseEntity
                .ok(ApiResponseWrapper.success(list, "Audit logs for action " + action + " retrieved successfully"));
    }

    /**
     * Retrieves audits related to a specific accounting entry ID.
     * 
     * @param tenant_id   the tenant ID
     * @param ecriture_id the entry ID
     * @return list of audit DTOs
     */
    @GetMapping("/tenant/{tenantId}/ecriture/{ecritureId}")
    @Operation(summary = "Get audits by accounting entry ID")
    public ResponseEntity<ApiResponseWrapper<List<JournalAuditDto>>> getByEcriture(
            @PathVariable(name = "tenantId") UUID tenant_id,
            @PathVariable(name = "ecritureId") UUID ecriture_id) {

        List<JournalAuditDto> list = audit_service.getByEcriture(tenant_id, ecriture_id);
        return ResponseEntity.ok(
                ApiResponseWrapper.success(list, "Audit logs for entry " + ecriture_id + " retrieved successfully"));
    }

    /**
     * Advanced search for audit logs with combined filters.
     * 
     * @param tenant_id   the tenant ID
     * @param utilisateur optional user filter
     * @param action      optional action filter
     * @param debut       optional start date filter
     * @param fin         optional end date filter
     * @return list of audit DTOs
     */
    @GetMapping("/rechercher")
    @Operation(summary = "Advanced search for audit logs")
    public ResponseEntity<ApiResponseWrapper<List<JournalAuditDto>>> rechercher(
            @RequestParam(name = "tenantId") UUID tenant_id,
            @RequestParam(required = false) String utilisateur,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {

        List<JournalAuditDto> list;
        if (debut != null && fin != null) {
            list = audit_service.getByPeriode(tenant_id, debut, fin);
        } else if (utilisateur != null && !utilisateur.isBlank()) {
            list = audit_service.getByUtilisateur(tenant_id, utilisateur);
        } else if (action != null && !action.isBlank()) {
            list = audit_service.getByAction(tenant_id, action);
        } else {
            list = audit_service.getAllByTenant(tenant_id, 200);
        }
        return ResponseEntity.ok(ApiResponseWrapper.success(list, "Audit logs search completed"));
    }
}