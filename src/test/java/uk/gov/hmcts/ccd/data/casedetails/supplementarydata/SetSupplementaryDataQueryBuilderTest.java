package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Ignore;
import javax.persistence.Query;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetSupplementaryDataQueryBuilderTest extends WireMockBaseTest {

    private static final String CASE_REFERENCE = "1234569";

    @PersistenceContext
    private EntityManager em;

    @Inject
    private SetSupplementaryDataQueryBuilder supplementaryDataQueryBuilder;

    @Mock
    private Object mockObject;

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
    void shouldReturnQueryStringListWhenRequestDataPassed() {
        Query query = supplementaryDataQueryBuilder.build(em,
            CASE_REFERENCE,
            "orgs_assigned_users.organisationA",
            "ABC");
        assertNotNull(query);
        assertEquals(CASE_REFERENCE, query.getParameterValue("reference"));
        assertEquals("{orgs_assigned_users,organisationA}", query.getParameterValue("leaf_node_key"));
        assertEquals("ABC", query.getParameterValue("value"));
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
    void shouldReturnValidValueForRequestedDataJsonForPath() {
        String jsonString = supplementaryDataQueryBuilder.requestedDataJsonForPath(
            "orgs_assigned_users.organisationA", 35, "orgs_assigned_users.organisationA");

        assertEquals("35", jsonString);
    }

    @Test
    void shouldThrowServiceExceptionForUnknownPathForRequestedDataJsonForPath() {
        try {
            supplementaryDataQueryBuilder.requestedDataJsonForPath(
                "orgs_assigned_users.organisationA", 35, "orgs_assigned_users.test");
            fail("Expected an ServiceException to be thrown");
        } catch (ServiceException se) {
            assertEquals("Path orgs_assigned_users.test is not found", se.getMessage());
        }
    }

    @Test
    void shouldThrowServiceExceptionForJsonNodeToString() {
        when(mockObject.toString()).thenReturn(mockObject.getClass().getName());

        try {
            supplementaryDataQueryBuilder.jsonNodeToString(mockObject);
            fail("Expected ServiceException");
        } catch (ServiceException se) {
            assertTrue(se.getMessage().startsWith("Unable to map object to JSON string"));
        }
    }

    @Ignore("Can only be used for local testing on multithreading")
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

    @Ignore("Can only be used for local testing on multithreading")
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
