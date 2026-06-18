package uk.gov.hmcts.ccd.v2.external.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.data.caseclosed.DateCaseClosedEntity;
import uk.gov.hmcts.ccd.data.caseclosed.DateCaseClosedRepository;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLinksResource;
import uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkService;
import uk.gov.hmcts.ccd.v2.V2;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping(path = "/testing-support")
@ConditionalOnProperty(value = "testing.support.endpoint.enabled", havingValue = "true")
@Slf4j
public class TestingSupportController {

    private final CaseLinkService caseLinkService;

    private final SessionFactory sessionFactory;

    private final DateCaseClosedRepository dateCaseClosedRepository;

    @Inject
    public TestingSupportController(CaseLinkService caseLinkService,
                                    SessionFactory sessionFactory,
                                    DateCaseClosedRepository dateCaseClosedRepository) {
        this.caseLinkService = caseLinkService;
        this.sessionFactory = sessionFactory;
        this.dateCaseClosedRepository = dateCaseClosedRepository;
    }

    @GetMapping(
        path = "/case-link/{caseReference}",
        headers = {
            V2.EXPERIMENTAL_HEADER
        }
    )
    public ResponseEntity<CaseLinksResource> getCaseLink(@PathVariable("caseReference") String caseReference) {
        return ResponseEntity.ok(CaseLinksResource.builder()
                                    .caseLinks(caseLinkService.findCaseLinks(caseReference))
                                    .build());
    }

    @DeleteMapping(value = "/cleanup-case-type/{changeId}")
    @Operation(summary = "Delete a list of Case Type Schemas", description = "Blank body response.\n")
    @ApiResponse(responseCode = "204", description = "Success")
    @ApiResponse(responseCode = "404", description = "Unable to find case type")
    @ApiResponse(responseCode = "500", description = "Unexpected error")
    public void dataCaseTypeIdDelete(
        @Parameter(name = "Change ID", required = true) @PathVariable("changeId") BigInteger changeId,
        @Parameter(name = "Case Type ID", required = true) @RequestParam("caseTypeIds") String caseTypeIds) {
        log.info("Invoked for changeId {} and caseTypeIds {} ", changeId, caseTypeIds);

        var caseIdList = Arrays.stream(caseTypeIds.split(",")).toList();
        var caseTypesWithChangeIds = caseIdList.stream().map(caseTypeId -> caseTypeId + "-" + changeId).toList();

        Session session = sessionFactory.openSession();

        executeSql(
            session,
            "DELETE FROM case_link WHERE case_type_id IN (:caseTypeReferences)",
            "caseTypeReferences",
            caseTypesWithChangeIds,
            StandardBasicTypes.STRING);
        executeSql(
            session,
            "DELETE FROM case_event WHERE case_type_id IN (:caseTypeReferences)",
            "caseTypeReferences",
            caseTypesWithChangeIds,
            StandardBasicTypes.STRING);
        executeSql(
            session,
            "DELETE FROM date_case_closed WHERE ccd_case_number IN ("
                + "SELECT reference FROM case_data WHERE case_type_id IN (:caseTypeReferences)"
                + ")",
            "caseTypeReferences",
            caseTypesWithChangeIds,
            StandardBasicTypes.STRING);
        executeSql(
            session,
            "DELETE FROM case_data WHERE case_type_id IN (:caseTypeReferences)",
            "caseTypeReferences",
            caseTypesWithChangeIds,
            StandardBasicTypes.STRING);

        session.close();
        log.info("Deleted records for changeId {} and caseTypeIds {} ", changeId, caseTypeIds);
    }

    @PostMapping(value = "/date-case-closed", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a DATE_CASE_CLOSED record for functional tests")
    @ApiResponse(responseCode = "201", description = "Success")
    public ResponseEntity<DateCaseClosedEntity> dateCaseClosedPost(@RequestBody DateCaseClosedRequest request) {
        DateCaseClosedEntity entity = new DateCaseClosedEntity();
        entity.setCcdCaseNumber(request.getCcdCaseNumber());
        entity.setState(request.getState());
        entity.setStateCategory(request.getStateCategory());
        entity.setStateChangedDate(request.getStateChangedDate());

        return ResponseEntity.status(HttpStatus.CREATED).body(dateCaseClosedRepository.save(entity));
    }

    @DeleteMapping(value = "/date-case-closed")
    @Operation(summary = "Delete DATE_CASE_CLOSED records for functional tests")
    @ApiResponse(responseCode = "204", description = "Success")
    public ResponseEntity<Void> dateCaseClosedDelete(
        @RequestParam @Parameter(name = "Case Type ID", required = true) String caseTypeId,
        @RequestParam @Parameter(name = "State", required = true) String state,
        @RequestParam @Parameter(name = "State Category", required = true) String stateCategory,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Parameter(name = "State Changed Date", required = true) LocalDate stateChangedDate) {
        Session session = sessionFactory.openSession();

        session.beginTransaction();
        session.createNativeQuery(
                "DELETE FROM date_case_closed "
                    + "WHERE ccd_case_number IN ("
                    + "SELECT reference FROM case_data WHERE case_type_id = :caseTypeId"
                    + ") "
                    + "AND state = :state "
                    + "AND state_category = :stateCategory "
                    + "AND state_changed_date < :stateChangedDateEnd")
            .setParameter("caseTypeId", caseTypeId)
            .setParameter("state", state)
            .setParameter("stateCategory", stateCategory)
            .setParameter("stateChangedDateEnd", Timestamp.valueOf(stateChangedDate.plusDays(1).atStartOfDay()))
            .executeUpdate();
        session.getTransaction().commit();

        session.close();

        return ResponseEntity.noContent().build();
    }

    private <T> void executeSql(Session session, String sql, String parameterName, List<T> ids,
                                BasicTypeReference<T> type) {
        session.beginTransaction();
        session.createNativeQuery(sql)
            .setParameterList(parameterName, ids, type)
            .executeUpdate();
        session.getTransaction().commit();
    }

    @Data
    public static class DateCaseClosedRequest {
        private Long ccdCaseNumber;
        private String state;
        private String stateCategory;
        private LocalDateTime stateChangedDate;
    }

}
