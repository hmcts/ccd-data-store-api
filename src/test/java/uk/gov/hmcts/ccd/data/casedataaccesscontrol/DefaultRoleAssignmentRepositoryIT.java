package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import javax.inject.Inject;
import java.time.LocalDateTime;

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefaultRoleAssignmentRepositoryIT extends WireMockBaseTest {

    private static final String ID = "4d96923f-891a-4cb1-863e-9bec44d1689d";
    private static final String ACTOR_ID_TYPE = "IDAM";
    private static final String ACTOR_ID = "567567";
    private static final String ROLE_TYPE = "ORGANISATION";
    private static final String ROLE_NAME = "judge";
    private static final String CLASSIFICATION = "PUBLIC";
    private static final String GRANT_TYPE = "STANDARD";
    private static final String ROLE_CATEGORY = "JUDICIAL";
    private static final Boolean READ_ONLY = Boolean.FALSE;
    private static final String BEGIN_TIME = "2021-01-01T00:00:00Z";
    private static final LocalDateTime EXPECTED_BEGIN_TIME = LocalDateTime.of(2021, 01, 01, 00, 00, 00);
    private static final String END_TIME = "2223-01-01T00:00:00Z";
    private static final LocalDateTime EXPECTED_END_TIME = LocalDateTime.of(2223, 01, 01, 00, 00, 00);
    private static final String CREATED = "2020-12-23T06:37:58.000196065Z";
    private static final LocalDateTime EXPECTED_CREATED = LocalDateTime.of(2020, 12, 23, 06, 37, 58, 196065);
    private static final String ATTRIBUTES_CONTRACT_TYPE = "SALARIED";
    private static final String ATTRIBUTES_JURISDICTION = "divorce";
    private static final String ATTRIBUTES_CASE_ID = "1504259907353529";
    private static final String ATTRIBUTES_REGION = "south-east";
    private static final String ATTRIBUTES_LOCATION = "south-east-cornwall";
    private static final String AUTHORISATIONS_AUTH_1 = "auth1";
    private static final String AUTHORISATIONS_AUTH_2 = "auth2";

    @Inject
    private RoleAssignmentRepository roleAssignmentRepository;

    @DisplayName("should return roleAssignments")
    @Test
    public void shouldReturnRoleAssignments() {
        stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID)).willReturn(okJson(jsonBody())));

        RoleAssignmentResponse roleAssignments = roleAssignmentRepository.getRoleAssignments(ACTOR_ID);

        assertThat(roleAssignments.getRoleAssignments().size(), is(1));
        RoleAssignmentDTO roleAssignmentDTO = roleAssignments.getRoleAssignments().get(0);
        assertThat(roleAssignmentDTO.getId(), is(ID));
        assertThat(roleAssignmentDTO.getActorIdType(), is(ACTOR_ID_TYPE));
        assertThat(roleAssignmentDTO.getActorId(), is(ACTOR_ID));
        assertThat(roleAssignmentDTO.getRoleType(), is(ROLE_TYPE));
        assertThat(roleAssignmentDTO.getRoleName(), is(ROLE_NAME));
        assertThat(roleAssignmentDTO.getClassification(), is(CLASSIFICATION));
        assertThat(roleAssignmentDTO.getGrantType(), is(GRANT_TYPE));
        assertThat(roleAssignmentDTO.getRoleCategory(), is(ROLE_CATEGORY));
        assertThat(roleAssignmentDTO.getReadOnly(), is(READ_ONLY));
        assertThat(roleAssignmentDTO.getBeginTime(), is(EXPECTED_BEGIN_TIME));
        assertThat(roleAssignmentDTO.getEndTime(), is(EXPECTED_END_TIME));
        assertThat(roleAssignmentDTO.getCreated(), is(EXPECTED_CREATED));

        assertThat(roleAssignmentDTO.getAttributes().getContractType(), is(ATTRIBUTES_CONTRACT_TYPE));
        assertThat(roleAssignmentDTO.getAttributes().getJurisdiction(), is(ATTRIBUTES_JURISDICTION));
        assertThat(roleAssignmentDTO.getAttributes().getCaseId(), is(ATTRIBUTES_CASE_ID));
        assertThat(roleAssignmentDTO.getAttributes().getLocation(), is(ATTRIBUTES_LOCATION));
        assertThat(roleAssignmentDTO.getAttributes().getRegion(), is(ATTRIBUTES_REGION));

        assertThat(roleAssignmentDTO.getAuthorisations().size(), is(2));
        assertThat(roleAssignmentDTO.getAuthorisations().get(0), is(AUTHORISATIONS_AUTH_1));
        assertThat(roleAssignmentDTO.getAuthorisations().get(1), is(AUTHORISATIONS_AUTH_2));
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

    private static String jsonBody() {
        return "{\n"
            + "  \"roleAssignmentResponse\": [\n"
            + "    {\n"
            + "      \"id\": \"" + ID + "\",\n"
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

}
