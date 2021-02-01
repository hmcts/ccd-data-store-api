package uk.gov.hmcts.ccd.data.definition;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;

public class DefaultCaseDefinitionRepositoryIT extends WireMockBaseTest {
    @Inject
    private uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository caseDefinitionRepository;

    @Test
    public void shouldGetCaseTypesForJurisdiction() {
        final List<CaseTypeDefinition> caseTypeDefinitions =
            caseDefinitionRepository.getCaseTypesForJurisdiction("probate");
        assertEquals("HTTP call results failed", 2, caseTypeDefinitions.size());
    }

    @Test
    public void shouldGetCaseType() {
        final CaseTypeDefinition caseTypeDefinition =
            caseDefinitionRepository.getCaseType("TestAddressBookCase");
        assertEquals("Incorrect Case Type", "TestAddressBookCase", caseTypeDefinition.getId());
    }

    @Test
    public void shouldGetBaseTypes() {
        final List<FieldTypeDefinition> baseTypes = caseDefinitionRepository.getBaseTypes();

        assertAll(
            "Assert All of these",
            () -> assertThat(baseTypes, IsCollectionWithSize.hasSize(15)),
            () -> assertThat(baseTypes, hasItem(hasProperty("type", is("Text")))),
            () -> assertThat(baseTypes, hasItem(hasProperty("type", is("Number")))),
            () -> assertThat(baseTypes, hasItem(hasProperty("type", is("Email")))),
            () -> assertThat(baseTypes, hasItem(hasProperty("type", is("YesOrNo")))),
            () -> assertThat(baseTypes, hasItem(hasProperty("type", is("Date")))),
            () -> assertThat(baseTypes, hasItem(hasProperty("type", is("Date")))),
            () -> assertThat(baseTypes, hasItem(hasProperty("type", is("FixedList")))),
            () -> assertThat(baseTypes, hasItem(hasProperty("type", is("PostCode")))),
            () -> assertThat(baseTypes, hasItem(hasProperty("type", is("MoneyGBP")))),
            () -> assertThat(baseTypes, hasItem(hasProperty("type", is("PhoneUK")))),
            () -> assertThat(baseTypes, hasItem(hasProperty("type", is("TextArea")))),
            () -> assertThat(baseTypes, hasItem(hasProperty("type", is(COLLECTION)))),
            () -> assertThat(baseTypes, hasItem(hasProperty("type", is("MultiSelectList")))),
            () -> assertThat(baseTypes, hasItem(hasProperty("type", is(COMPLEX)))),
            () -> assertThat(baseTypes, hasItem(hasProperty("type", is("Document"))))
        );
    }

    @Test
    public void shouldGetClassificationForUserRole() {
        final UserRole userRole = caseDefinitionRepository.getUserRoleClassifications("caseworker-probate");

        assertAll(
            () -> assertThat(userRole, hasProperty("securityClassification", is("PUBLIC"))),
            () -> assertThat(userRole, hasProperty("role", is("caseworker-probate")))
        );
    }

    @Test
    public void shouldGetClassificationsForUserRolesList() {
        List<String> roles = Arrays.asList("caseworker-test", "caseworker-probate", "caseworker-divorce", "caseworker");
        final List<UserRole> userRoles = caseDefinitionRepository.getClassificationsForUserRoleList(roles);

        assertAll(
            () -> assertThat(userRoles.size(), is(3)),
            () -> assertThat(userRoles.get(0), hasProperty("securityClassification", is("PRIVATE"))),
            () -> assertThat(userRoles.get(0), hasProperty("role", is("caseworker-test"))),
            () -> assertThat(userRoles.get(1), hasProperty("securityClassification", is("PUBLIC"))),
            () -> assertThat(userRoles.get(1), hasProperty("role", is("caseworker-probate"))),
            () -> assertThat(userRoles.get(2), hasProperty("securityClassification", is("RESTRICTED"))),
            () -> assertThat(userRoles.get(2), hasProperty("role", is("caseworker-divorce")))
        );
    }

    @Test
    public void shouldReturnNullWhenNoClassificationFound() {
        final UserRole userRole = caseDefinitionRepository.getUserRoleClassifications("caseworker-probate-loa0");
        assertThat(userRole, is(nullValue()));
    }

    @Test
    public void shouldGetJurisdictionsDefinition() {
        List<JurisdictionDefinition> allJurisdictionDefinitions =
            newArrayList("PROBATE", "DIVORCE", "SSCS").stream()
                .map(id -> caseDefinitionRepository.getJurisdiction(id)).collect(Collectors.toList());

        assertAll(
            () -> assertThat(allJurisdictionDefinitions, hasSize(3)),
            () -> assertThat(allJurisdictionDefinitions, hasItem(hasProperty("id", is("SSCS")))),
            () -> assertThat(allJurisdictionDefinitions, hasItem(hasProperty("id", is("PROBATE"))))
        );
    }

    @Test
    public void shouldFailToGetCaseTypesForJurisdiction() {
        stubFor(WireMock.get(urlMatching("/api/data/jurisdictions/server_error/case-type")).willReturn(serverError()));
        final ServiceException exception = assertThrows(ServiceException.class,
            () -> caseDefinitionRepository.getCaseTypesForJurisdiction("server_error"));
        assertThat(exception.getMessage(), startsWith("Problem getting case types for the Jurisdiction:server_error "
            + "because of "));
    }

    @Test
    public void shouldFailToGetCaseType() {
        stubFor(WireMock.get(urlMatching("/api/data/case-type/anything")).willReturn(serverError()));
        final ServiceException exception = assertThrows(ServiceException.class,
            () -> caseDefinitionRepository.getCaseType("anything"));
        assertThat(exception.getMessage(), startsWith("Problem getting case type definition for anything because of "));
    }

    @Test
    public void shouldFailToGetBaseTypes() {
        when(caseDefinitionRepository.getBaseTypes()).thenCallRealMethod();
        stubFor(WireMock.get(urlMatching("/api/base-types")).willReturn(serverError()));
        final ServiceException exception = assertThrows(ServiceException.class,
            () -> caseDefinitionRepository.getBaseTypes());
        assertThat(exception.getMessage(), startsWith("Problem getting base types definition from definition store "
            + "because of "));
    }

    @Test
    public void shouldFailToGetClassificationsForUserRoleList() {
        List<String> userRoles = Arrays.asList("neither_defined", "nor_defined");
        stubFor(WireMock.get(urlMatching("/api/user-roles/neither_defined,nor_defined"))
            .willReturn(serverError()));
        final ServiceException exception = assertThrows(ServiceException.class,
            () -> caseDefinitionRepository.getClassificationsForUserRoleList(userRoles));
        Assert.assertThat(exception.getMessage(),
            startsWith("Error while retrieving classification for user roles " + userRoles + " because of "));
    }
}
