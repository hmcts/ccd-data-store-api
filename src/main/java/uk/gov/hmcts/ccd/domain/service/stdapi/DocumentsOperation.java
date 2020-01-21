package uk.gov.hmcts.ccd.domain.service.stdapi;

import com.google.common.collect.ImmutableList;
import com.launchdarkly.client.LDClient;
import com.launchdarkly.client.LDUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

@Named
@Singleton
public class DocumentsOperation {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentsOperation.class);

    private final SecurityUtils securityUtils;
    private final CaseDetailsRepository caseDetailsRepository;
    private final CaseTypeService caseTypeService;
    private final UIDService uidService;

    private final LDClient ldClient;
    private final LDUser ldUser;

    @Inject
    public DocumentsOperation(final SecurityUtils securityUtils,
                              final CaseTypeService caseTypeService,
                              @Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                              final UIDService uidService,
                              final LDClient ldClient,
                              final LDUser ldUser) {
        this.securityUtils = securityUtils;
        this.caseDetailsRepository = caseDetailsRepository;
        this.caseTypeService = caseTypeService;
        this.uidService = uidService;
        this.ldClient = ldClient;
        this.ldUser = ldUser;
    }

    public List<Document> getPrintableDocumentsForCase(final String caseReference) {

        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Invalid Case Reference");
        }

        boolean isUsingCaseDocumentApi = ldClient.boolVariation(
            "download-documents-using-case-document-access-management-api", ldUser, false);

        List<Document> documents;
        if (!isUsingCaseDocumentApi) {
            ldClient.track("Downloading document directly from doc store", ldUser);
            documents = getDocumentsFromDocumentManagementStoreApi();
        } else {
            ldClient.track("Downloading document via Case Document Api", ldUser);
            documents = getDocumentsFromCaseDocumentAccessManagementApi();
        }

        return documents;
    }

    private List<Document> getDocumentsFromDocumentManagementStoreApi() {
        Document documentFromDocStoreApi = new Document();
        documentFromDocStoreApi.setName("Screenshot 2019-09-26 at 13.06.47.png");
        documentFromDocStoreApi.setDescription("Evidence screen capture");
        documentFromDocStoreApi.setType("png");
        documentFromDocStoreApi.setUrl("http://dm-store-aat.service.core-compute-aat.internal"
            + "/documents/1d9e2f5f-2114-4748-b01c-70481000ce6d/binary");
        return ImmutableList.of(documentFromDocStoreApi);
    }

    private List<Document> getDocumentsFromCaseDocumentAccessManagementApi() {
        Document documentFromCaseDocumentApi = new Document();
        documentFromCaseDocumentApi.setName("SAMPLE FROM NEW CASE DOCUMENT API");
        documentFromCaseDocumentApi.setDescription("SAMPLE FROM NEW CASE DOCUMENT API");
        documentFromCaseDocumentApi.setType("SAMPLE FROM NEW CASE DOCUMENT API");
        documentFromCaseDocumentApi.setUrl("SAMPLE FROM NEW CASE DOCUMENT API");
        return ImmutableList.of(documentFromCaseDocumentApi);
    }
}

