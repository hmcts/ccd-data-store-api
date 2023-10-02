package uk.gov.hmcts.ccd.v2.internal.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.type.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;


@RestController
@Api(value = "/internal/testing-support")
@RequestMapping(value = "/internal/testing-support")
public class TestingSupportController {

    private final SessionFactory sessionFactory;

    @Autowired
    public TestingSupportController(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private static final Logger LOG = LoggerFactory.getLogger(TestingSupportController.class);

    @DeleteMapping(value = "/cleanup-case-type/{changeId}")
    @ApiOperation(value = "Delete a list of Case Type Schemas", notes = "Blank body response.\n")
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "Success"),
        @ApiResponse(code = 404, message = "Unable to find case type"),
        @ApiResponse(code = 500, message = "Unexpected error")
    })
    @ConditionalOnExpression("${testing-support-endpoints.enabled:false}")
    public void dataCaseTypeIdDelete(
        @ApiParam(value = "Change ID", required = true) @PathVariable("changeId") BigInteger changeId,
        @ApiParam(value = "Case Type ID", required = true) @RequestParam("caseTypeIds") String caseTypeIds) {

        var caseIdList = Arrays.stream(caseTypeIds.split(",")).toList();
        var caseTypesWithChangeIds = caseIdList.stream().map(caseTypeId -> caseTypeId + "-" + changeId).toList();

        Session session = sessionFactory.openSession();
        session.beginTransaction();

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
            .setParameterList("caseTypeReferences", ids, StringType.INSTANCE)
            .executeUpdate();
        session.getTransaction().commit();
    }
}
