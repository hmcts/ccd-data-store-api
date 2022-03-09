package uk.gov.hmcts.ccd.endpoint.std;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.data.documentdata.DocumentDataRequest;
import uk.gov.hmcts.ccd.domain.service.documentdata.DocumentDataService;

@Slf4j
@RestController
@Validated
public class DocumentDataEndpoint {

    private final DocumentDataService documentDataService;

    @Autowired
    public DocumentDataEndpoint(DocumentDataService documentDataService) {
        this.documentDataService = documentDataService;
    }

    @RequestMapping(
        value = "documentData/caseref/{caseref}",
        method = RequestMethod.PUT,
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiOperation(value = "Document Data Endpoint", notes = "Document Data Endpoint")
    public void updateDocumentField(@ApiParam(value = "Case Reference", required = true)
                                        @PathVariable("caseref") final String caseRef,
                                        @RequestBody final DocumentDataRequest request) {
        documentDataService.updateDocumentCategoryId(caseRef, request.getCaseVersion(), request.getAttributePath(),
            request.getCategoryId());
    }

}
