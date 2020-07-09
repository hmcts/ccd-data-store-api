package uk.gov.hmcts.ccd.data.casedetails.search;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetaDataTest {
    private static final String CASE_TYPE_ID = "CaseOne";
    private static final String JURISDICTION_ID = "JurisdictionOne";
    private static final String STATE = "StateOne";
    private static final String CASE_REFERENCE = "REF#8888";
    private static final String SECURITY_CLASSIFICATION = "Public";
    private static final String PAGE = "PageOne";
    private static final String CREATED_DATE = "Now";
    private static final String MODIFIED_DATE = "Then";
    private static final String STATE_MODIFIED_DATE = "Then Again";



    private MetaData classUnderTest;

    @BeforeAll
    void setUp() {
        classUnderTest = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);
        classUnderTest.setState(Optional.of(STATE));
        classUnderTest.setCaseReference(Optional.of(CASE_REFERENCE));
        classUnderTest.setSecurityClassification(Optional.of(SECURITY_CLASSIFICATION));
        classUnderTest.setPage(Optional.of(PAGE));
        classUnderTest.setCreatedDate(Optional.of(CREATED_DATE));
        classUnderTest.setLastModifiedDate(Optional.of(MODIFIED_DATE));
        classUnderTest.setLastStateModifiedDate(Optional.of(STATE_MODIFIED_DATE));
    }

    @Test
    void shouldBeEqualToItself() {
        assertThat(classUnderTest.equals(classUnderTest), is(true));

    }

    @Test
    void shouldReturnFalseIfCheckedWithNullObject() {
        assertThat(classUnderTest.equals(null), is(false));
    }

    @Test
    void shouldReturnFalseIfCheckedWithObjectOfOtherClass() {
        Object o = new Object();
        assertThat(classUnderTest.equals(o), is(false));
    }

    @Test
    void shouldReturnFalseIfCheckedWithDifferentModifiedDate() {
        MetaData compare = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);
        compare.setState(Optional.of(STATE));
        compare.setCaseReference(Optional.of(CASE_REFERENCE));
        compare.setSecurityClassification(Optional.of(SECURITY_CLASSIFICATION));
        compare.setPage(Optional.of(PAGE));
        compare.setCreatedDate(Optional.of(CREATED_DATE));
        compare.setLastModifiedDate(Optional.of(MODIFIED_DATE + " and Bye"));
        compare.setLastStateModifiedDate(Optional.of(STATE_MODIFIED_DATE));
        assertThat(classUnderTest.equals(compare), is(false));
    }

    @Test
    @DisplayName("Should return false if checked with different CreatedDate")
    void shouldReturnFalseIfCheckedWithDifferentCreatedDate() {
        MetaData compare = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);
        compare.setState(Optional.of(STATE));
        compare.setCaseReference(Optional.of(CASE_REFERENCE));
        compare.setSecurityClassification(Optional.of(SECURITY_CLASSIFICATION));
        compare.setPage(Optional.of(PAGE));
        compare.setCreatedDate(Optional.of(CREATED_DATE + " and Bye"));
        compare.setLastModifiedDate(Optional.of(MODIFIED_DATE));
        compare.setLastStateModifiedDate(Optional.of(STATE_MODIFIED_DATE));
        assertThat(classUnderTest.equals(compare), is(false));
    }

    @Test
    void shouldReturnTrueIfCheckedWithIdenticalMetadata() {
        MetaData compare = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);
        compare.setState(Optional.of(STATE));
        compare.setCaseReference(Optional.of(CASE_REFERENCE));
        compare.setSecurityClassification(Optional.of(SECURITY_CLASSIFICATION));
        compare.setPage(Optional.of(PAGE));
        compare.setCreatedDate(Optional.of(CREATED_DATE));
        compare.setLastModifiedDate(Optional.of(MODIFIED_DATE));
        compare.setLastStateModifiedDate(Optional.of(STATE_MODIFIED_DATE));
        assertThat(classUnderTest.equals(compare), is(true));
    }

    @Test
    void shouldGetOptionalMetadataValues() {
        final Optional<String> result = classUnderTest.getOptionalMetadata(MetaData.CaseField.CASE_REFERENCE);

        assertAll(
            () -> assertThat(result.get(), is(CASE_REFERENCE))
        );
    }

    @Test
    void shouldFailToGetOptionalFieldValuesForNonOptionalField() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> classUnderTest.getOptionalMetadata(MetaData.CaseField.JURISDICTION));

        assertAll(
            () -> assertThat(exception.getMessage(),
                is("No getter method with Optional<String> return value found for 'jurisdiction'; looking for 'getJurisdiction()'"))
        );
    }

    @Test
    void shouldSetOptionalMetadataValues() {
        MetaData metadata = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);

        metadata.setOptionalMetadata(MetaData.CaseField.CASE_REFERENCE, CASE_REFERENCE);
        metadata.setOptionalMetadata(MetaData.CaseField.CREATED_DATE, CREATED_DATE);
        metadata.setOptionalMetadata(MetaData.CaseField.LAST_MODIFIED_DATE, MODIFIED_DATE);
        metadata.setOptionalMetadata(MetaData.CaseField.LAST_STATE_MODIFIED_DATE, STATE_MODIFIED_DATE);
        metadata.setOptionalMetadata(MetaData.CaseField.SECURITY_CLASSIFICATION, SECURITY_CLASSIFICATION);
        metadata.setOptionalMetadata(MetaData.CaseField.STATE, STATE);

        assertAll(
            () -> assertThat(metadata.getCaseReference().get(), is(CASE_REFERENCE)),
            () -> assertThat(metadata.getCreatedDate().get(), is(CREATED_DATE)),
            () -> assertThat(metadata.getLastModifiedDate().get(), is(MODIFIED_DATE)),
            () -> assertThat(metadata.getLastStateModifiedDate().get(), is(STATE_MODIFIED_DATE)),
            () -> assertThat(metadata.getSecurityClassification().get(), is("public")),
            () -> assertThat(metadata.getState().get(), is(STATE))
        );
    }

    @Test
    void shouldSetOptionalMetadataValueToEmptyOptionalForNullValues() {
        MetaData metadata = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);

        metadata.setOptionalMetadata(MetaData.CaseField.CASE_REFERENCE, null);

        assertAll(
            () -> assertThat(metadata.getCaseReference().isPresent(), is(false))
        );
    }

    @Test
    void shouldFailToSetOptionalFieldValuesForNonOptionalField() {
        MetaData metadata = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> metadata.setOptionalMetadata(MetaData.CaseField.JURISDICTION, "Value"));

        assertAll(
            () -> assertThat(exception.getMessage(),
                is("No setter method with Optional argument found for 'jurisdiction'; looking for 'setJurisdiction()'"))
        );
    }

    @Test
    void shouldGetEnumByReference() {
        MetaData.CaseField result = MetaData.CaseField.valueOfReference("[CREATED_DATE]");

        assertAll(
            () -> assertThat(result, is(MetaData.CaseField.CREATED_DATE))
        );
    }

    @Test
    void shouldGetEnumByReferenceWithoutPreAndSuffix() {
        MetaData.CaseField result = MetaData.CaseField.valueOfReference("CREATED_DATE");

        assertAll(
            () -> assertThat(result, is(MetaData.CaseField.CREATED_DATE))
        );
    }

    @Test
    void shouldErrorWhenRetrievingEnumByInvalidReference() {
        assertThrows(IllegalArgumentException.class,
            () -> MetaData.CaseField.valueOfReference("[INVALID]"));
    }

    @Test
    void shouldGetEnumByDbColumnName() {
        MetaData.CaseField result = MetaData.CaseField.valueOfColumnName("reference");

        assertAll(
            () -> assertThat(result, is(MetaData.CaseField.CASE_REFERENCE))
        );
    }

    @Test
    void shouldErrorWhenRetrievingEnumByInvalidDbColumnName() {
        assertThrows(IllegalArgumentException.class,
            () -> MetaData.CaseField.valueOfReference("INVALID"));
    }
}
