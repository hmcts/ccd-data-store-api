package uk.gov.hmcts.ccd.data.definition;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.ApplicationParams.encodeBase64;

@SuppressWarnings("checkstyle:SummaryJavadoc")
// partial javadoc attributes added prior to checkstyle implementation in module
@Service
@Qualifier(DefaultCaseDefinitionRepository.QUALIFIER)
public class DefaultCaseDefinitionRepository implements CaseDefinitionRepository {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultCaseDefinitionRepository.class);

    public static final String QUALIFIER = "default";
    private static final int RESOURCE_NOT_FOUND = 404;

    private final ApplicationParams applicationParams;
    private final DefinitionStoreClient definitionStoreClient;

    public DefaultCaseDefinitionRepository(final ApplicationParams applicationParams,
                                           final DefinitionStoreClient definitionStoreClient) {
        this.applicationParams = applicationParams;
        this.definitionStoreClient = definitionStoreClient;
    }

    /**
     * @deprecated current implementation has serious performance issues
     */
    @Deprecated
    @SuppressWarnings("squid:S1133")
    @Override
    public List<CaseTypeDefinition> getCaseTypesForJurisdiction(final String jurisdictionId) {
        try {
            return Arrays.asList(Objects.requireNonNull(definitionStoreClient.invokeGetRequest(
                applicationParams.jurisdictionCaseTypesDefURL(jurisdictionId),
                CaseTypeDefinition[].class).getBody()));
        } catch (Exception e) {
            LOG.warn("Error while retrieving base type", e);
            if (e instanceof HttpClientErrorException
                    && ((HttpClientErrorException) e).getRawStatusCode() == RESOURCE_NOT_FOUND) {
                throw new ResourceNotFoundException("Resource not found when getting case types for Jurisdiction:"
                        + jurisdictionId + " because of " + e.getMessage());
            } else {
                throw new ServiceException("Problem getting case types for the Jurisdiction:" + jurisdictionId
                        + " because of " + e.getMessage(), e);
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
            final CaseTypeDefinition caseTypeDefinition = definitionStoreClient
                .invokeGetRequest(applicationParams.caseTypeDefURL(caseTypeId),
                CaseTypeDefinition.class).getBody();
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
                        "Problem getting case type definition for " + caseTypeId + " because of " + e.getMessage(), e);
            }
        }
    }

    @Override
    public CaseTypeDefinition getScopedCachedCaseType(String caseTypeId) {
        return this.getCaseType(caseTypeId);
    }

    @Override
    public List<FieldTypeDefinition> getBaseTypes() {
        try {
            return Arrays.asList(Objects.requireNonNull(definitionStoreClient
                .invokeGetRequest(applicationParams.baseTypesURL(),
                FieldTypeDefinition[].class).getBody()));
        } catch (Exception e) {
            LOG.warn("Error while retrieving base types", e);
            if (e instanceof HttpClientErrorException
                && ((HttpClientErrorException) e).getRawStatusCode() == RESOURCE_NOT_FOUND) {
                throw new ResourceNotFoundException(
                    "Problem getting base types definition from definition store because of " + e.getMessage());
            } else {
                throw new ServiceException(
                    "Problem getting base types definition from definition store because of " + e.getMessage(), e);
            }
        }
    }

    @Override
    @Cacheable("userRolesCache")
    public UserRole getUserRoleClassifications(String userRole) {
        try {
            final Map<String, String> queryParams = new HashMap<>();
            queryParams.put("userRole", encodeBase64(userRole));
            return definitionStoreClient.invokeGetRequest(applicationParams.userRoleClassification(),
                UserRole.class, queryParams).getBody();
        } catch (Exception e) {
            if (e instanceof HttpClientErrorException
                && ((HttpClientErrorException) e).getRawStatusCode() == RESOURCE_NOT_FOUND) {
                LOG.debug("No classification found for user role {} because of ", userRole, e);
                return null;
            } else {
                LOG.warn("Error while retrieving classification for user role {} because of ", userRole, e);
                throw new ServiceException("Error while retrieving classification for user role " + userRole
                    + " because of " + e.getMessage(), e);
            }
        }
    }

    @Override
    public List<UserRole> getClassificationsForUserRoleList(List<String> userRoles) {
        try {
            if (userRoles.isEmpty()) {
                return Collections.emptyList();
            }
            final Map<String, String> queryParams = new HashMap<>();
            queryParams.put("roles", StringUtils.join(userRoles, ","));
            return Arrays.asList(Objects.requireNonNull(definitionStoreClient
                .invokeGetRequest(applicationParams.userRolesClassificationsURL(),
                UserRole[].class, queryParams).getBody()));
        } catch (Exception e) {
            LOG.warn("Error while retrieving classification for user roles {} because of ", userRoles, e);
            throw new ServiceException("Error while retrieving classification for user roles " + userRoles
                    + " because of " + e.getMessage(), e);
        }
    }

    @Override
    @Cacheable("caseTypeDefinitionLatestVersionCache")
    public CaseTypeDefinitionVersion getLatestVersion(String caseTypeId) {
        return getLatestVersionFromDefinitionStore(caseTypeId);
    }

    public CaseTypeDefinitionVersion getLatestVersionFromDefinitionStore(String caseTypeId) {
        try {
            CaseTypeDefinitionVersion version = definitionStoreClient
                .invokeGetRequest(applicationParams.caseTypeLatestVersionUrl(caseTypeId),
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
        return retrieveJurisdictions(Optional.of(Collections.singletonList(jurisdictionId)))
            .stream()
            .findFirst()
            .orElse(null);
    }

    @Cacheable(value = "allJurisdictionsCache")
    @Override
    public List<JurisdictionDefinition> getAllJurisdictionsFromDefinitionStore() {
        return retrieveJurisdictions(Optional.of(Collections.emptyList()));
    }

    public List<JurisdictionDefinition> retrieveJurisdictions(Optional<List<String>> jurisdictionIds) {
        return getJurisdictionsFromDefinitionStore(jurisdictionIds);
    }

    @Override
    public List<String> getCaseTypesIDsByJurisdictions(List<String> jurisdictionIds) {

        List<JurisdictionDefinition> jurisdictionDefinitions = getJurisdictionsFromDefinitionStore(
                Optional.of(jurisdictionIds));
        if (jurisdictionDefinitions.isEmpty()) {
            LOG.warn("Definitions not found for requested jurisdictions {}", jurisdictionIds);
            return Collections.emptyList();
        }
        return getCaseTypeIdFromJurisdictionDefinition(jurisdictionDefinitions);
    }

    @Override
    public List<String> getAllCaseTypesIDs() {
        List<JurisdictionDefinition> jurisdictionDefinitions = getJurisdictionsFromDefinitionStore(
                Optional.empty());
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
            jurisdictionIds.ifPresent(ids -> builder.queryParam("ids", String.join(",", ids)));

            LOG.debug("Retrieving jurisdiction object(s) from definition store for Jurisdiction IDs: {}.",
                jurisdictionIds.orElse(Collections.emptyList()));

            List<JurisdictionDefinition> jurisdictionDefinitionList = Optional.ofNullable(
                    definitionStoreClient.invokeGetRequest(builder.build().encode().toUriString(),
                        JurisdictionDefinition[].class).getBody())
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
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
                throw new ServiceException("Problem retrieving jurisdictions definition because of " + e.getMessage(),
                    e);
            }
        }
    }
}
