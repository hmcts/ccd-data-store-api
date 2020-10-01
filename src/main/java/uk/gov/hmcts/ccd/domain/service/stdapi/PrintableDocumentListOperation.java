package uk.gov.hmcts.ccd.domain.service.stdapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.Document;

import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
public class PrintableDocumentListOperation {
    private final ApplicationParams applicationParams;

    @Autowired
    public PrintableDocumentListOperation(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }

    public List<Document> getPrintableDocumentList(String jurisdictionId, String caseTypeId,
                                                   CaseDetails caseDetails) throws IllegalArgumentException {
        if (isBlank(jurisdictionId)) {
            throw new IllegalArgumentException("Invalid value for Jurisdiction ID");
        }
        if (isBlank(caseTypeId)) {
            throw new IllegalArgumentException("Invalid value for Case Type ID");
        }
        if (null == caseDetails || null == caseDetails.getReference()) {
            throw new IllegalArgumentException("Invalid value for Case Reference");
        }

        final Document document = new Document();
        document.setUrl(applicationParams.getDefaultPrintUrl()
            .replace(":jid", jurisdictionId)
            .replace(":ctid", caseTypeId)
            .replace(":cid", caseDetails.getReference().toString()));
        document.setName(applicationParams.getDefaultPrintName());
        document.setDescription(applicationParams.getDefaultPrintDescription());
        document.setType(applicationParams.getDefaultPrintType());
        return Collections.singletonList(document);
    }
}
