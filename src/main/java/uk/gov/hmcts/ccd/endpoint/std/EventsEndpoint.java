package uk.gov.hmcts.ccd.endpoint.std;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.getevents.GetEventsOperation;

import javax.transaction.Transactional;
import java.util.List;

@RestController
@RequestMapping(path = "/",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "/", description = "Events API")
public class EventsEndpoint {
    private final GetEventsOperation getEventsOperation;

    @Autowired
    public EventsEndpoint(@Qualifier("authorised") final GetEventsOperation getEventsOperation) {
        this.getEventsOperation = getEventsOperation;
    }

    @Transactional
    @RequestMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/events",
        method = RequestMethod.GET)
    @ApiOperation(value = "Get events for case", notes = "Retrieve all events for a case")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Events found for the given ID"),
        @ApiResponse(code = 400, message = "Invalid case ID"),
        @ApiResponse(code = 404, message = "No case found for the given ID")
    })
    public List<AuditEvent> findEventDetailsForCase(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Case ID", required = true)
        @PathVariable("cid") final String caseId) {

        return getEventsOperation.getEvents(jurisdictionId, caseTypeId, caseId);
    }

}
