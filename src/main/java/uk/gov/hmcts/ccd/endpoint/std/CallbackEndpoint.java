package uk.gov.hmcts.ccd.endpoint.std;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.domain.service.stdapi.PrintableDocumentListOperation;

import java.util.List;

@RestController
@RequestMapping(path = "/callback/",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Default callbacks")
public class CallbackEndpoint {
    private final PrintableDocumentListOperation printableDocumentListOperation;

    @Autowired
    public CallbackEndpoint(final PrintableDocumentListOperation printableDocumentListOperation) {
        this.printableDocumentListOperation = printableDocumentListOperation;
    }

    @RequestMapping(value = "/jurisdictions/{jid}/case-types/{ctid}/documents", method = RequestMethod.POST)
    @Operation(summary = "Get printable documents", description = "Retrieve a list of printable documents for a case")
    @ApiResponse(responseCode = "200", description = "Printable documents list retrieved")
    public List<Document> getPrintableDocuments(
        @Parameter(name = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @Parameter(name = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @RequestBody final CaseDetails caseDetails) {
        return printableDocumentListOperation.getPrintableDocumentList(jurisdictionId, caseTypeId, caseDetails);
    }
}
