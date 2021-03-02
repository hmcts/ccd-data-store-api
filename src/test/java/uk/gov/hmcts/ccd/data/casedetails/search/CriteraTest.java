package uk.gov.hmcts.ccd.data.casedetails.search;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class CriteraTest {

    private static final String META_DATA_1_VALUE = "TESTJ";
    private static final String META_DATA_1 = "jurestion";

    private static final String FIELD_DATA_1 = "case.complex.simple.value";
    private static final String FIELD_DATA_1_CONVERTED = "'{complex,simple,value}'";
    private static final String FIELD_DATA_1_VALUE = "simple";

    @Test
    public void checkFieldDataCreationTestCriteraString() {
        FieldDataCriterion subject = new FieldDataCriterion(FIELD_DATA_1, FIELD_DATA_1_VALUE);
        assertTrue(subject.buildClauseString("AND").contains(FIELD_DATA_1_CONVERTED));
    }

    @Test
    public void checkMetaDataCreationTestCriteraString() {
        MetaDataCriterion subject = new MetaDataCriterion(META_DATA_1, META_DATA_1_VALUE);
        assertTrue(subject.buildClauseString("AND").contains(META_DATA_1));
    }

}
