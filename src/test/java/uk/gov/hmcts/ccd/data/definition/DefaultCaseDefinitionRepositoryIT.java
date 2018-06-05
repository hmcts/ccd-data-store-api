package uk.gov.hmcts.ccd.data.definition;

import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.Test;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;

import javax.inject.Inject;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;

public class DefaultCaseDefinitionRepositoryIT extends WireMockBaseTest {
    @Inject
    private CaseDefinitionRepository caseDefinitionRepository;

    @Test
    public void shouldGetCaseTypesForJurisdiction() {
        final List<CaseType> caseTypes = caseDefinitionRepository.getCaseTypesForJurisdiction("probate");
        assertEquals("HTTP call results failed", 2, caseTypes.size());

    }

    @Test
    public void shouldGetCaseType() {
        final CaseType caseType = caseDefinitionRepository.getCaseType("TestAddressBookCase");
        assertEquals("Incorrect Case Type", "TestAddressBookCase", caseType.getId());
    }

    @Test
    public void shouldGetBaseTypes() {
        final List<FieldType> baseTypes = caseDefinitionRepository.getBaseTypes();

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
            () -> assertThat(baseTypes, hasItem(hasProperty("type", is("Complex")))),
            () -> assertThat(baseTypes, hasItem(hasProperty("type", is("Collection")))),
            () -> assertThat(baseTypes, hasItem(hasProperty("type", is("MultiSelectList")))),
            () -> assertThat(baseTypes, hasItem(hasProperty("type", is("Complex")))),
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
    public void shouldReturnNullWhenNoClassificationFound() {
        final UserRole userRole = caseDefinitionRepository.getUserRoleClassifications("caseworker-probate-loa0");
        assertThat(userRole, is(nullValue()));
    }

    @Test
    public void shouldGetJurisdictionsDefinition() {

        List<Jurisdiction> allJurisdictions = caseDefinitionRepository.getJurisdictions(newArrayList("PROBATE", "DIVORCE", "SSCS"));

        assertAll(
                () -> assertThat(allJurisdictions, hasSize(3)),
                () -> assertThat(allJurisdictions, hasItem(hasProperty("id", is("SSCS")))),
                () -> assertThat(allJurisdictions, hasItem(hasProperty("id", is("PROBATE"))))
        );
    }
}
