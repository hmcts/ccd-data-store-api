package uk.gov.hmcts.ccd.data.definition;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

@Named
@Singleton
public class UIDefinitionRepository {
    private final ApplicationParams applicationParams;
    private final SecurityUtils securityUtils;
    private final RestTemplate restTemplate;

    @Inject
    public UIDefinitionRepository(final ApplicationParams applicationParams, SecurityUtils securityUtils, RestTemplate restTemplate) {
        this.applicationParams = applicationParams;
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
    }

    public SearchResult getWorkBasketResult(final String caseTypeId) {
        try {
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            return restTemplate.exchange(applicationParams.displayWorkbasketDefURL(caseTypeId), HttpMethod.GET, requestEntity, SearchResult.class).getBody();
        } catch (final Exception e) {
            throw new ServiceException(String.format("Problem getting WorkBasketResult definition for case type: %s because of %s", caseTypeId, e.getMessage()));
        }
    }

    public SearchResult getSearchResult(final String caseTypeId) {
        try {
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            return restTemplate.exchange(applicationParams.displaySearchResultDefURL(caseTypeId), HttpMethod.GET, requestEntity,SearchResult.class).getBody();
        } catch (final Exception e) {
            throw new ServiceException(String.format("Problem getting SearchResult definition for case type: %s because of %s", caseTypeId, e.getMessage()));
        }
    }

    public SearchInputDefinition getSearchInputDefinitions(final String caseTypeId) {
        try {
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            return restTemplate.exchange(applicationParams.searchInputDefinition(caseTypeId), HttpMethod.GET, requestEntity, SearchInputDefinition.class).getBody();
        } catch (final Exception e) {
            throw new ServiceException(String.format("Problem getting SearchInputs definition for case type: %s because of %s", caseTypeId, e.getMessage()));
        }
    }
    public WorkbasketInputDefinition getWorkbasketInputDefinitions(final String caseTypeId) {
        try {
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            return restTemplate.exchange(applicationParams.workbasketInputDefinition(caseTypeId), HttpMethod.GET, requestEntity, WorkbasketInputDefinition.class).getBody();
        } catch (final Exception e) {
            throw new ServiceException(String.format("Problem getting WorkbasketInputs definition for case type: %s because of %s", caseTypeId, e.getMessage()));
        }
    }

    public CaseTabCollection getCaseTabCollection(final String caseTypeId) {
        final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
        return restTemplate.exchange(applicationParams.displayCaseTabCollection(caseTypeId), HttpMethod.GET, requestEntity, CaseTabCollection.class).getBody();
    }

    public List<WizardPage> getWizardPageCollection(final String caseTypeId, final String eventTriggerId) {
        final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
        WizardPageCollection wpc =  restTemplate.exchange(applicationParams.displayWizardPageCollection(caseTypeId, eventTriggerId), HttpMethod.GET, requestEntity, WizardPageCollection.class).getBody();
        return wpc.getWizardPages();
    }
}
