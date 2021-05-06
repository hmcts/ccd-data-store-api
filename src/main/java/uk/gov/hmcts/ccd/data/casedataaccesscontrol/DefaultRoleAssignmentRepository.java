package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleType;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Qualifier(DefaultRoleAssignmentRepository.QUALIFIER)
public class DefaultRoleAssignmentRepository implements RoleAssignmentRepository {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultRoleAssignmentRepository.class);

    public static final String QUALIFIER = "default";
    public static final String ROLE_ASSIGNMENTS_NOT_FOUND =
        "No Role Assignments found for userId=%s when getting from Role Assignment Service because of %s";

    @SuppressWarnings("checkstyle:LineLength") // don't want to break error messages and add unwanted +
    public static final String R_A_NOT_FOUND_FOR_CASE_AND_USER =
        "No Role Assignments found for userIds=%s and casesIds=%s when getting from Role Assignment Service because of %s";

    public static final String ROLE_ASSIGNMENTS_CLIENT_ERROR =
        "Client error when getting Role Assignments from Role Assignment Service because of %s";
    public static final String ROLE_ASSIGNMENT_SERVICE_ERROR =
        "Problem getting Role Assignments from Role Assignment Service because of %s";

    private final ApplicationParams applicationParams;
    private final SecurityUtils securityUtils;
    private final RestTemplate restTemplate;

    public DefaultRoleAssignmentRepository(final ApplicationParams applicationParams,
                                           final SecurityUtils securityUtils,
                                           @Qualifier("restTemplate") final RestTemplate restTemplate) {
        this.applicationParams = applicationParams;
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
    }

    @Override
    public RoleAssignmentResponse getRoleAssignments(String userId) {
        try {
            final HttpEntity<Object> requestEntity = new HttpEntity<>(securityUtils.authorizationHeaders());
            final Map<String, String> queryParams = new HashMap<>();
            queryParams.put("uid", ApplicationParams.encode(userId.toLowerCase()));

            final String encodedUrl = UriComponentsBuilder.fromHttpUrl(applicationParams.amGetRoleAssignmentsURL())
                .buildAndExpand(queryParams).toUriString();
            return restTemplate.exchange(new URI(encodedUrl),
                HttpMethod.GET, requestEntity,
                RoleAssignmentResponse.class).getBody();

        } catch (Exception exception) {
            final ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                String.format(ROLE_ASSIGNMENTS_NOT_FOUND, userId, exception.getMessage())
            );
            throw mapException(exception, resourceNotFoundException);
        }
    }

    @Override
    public RoleAssignmentResponse findRoleAssignmentsByCasesAndUsers(List<String> caseIds, List<String> userIds) {
        try {
            final HttpEntity requestEntity =
                new HttpEntity(getRoleAssignmentQuery(caseIds, userIds), securityUtils.authorizationHeaders());
            return restTemplate.exchange(applicationParams.amPostRoleAssignmentsQueryURL(),
                HttpMethod.POST,
                requestEntity,
                RoleAssignmentResponse.class).getBody();

        } catch (Exception exception) {
            final ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                String.format(R_A_NOT_FOUND_FOR_CASE_AND_USER, userIds, caseIds, exception.getMessage())
            );
            throw mapException(exception, resourceNotFoundException);
        }
    }

    private RuntimeException mapException(Exception exception, ResourceNotFoundException resourceNotFoundException) {

        LOG.warn("Error while retrieving Role Assignments", exception);
        if (exception instanceof HttpClientErrorException
            && ((HttpClientErrorException) exception).getRawStatusCode() == HttpStatus.NOT_FOUND.value()) {
            return resourceNotFoundException;
        } else if (exception instanceof HttpClientErrorException
            && HttpStatus.valueOf(((HttpClientErrorException) exception).getRawStatusCode()).is4xxClientError()) {
            return new BadRequestException(String.format(ROLE_ASSIGNMENTS_CLIENT_ERROR, exception.getMessage()));
        } else {
            return new ServiceException(String.format(ROLE_ASSIGNMENT_SERVICE_ERROR, exception.getMessage()));
        }
    }

    private RoleAssignmentQuery getRoleAssignmentQuery(List<String> caseIds, List<String> userIds) {
        final Attributes attribute = Attributes.builder().caseId(caseIds).build();
        final ArrayList attributes = new ArrayList<Attributes>();
        final ArrayList roleType = new ArrayList<Attributes>();
        roleType.add(RoleType.CASE.name());
        attributes.add(attribute);
        return RoleAssignmentQuery.builder().actorId(userIds).attributes(attributes).roleType(roleType).build();
    }

}
