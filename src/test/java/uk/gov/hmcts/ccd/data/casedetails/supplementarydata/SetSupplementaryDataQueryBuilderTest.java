package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.WireMockBaseTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SetSupplementaryDataQueryBuilderTest extends WireMockBaseTest {

    private static final String CASE_REFERENCE = "1234569";

    @PersistenceContext
    private EntityManager em;

    @Inject
    private SetSupplementaryDataQueryBuilder supplementaryDataQueryBuilder;

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
        assertEquals("{orgs_assigned_users}", query.getParameterValue("parent_key"));
    }

    @Test
    void shouldReturnMoreThanOneQueryInTheListWhenRequestDataPassedWithMultipleValues() {
        Query query = supplementaryDataQueryBuilder.build(em,
            CASE_REFERENCE,
            "orgs_assigned_users.organisationB",
            36);
        assertNotNull(query);
        assertEquals(CASE_REFERENCE, query.getParameterValue("reference"));
    }
}
