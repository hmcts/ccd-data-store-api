package uk.gov.hmcts.ccd.data.casedetails.search;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetaDataTest {
    private final String CASE_TYPE_ID = "CaseOne";
    private final String JURISDICTION_ID = "JurisdictionOne";
    private final String STATE = "StateOne";
    private final String CASE_REFERENCE = "REF#8888";
    private final String SECURITY_CLASSIFICATION = "Public";
    private final String PAGE = "PageOne";
    private final String CREATED_DATE = "Now";
    private final String MODIFIED_DATE = "Then";



    private MetaData classUnderTest;

    @BeforeAll
    void setUp() {
        classUnderTest = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);
        classUnderTest.setState(Optional.of(STATE));
        classUnderTest.setCaseReference(Optional.of(CASE_REFERENCE));
        classUnderTest.setSecurityClassification(Optional.of(SECURITY_CLASSIFICATION));
        classUnderTest.setPage(Optional.of(PAGE));
        classUnderTest.setCreatedDate(Optional.of(CREATED_DATE));
        classUnderTest.setLastModified(Optional.of(MODIFIED_DATE));
    }

    @Test
    void shouldBeEqualToItself(){
        assertThat(classUnderTest.equals(classUnderTest), is(true));

    }

    @Test
    void shouldReturnFalseIfCheckedWithNullObject(){
        assertThat(classUnderTest.equals(null), is(false));
    }

    @Test
    void shouldReturnFalseIfCheckedWithObjectOfOtherClass(){
        Object o = new Object();
        assertThat(classUnderTest.equals(o), is(false));
    }

    @Test
    void shouldReturnFalseIfCheckedWithDifferentModifiedDate(){
        MetaData compare = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);
        compare.setState(Optional.of(STATE));
        compare.setCaseReference(Optional.of(CASE_REFERENCE));
        compare.setSecurityClassification(Optional.of(SECURITY_CLASSIFICATION));
        compare.setPage(Optional.of(PAGE));
        compare.setCreatedDate(Optional.of(CREATED_DATE));
        compare.setLastModified(Optional.of(MODIFIED_DATE + " and Bye"));
        assertThat(classUnderTest.equals(compare), is(false));
    }

    @Test
    @DisplayName("Should return false if checked with different CreatedDate")
    void shouldReturnFalseIfCheckedWithDifferentCreatedDate(){
        MetaData compare = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);
        compare.setState(Optional.of(STATE));
        compare.setCaseReference(Optional.of(CASE_REFERENCE));
        compare.setSecurityClassification(Optional.of(SECURITY_CLASSIFICATION));
        compare.setPage(Optional.of(PAGE));
        compare.setCreatedDate(Optional.of(CREATED_DATE + " and Bye"));
        compare.setLastModified(Optional.of(MODIFIED_DATE));
        assertThat(classUnderTest.equals(compare), is(false));
    }

    @Test
    void shouldReturnTrueIfCheckedWithIdenticalMetadata(){
        MetaData compare = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);
        compare.setState(Optional.of(STATE));
        compare.setCaseReference(Optional.of(CASE_REFERENCE));
        compare.setSecurityClassification(Optional.of(SECURITY_CLASSIFICATION));
        compare.setPage(Optional.of(PAGE));
        compare.setCreatedDate(Optional.of(CREATED_DATE));
        compare.setLastModified(Optional.of(MODIFIED_DATE));
        assertThat(classUnderTest.equals(compare), is(true));
    }
}
