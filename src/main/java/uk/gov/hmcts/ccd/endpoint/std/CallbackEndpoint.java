package uk.gov.hmcts.ccd.endpoint.std;

import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.domain.service.stdapi.PrintableDocumentListOperation;

import javax.transaction.Transactional;
import java.util.List;

@RestController
@RequestMapping(path = "/callback/",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@Api(description = "Default callbacks")
public class CallbackEndpoint {
    private final PrintableDocumentListOperation printableDocumentListOperation;

    @Autowired
    public CallbackEndpoint(final PrintableDocumentListOperation printableDocumentListOperation) {
        this.printableDocumentListOperation = printableDocumentListOperation;
    }

    @Transactional
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
}
