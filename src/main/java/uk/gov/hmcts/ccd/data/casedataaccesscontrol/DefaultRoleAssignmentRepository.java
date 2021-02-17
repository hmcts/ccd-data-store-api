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
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Repository
@Qualifier(DefaultRoleAssignmentRepository.QUALIFIER)
public class DefaultRoleAssignmentRepository implements RoleAssignmentRepository {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultRoleAssignmentRepository.class);

    public static final String QUALIFIER = "default";
    public static final String ROLE_ASSIGNMENTS_NOT_FOUND =
        "No Role Assignments found for userId=%s when getting from Role Assignment Service because of %s";
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

        } catch (Exception e) {
            LOG.warn("Error while retrieving Role Assignments", e);
            if (e instanceof HttpClientErrorException
                && ((HttpClientErrorException) e).getRawStatusCode() == HttpStatus.NOT_FOUND.value()) {
                throw new ResourceNotFoundException(String.format(ROLE_ASSIGNMENTS_NOT_FOUND,
                                                                  userId, e.getMessage()));
            } else if (e instanceof HttpClientErrorException
                && HttpStatus.valueOf(((HttpClientErrorException) e).getRawStatusCode()).is4xxClientError()) {
                throw new BadRequestException(String.format(ROLE_ASSIGNMENTS_CLIENT_ERROR, e.getMessage()));
            } else {
                throw new ServiceException(String.format(ROLE_ASSIGNMENT_SERVICE_ERROR, e.getMessage()));
            }
        }
    }
}
