package uk.gov.hmcts.ccd;

import com.hazelcast.config.EvictionPolicy;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Named
@Singleton
public class ApplicationParams {

    @Value("#{'${ccd.s2s-authorised.services.add_case_user_roles}'.split(',')}")
    private List<String> authorisedServicesForAddUserCaseRoles;

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

    @Value("${ccd.token.secret}")
    private String tokenSecret;

    @Value("${ccd.case-definition.host}")
    private String caseDefinitionHost;

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

    @Value("${ccd.dm.domain}")
    private String validDMDomain;

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

    @Value("${definition.cache.max-idle.secs}")
    private Integer definitionCacheMaxIdleSecs;

    @Value("${definition.cache.latest-version-ttl}")
    private Integer latestVersionTTLSecs;

    @Value("${definition.cache.jurisdiction-ttl}")
    private Integer jurisdictionTTL;

    @Value("${user.cache.ttl.secs}")
    private Integer userCacheTTLSecs;

    @Value("${definition.cache.max.size}")
    private Integer definitionCacheMaxSize;

    @Value("${definition.cache.eviction.policy}")
    private EvictionPolicy definitionCacheEvictionPolicy;

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

    @Value("#{'${audit.log.ignore.statues}'.split(',')}")
    private List<Integer> auditLogIgnoreStatuses;

    @Value("#{'${ccd.access-control.cross-jurisdictional-roles}'.split(',')}")
    private List<String> ccdAccessControlCrossJurisdictionRoles;

    @Value("#{'${ccd.access-control.citizen-roles}'.split(',')}")
    private List<String> ccdAccessControlCitizenRoles;

    @Value("${audit.log.enabled:true}")
    private boolean auditLogEnabled;

    public static String encode(final String stringToEncode) {
        try {
            return URLEncoder.encode(stringToEncode, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public static String encodeBase64(final String stringToEncode) {
        try {
            return Base64.getEncoder().encodeToString(stringToEncode.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public List<String> getAuthorisedServicesForAddUserCaseRoles() {
        return authorisedServicesForAddUserCaseRoles;
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
        return draftHost + "/drafts/" + draftId;
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
        return uiDefinitionHost + "/api/display/search-cases-result-fields/" + encode(caseTypeId) + "?usecase=" + useCase;
    }

    public String displayCaseTabCollection(final String caseTypeId) {
        return uiDefinitionHost + "/api/display/tab-structure/" + encode(caseTypeId);
    }

    public String displayWizardPageCollection(final String caseTypeId, final String eventId) {
        return uiDefinitionHost + "/api/display/wizard-page-structure/case-types/" + encode(caseTypeId) + "/event-triggers/" + eventId;
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

    public String caseRolesURL() {
        return caseDefinitionHost + "/api/data/caseworkers/uid/jurisdictions/jid/case-types";
    }

    public String userDefaultSettingsURL() {
        return userProfileHost + "/user-profile/users?uid={uid}";
    }

    public String getTokenSecret() {
        return tokenSecret;
    }

    public List<Integer> getCallbackRetries() {
        return callbackRetries;
    }

    public String getValidDMDomain() {
        return validDMDomain;
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

    public int getDefinitionCacheMaxIdleSecs() {
        return definitionCacheMaxIdleSecs;
    }

    public int getLatestVersionTTLSecs() {
        return latestVersionTTLSecs;
    }

    public int getJurisdictionTTLSecs() {
        return jurisdictionTTL;
    }

    public Integer getUserCacheTTLSecs() {
        return userCacheTTLSecs;
    }

    public int getDefinitionCacheMaxSize() {
        return definitionCacheMaxSize;
    }

    public EvictionPolicy getDefinitionCacheEvictionPolicy() {
        return definitionCacheEvictionPolicy;
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
        return elasticSearchDataHosts.stream().map(quotedHost -> quotedHost.replace("\"", "")).collect(toList());
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

    public List<String> getCcdAccessControlCitizenRoles() {
        return ccdAccessControlCitizenRoles;
    }

    public String getCasesIndexNameCaseTypeIdGroup() {
        return casesIndexNameCaseTypeIdGroup;
    }

    public Integer getCasesIndexNameCaseTypeIdGroupPosition() {
        return casesIndexNameCaseTypeIdGroupPosition;
    }
}
