package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
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
        Query  query = supplementaryDataQueryBuilder.build(em, CASE_REFERENCE, null, null);
        assertNotNull(query);
        assertEquals(CASE_REFERENCE, query.getParameterValue("reference"));
    }
}
