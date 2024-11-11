package uk.gov.hmcts.ccd;

import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Named
@Singleton
public class ApplicationParams {

    @Value("#{'${ccd.s2s-authorised.services.case_user_roles}'.split(',')}")
    private List<String> authorisedServicesForCaseUserRoles;

    @Value("#{'${ccd.am.write.to_ccd_only}'.split(',')}")
    private List<String> writeToCCDCaseTypesOnly;

    @Value("#{'${ccd.am.write.to_am_only}'.split(',')}")
    private List<String> writeToAMCaseTypesOnly;

    @Value("#{'${ccd.am.write.to_both}'.split(',')}")
    private List<String> writeToBothCaseTypes;

    @Value("#{'${ccd.am.read.from_ccd}'.split(',')}")
    private List<String> readFromCCDCaseTypes;

    @Value("#{'${ccd.am.read.from_am}'.split(',')}")
    private List<String> readFromAMCaseTypes;

    @Value("#{'${ccd.callback.retries}'.split(',')}")
    private List<Integer> callbackRetries;

    @Value("#{'${ccd.callback.log.control}'.split(',')}")
    private List<String> ccdCallbackLogControl;

    @Value("${ccd.token.secret}")
    private String tokenSecret;

    @Value("${ccd.case-definition.host}")
    private String caseDefinitionHost;

    @Value("${role.assignment.api.host}")
    private String roleAssignmentServiceHost;

    @Value("${role.assignment.pagination.enabled}")
    private boolean roleAssignmentPaginationEnabled;

    @Value("${role.assignment.page.size}")
    private String roleAssignmentPageSize;

    @Value("${ccd.draft.host}")
    private String draftHost;

    @Value("${ccd.draft.encryptionKey}")
    private String draftEncryptionKey;

    @Value("${ccd.draft.maxTTLDays}")
    private Integer draftMaxTTLDays;

    @Value("${ccd.ui-definition.host}")
    private String uiDefinitionHost;

    @Value("${ccd.case.search.wildcards.allowed}")
    private boolean wildcardSearchAllowed;

    @Value("${ccd.user-profile.host}")
    private String userProfileHost;

    @Value("${ccd.document.url.pattern}")
    private String documentURLPattern;

    @Value("${ccd.defaultPrintUrl}")
    private String defaultPrintUrl;

    @Value("${ccd.defaultPrintName}")
    private String defaultPrintName;

    @Value("${ccd.defaultPrintDescription}")
    private String defaultPrintDescription;

    @Value("${ccd.defaultPrintType}")
    private String defaultPrintType;

    @Value("${pagination.page.size}")
    private Integer paginationPageSize;

    @Value("${default.cache.max-idle}")
    private Integer defaultCacheMaxIdleSecs;

    @Value("${default.cache.ttl}")
    private Integer defaultCacheTtlSecs;

    @Value("${definition.cache.jurisdiction-ttl}")
    private Integer jurisdictionTTL;

    @Value("#{'${definition.cache.request-scope.case-types}'.split(',')}")
    private List<String> requestScopeCachedCaseTypes;

    @Value("${definition.cache.request-scope.case-types.from-hour}")
    private Integer requestScopeCachedCaseTypesFromHour;

    @Value("${definition.cache.request-scope.case-types.till-hour}")
    private Integer requestScopeCachedCaseTypesTillHour;

    @Value("${user.cache.ttl.secs}")
    private Integer userCacheTTLSecs;

    @Value("${user.role.cache.ttl.secs}")
    private Integer userRoleCacheTTLSecs;

    @Value("${default.cache.max.size}")
    private Integer defaultCacheMaxSize;

    @Value("#{'${search.elastic.hosts}'.split(',')}")
    private List<String> elasticSearchHosts;

    @Value("#{'${search.elastic.data.hosts}'.split(',')}")
    private List<String> elasticSearchDataHosts;

    @Value("${search.elastic.request.timeout}")
    private Integer elasticSearchRequestTimeout;

    @Value("#{'${search.blacklist}'.split(',')}")
    private List<String> searchBlackList;

    @Value("${search.cases.index.name.format}")
    private String casesIndexNameFormat;

    @Value("${search.cases.index.name.case-type-id.group}")
    private String casesIndexNameCaseTypeIdGroup;

    @Value("${search.cases.index.name.case-type-id.group.position}")
    private Integer casesIndexNameCaseTypeIdGroupPosition;

    @Value("${search.cases.index.name.type}")
    private String casesIndexType;

    @Value("${search.elastic.nodes.discovery.enabled}")
    private Boolean elasticsearchNodeDiscoveryEnabled;

    @Value("${search.elastic.nodes.discovery.frequency.millis}")
    private Long elasticsearchNodeDiscoveryFrequencyMillis;

    @Value("${search.elastic.nodes.discovery.filter}")
    private String elasticsearchNodeDiscoveryFilter;

    @Value("${search.global.index.name}")
    private String globalSearchIndexName;

    @Value("${search.global.index.type}")
    private String globalSearchIndexType;

    @Value("#{'${audit.log.ignore.statues}'.split(',')}")
    private List<Integer> auditLogIgnoreStatuses;

    @Value("#{'${ccd.access-control.cross-jurisdictional-roles}'.split(',')}")
    private List<String> ccdAccessControlCrossJurisdictionRoles;

    @Value("#{'${ccd.access-control.citizen-roles}'.split(',')}")
    private List<String> ccdAccessControlCitizenRoles;

    @Value("${ccd.access-control.caseworker.role.regex}")
    private String ccdAccessControlCaseworkerRoleRegex;

    @Value("#{'${ccd.access-control.restricted-roles}'.split(',')}")
    private List<String> ccdAccessControlRestrictedRoles;

    @Value("${enable-attribute-based-access-control}")
    private boolean enableAttributeBasedAccessControl;

    @Value("${enable-pseudo-role-assignments-generation}")
    private boolean enablePseudoRoleAssignmentsGeneration;

    @Value("${enable-case-users-db-sync}")
    private boolean enableCaseUsersDbSync;

    @Value("#{'${ccd.upload-timestamp-featured-case-types}'.split(',')}")
    private List<String> uploadTimestampFeaturedCaseTypes;

    @Value("${audit.log.enabled:true}")
    private boolean auditLogEnabled;

    @Value("${document.hash.check.enabled}")
    private boolean enableDocumentHashCheck;

    @Value("${ttl.guard}")
    private Integer ttlGuard;

    @Value("${ccd.multiparty.fix.enabled}")
    private boolean multipartyFixEnabled;

    @Value("#{'${ccd.multiparty.events}'.split(',')}")
    private List<String> multipartyEvents;

    @Value("#{'${ccd.multiparty.case-types}'.split(',')}")
    private List<String> multipartyCaseTypes;

    @Value("${ccd.case-document-am-api.attachDocumentEnabled:true}")
    private boolean attachDocumentEnabled;

    @Value("${ccd.documentHashCloneEnabled:true}")
    private boolean documentHashCloneEnabled;

    @Value("${idam.data-store.system-user.username}")
    private String dataStoreSystemUserId;

    @Value("${idam.data-store.system-user.password}")
    private String dataStoreSystemUserPassword;

    @Value("#{'${case.data.issue.logging.jurisdictions}'.split(',')}")
    private List<String> caseDataIssueLoggingJurisdictions;

    @Value("${reference.data.api.url}")
    private String referenceDataApiUrl;

    @Value("${reference.data.cache.ttl.in.days}")
    private String referenceDataCacheTtlInDays;

    @Value("${system.user.token.cache.ttl.secs}")
    private Integer systemUserTokenCacheTTLSecs;

    @Value("${search.internal.case-access-metadata.enabled}")
    private boolean internalSearchCaseAccessMetadataEnabled;

    @Value("${enable-case-group-access-filtering}")
    private boolean enableCaseGroupAccessFiltering;

    @Value("#{'${ccd.callback.passthru-header-contexts}'.split(',')}")
    private List<String> callbackPassthruHeaderContexts;

    @Value("#{'${ccd.poc.case-types}'.split(',')}")
    private List<String> pocCaseTypes;

    @Value("${poc.feature.enabled}")
    private boolean isPocFeatureEnabled;

    public static String encode(final String stringToEncode) {
        try {
            return URLEncoder.encode(stringToEncode, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public static String encodeBase64(final String stringToEncode) {
        return Base64.getEncoder().encodeToString(stringToEncode.getBytes(StandardCharsets.UTF_8));
    }

    public List<String> getAuthorisedServicesForCaseUserRoles() {
        return authorisedServicesForCaseUserRoles;
    }

    public boolean isWildcardSearchAllowed() {
        return wildcardSearchAllowed;
    }

    public String jurisdictionCaseTypesDefURL(final String jurisdictionId) {
        return caseDefinitionHost + "/api/data/jurisdictions/" + encode(jurisdictionId) + "/case-type";
    }

    public String caseTypeDefURL(final String caseTypeId) {
        return caseDefinitionHost + "/api/data/case-type/" + encode(caseTypeId);
    }

    public String draftBaseURL() {
        return draftHost + "/drafts";
    }

    public String draftURL(String draftId) {
        return draftHost + "/drafts/" + encode(draftId);
    }

    public String getDraftEncryptionKey() {
        return draftEncryptionKey;
    }

    public Integer getDraftMaxTTLDays() {
        return draftMaxTTLDays;
    }

    public String caseTypeLatestVersionUrl(String caseTypeId) {
        return caseDefinitionHost + "/api/data/case-type/" + encode(caseTypeId) + "/version";
    }

    public String userRoleClassification() {
        return caseDefinitionHost + "/api/user-role?role={userRole}";
    }

    public String userRolesClassificationsURL() {
        return caseDefinitionHost + "/api/user-roles/{roles}";
    }

    public String displayWorkbasketDefURL(final String caseTypeId) {
        return uiDefinitionHost + "/api/display/work-basket-definition/" + encode(caseTypeId);
    }

    public String displaySearchResultDefURL(final String caseTypeId) {
        return uiDefinitionHost + "/api/display/search-result-definition/" + encode(caseTypeId);
    }

    public String displaySearchCasesResultDefURL(final String caseTypeId, final String useCase) {
        return uiDefinitionHost + "/api/display/search-cases-result-fields/" + encode(caseTypeId)
            + "?use_case=" + useCase;
    }

    public String displayCaseTabCollection(final String caseTypeId) {
        return uiDefinitionHost + "/api/display/tab-structure/" + encode(caseTypeId);
    }

    public String displayWizardPageCollection(final String caseTypeId, final String eventId) {
        return uiDefinitionHost + "/api/display/wizard-page-structure/case-types/" + encode(caseTypeId)
            + "/event-triggers/" + eventId;
    }

    public String jurisdictionDefURL() {
        return caseDefinitionHost + "/api/data/jurisdictions";
    }

    public String bannersURL() {
        return uiDefinitionHost + "/api/display/banners";
    }

    public String jurisdictionUiConfigsURL() {
        return uiDefinitionHost + "/api/display/jurisdiction-ui-configs";
    }

    public String searchInputDefinition(final String caseTypeId) {
        return uiDefinitionHost + "/api/display/search-input-definition/" + encode(caseTypeId);
    }

    public String workbasketInputDefinition(final String caseTypeId) {
        return uiDefinitionHost + "/api/display/work-basket-input-definition/" + encode(caseTypeId);
    }

    public String baseTypesURL() {
        return caseDefinitionHost + "/api/base-types";
    }

    public String caseRolesURL(final String userId, final String jurisdictionId, final String caseTypeId) {
        return caseDefinitionHost + "/api/data/caseworkers/" + encode(userId)
            + "/jurisdictions/" + encode(jurisdictionId) + "/case-types/" + encode(caseTypeId) + "/roles";
    }

    public String accessProfileRolesURL(String caseTypeId) {
        return String.format(
            "%s/api/data/caseworkers/uid/jurisdictions/jid/case-types/%s/access/profile/roles", caseDefinitionHost,
            encode(caseTypeId)
        );
    }

    public String roleAssignmentBaseURL() {
        return roleAssignmentServiceHost + "/am/role-assignments";
    }

    public boolean isRoleAssignmentPaginationEnabled() {
        return roleAssignmentPaginationEnabled;
    }

    public String getRoleAssignmentPageSize() {
        return roleAssignmentPageSize;
    }

    public String amDeleteByQueryRoleAssignmentsURL() {
        return roleAssignmentBaseURL() + "/query/delete";
    }

    public String amGetRoleAssignmentsURL() {
        return roleAssignmentBaseURL() + "/actors/{uid}";
    }

    public String amQueryRoleAssignmentsURL() {
        return roleAssignmentBaseURL() + "/query";
    }

    public String userDefaultSettingsURL() {
        return userProfileHost + "/user-profile/users";
    }

    public String getTokenSecret() {
        return tokenSecret;
    }

    public List<Integer> getCallbackRetries() {
        return callbackRetries;
    }

    public String getDocumentURLPattern() {
        return documentURLPattern;
    }

    public String getDefaultPrintUrl() {
        return defaultPrintUrl;
    }

    public String getDefaultPrintName() {
        return defaultPrintName;
    }

    public String getDefaultPrintDescription() {
        return defaultPrintDescription;
    }

    public String getDefaultPrintType() {
        return defaultPrintType;
    }

    public int getPaginationPageSize() {
        return paginationPageSize;
    }

    public int getDefaultCacheMaxIdleSecs() {
        return defaultCacheMaxIdleSecs;
    }

    public int getDefaultCacheTtlSecs() {
        return defaultCacheTtlSecs;
    }

    public int getJurisdictionTTLSecs() {
        return jurisdictionTTL;
    }

    public Integer getUserCacheTTLSecs() {
        return userCacheTTLSecs;
    }

    public Integer getUserRoleCacheTTLSecs() {
        return userRoleCacheTTLSecs;
    }

    public int getDefaultCacheMaxSize() {
        return defaultCacheMaxSize;
    }

    public List<String> getSearchBlackList() {
        return searchBlackList;
    }

    public List<String> getElasticSearchHosts() {
        return elasticSearchHosts;
    }

    public String getCasesIndexNameFormat() {
        return casesIndexNameFormat;
    }

    public String getCasesIndexType() {
        return casesIndexType;
    }

    public List<String> getElasticSearchDataHosts() {
        return elasticSearchDataHosts.stream().map(quotedHost ->
            quotedHost.replace("\"", "")).toList();
    }

    public Boolean isElasticsearchNodeDiscoveryEnabled() {
        return elasticsearchNodeDiscoveryEnabled;
    }

    public Long getElasticsearchNodeDiscoveryFrequencyMillis() {
        return elasticsearchNodeDiscoveryFrequencyMillis;
    }

    public String getElasticsearchNodeDiscoveryFilter() {
        return elasticsearchNodeDiscoveryFilter;
    }

    public String getGlobalSearchIndexName() {
        return globalSearchIndexName;
    }

    public String getGlobalSearchIndexType() {
        return globalSearchIndexType;
    }

    public List<String> getWriteToCCDCaseTypesOnly() {
        return writeToCCDCaseTypesOnly;
    }

    public List<String> getWriteToAMCaseTypesOnly() {
        return writeToAMCaseTypesOnly;
    }

    public List<String> getWriteToBothCaseTypes() {
        return writeToBothCaseTypes;
    }

    public List<String> getReadFromCCDCaseTypes() {
        return readFromCCDCaseTypes;
    }

    public List<String> getReadFromAMCaseTypes() {
        return readFromAMCaseTypes;
    }

    public List<String> getCcdCallbackLogControl() {
        return ccdCallbackLogControl;
    }

    public Integer getElasticSearchRequestTimeout() {
        return elasticSearchRequestTimeout;
    }

    public List<Integer> getAuditLogIgnoreStatuses() {
        return auditLogIgnoreStatuses;
    }

    public boolean isAuditLogEnabled() {
        return auditLogEnabled;
    }

    public List<String> getCcdAccessControlCrossJurisdictionRoles() {
        return ccdAccessControlCrossJurisdictionRoles;
    }

    public String getCcdAccessControlCaseworkerRoleRegex() {
        return ccdAccessControlCaseworkerRoleRegex;
    }

    public List<String> getCcdAccessControlRestrictedRoles() {
        return ccdAccessControlRestrictedRoles;
    }

    public boolean getEnableAttributeBasedAccessControl() {
        return enableAttributeBasedAccessControl;
    }

    public boolean getEnablePseudoRoleAssignmentsGeneration() {
        return enablePseudoRoleAssignmentsGeneration;
    }

    public boolean getEnableCaseUsersDbSync() {
        return enableCaseUsersDbSync;
    }

    public List<String> getCcdAccessControlCitizenRoles() {
        return ccdAccessControlCitizenRoles;
    }

    public String getCasesIndexNameCaseTypeIdGroup() {
        return casesIndexNameCaseTypeIdGroup;
    }

    public Integer getCasesIndexNameCaseTypeIdGroupPosition() {
        return casesIndexNameCaseTypeIdGroupPosition;
    }

    public boolean isDocumentHashCheckingEnabled() {
        return enableDocumentHashCheck;
    }

    public Integer getTtlGuard() {
        return ttlGuard;
    }

    public boolean isAttachDocumentEnabled() {
        return attachDocumentEnabled;
    }

    public String getDataStoreSystemUserId() {
        return dataStoreSystemUserId;
    }

    public void setDataStoreSystemUserId(String dateStoreSystemUserId) {
        this.dataStoreSystemUserId = dateStoreSystemUserId;
    }

    public String getDataStoreSystemUserPassword() {
        return dataStoreSystemUserPassword;
    }

    public void setDataStoreSystemUserPassword(String dataStoreSystemUserPassword) {
        this.dataStoreSystemUserPassword = dataStoreSystemUserPassword;
    }

    public List<String> getCaseDataIssueLoggingJurisdictions() {
        return caseDataIssueLoggingJurisdictions;
    }

    public String getReferenceDataApiUrl() {
        return referenceDataApiUrl;
    }

    public int getRefDataCacheTtlInSec() {
        return Math.toIntExact(Duration.ofDays(Long.parseLong(referenceDataCacheTtlInDays)).toSeconds());
    }

    public boolean isDocumentHashCloneEnabled() {
        return this.documentHashCloneEnabled;
    }

    public boolean isMultipartyFixEnabled() {
        return multipartyFixEnabled;
    }

    public List<String> getMultipartyEvents() {
        return multipartyEvents;
    }

    public List<String> getMultipartyCaseTypes() {
        return multipartyCaseTypes;
    }

    public List<String>  getRequestScopeCachedCaseTypes() {
        return requestScopeCachedCaseTypes;
    }

    public Integer getSystemUserTokenCacheTTLSecs() {
        return systemUserTokenCacheTTLSecs;
    }

    public boolean getInternalSearchCaseAccessMetadataEnabled() {
        return internalSearchCaseAccessMetadataEnabled;
    }

    public boolean getCaseGroupAccessFilteringEnabled() {
        return this.enableCaseGroupAccessFiltering;
    }

    public void setCaseGroupAccessFilteringEnabled(boolean enableCaseGroupAccessFiltering) {
        this.enableCaseGroupAccessFiltering = enableCaseGroupAccessFiltering;
    }

    public List<String> getCallbackPassthruHeaderContexts() {
        return callbackPassthruHeaderContexts;
    }

    public List<String> getUploadTimestampFeaturedCaseTypes() {
        return uploadTimestampFeaturedCaseTypes;
    }

    public boolean isPocFeatureEnabled() {
        return isPocFeatureEnabled;
    }

    public void setPocFeatureEnabled(boolean pocFeatureEnabled) {
        isPocFeatureEnabled = pocFeatureEnabled;
    }

    public List<String> getPocCaseTypes() {
        return pocCaseTypes;
    }

    public void setPocCaseTypes(List<String> pocCaseTypes) {
        this.pocCaseTypes = pocCaseTypes;
    }
}
