package uk.gov.hmcts.ccd.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;
import uk.gov.hmcts.ccd.domain.service.DocumentRestoreService;

import java.util.List;

@RestController
public class DocumentRestoreEndpoint {

    private final DocumentRestoreService documentRestoreService;

    @Autowired
    public DocumentRestoreEndpoint(DocumentRestoreService documentRestoreService) {
        this.documentRestoreService = documentRestoreService;
    }

    @RequestMapping(value = "/document/restore", method = RequestMethod.POST)
    public List<CaseDetailsEntity> getPrintableDocuments() {
        return documentRestoreService.findDocumentMissingCases();
    }
}

