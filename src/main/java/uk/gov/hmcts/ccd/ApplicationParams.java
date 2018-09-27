package uk.gov.hmcts.ccd;

import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.List;

@Named
@Singleton
public class ApplicationParams {
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

    @Value("${auth.idam.client.baseUrl}")
    private String idamHost;

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

    @Value("${definition.cache.ttl.secs}")
    private Integer definitionCacheTTLSecs;

    @Value("#{'${search.elastic.hosts}'.split(',')}")
    private List<String> elasticSearchHosts;

    @Value("#{'${search.blacklist}'.split(',')}")
    private List<String> searchBlackList;

    @Value("${search.cases.index.name.format}")
    private String casesIndexNameFormat;

    @Value("${search.cases.index.name.type}")
    private String casesIndexType;

    private static String encode(final String stringToEncode) {
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

    public String displayCaseTabCollection(final String caseTypeId) {
        return uiDefinitionHost + "/api/display/tab-structure/" + encode(caseTypeId);
    }

    public String displayWizardPageCollection(final String caseTypeId, final String eventTriggerId) {
        return uiDefinitionHost + "/api/display/wizard-page-structure/case-types/" + encode(caseTypeId) +"/event-triggers/" + encode(eventTriggerId);
    }

    public String jurisdictionDefURL() {
        return caseDefinitionHost + "/api/data/jurisdictions";
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

    public String idamUserProfileURL() {
        return idamHost + "/details";
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

    public int getDefinitionCacheTTLSecs() {
        return definitionCacheTTLSecs;
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
}
