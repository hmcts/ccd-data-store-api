package uk.gov.hmcts.ccd.data.definition;

import static uk.gov.hmcts.ccd.ApplicationParams.encodeBase64;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

@SuppressWarnings("checkstyle:SummaryJavadoc")
// partial javadoc attributes added prior to checkstyle implementation in module
@Service
@Qualifier(DefaultCaseDefinitionRepository.QUALIFIER)
public class DefaultCaseDefinitionRepository implements CaseDefinitionRepository {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultCaseDefinitionRepository.class);

    public static final String QUALIFIER = "default";
    private static final int RESOURCE_NOT_FOUND = 404;

    private final ApplicationParams applicationParams;
    private final SecurityUtils securityUtils;
    @Qualifier("restTemplate")
    @Autowired
    private final RestTemplate restTemplate;

    @Inject
    public DefaultCaseDefinitionRepository(final ApplicationParams applicationParams, final SecurityUtils securityUtils,
            final RestTemplate restTemplate) {
        this.applicationParams = applicationParams;
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
    }

    /**
     * @deprecated current implementation has serious performance issues
     */
    @Deprecated
    @SuppressWarnings("squid:S1133")
    @Override
    public List<CaseTypeDefinition> getCaseTypesForJurisdiction(final String jurisdictionId) {
        try {
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            return Arrays.asList(restTemplate.exchange(applicationParams.jurisdictionCaseTypesDefURL(jurisdictionId),
                    HttpMethod.GET, requestEntity, CaseTypeDefinition[].class).getBody());
        } catch (Exception e) {
            LOG.warn("Error while retrieving base type", e);
            if (e instanceof HttpClientErrorException
                    && ((HttpClientErrorException) e).getRawStatusCode() == RESOURCE_NOT_FOUND) {
                throw new ResourceNotFoundException("Resource not found when getting case types for Jurisdiction:"
                        + jurisdictionId + " because of " + e.getMessage());
            } else {
                throw new ServiceException("Problem getting case types for the Jurisdiction:" + jurisdictionId
                        + " because of " + e.getMessage());
            }
        }
    }

    @Override
    @Cacheable("caseTypeDefinitionsCache")
    public CaseTypeDefinition getCaseType(int version, String caseTypeId) {
        return this.getCaseType(caseTypeId);
    }

    @Override
    public CaseTypeDefinition getCaseType(final String caseTypeId) {
        LOG.debug("retrieving case type definition for case type: {}", caseTypeId);
        try {
            final HttpEntity<CaseTypeDefinition> requestEntity = new HttpEntity<>(securityUtils.authorizationHeaders());
            final CaseTypeDefinition caseTypeDefinition = restTemplate
                    .exchange(applicationParams.caseTypeDefURL(caseTypeId), HttpMethod.GET, requestEntity,
                            CaseTypeDefinition.class)
                    .getBody();
            if (caseTypeDefinition != null) {
                caseTypeDefinition.getCaseFieldDefinitions().stream()
                        .forEach(CaseFieldDefinition::propagateACLsToNestedFields);
            }
            return caseTypeDefinition;

        } catch (Exception e) {
            LOG.warn("Error while retrieving case type", e);
            if (e instanceof HttpClientErrorException
                    && ((HttpClientErrorException) e).getRawStatusCode() == RESOURCE_NOT_FOUND) {
                throw new ResourceNotFoundException("Resource not found when getting case type definition for "
                        + caseTypeId + " because of " + e.getMessage());
            } else {
                throw new ServiceException(
                        "Problem getting case type definition for " + caseTypeId + " because of " + e.getMessage());
            }
        }
    }

    @Override
    public List<FieldTypeDefinition> getBaseTypes() {
        try {
            final HttpEntity requestEntity = new HttpEntity<CaseTypeDefinition>(securityUtils.authorizationHeaders());
            return Arrays.asList(restTemplate.exchange(applicationParams.baseTypesURL(), HttpMethod.GET, requestEntity,
                    FieldTypeDefinition[].class).getBody());
        } catch (Exception e) {
            LOG.warn("Error while retrieving base types", e);
            if (e instanceof HttpClientErrorException
                    && ((HttpClientErrorException) e).getRawStatusCode() == RESOURCE_NOT_FOUND) {
                throw new ResourceNotFoundException(
                        "Problem getting base types definition from definition store because of " + e.getMessage());
            } else {
                throw new ServiceException(
                        "Problem getting base types definition from definition store because of " + e.getMessage());
            }
        }
    }

    @Override
    @Cacheable("userRolesCache")
    public UserRole getUserRoleClassifications(String userRole) {
        try {
            final HttpEntity requestEntity = new HttpEntity<CaseTypeDefinition>(securityUtils.authorizationHeaders());
            final Map<String, String> queryParams = new HashMap<>();
            queryParams.put("userRole", encodeBase64(userRole));
            return restTemplate.exchange(applicationParams.userRoleClassification(), HttpMethod.GET, requestEntity,
                    UserRole.class, queryParams).getBody();
        } catch (Exception e) {
            if (e instanceof HttpClientErrorException
                    && ((HttpClientErrorException) e).getRawStatusCode() == RESOURCE_NOT_FOUND) {
                LOG.debug("No classification found for user role {} because of ", userRole, e);
                return null;
            } else {
                LOG.warn("Error while retrieving classification for user role {} because of ", userRole, e);
                throw new ServiceException("Error while retrieving classification for user role " + userRole
                        + " because of " + e.getMessage());
            }
        }
    }

    @Override
    public List<UserRole> getClassificationsForUserRoleList(List<String> userRoles) {
        try {
            if (userRoles.isEmpty()) {
                return Collections.emptyList();
            }
            final HttpEntity requestEntity = new HttpEntity<CaseTypeDefinition>(securityUtils.authorizationHeaders());
            final Map<String, String> queryParams = new HashMap<>();
            queryParams.put("roles", StringUtils.join(userRoles, ","));
            return Arrays.asList(restTemplate.exchange(applicationParams.userRolesClassificationsURL(), HttpMethod.GET,
                    requestEntity, UserRole[].class, queryParams).getBody());
        } catch (Exception e) {
            LOG.warn("Error while retrieving classification for user roles {} because of ", userRoles, e);
            throw new ServiceException("Error while retrieving classification for user roles " + userRoles
                    + " because of " + e.getMessage());
        }
    }

    @Override
    @Cacheable("caseTypeDefinitionLatestVersionCache")
    public CaseTypeDefinitionVersion getLatestVersion(String caseTypeId) {
        return getLatestVersionFromDefinitionStore(caseTypeId);
    }

    public CaseTypeDefinitionVersion getLatestVersionFromDefinitionStore(String caseTypeId) {
        try {
            final HttpEntity<CaseTypeDefinition> requestEntity = new HttpEntity<>(securityUtils.authorizationHeaders());
            CaseTypeDefinitionVersion version = restTemplate
                    .exchange(applicationParams.caseTypeLatestVersionUrl(caseTypeId), HttpMethod.GET, requestEntity,
                            CaseTypeDefinitionVersion.class)
                    .getBody();
            LOG.debug("retrieved latest version for case type: {}: {}", caseTypeId, version);
            return version;

        } catch (Exception e) {
            LOG.warn("Error while retrieving case type version", e);
            if (e instanceof HttpClientErrorException
                    && ((HttpClientErrorException) e).getRawStatusCode() == RESOURCE_NOT_FOUND) {
                throw new ResourceNotFoundException(
                        "Error when getting case type version. Unknown case type '" + caseTypeId + "'.", e);
            } else {
                throw new ServiceException("Problem getting case type version for '" + caseTypeId + "'.", e);
            }
        }
    }

    @Cacheable(value = "jurisdictionCache")
    @Override
    public JurisdictionDefinition getJurisdiction(String jurisdictionId) {
        return getJurisdictionFromDefinitionStore(jurisdictionId);
    }

    public JurisdictionDefinition getJurisdictionFromDefinitionStore(String jurisdictionId) {
        List<JurisdictionDefinition> jurisdictionDefinitions = getJurisdictionsFromDefinitionStore(
                Optional.of(Arrays.asList(jurisdictionId)));
        if (jurisdictionDefinitions.isEmpty()) {
            return null;
        }
        return jurisdictionDefinitions.get(0);
    }

    @Override
    public List<String> getCaseTypesIDsByJurisdictions(List<String> jurisdictionIds) {

        List<JurisdictionDefinition> jurisdictionDefinitions = getJurisdictionsFromDefinitionStore(
                Optional.of(jurisdictionIds));
        if (jurisdictionDefinitions.isEmpty()) {
            LOG.warn("Definitions not found for requested jurisdictions {}", jurisdictionIds);
            return Collections.emptyList();
        }
        List<String> caseTypes = getCaseTypeIdFromJurisdictionDefinition(jurisdictionDefinitions);
        return caseTypes;
    }

    @Override
    public List<String> getAllCaseTypesIDs() {
        List<JurisdictionDefinition> jurisdictionDefinitions = getJurisdictionsFromDefinitionStore(
                Optional.ofNullable(null));
        return getCaseTypeIdFromJurisdictionDefinition(jurisdictionDefinitions);
    }

    private List<String> getCaseTypeIdFromJurisdictionDefinition(List<JurisdictionDefinition> jurisdictionDefinitions) {
        return jurisdictionDefinitions.stream()
                .flatMap(jurisdictionDefinition -> jurisdictionDefinition.getCaseTypesIDs().stream()).distinct()
                .collect(Collectors.toList());
    }

    private List<JurisdictionDefinition> getJurisdictionsFromDefinitionStore(Optional<List<String>> jurisdictionIds) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(applicationParams.jurisdictionDefURL());
            if (jurisdictionIds.isPresent()) {
                builder.queryParam("ids", String.join(",", jurisdictionIds.get()));
            }
            LOG.debug("Retrieving jurisdiction object(s) from definition store for Jurisdiction IDs: {}.",
                    jurisdictionIds.orElse(Collections.emptyList()));
            HttpEntity<List<JurisdictionDefinition>> requestEntity = new HttpEntity<>(
                    securityUtils.authorizationHeaders());

            List<JurisdictionDefinition> jurisdictionDefinitionList = restTemplate
                    .exchange(builder.build().encode().toUri(), HttpMethod.GET, requestEntity,
                            new ParameterizedTypeReference<List<JurisdictionDefinition>>() {
                            })
                    .getBody();
            if (jurisdictionDefinitionList == null) {
                jurisdictionDefinitionList = Collections.emptyList();
            }
            LOG.debug("Retrieved jurisdiction object(s) from definition store: {}.", jurisdictionDefinitionList);
            return jurisdictionDefinitionList;
        } catch (Exception e) {
            LOG.warn("Error while retrieving jurisdictions definition", e);
            if (e instanceof HttpClientErrorException
                    && ((HttpClientErrorException) e).getRawStatusCode() == RESOURCE_NOT_FOUND) {
                LOG.warn("Jurisdiction object(s) configured for user couldn't be found on definition store: {}.",
                        jurisdictionIds.orElse(Collections.emptyList()));
                return new ArrayList<>();
            } else {
                throw new ServiceException("Problem retrieving jurisdictions definition because of " + e.getMessage());
            }
        }
    }

}
