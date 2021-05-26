package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.springframework.http.HttpHeaders.ETAG;

@Slf4j
@Repository
@Qualifier(DefaultRoleAssignmentRepository.QUALIFIER)
public class DefaultRoleAssignmentRepository implements RoleAssignmentRepository {

    public static final String QUALIFIER = "default";
    private static final String ROLE_ASSIGNMENTS_NOT_FOUND =
        "No Role Assignments found for userId=%s when getting from Role Assignment Service because of %s";
    private static final String ROLE_ASSIGNMENTS_CLIENT_ERROR =
        "Client error when getting Role Assignments from Role Assignment Service because of %s";
    private static final String ROLE_ASSIGNMENT_SERVICE_ERROR =
        "Problem getting Role Assignments from Role Assignment Service because of %s";
    private static final String GZIP_POSTFIX = "--gzip";

    private final ApplicationParams applicationParams;
    private final SecurityUtils securityUtils;
    private final RestTemplate restTemplate;

    // UserId as a key, Pair<ETag, RoleAssignmentResponse> as a value
    private final Map<String, Pair<String, RoleAssignmentResponse>> roleAssignments = newHashMap();

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
            HttpHeaders headers = securityUtils.authorizationHeaders();
            addETagHeader(userId, headers);

            final HttpEntity<Object> requestEntity = new HttpEntity<>(headers);

            return getRoleAssignmentResponse(userId, requestEntity);
        } catch (Exception e) {
            log.warn("Error while retrieving Role Assignments", e);
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

    private void addETagHeader(String userId, HttpHeaders headers) {
        if (roleAssignments.containsKey(userId)) {
            Pair<String, RoleAssignmentResponse> stringRoleAssignmentResponsePair = roleAssignments.get(userId);
            headers.setIfNoneMatch(stringRoleAssignmentResponsePair.getKey());
        }
    }

    private RoleAssignmentResponse getRoleAssignmentResponse(String userId, HttpEntity<Object> requestEntity)
        throws URISyntaxException {

        ResponseEntity<RoleAssignmentResponse> exchange = exchangeGet(userId, requestEntity);
        log.debug("GET RoleAssignments for user={} returned response status={}", userId, exchange.getStatusCode());

        if (exchange.getStatusCode() == HttpStatus.NOT_MODIFIED && roleAssignments.containsKey(userId)) {
            return roleAssignments.get(userId).getRight();
        }
        if (exchange.getHeaders().containsKey(ETAG) && exchange.getHeaders().getETag() != null) {
            log.debug("GET RoleAssignments response contains header ETag={}", exchange.getHeaders().getETag());
            if (thereAreRoleAssignmentsInTheBody(exchange)) {
                roleAssignments.put(userId, Pair.of(getETag(exchange.getHeaders().getETag()), exchange.getBody()));
            }
        }

        return exchange.getBody();
    }

    private boolean thereAreRoleAssignmentsInTheBody(ResponseEntity<RoleAssignmentResponse> exchange) {
        RoleAssignmentResponse body = exchange.getBody();
        if (body == null) {
            return false;
        }

        List<RoleAssignmentResource> roleAssignments = body.getRoleAssignments();
        if (roleAssignments == null) {
            return false;
        }

        return !roleAssignments.isEmpty();
    }

    /**
     * 'Accept-Encoding: gzip' makes the response ETag header being suffixed with the '--gzip'.
     * This method is to drop this suffix before using the ETag.
     */
    private String getETag(String etag) {
        if (etag != null && etag.endsWith(GZIP_POSTFIX + "\"")) {
            return etag.substring(0, etag.length() - GZIP_POSTFIX.length() - 1) + "\"";
        }
        return etag;
    }

    private ResponseEntity<RoleAssignmentResponse> exchangeGet(String userId, HttpEntity<Object> requestEntity)
        throws URISyntaxException {
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("uid", ApplicationParams.encode(userId.toLowerCase()));

        final String encodedUrl = UriComponentsBuilder.fromHttpUrl(applicationParams.amGetRoleAssignmentsURL())
            .buildAndExpand(queryParams).toUriString();

        return restTemplate.exchange(new URI(encodedUrl),
                                     HttpMethod.GET, requestEntity,
                                     RoleAssignmentResponse.class);
    }
}
