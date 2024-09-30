package uk.gov.hmcts.ccd.v2.external.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.type.StringType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLinksResource;
import uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkService;
import uk.gov.hmcts.ccd.v2.V2;

import javax.inject.Inject;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping(path = "/testing-support")
@ConditionalOnProperty(value = "testing.support.endpoint.enabled", havingValue = "true")
@Slf4j
public class TestingSupportController {

    private final CaseLinkService caseLinkService;

    private final SessionFactory sessionFactory;

    @Inject
    public TestingSupportController(CaseLinkService caseLinkService,
                                    SessionFactory sessionFactory) {
        this.caseLinkService = caseLinkService;
        this.sessionFactory = sessionFactory;
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
    @ApiOperation(value = "Delete a list of Case Type Schemas", notes = "Blank body response.\n")
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "Success"),
        @ApiResponse(code = 404, message = "Unable to find case type"),
        @ApiResponse(code = 500, message = "Unexpected error")
    })
    public void dataCaseTypeIdDelete(
        @ApiParam(value = "Change ID", required = true) @PathVariable("changeId") BigInteger changeId,
        @ApiParam(value = "Case Type ID", required = true) @RequestParam("caseTypeIds") String caseTypeIds) {
        log.info("Invoked for changeId {} and caseTypeIds {} ", changeId, caseTypeIds);

        var caseIdList = Arrays.stream(caseTypeIds.split(",")).toList();
        var caseTypesWithChangeIds = caseIdList.stream().map(caseTypeId -> caseTypeId + "-" + changeId).toList();

        Session session = sessionFactory.openSession();

        executeSql(
            session,
            "DELETE FROM case_link WHERE case_type_id IN (:caseTypeReferences)",
            caseTypesWithChangeIds);
        executeSql(
            session,
            "DELETE FROM case_event WHERE case_type_id IN (:caseTypeReferences)",
            caseTypesWithChangeIds);
        executeSql(
            session,
            "DELETE FROM case_data WHERE case_type_id IN (:caseTypeReferences)",
            caseTypesWithChangeIds);

        session.close();
        log.info("Deleted records for changeId {} and caseTypeIds {} ", changeId, caseTypeIds);
    }

    private void executeSql(Session session, String sql, List<String> ids) {
        session.beginTransaction();
        session.createNativeQuery(sql)
            .setParameterList("caseTypeReferences", ids, StringType.INSTANCE)
            .executeUpdate();
        session.getTransaction().commit();
    }

}
