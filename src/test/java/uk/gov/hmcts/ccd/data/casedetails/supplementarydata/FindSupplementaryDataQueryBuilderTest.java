package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.WireMockBaseTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FindSupplementaryDataQueryBuilderTest  extends WireMockBaseTest {

    private static final String CASE_REFERENCE = "1234567";

    @PersistenceContext
    private EntityManager em;

    @Inject
    private FindSupplementaryDataQueryBuilder supplementaryDataQueryBuilder;

    @Test
    void shouldBuildFindQuery() {
        List<Query>  queryList = supplementaryDataQueryBuilder.buildQueries(em, CASE_REFERENCE, new HashMap<>());
        assertNotNull(queryList);
        assertEquals(1, queryList.size());
        assertEquals(CASE_REFERENCE, queryList.get(0).getParameterValue("reference"));
    }
}
