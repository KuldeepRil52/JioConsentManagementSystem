package com.jio.partnerportal.service;

import com.jio.partnerportal.client.audit.AuditManager;
import com.jio.partnerportal.client.audit.request.Actor;
import com.jio.partnerportal.client.audit.request.AuditRequest;
import com.jio.partnerportal.client.audit.request.Context;
import com.jio.partnerportal.client.audit.request.Resource;
import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.ActionType;
import com.jio.partnerportal.dto.AuditComponent;
import com.jio.partnerportal.dto.request.RoleRequest;
import com.jio.partnerportal.dto.request.RoleUpdateRequest;
import com.jio.partnerportal.dto.response.RoleResponse;
import com.jio.partnerportal.dto.response.RoleUpdateResponse;
import com.jio.partnerportal.entity.Component;
import com.jio.partnerportal.entity.Role;

import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.util.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 *
 *
 * @author Kirte.Bhatt
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRolesService {
    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;
    private final AuditManager auditManager;

    public RoleResponse createRole(RoleRequest request, String txn, String tenantId, String sessionToken, HttpServletRequest req) {

        String activity = "Create Role";
        // ===== Validate request =====
        if (request == null
                || !StringUtils.hasText(request.getRole())
                || !StringUtils.hasText(request.getDescription())
                || request.getPermissions() == null || request.getPermissions().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    ErrorCodes.JCMP3034);
        }

        // Validate each permission's component and action
        for (Role.RolePermission permission : request.getPermissions()) {
            if (permission.getComponentId() == null || permission.getAccess() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        ErrorCodes.JCMP3035);
            }
        }

        MongoTemplate tenantDb = tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));

        // Check if role with the same name already exists
        Query roleNameQuery = new Query(Criteria.where("role").is(request.getRole().trim()));
        if (tenantDb.exists(roleNameQuery, Role.class, Constants.ROLES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ErrorCodes.JCMP3070);
        }

        List<Role.RolePermission> resolvedPermissions = new ArrayList<>();

        // ===== Validate and enrich each permission =====
        for (Role.RolePermission permissionReq : request.getPermissions()) {
            if (!StringUtils.hasText(permissionReq.getComponentId()) || permissionReq.getAccess() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        ErrorCodes.JCMP3036);
            }
            Query query = new Query(Criteria.where("componentId").is(permissionReq.getComponentId()));
            Component component = tenantDb.findOne(query, Component.class, Constants.COMPONENTS);

            if (component == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        ErrorCodes.JCMP3037);
            }

            // Build enriched RolePermission (take actions from request)
            Role.RolePermission resolvedPermission = Role.RolePermission.builder()
                    .componentId(component.getComponentId())
                    .componentName(String.valueOf(component.getComponentName()))
                    .componentUrl(component.getComponentUrl())
                    .displayLabel(component.getDisplayLabel())
                    .section(component.getSection())
                    .access(permissionReq.getAccess())
                    .build();

            resolvedPermissions.add(resolvedPermission);
        }

        // ===== Build Role entity =====
        Role role = Role.builder()
                .roleId(UUID.randomUUID().toString())
                .role(request.getRole().trim())
                .description(request.getDescription().trim())
                .permissions(resolvedPermissions)
                .build();

        // ===== Save role =====
        Role savedRole = tenantDb.save(role, Constants.ROLES);

        this.logUserRoleAudit(savedRole, ActionType.CREATE, savedRole.getRoleId());
        LogUtil.logActivity(req, activity, "Success: Role created successfully");

        // ===== Build response =====
        return RoleResponse.builder()
                .message("Role successfully created")
                .roleId(savedRole.getRoleId())
                .role(savedRole.getRole())
                .description(savedRole.getDescription())
                .permissions(savedRole.getPermissions())
                .createdAt(savedRole.getCreatedAt())
                .updatedAt(savedRole.getUpdatedAt())
                .build();
    }


    public RoleUpdateResponse updateRole(String roleId,
                                         RoleUpdateRequest request,
                                         String txn,
                                         String tenantId,
                                         String sessionToken,HttpServletRequest req) {

        String activity = "Update Role";

        // ===== Validate request =====
        if (request == null
                || !StringUtils.hasText(request.getRole())
                || !StringUtils.hasText(request.getDescription())
                || request.getPermissions() == null || request.getPermissions().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    ErrorCodes.JCMP3034);
        }

        // Validate each permission's component and action
        for (Role.RolePermission permission : request.getPermissions()) {
            if (permission.getComponentId() == null || permission.getAccess() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        ErrorCodes.JCMP3035);
            }
        }

        MongoTemplate tenantDb = tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));

        // ===== Find existing role =====
        Query query = new Query(Criteria.where(Constants.ROLE_ID).is(roleId));
        Role existingRole = tenantDb.findOne(query, Role.class, Constants.ROLES);
        if (existingRole == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.JCMP3012);
        }

        // ===== Update basic fields =====
        if (StringUtils.hasText(request.getRole())) {
            // Check if another role with the same name already exists (excluding current role)
            Query roleNameQuery = new Query(Criteria.where("role").is(request.getRole().trim())
                    .and(Constants.ROLE_ID).ne(roleId));
            if (tenantDb.exists(roleNameQuery, Role.class, Constants.ROLES)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, ErrorCodes.JCMP3071);
            }
            existingRole.setRole(request.getRole().trim());
        }
        if (StringUtils.hasText(request.getDescription())) {
            existingRole.setDescription(request.getDescription().trim());
        }

        // ===== Handle permissions =====
        if (request.getPermissions() != null && !request.getPermissions().isEmpty()) {
            List<Role.RolePermission> updatedPermissions = new ArrayList<>();

            for (Role.RolePermission reqPerm : request.getPermissions()) {
                if (!StringUtils.hasText(reqPerm.getComponentId()) || reqPerm.getAccess() == null ) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            ErrorCodes.JCMP3035);
                }

                // Find component in Components collection
                Query compQuery = new Query(Criteria.where("componentId").is(reqPerm.getComponentId()));
                Component component = tenantDb.findOne(compQuery, Component.class, Constants.COMPONENTS);

                if (component == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                            ErrorCodes.JCMP3037);
                }

                Role.RolePermission updatedPerm = Role.RolePermission.builder()
                        .componentId(component.getComponentId())
                        .componentName(component.getComponentName().toString())
                        .componentUrl(component.getComponentUrl())
                        .displayLabel(component.getDisplayLabel())
                        .section(component.getSection())
                        .access(reqPerm.getAccess())
                        .build();

                updatedPermissions.add(updatedPerm);
            }

            // Replace existing permissions with new enriched list
            existingRole.setPermissions(updatedPermissions);
        }

        existingRole.setUpdatedAt(LocalDateTime.now());

        // ===== Save updated role =====
        Role savedRole = tenantDb.save(existingRole, Constants.ROLES);

        this.logUserRoleAudit(savedRole, ActionType.UPDATE, savedRole.getRoleId());
        LogUtil.logActivity(req, activity, "Success: Role updated successfully");

        // ===== Build response =====
        return RoleUpdateResponse.builder()
                .message("Role successfully updated")
                .roleId(savedRole.getRoleId())
                .role(savedRole.getRole())
                .description(savedRole.getDescription())
                .permissions(
                        savedRole.getPermissions().stream()
                                .map(perm -> RoleUpdateResponse.PermissionResponse.builder()
                                        .componentId(perm.getComponentId())
                                        .componentName(perm.getComponentName())
                                        .componentUrl(perm.getComponentUrl())
                                        .displayLabel(perm.getDisplayLabel())
                                        .section(perm.getSection())
                                        .access(perm.getAccess())
                                        .build())
                                .toList()
                )
                .createdAt(savedRole.getCreatedAt())
                .updatedAt(savedRole.getUpdatedAt())
                .build();
    }


    public ResponseEntity<Map<String, Object>> deleteRole(String txn,
                                                          String tenantId,
                                                          String sessionToken,
                                                          String roleId,HttpServletRequest req) {

        String activity = "Delete Role";

        MongoTemplate tenantDb = tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));

        // ===== Find role =====
        Query query = new Query(Criteria.where(Constants.ROLE_ID).is(roleId));
        Role existingRole = tenantDb.findOne(query, Role.class, Constants.ROLES);

        if (existingRole == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.JCMP3012);
        }

        // ===== Delete role =====
        tenantDb.remove(query, Role.class, Constants.ROLES);

        // ===== Build success response =====
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Role successfully deleted");
        response.put(Constants.ROLE_ID, roleId);
        response.put("txn", txn);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        response.put("timestamp", LocalDateTime.now().format(formatter));
        this.logUserRoleAudit(existingRole, ActionType.DELETE, roleId);
        LogUtil.logActivity(req, activity, "Success: Role Deleted successfully");
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<RoleResponse> searchRole(
            String txn,
            String tenantId,
            String sessionToken,
            String roleId,
            String role,HttpServletRequest req) {

        String activity = "Search Role";

        // ====== Request param validation ======
        if ((roleId == null || roleId.isBlank()) && (role == null || role.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request: roleId or role is required");
        }

        MongoTemplate tenantDb = tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));

        // ===== Build query =====
        List<Criteria> criteriaList = new ArrayList<>();
        if (roleId != null && !roleId.isBlank()) {
            criteriaList.add(Criteria.where(Constants.ROLE_ID).is(roleId));
        }
        if (role != null && !role.isBlank()) {
            criteriaList.add(Criteria.where("role").is(role));
        }

        Criteria combinedCriteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        Query query = new Query(combinedCriteria);

        Role foundRole = tenantDb.findOne(query, Role.class, Constants.ROLES);

        if (foundRole == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.JCMP3012);
        }

        // ====== Map entity → response DTO ======
        RoleResponse response = RoleResponse.builder()
                .message("Role found")
                .roleId(foundRole.getRoleId())
                .role(foundRole.getRole())
                .description(foundRole.getDescription())
                .createdAt(foundRole.getCreatedAt())
                .updatedAt(foundRole.getUpdatedAt())
                .permissions(foundRole.getPermissions())
                .build();

        LogUtil.logActivity(req, activity, "Success: Searched Role successfully");

        return ResponseEntity.ok(response);
    }
    public String countRoles(String txn,
                             String tenantId,
                             String sessionToken,HttpServletRequest req) {

        String activity = "Count Role";

        // ===== Get tenant DB =====
        MongoTemplate tenantDb = tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));

        // ===== Count roles =====
        long count = tenantDb.getCollection(Constants.ROLES).countDocuments();

        LogUtil.logActivity(req, activity, "Success: Count Role successfully");
        return count + " roles found";
    }

    public List<Role> listRoles(String txn,
                                String tenantId,
                                String sessionToken,HttpServletRequest req) {

        String activity = "List Roles";
        // ===== Get tenant DB =====
        MongoTemplate tenantDb = tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));


        LogUtil.logActivity(req, activity, "Success: List Roles successfully");

        // ===== Fetch all roles =====
        return tenantDb.findAll(Role.class, Constants.ROLES);
    }

    public List<Component> listComponents(String txn, String tenantId, String sessionToken,HttpServletRequest req) {

        String activity = "List components";

        // ===== Get tenant-specific MongoTemplate =====
        MongoTemplate tenantDb = tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));

        LogUtil.logActivity(req, activity, "Success: List Component successfully");
        // ===== Fetch all components =====
        return tenantDb.findAll(Component.class, Constants.COMPONENTS);

    }

    /**
     * Modular function to log user role audit events
     * Can be used in both create and update user role flows
     *
     * @param actionType The action type (CREATE, UPDATE, DELETE)
     */
    public void logUserRoleAudit(Role roleResponse, ActionType actionType, String roleId) {
        try {
            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            Actor actor = Actor.builder()
                    .id(ThreadContext.get(Constants.USER_ID_THREAD_CONTEXT))
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.ROLE_ID_CONSTANT)
                    .id(roleId)
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();

            extra.put(Constants.DATA, roleResponse);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(tenantId)
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.USER_ROLE)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for role id: {}, action: {}, error: {}", 
                    roleId, actionType, e.getMessage(), e);
        }
    }
}

