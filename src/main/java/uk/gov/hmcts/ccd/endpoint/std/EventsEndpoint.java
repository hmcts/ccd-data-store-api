package uk.gov.hmcts.ccd.endpoint.std;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.getevents.GetEventsOperation;

import java.util.List;

@RestController
@RequestMapping(path = "/",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Events API")
public class EventsEndpoint {
    private final GetEventsOperation getEventsOperation;

    @Autowired
    public EventsEndpoint(@Qualifier("authorised") final GetEventsOperation getEventsOperation) {
        this.getEventsOperation = getEventsOperation;
    }

    @RequestMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/events",
        method = RequestMethod.GET)
    @Operation(summary = "Get events for case", description = "Retrieve all events for a case")
    @ApiResponse(responseCode = "200", description = "Events found for the given ID")
    @ApiResponse(responseCode = "400", description = "Invalid case ID")
    @ApiResponse(responseCode = "404", description = "No case found for the given ID")
    public List<AuditEvent> findEventDetailsForCase(
        @Parameter(name = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @Parameter(name = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @Parameter(name = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @Parameter(name = "Case ID", required = true)
        @PathVariable("cid") final String caseId) {

        return getEventsOperation.getEvents(jurisdictionId, caseTypeId, caseId);
    }

}
