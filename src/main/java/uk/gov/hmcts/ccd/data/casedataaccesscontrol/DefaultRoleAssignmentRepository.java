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
import org.springframework.web.client.HttpStatusCodeException;
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
import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;

import static com.google.common.collect.Maps.newHashMap;
import static org.springframework.http.HttpHeaders.ETAG;

@Slf4j
@Repository
@Qualifier(DefaultRoleAssignmentRepository.QUALIFIER)
public class DefaultRoleAssignmentRepository implements RoleAssignmentRepository {

    public static final String QUALIFIER = "default";
    public static final String ROLE_ASSIGNMENTS_NOT_FOUND =
        "No Role Assignments found for userId=%s when getting from Role Assignment Service because of %s";

    @SuppressWarnings("checkstyle:LineLength") // don't want to break error messages and add unwanted +
    public static final String R_A_NOT_FOUND_FOR_CASE_AND_USER =
        "No Role Assignments found for userIds=%s and casesIds=%s when getting from Role Assignment Service because of %s";

    public static final String ROLE_ASSIGNMENTS_CLIENT_ERROR =
        "Client error when %s Role Assignments from Role Assignment Service because of %s";
    public static final String ROLE_ASSIGNMENT_SERVICE_ERROR =
        "Problem %s Role Assignments from Role Assignment Service because of %s";
    private static final String GZIP_POSTFIX = "--gzip";

    public static final Integer ROLE_ASSIGNMENT_STARTING_PAGE_NUMBER = 0;
    public static final String ROLE_ASSIGNMENT_PAGE_NUMBER_HEADER = "pageNumber";
    public static final String ROLE_ASSIGNMENT_PAGE_SIZE_HEADER = "size";
    public static final String ROLE_ASSIGNMENT_TOTAL_RECORDS_HEADER = "Total-Records";

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
    public RoleAssignmentRequestResponse createRoleAssignment(RoleAssignmentRequestResource assignmentRequest) {
        try {
            final HttpEntity<Object> requestEntity =
                new HttpEntity<>(assignmentRequest, securityUtils.authorizationHeaders());

            return restTemplate.exchange(
                applicationParams.roleAssignmentBaseURL(),
                HttpMethod.POST,
                requestEntity,
                RoleAssignmentRequestResponse.class
            ).getBody();

        } catch (HttpStatusCodeException e) {
            log.warn("Error while creating Role Assignments", e);
            throw mapException(e, "creating");
        }
    }

    @Override
    public void deleteRoleAssignmentsByQuery(List<RoleAssignmentQuery> queryRequests) {
        try {
            final HttpEntity<Object> requestEntity = new HttpEntity<>(
                MultipleQueryRequestResource.builder().queryRequests(queryRequests).build(),
                securityUtils.authorizationHeaders()
            );

            restTemplate.exchange(
                applicationParams.amDeleteByQueryRoleAssignmentsURL(),
                HttpMethod.POST,
                requestEntity,
                Void.class
            );

        } catch (HttpStatusCodeException e) {
            log.warn("Error while deleting Role Assignments", e);
            throw mapException(e, "deleting");
        }

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
            } else {
                throw mapException(e, "getting");
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

    @Override
    public RoleAssignmentResponse findRoleAssignmentsByCasesAndUsers(List<String> caseIds, List<String> userIds) {
        try {
            final var roleAssignmentQuery = new RoleAssignmentQuery(caseIds, userIds);

            if (applicationParams.isRoleAssignmentPaginationEnabled()) {
                return findRoleAssignmentsByCasesAndUsers(roleAssignmentQuery);
            } else {
                final var requestEntity = new HttpEntity(roleAssignmentQuery, securityUtils.authorizationHeaders());
                return restTemplate.exchange(
                    applicationParams.amQueryRoleAssignmentsURL(),
                    HttpMethod.POST,
                    requestEntity,
                    RoleAssignmentResponse.class).getBody();
            }
        } catch (Exception exception) {
            final ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                String.format(R_A_NOT_FOUND_FOR_CASE_AND_USER, userIds, caseIds, exception.getMessage())
            );
            throw mapException(exception, resourceNotFoundException);
        }
    }

    private RoleAssignmentResponse findRoleAssignmentsByCasesAndUsers(RoleAssignmentQuery roleAssignmentQuery) {
        int pageNumber = ROLE_ASSIGNMENT_STARTING_PAGE_NUMBER;
        int pageSize = Integer.parseInt(applicationParams.getRoleAssignmentPageSize());
        int totalRecords = 0;

        HttpHeaders headers = securityUtils.authorizationHeaders();
        headers.add(ROLE_ASSIGNMENT_PAGE_SIZE_HEADER, Integer.toString(pageSize));
        RoleAssignmentResponse roleAssignmentResponse = new RoleAssignmentResponse();

        do {
            headers.put(ROLE_ASSIGNMENT_PAGE_NUMBER_HEADER, Collections.singletonList(Integer.toString(pageNumber)));
            final var requestEntity = new HttpEntity(roleAssignmentQuery, headers);

            final ResponseEntity<RoleAssignmentResponse> responseEntity = restTemplate.exchange(
                applicationParams.amQueryRoleAssignmentsURL(),
                HttpMethod.POST,
                requestEntity,
                RoleAssignmentResponse.class);

            if (pageNumber == ROLE_ASSIGNMENT_STARTING_PAGE_NUMBER) {
                totalRecords = Integer.parseInt(Objects.requireNonNull(
                    responseEntity.getHeaders().get(ROLE_ASSIGNMENT_TOTAL_RECORDS_HEADER)).get(0));
                roleAssignmentResponse.setRoleAssignments(new ArrayList<>(totalRecords));
            }

            List<RoleAssignmentResource> roleAssignments = roleAssignmentResponse.getRoleAssignments();
            roleAssignments.addAll(Objects.requireNonNull(responseEntity.getBody()).getRoleAssignments());
            pageNumber++;
        } while ((pageNumber * pageSize) < totalRecords);

        return roleAssignmentResponse;
    }

    private RuntimeException mapException(Exception exception, ResourceNotFoundException resourceNotFoundException) {

        if (exception instanceof HttpClientErrorException
            && ((HttpClientErrorException) exception).getRawStatusCode() == HttpStatus.NOT_FOUND.value()) {
            return resourceNotFoundException;
        } else {
            return mapException(exception, "getting");
        }
    }

    private RuntimeException mapException(Exception exception, String processDescription) {

        if (exception instanceof HttpClientErrorException
            && HttpStatus.valueOf(((HttpClientErrorException) exception).getRawStatusCode()).is4xxClientError()) {
            return new BadRequestException(
                String.format(ROLE_ASSIGNMENTS_CLIENT_ERROR, processDescription, exception.getMessage()));
        } else {
            return new ServiceException(
                String.format(ROLE_ASSIGNMENT_SERVICE_ERROR, processDescription, exception.getMessage()), exception);
        }
    }



}
