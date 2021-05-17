package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import javax.inject.Inject;
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpHeaders.ETAG;
import static org.springframework.http.HttpHeaders.IF_NONE_MATCH;

public class DefaultRoleAssignmentRepositoryIT extends WireMockBaseTest {

    private static final String ID = "4d96923f-891a-4cb1-863e-9bec44d1689d";
    private static final String ID1 = "4d96923f-891a-4cb1-863e-9bec44d1612d";
    private static final String ACTOR_ID_TYPE = "IDAM";
    private static final String ACTOR_ID = "567567";
    private static final String ROLE_TYPE = "ORGANISATION";
    private static final String ROLE_NAME = "judge";
    private static final String CLASSIFICATION = "PUBLIC";
    private static final String GRANT_TYPE = "STANDARD";
    private static final String ROLE_CATEGORY = "JUDICIAL";
    private static final Boolean READ_ONLY = Boolean.FALSE;
    private static final String BEGIN_TIME = "2021-01-01T00:00:00Z";
    private static final String END_TIME = "2223-01-01T00:00:00Z";
    private static final String CREATED = "2020-12-23T06:37:58.000196065Z";
    private static final Instant EXPECTED_BEGIN_TIME = Instant.parse(BEGIN_TIME);
    private static final Instant EXPECTED_END_TIME = Instant.parse(END_TIME);
    private static final Instant EXPECTED_CREATED = Instant.parse(CREATED);
    private static final String ATTRIBUTES_CONTRACT_TYPE = "SALARIED";
    private static final String ATTRIBUTES_JURISDICTION = "divorce";
    private static final String ATTRIBUTES_CASE_ID = "1504259907353529";
    private static final String ATTRIBUTES_REGION = "south-east";
    private static final String ATTRIBUTES_LOCATION = "south-east-cornwall";
    private static final String AUTHORISATIONS_AUTH_1 = "auth1";
    private static final String AUTHORISATIONS_AUTH_2 = "auth2";
    private static final String POST_CODE = "EC12 3LN";

    @Inject
    private RoleAssignmentRepository roleAssignmentRepository;

    @DisplayName("should return roleAssignments")
    @Test
    public void shouldReturnRoleAssignments() {
        stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID))
                    .willReturn(okJson(jsonBody(ID))));

        validateRoleAssignments(ID);
    }

    @DisplayName("should error on 404 when GET roleAssignments")
    @Test
    public void shouldErrorOn404WhenGetRoleAssignments() {
        stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID)).willReturn(notFound()));

        final ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class, () -> roleAssignmentRepository.getRoleAssignments(ACTOR_ID));

        assertThat(exception.getMessage(),
                   startsWith("No Role Assignments found for userId="
                                  + ACTOR_ID + " when getting from Role Assignment Service because of"));
    }

    @DisplayName("should GET roleAssignments from cache when ETag found")
    @Test
    public void shouldUseETagToGetRoleAssignmentsFromCache() {
        // store the response and ETag in the cache
        stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID)).inScenario("ETag")
                    .whenScenarioStateIs(STARTED)
                    .willReturn(okJson(jsonBody(ID))
                                    .withHeader(ETAG, "\"W/123456789\"")
                    )
                    .willSetStateTo("Cache populated with RoleAssignments"));

        stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID)).inScenario("ETag")
                    .whenScenarioStateIs("Cache populated with RoleAssignments")
                    .withHeader(IF_NONE_MATCH, equalTo("\"W/123456789\""))
                    .willReturn(aResponse()
                        .withStatus(304)
                        .withHeader(ETAG, "\"W/123456789\"")));

        validateRoleAssignments(ID);
        validateRoleAssignments(ID);
    }

    @DisplayName("should update the cache when ETag differs from the one from the response")
    @Test
    public void shouldUpdateCacheWhenETagDiffersFromTheOneFromTheResponse() {
        // store the response and ETag in the cache
        stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID)).inScenario("ETag1")
                    .whenScenarioStateIs(STARTED)
                    .willReturn(okJson(jsonBody(ID))
                                    .withHeader(ETAG, "\"W/553456789\"")
                    )
                    .willSetStateTo("Cache populated with RoleAssignments"));

        // data has changed on the server and the response contains a new ETag and body
        stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID)).inScenario("ETag1")
                    .whenScenarioStateIs("Cache populated with RoleAssignments")
                    .withHeader(IF_NONE_MATCH, equalTo("\"W/553456789\""))
                    .willReturn(okJson(jsonBody(ID1))
                                    .withHeader(ETAG, "\"W/663456789\"")
                    )
                    .willSetStateTo("Cache updated with RoleAssignments"));

        stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID)).inScenario("ETag1")
                    .whenScenarioStateIs("Cache updated with RoleAssignments")
                    .withHeader(IF_NONE_MATCH, equalTo("\"W/663456789\""))
                    .willReturn(aResponse().withStatus(304)));

        validateRoleAssignments(ID);
        validateRoleAssignments(ID1);
        validateRoleAssignments(ID1);
    }

    @DisplayName("should error on 400 when GET roleAssignments")
    @Test
    public void shouldErrorOn400WhenGetRoleAssignments() {
        stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID)).willReturn(badRequest()));

        final BadRequestException exception = assertThrows(BadRequestException.class,
            () -> roleAssignmentRepository.getRoleAssignments(ACTOR_ID));

        assertThat(exception.getMessage(),
                   startsWith("Client error when getting Role Assignments from Role Assignment Service because of "));
    }

    @DisplayName("should error on 500 when GET roleAssignments")
    @Test
    public void shouldErrorOn500WhenGetRoleAssignments() {
        stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID)).willReturn(serverError()));

        final ServiceException exception = assertThrows(ServiceException.class,
            () -> roleAssignmentRepository.getRoleAssignments(ACTOR_ID));

        assertThat(exception.getMessage(),
                   startsWith("Problem getting Role Assignments from Role Assignment Service because of "));
    }

    @DisplayName("should return roleAssignments")
    @Test
    public void shouldReturnRoleAssignmentsWhenUnknownFieldsOnRequest() {
        stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID))
            .willReturn(okJson(jsonBodyUnknownFields(ID))));

        validateRoleAssignments(ID);
    }

    private static String jsonBody(String id) {
        return "{\n"
            + "  \"roleAssignmentResponse\": [\n"
            + "    {\n"
            + "      \"id\": \"" + id + "\",\n"
            + "      \"actorIdType\": \"" + ACTOR_ID_TYPE + "\",\n"
            + "      \"actorId\": \"" + ACTOR_ID + "\",\n"
            + "      \"roleType\": \"" + ROLE_TYPE + "\",\n"
            + "      \"roleName\": \"" + ROLE_NAME + "\",\n"
            + "      \"classification\": \"" + CLASSIFICATION + "\",\n"
            + "      \"grantType\": \"" + GRANT_TYPE + "\",\n"
            + "      \"roleCategory\": \"" + ROLE_CATEGORY + "\",\n"
            + "      \"readOnly\": " + READ_ONLY + ",\n"
            + "      \"beginTime\": \"" + BEGIN_TIME + "\",\n"
            + "      \"endTime\": \"" + END_TIME + "\",\n"
            + "      \"created\": \"" + CREATED + "\",\n"
            + "      \"attributes\": {\n"
            + "        \"contractType\": \"" + ATTRIBUTES_CONTRACT_TYPE + "\",\n"
            + "        \"jurisdiction\": \"" + ATTRIBUTES_JURISDICTION + "\",\n"
            + "        \"caseId\": \"" + ATTRIBUTES_CASE_ID + "\",\n"
            + "        \"location\": \"" + ATTRIBUTES_LOCATION + "\",\n"
            + "        \"region\": \"" + ATTRIBUTES_REGION + "\"\n"
            + "      },\n"
            + "      \"authorisations\": [\"" + AUTHORISATIONS_AUTH_1 + "\", \"" + AUTHORISATIONS_AUTH_2 + "\"]\n"
            + "    }\n"
            + "  ]\n"
            + "}";
    }

    private void validateRoleAssignments(String id) {
        RoleAssignmentResponse roleAssignments = roleAssignmentRepository.getRoleAssignments(ACTOR_ID);

        assertThat(roleAssignments.getRoleAssignments().size(), is(1));
        RoleAssignmentResource roleAssignmentResource = roleAssignments.getRoleAssignments().get(0);
        assertThat(roleAssignmentResource.getId(), is(id));
        assertThat(roleAssignmentResource.getActorIdType(), is(ACTOR_ID_TYPE));
        assertThat(roleAssignmentResource.getActorId(), is(ACTOR_ID));
        assertThat(roleAssignmentResource.getRoleType(), is(ROLE_TYPE));
        assertThat(roleAssignmentResource.getRoleName(), is(ROLE_NAME));
        assertThat(roleAssignmentResource.getClassification(), is(CLASSIFICATION));
        assertThat(roleAssignmentResource.getGrantType(), is(GRANT_TYPE));
        assertThat(roleAssignmentResource.getRoleCategory(), is(ROLE_CATEGORY));
        assertThat(roleAssignmentResource.getReadOnly(), is(READ_ONLY));
        assertThat(roleAssignmentResource.getBeginTime(), is(EXPECTED_BEGIN_TIME));
        assertThat(roleAssignmentResource.getEndTime(), is(EXPECTED_END_TIME));
        assertThat(roleAssignmentResource.getCreated(), is(EXPECTED_CREATED));

        assertThat(roleAssignmentResource.getAttributes().getContractType().get(), is(ATTRIBUTES_CONTRACT_TYPE));
        assertThat(roleAssignmentResource.getAttributes().getJurisdiction().get(), is(ATTRIBUTES_JURISDICTION));
        assertThat(roleAssignmentResource.getAttributes().getCaseId().get(), is(ATTRIBUTES_CASE_ID));
        assertThat(roleAssignmentResource.getAttributes().getLocation().get(), is(ATTRIBUTES_LOCATION));
        assertThat(roleAssignmentResource.getAttributes().getRegion().get(), is(ATTRIBUTES_REGION));

        assertThat(roleAssignmentResource.getAuthorisations().size(), is(2));
        assertThat(roleAssignmentResource.getAuthorisations().get(0), is(AUTHORISATIONS_AUTH_1));
        assertThat(roleAssignmentResource.getAuthorisations().get(1), is(AUTHORISATIONS_AUTH_2));
    }

    private static String jsonBodyUnknownFields(String id) {
        return "{\n"
            + "  \"roleAssignmentResponse\": [\n"
            + "    {\n"
            + "      \"id\": \"" + id + "\",\n"
            + "      \"actorIdType\": \"" + ACTOR_ID_TYPE + "\",\n"
            + "      \"actorId\": \"" + ACTOR_ID + "\",\n"
            + "      \"roleType\": \"" + ROLE_TYPE + "\",\n"
            + "      \"roleName\": \"" + ROLE_NAME + "\",\n"
            + "      \"classification\": \"" + CLASSIFICATION + "\",\n"
            + "      \"grantType\": \"" + GRANT_TYPE + "\",\n"
            + "      \"roleCategory\": \"" + ROLE_CATEGORY + "\",\n"
            + "      \"readOnly\": " + READ_ONLY + ",\n"
            + "      \"beginTime\": \"" + BEGIN_TIME + "\",\n"
            + "      \"endTime\": \"" + END_TIME + "\",\n"
            + "      \"created\": \"" + CREATED + "\",\n"
            + "      \"fieldA\": \"" + CREATED + "\",\n"
            + "      \"attributes\": {\n"
            + "        \"contractType\": \"" + ATTRIBUTES_CONTRACT_TYPE + "\",\n"
            + "        \"jurisdiction\": \"" + ATTRIBUTES_JURISDICTION + "\",\n"
            + "        \"caseId\": \"" + ATTRIBUTES_CASE_ID + "\",\n"
            + "        \"location\": \"" + ATTRIBUTES_LOCATION + "\",\n"
            + "        \"region\": \"" + ATTRIBUTES_REGION + "\",\n"
            + "        \"postCode\": \"" + POST_CODE + "\"\n"
            + "      },\n"
            + "      \"authorisations\": [\"" + AUTHORISATIONS_AUTH_1 + "\", \"" + AUTHORISATIONS_AUTH_2 + "\"]\n"
            + "    }\n"
            + "  ]\n"
            + "}";
    }

}
