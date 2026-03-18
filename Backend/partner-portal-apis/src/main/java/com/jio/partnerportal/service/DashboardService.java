package com.jio.partnerportal.service;

import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.dto.DataCatagory;
import com.jio.partnerportal.dto.response.DashboardResponse;
import com.jio.partnerportal.dto.response.DataProcessorListResponse;
import com.jio.partnerportal.dto.response.UserListResponse;
import com.jio.partnerportal.entity.*;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.util.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DashboardService {

    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;

    @Autowired
    public DashboardService(TenantMongoTemplateProvider tenantMongoTemplateProvider) {
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
    }

    public DashboardResponse getDashboardData(String tenantId, HttpServletRequest req) {
        String activity = "Get Dashboard Data";

        MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(tenantId);

        // Fetch all data in one go for each collection
        List<BusinessApplication> businesses = mongoTemplate.findAll(BusinessApplication.class, Constants.BUSINESS_APPLICATIONS);
        List<DataProcessor> processors = mongoTemplate.findAll(DataProcessor.class, "data_processors");
        List<ProcessorActivity> processorActivities = mongoTemplate.findAll(ProcessorActivity.class, "processor_activities");
        List<User> users = mongoTemplate.findAll(User.class, Constants.USERS);
        List<Role> roles = mongoTemplate.findAll(Role.class, Constants.ROLES);

        // Create helper maps for efficient lookups
        Map<String, Role> roleMap = roles.stream()
                .collect(Collectors.toMap(Role::getRoleId, role -> role, (existing, replacement) -> existing));

        // Group processors by businessId
        Map<String, List<DataProcessor>> processorsByBusiness = processors.stream()
                .filter(p -> p.getBusinessId() != null)
                .collect(Collectors.groupingBy(DataProcessor::getBusinessId));

        // Group processor activities by processorId
        Map<String, List<ProcessorActivity>> activitiesByProcessor = processorActivities.stream()
                .filter(pa -> pa.getProcessorId() != null)
                .collect(Collectors.groupingBy(ProcessorActivity::getProcessorId));

        // Build dashboard response organized by business name
        Map<String, DashboardResponse.BusinessDashboardData> dashboardDataMap = new LinkedHashMap<>();

        for (BusinessApplication business : businesses) {
            String businessName = business.getName();
            String businessId = business.getBusinessId();

            // Build business info
            DashboardResponse.BusinessInfo businessInfo = DashboardResponse.BusinessInfo.builder()
                    .businessId(businessId)
                    .name(business.getName())
                    .description(business.getDescription())
                    .scopeLevel(business.getScopeLevel())
                    .build();

            // Build processors list
            List<DashboardResponse.ProcessorData> processorDataList = new ArrayList<>();
            List<DataProcessor> businessProcessors = processorsByBusiness.getOrDefault(businessId, new ArrayList<>());

            for (DataProcessor processor : businessProcessors) {
                String processorId = processor.getDataProcessorId();
                List<ProcessorActivity> processorActivitiesList = activitiesByProcessor.getOrDefault(processorId, new ArrayList<>());

                // Build processor activities
                List<DashboardResponse.ProcessorActivityData> activityDataList = new ArrayList<>();
                for (ProcessorActivity processorActivity : processorActivitiesList) {
                    // Collect all data items from all data types
                    List<String> allDataItems = new ArrayList<>();
                    if (processorActivity.getDataTypesList() != null) {
                        for (DataCatagory dataCategory : processorActivity.getDataTypesList()) {
                            if (dataCategory.getDataItems() != null) {
                                allDataItems.addAll(dataCategory.getDataItems());
                            }
                        }
                    }

                    DashboardResponse.ActivityInfo activityInfo = DashboardResponse.ActivityInfo.builder()
                            .processorActivityId(processorActivity.getProcessorActivityId())
                            .activityName(processorActivity.getActivityName())
                            .processorId(processorActivity.getProcessorId())
                            .processorName(processorActivity.getProcessorName())
                            .details(processorActivity.getDetails())
                            .businessId(processorActivity.getBusinessId())
                            .scopeType(processorActivity.getScopeType())
                            .status(processorActivity.getStatus())
                            .dataItems(allDataItems)
                            .build();

                    DashboardResponse.ProcessorActivityData activityData = DashboardResponse.ProcessorActivityData.builder()
                            .activityName(processorActivity.getActivityName())
                            .activityInfo(activityInfo)
                            .build();

                    activityDataList.add(activityData);
                }

                DashboardResponse.ProcessorInfo processorInfo = DashboardResponse.ProcessorInfo.builder()
                        .dataProcessorId(processor.getDataProcessorId())
                        .dataProcessorName(processor.getDataProcessorName())
                        .callbackUrl(processor.getCallbackUrl())
                        .details(processor.getDetails())
                        .businessId(processor.getBusinessId())
                        .scopeType(processor.getScopeType())
                        .status(processor.getStatus())
                        .isCrossBordered(processor.getIsCrossBordered())
                        .processorActivities(activityDataList)
                        .build();

                DashboardResponse.ProcessorData processorData = DashboardResponse.ProcessorData.builder()
                        .processorName(processor.getDataProcessorName())
                        .processorInfo(processorInfo)
                        .build();

                processorDataList.add(processorData);
            }

            // Build users list - iterate through all users and check if they have roles for this business
            List<DashboardResponse.UserData> userDataList = new ArrayList<>();
            Set<String> processedUserIds = new HashSet<>();
            
            for (User user : users) {
                // Skip if user has no roles
                if (user.getRoles() == null || user.getRoles().isEmpty()) {
                    continue;
                }
                
                // Check if user has any role for this business
                boolean hasRoleForBusiness = user.getRoles().stream()
                        .anyMatch(r -> businessId.equals(r.getBusinessId()));
                
                if (!hasRoleForBusiness) {
                    continue;
                }
                
                // Skip if we've already processed this user for this business (shouldn't happen, but safety check)
                if (processedUserIds.contains(user.getUserId())) {
                    continue;
                }
                processedUserIds.add(user.getUserId());

                // Build roles map for this user - only include roles for this specific business
                Map<String, DashboardResponse.RoleInfo> rolesMap = new LinkedHashMap<>();
                for (User.Role userRole : user.getRoles()) {
                    // Only include roles that match this businessId
                    if (businessId.equals(userRole.getBusinessId())) {
                        Role role = roleMap.get(userRole.getRoleId());
                        if (role != null) {
                            List<DashboardResponse.RolePermissionInfo> permissions = new ArrayList<>();
                            if (role.getPermissions() != null) {
                                for (Role.RolePermission perm : role.getPermissions()) {
                                    DashboardResponse.RolePermissionInfo permissionInfo = DashboardResponse.RolePermissionInfo.builder()
                                            .componentId(perm.getComponentId())
                                            .componentName(perm.getComponentName())
                                            .componentUrl(perm.getComponentUrl())
                                            .displayLabel(perm.getDisplayLabel())
                                            .section(perm.getSection())
                                            .access(perm.getAccess())
                                            .build();
                                    permissions.add(permissionInfo);
                                }
                            }

                            DashboardResponse.RoleInfo roleInfo = DashboardResponse.RoleInfo.builder()
                                    .roleId(role.getRoleId())
                                    .role(role.getRole())
                                    .description(role.getDescription())
                                    .businessId(userRole.getBusinessId())
                                    .permissions(permissions)
                                    .build();

                            rolesMap.put(role.getRoleId(), roleInfo);
                        }
                    }
                }

                // Only add user if they have at least one role for this business
                if (!rolesMap.isEmpty()) {
                    DashboardResponse.UserInfo userInfo = DashboardResponse.UserInfo.builder()
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .mobile(user.getMobile())
                            .designation(user.getDesignation())
                            .identityType(user.getIdentityType())
                            .roles(rolesMap)
                            .build();

                    DashboardResponse.UserData userData = DashboardResponse.UserData.builder()
                            .userId(user.getUserId())
                            .userInfo(userInfo)
                            .build();

                    userDataList.add(userData);
                }
            }

            DashboardResponse.BusinessDashboardData businessDashboardData = DashboardResponse.BusinessDashboardData.builder()
                    .businessInfo(businessInfo)
                    .processors(processorDataList)
                    .users(userDataList)
                    .build();

            dashboardDataMap.put(businessName, businessDashboardData);
        }

        LogUtil.logActivity(req, activity, "Success: Get Dashboard Data successfully");

        return DashboardResponse.builder()
                .dashboardData(dashboardDataMap)
                .build();
    }

    public DataProcessorListResponse getDataProcessorList(String tenantId, HttpServletRequest req) {
        String activity = "Get Data Processor List";

        MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(tenantId);

        // Fetch all processors and activities
        List<DataProcessor> processors = mongoTemplate.findAll(DataProcessor.class, "data_processors");
        List<ProcessorActivity> processorActivities = mongoTemplate.findAll(ProcessorActivity.class, "processor_activities");

        // Group processor activities by processorId
        Map<String, List<ProcessorActivity>> activitiesByProcessor = processorActivities.stream()
                .filter(pa -> pa.getProcessorId() != null)
                .collect(Collectors.groupingBy(ProcessorActivity::getProcessorId));

        // Build processor list (not business-wise sorted)
        List<DataProcessorListResponse.ProcessorData> processorDataList = new ArrayList<>();

        for (DataProcessor processor : processors) {
            String processorId = processor.getDataProcessorId();
            List<ProcessorActivity> processorActivitiesList = activitiesByProcessor.getOrDefault(processorId, new ArrayList<>());

            // Build processor activities
            List<DataProcessorListResponse.ProcessorActivityData> activityDataList = new ArrayList<>();
            for (ProcessorActivity processorActivity : processorActivitiesList) {
                // Collect all data items from all data types
                List<String> allDataItems = new ArrayList<>();
                if (processorActivity.getDataTypesList() != null) {
                    for (DataCatagory dataCategory : processorActivity.getDataTypesList()) {
                        if (dataCategory.getDataItems() != null) {
                            allDataItems.addAll(dataCategory.getDataItems());
                        }
                    }
                }

                DataProcessorListResponse.ActivityInfo activityInfo = DataProcessorListResponse.ActivityInfo.builder()
                        .processorActivityId(processorActivity.getProcessorActivityId())
                        .activityName(processorActivity.getActivityName())
                        .processorId(processorActivity.getProcessorId())
                        .processorName(processorActivity.getProcessorName())
                        .details(processorActivity.getDetails())
                        .businessId(processorActivity.getBusinessId())
                        .scopeType(processorActivity.getScopeType())
                        .status(processorActivity.getStatus())
                        .dataItems(allDataItems)
                        .build();

                DataProcessorListResponse.ProcessorActivityData activityData = DataProcessorListResponse.ProcessorActivityData.builder()
                        .activityName(processorActivity.getActivityName())
                        .activityInfo(activityInfo)
                        .build();

                activityDataList.add(activityData);
            }

            DataProcessorListResponse.ProcessorInfo processorInfo = DataProcessorListResponse.ProcessorInfo.builder()
                    .dataProcessorId(processor.getDataProcessorId())
                    .dataProcessorName(processor.getDataProcessorName())
                    .callbackUrl(processor.getCallbackUrl())
                    .details(processor.getDetails())
                    .businessId(processor.getBusinessId())
                    .scopeType(processor.getScopeType())
                    .status(processor.getStatus())
                    .isCrossBordered(processor.getIsCrossBordered())
                    .processorActivities(activityDataList)
                    .build();

            DataProcessorListResponse.ProcessorData processorData = DataProcessorListResponse.ProcessorData.builder()
                    .processorName(processor.getDataProcessorName())
                    .processorInfo(processorInfo)
                    .build();

            processorDataList.add(processorData);
        }

        LogUtil.logActivity(req, activity, "Success: Get Data Processor List successfully");

        return DataProcessorListResponse.builder()
                .processors(processorDataList)
                .build();
    }

    public UserListResponse getUserList(String tenantId, HttpServletRequest req) {
        String activity = "Get User List";

        MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(tenantId);

        // Fetch all users and roles
        List<User> users = mongoTemplate.findAll(User.class, Constants.USERS);
        List<Role> roles = mongoTemplate.findAll(Role.class, Constants.ROLES);

        // Create helper map for efficient role lookups
        Map<String, Role> roleMap = roles.stream()
                .collect(Collectors.toMap(Role::getRoleId, role -> role, (existing, replacement) -> existing));

        // Build user list
        List<UserListResponse.UserData> userDataList = new ArrayList<>();

        for (User user : users) {
            // Skip if user has no roles
            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                continue;
            }

            // Build roles map for this user
            Map<String, UserListResponse.RoleInfo> rolesMap = new LinkedHashMap<>();
            for (User.Role userRole : user.getRoles()) {
                Role role = roleMap.get(userRole.getRoleId());
                if (role != null) {
                    List<UserListResponse.RolePermissionInfo> permissions = new ArrayList<>();
                    if (role.getPermissions() != null) {
                        for (Role.RolePermission perm : role.getPermissions()) {
                            UserListResponse.RolePermissionInfo permissionInfo = UserListResponse.RolePermissionInfo.builder()
                                    .componentId(perm.getComponentId())
                                    .componentName(perm.getComponentName())
                                    .componentUrl(perm.getComponentUrl())
                                    .displayLabel(perm.getDisplayLabel())
                                    .section(perm.getSection())
                                    .access(perm.getAccess())
                                    .build();
                            permissions.add(permissionInfo);
                        }
                    }

                    UserListResponse.RoleInfo roleInfo = UserListResponse.RoleInfo.builder()
                            .roleId(role.getRoleId())
                            .role(role.getRole())
                            .description(role.getDescription())
                            .businessId(userRole.getBusinessId())
                            .permissions(permissions)
                            .build();

                    rolesMap.put(role.getRoleId(), roleInfo);
                }
            }

            // Only add user if they have at least one role
            if (!rolesMap.isEmpty()) {
                UserListResponse.UserInfo userInfo = UserListResponse.UserInfo.builder()
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .mobile(user.getMobile())
                        .designation(user.getDesignation())
                        .identityType(user.getIdentityType())
                        .roles(rolesMap)
                        .build();

                UserListResponse.UserData userData = UserListResponse.UserData.builder()
                        .userId(user.getUserId())
                        .userInfo(userInfo)
                        .build();

                userDataList.add(userData);
            }
        }

        LogUtil.logActivity(req, activity, "Success: Get User List successfully");

        return UserListResponse.builder()
                .users(userDataList)
                .build();
    }
}

