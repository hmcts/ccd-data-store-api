package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    public void shouldReturnValidValueForRequestedDataJsonForPath() {
        String jsonString = supplementaryDataQueryBuilder.requestedDataJsonForPath(
            "orgs_assigned_users.organisationA", 35, "orgs_assigned_users.organisationA");

        assertEquals("35", jsonString);
    }

    @Test
    public void shouldThrowServiceExceptionForUnknownPathForRequestedDataJsonForPath() {
        ServiceException serviceException = assertThrows(ServiceException.class,
            () -> supplementaryDataQueryBuilder.requestedDataJsonForPath(
                "orgs_assigned_users.organisationA", 35, "orgs_assigned_users.test"));

        assertEquals("Path orgs_assigned_users.test is not found", serviceException.getMessage());
    }

    @Ignore // Can only be used for local testing on multithreading
    @Test
    void shouldReturnValidRequestDataPassed() {
        Thread t1 = new Thread(() -> {
            String s = supplementaryDataQueryBuilder.requestedDataToJson("orgs_assigned_users.organisationA", 32);
            assertEquals("{\n"
                + "  \"orgs_assigned_users\": {\n"
                + "    \"organisationA\": 32\n"
                + "  }\n"
                + "}", s);
        });

        Thread t2 = new Thread(() -> {
            String s = supplementaryDataQueryBuilder.requestedDataToJson("orgs_assigned_users.organisationA", 32);
            assertEquals("{\n"
                + "  \"orgs_assigned_users\": {\n"
                + "    \"organisationA\": 32\n"
                + "  }\n"
                + "}", s);
        });

        t1.start();
        t2.start();
    }

    @Ignore // Can only be used for local testing on multithreading
    @Test
    void shouldReturnValidRequestDataPassed1() {
        Thread t1 = new Thread(() -> {
            String s = supplementaryDataQueryBuilder.requestedDataToJson("orgs_assigned_users.organisationA", 32);
            assertEquals("{\n"
                + "  \"orgs_assigned_users\": {\n"
                + "    \"organisationA\": 32\n"
                + "  }\n"
                + "}", s);
        });

        Thread t2 = new Thread(() -> {
            String s = supplementaryDataQueryBuilder.requestedDataToJson("orgs_assigned_users.organisationA", 35);
            assertEquals("{\n"
                    + "  \"orgs_assigned_users\": {\n"
                    + "    \"organisationA\": 35\n"
                    + "  }\n"
                + "}", s);
        });

        t1.start();
        t2.start();
    }

}
