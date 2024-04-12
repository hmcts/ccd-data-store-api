package uk.gov.hmcts.ccd.endpoint.std;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.domain.service.stdapi.PrintableDocumentListOperation;

import java.util.List;

@RestController
@RequestMapping(path = "/callback/",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@Api(description = "Default callbacks")
public class CallbackEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackEndpoint.class);
    private final PrintableDocumentListOperation printableDocumentListOperation;

    @Autowired
    public CallbackEndpoint(final PrintableDocumentListOperation printableDocumentListOperation) {
        this.printableDocumentListOperation = printableDocumentListOperation;
    }

    @RequestMapping(value = "/jurisdictions/{jid}/case-types/{ctid}/documents", method = RequestMethod.POST)
    @ApiOperation(value = "Get printable documents", notes = "Retrieve a list of printable documents for a case")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Printable documents list retrieved")
    })
    public List<Document> getPrintableDocuments(
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @RequestBody final CaseDetails caseDetails) {
        return printableDocumentListOperation.getPrintableDocumentList(jurisdictionId, caseTypeId, caseDetails);
    }

    @RequestMapping(value = "jcdebug", method = RequestMethod.POST)
    public ResponseEntity<String> jcdebug(@RequestParam("message") String message) {
        if (message != null) {
            mesaage = message.replaceAll("[\n\r]", "_");
            LOG.debug("JCDEBUG: debug: Message: " + message);
            LOG.error("JCDEBUG: error: Message: " + message);
            LOG.warn("JCDEBUG: warn: Message: " + message);
            LOG.info("JCDEBUG: info: Message: " + message);
        }
        return new ResponseEntity<>("Message: " + message==null ? "NULL" : message, HttpStatus.OK);
    }

    @RequestMapping(value = "jcdebugtest", method = RequestMethod.GET)
    public ResponseEntity<String> jcdebug() {
        return new ResponseEntity<>("jcdebugtest", HttpStatus.OK);
    }
}
