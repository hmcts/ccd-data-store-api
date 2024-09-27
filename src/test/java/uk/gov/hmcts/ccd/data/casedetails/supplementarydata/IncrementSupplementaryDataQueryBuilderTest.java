package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.WireMockBaseContractTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IncrementSupplementaryDataQueryBuilderTest extends WireMockBaseContractTest {

    private static final String CASE_REFERENCE = "1234568";

    @PersistenceContext
    private EntityManager em;

    @Inject
    private IncrementSupplementaryDataQueryBuilder supplementaryDataQueryBuilder;

    @Test
    void shouldReturnQueryListWhenRequestDataPassed() {
        Query query = supplementaryDataQueryBuilder.build(em,
            CASE_REFERENCE,
            "orgs_assigned_users.organisationA",
            32);
        assertNotNull(query);
        assertEquals(CASE_REFERENCE, query.getParameterValue("reference"));
        assertEquals("{orgs_assigned_users,organisationA}", query.getParameterValue("leaf_node_key"));
        assertEquals(32, query.getParameterValue("value"));
    }

    @Test
    void shouldReturnMoreThanOneQueryInListWhenRequestDataPassedWithMultipleLeafNodes() {
        Query query = supplementaryDataQueryBuilder.build(em,
            CASE_REFERENCE,
            "orgs_assigned_users.organisationB",
            33);
        assertNotNull(query);
        assertEquals(CASE_REFERENCE, query.getParameterValue("reference"));
    }
}
