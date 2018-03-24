package uk.gov.hmcts.ccd.data.casedetails.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class CriteriaFactoryTest {

    private static final String META_DATA_0_VALUE = "someValue";
    private static final String META_DATA_1_VALUE = "TESTJ";
    private static final String META_DATA_1 = "jurisdiction";

    private static final String FIELD_DATA_1 = "case.complex.simple.value";
    private static final String FIELD_DATA_1_CONVERTED = "'{complex,simple,value}'";
    private static final String FIELD_DATA_1_VALUE = "simple";

    Map<String, String> params = new HashMap<String, String>();
    private CriterionFactory subject;

    @Before
    public void create() {
        subject = new CriterionFactory();
        params.clear();
    }

    @Test
    public void checetaMetaDataCreationTest() {

        List<Criterion> result = subject.build(new MetaData(META_DATA_0_VALUE, META_DATA_1_VALUE), params);
        assertEquals(2, result.size());
        Criterion criterion = result.get(1);
        assertTrue(criterion instanceof MetaDataCriterion);
        assertEquals(criterion.getField(), META_DATA_1);
        assertEquals(criterion.getSoughtValue(), META_DATA_1_VALUE);
    }

    @Test
    public void checkDataCreationTest() {
        params.put(FIELD_DATA_1, FIELD_DATA_1_VALUE);
        List<Criterion> result = subject.build(new MetaData(META_DATA_0_VALUE, META_DATA_1_VALUE), params);
        assertEquals(3, result.size());
        Criterion criterion = result.get(0);
        assertTrue(criterion instanceof FieldDataCriterion);
        assertEquals(FIELD_DATA_1, FIELD_DATA_1);
        assertEquals(criterion.getSoughtValue(), FIELD_DATA_1_VALUE);
        assertTrue(criterion.buildClauseString(1, "AND").contains(FIELD_DATA_1_CONVERTED));
    }

    @Test
    public void checkSingleDataAndMetaDateCreationTest() {
        params.put(FIELD_DATA_1, FIELD_DATA_1_VALUE);

        List<Criterion> result = subject.build(new MetaData(META_DATA_0_VALUE, META_DATA_1_VALUE), params);
        assertEquals(3, result.size());
        assertEquals(result.stream().filter(c -> c instanceof FieldDataCriterion).count(), 1);
        assertEquals(result.stream().filter(c -> c instanceof MetaDataCriterion).count(), 2);
    }

    @Test
    public void checetaDataCreationTest() {

        List<Criterion> result = subject.build(new MetaData(META_DATA_0_VALUE, META_DATA_1_VALUE), params);
        assertEquals(2, result.size());
        Criterion criterion = result.get(1);
        assertTrue(criterion instanceof MetaDataCriterion);
        assertEquals(criterion.getField(), META_DATA_1);
        assertEquals(criterion.getSoughtValue(), META_DATA_1_VALUE);
    }

}
