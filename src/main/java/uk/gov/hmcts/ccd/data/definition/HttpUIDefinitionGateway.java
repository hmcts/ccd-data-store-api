package uk.gov.hmcts.ccd.data.definition;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.BannersResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfigResult;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputFieldsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResultDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageCollection;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputFieldsDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.net.URI;
import java.util.List;

/**
 * NOTE: We want to cache definitions, so the only client of this class should be CachedUIDefinitionGateway.
 */
@Named
@Qualifier("Http")
@Singleton
public class HttpUIDefinitionGateway implements UIDefinitionGateway {

    private final ApplicationParams applicationParams;
    private final DefinitionStoreClient definitionStoreClient;

    @Inject
    HttpUIDefinitionGateway(final ApplicationParams applicationParams,
                            final DefinitionStoreClient definitionStoreClient) {
        this.applicationParams = applicationParams;
        this.definitionStoreClient = definitionStoreClient;
    }

    @Override
    public SearchResultDefinition getSearchResult(int version, String caseTypeId) {
        try {
            return definitionStoreClient.invokeGetRequest(
                    withVersionQueryParam(applicationParams.displaySearchResultDefURL(caseTypeId), version),
                    SearchResultDefinition.class)
                .getBody();
        } catch (final Exception e) {
            throw new ServiceException(String.format(
                "Problem getting SearchResultDefinition definition for case type: %s because of %s",
                caseTypeId,
                e.getMessage()), e);
        }
    }

    @Override
    public SearchInputFieldsDefinition getSearchInputFieldDefinitions(int version, String caseTypeId) {
        try {
            return definitionStoreClient.invokeGetRequest(
                    withVersionQueryParam(applicationParams.searchInputDefinition(caseTypeId), version),
                    SearchInputFieldsDefinition.class)
                .getBody();
        } catch (final Exception e) {
            throw new ServiceException(String.format(
                "Problem getting SearchInputs definition for case type: %s because of %s",
                caseTypeId,
                e.getMessage()), e);
        }
    }

    @Override
    public WorkbasketInputFieldsDefinition getWorkbasketInputFieldsDefinitions(int version, String caseTypeId) {
        try {
            return definitionStoreClient.invokeGetRequest(
                    withVersionQueryParam(applicationParams.workbasketInputDefinition(caseTypeId), version),
                    WorkbasketInputFieldsDefinition.class)
                .getBody();
        } catch (final Exception e) {
            throw new ServiceException(String.format(
                "Problem getting WorkbasketInputs definition for case type: %s because of %s",
                caseTypeId,
                e.getMessage()), e);
        }
    }

    @Override
    public CaseTypeTabsDefinition getCaseTypeTabsCollection(int version, String caseTypeId) {
        return definitionStoreClient.invokeGetRequest(
                withVersionQueryParam(applicationParams.displayCaseTabCollection(caseTypeId), version),
                CaseTypeTabsDefinition.class)
            .getBody();
    }

    @Override
    public List<WizardPage> getWizardPageCollection(int version, String caseTypeId, String eventId) {
        final WizardPageCollection wizardPageCollection = definitionStoreClient.invokeGetRequest(
                withVersionQueryParam(applicationParams.displayWizardPageCollection(caseTypeId, eventId), version),
                WizardPageCollection.class)
            .getBody();
        return wizardPageCollection != null ? wizardPageCollection.getWizardPages() : null;
    }

    @Override
    public SearchResultDefinition getWorkBasketResult(int version, String caseTypeId) {
        try {
            return definitionStoreClient.invokeGetRequest(
                    withVersionQueryParam(applicationParams.displayWorkbasketDefURL(caseTypeId), version),
                    SearchResultDefinition.class)
                    .getBody();
        } catch (final Exception e) {
            throw new ServiceException(String.format(
                "Problem getting WorkBasketResult definition for case type: %s because of %s",
                caseTypeId,
                e.getMessage()), e);
        }
    }

    @Override
    public SearchResultDefinition getSearchCasesResultDefinition(int version, String caseTypeId, String useCase) {
        try {
            return definitionStoreClient.invokeGetRequest(
                    withVersionQueryParam(applicationParams.displaySearchCasesResultDefURL(caseTypeId, useCase),
                        version),
                    SearchResultDefinition.class)
                    .getBody();
        } catch (Exception e) {
            throw new ServiceException(String.format(
                "Problem getting SearchCasesResult definition for case type: %s because of %s",
                caseTypeId,
                e.getMessage()), e);
        }
    }

    private URI withVersionQueryParam(String url, int version) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url).queryParam("version", version);
        return builder.build().encode().toUri();
    }

    public BannersResult getBanners(final List<String> jurisdictionIds) {
        try {
            return definitionStoreClient.invokeGetRequest(
                withJurisdictionIds(applicationParams.bannersURL(), jurisdictionIds),
                    BannersResult.class)
                .getBody();
        } catch (final Exception e) {
            throw new ServiceException(String.format(
                "Problem getting banners for jurisdiction references: %s because of %s",
                jurisdictionIds,
                e.getMessage()), e);
        }
    }

    public JurisdictionUiConfigResult getJurisdictionUiConfigs(final List<String> jurisdictionIds) {
        try {
            return definitionStoreClient.invokeGetRequest(
                    withJurisdictionIds(applicationParams.jurisdictionUiConfigsURL(), jurisdictionIds),
                    JurisdictionUiConfigResult.class)
                    .getBody();
        } catch (final Exception e) {
            throw new ServiceException(String.format(
                "Problem getting jurisdiction UI configs for jurisdiction references: %s because of %s",
                jurisdictionIds,
                e.getMessage()), e);
        }
    }

    private URI withJurisdictionIds(String url, final List<String> jurisdictionIds) {
        UriComponentsBuilder builder =
            UriComponentsBuilder.fromHttpUrl(url).queryParam("ids", String.join(",", jurisdictionIds));
        return builder.build().encode().toUri();
    }
}
