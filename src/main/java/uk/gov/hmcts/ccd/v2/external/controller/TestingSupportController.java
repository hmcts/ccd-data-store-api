package uk.gov.hmcts.ccd.v2.external.controller;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLinksResource;
import uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkService;
import uk.gov.hmcts.ccd.v2.V2;

import jakarta.inject.Inject;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping(path = "/testing-support")
@ConditionalOnProperty(value = "testing.support.endpoint.enabled", havingValue = "true")
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
    @Operation(summary = "Delete a list of Case Type Schemas", description = "Blank body response.\n")
    @ApiResponse(responseCode = "204", description = "Success")
    @ApiResponse(responseCode = "404", description = "Unable to find case type")
    @ApiResponse(responseCode = "500", description = "Unexpected error")
    public void dataCaseTypeIdDelete(
        @Parameter(name = "Change ID", required = true) @PathVariable("changeId") BigInteger changeId,
        @Parameter(name = "Case Type ID", required = true) @RequestParam("caseTypeIds") String caseTypeIds) {

        var caseIdList = Arrays.stream(caseTypeIds.split(",")).toList();
        var caseTypesWithChangeIds = caseIdList.stream().map(caseTypeId -> caseTypeId + "-" + changeId).toList();

        Session session = sessionFactory.openSession();

        executeSql(
            session,
            "DELETE FROM case_event WHERE case_type_id IN (:caseTypeReferences)",
            caseTypesWithChangeIds);
        executeSql(
            session,
            "DELETE FROM case_data WHERE case_type_id IN (:caseTypeReferences)",
            caseTypesWithChangeIds);

        session.close();
    }

    private void executeSql(Session session, String sql, List<String> ids) {
        session.beginTransaction();
        session.createNativeQuery(sql)
            .setParameterList("caseTypeReferences", ids, StandardBasicTypes.STRING)
            .executeUpdate();
        session.getTransaction().commit();
    }

}
