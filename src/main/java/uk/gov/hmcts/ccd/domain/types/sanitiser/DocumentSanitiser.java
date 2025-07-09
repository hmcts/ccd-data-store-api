package uk.gov.hmcts.ccd.domain.types.sanitiser;

import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.types.sanitiser.client.DocumentManagementRestClient;
import uk.gov.hmcts.ccd.domain.types.sanitiser.document.Binary;
import uk.gov.hmcts.ccd.domain.types.sanitiser.document.Document;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class DocumentSanitiser implements Sanitiser {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentSanitiser.class);

    public static final String DOCUMENT_URL = "document_url";
    public static final String DOCUMENT_BINARY_URL = "document_binary_url";
    public static final String DOCUMENT_FILENAME = "document_filename";
    public static final String DOCUMENT_HASH = "document_hash";
    public static final String CATEGORY_ID = "category_id";
    public static final String UPLOAD_TIMESTAMP = "upload_timestamp";

    public static final String TYPE = "Document";
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    private final DocumentManagementRestClient documentManagementRestClient;
    private final ApplicationParams applicationParams;

    @Inject
    public DocumentSanitiser(final DocumentManagementRestClient documentManagementRestClient,
                             ApplicationParams applicationParams) {
        this.documentManagementRestClient = documentManagementRestClient;
        this.applicationParams = applicationParams;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public JsonNode sanitise(FieldTypeDefinition fieldTypeDefinition, JsonNode fieldData) {
        final ObjectNode sanitisedData = JSON_NODE_FACTORY.objectNode();

        if ((fieldData.has(DOCUMENT_BINARY_URL)
            && fieldData.has(DOCUMENT_FILENAME))
            || fieldData.isNull()) {
            return fieldData;
        } else {
            final String documentUrl = fieldData.get(DOCUMENT_URL).textValue();

            sanitisedData.put(DOCUMENT_URL, documentUrl);
            Document document = retrieveDocument(fieldTypeDefinition, documentUrl);

            Binary binary = document.get_links().getBinary();
            validateBinaryLink(fieldTypeDefinition, binary);
            sanitisedData.put(DOCUMENT_BINARY_URL, binary.getHref());

            final JsonNode documentHashNode = fieldData.get(DOCUMENT_HASH);
            if (documentHashNode != null) {
                sanitisedData.put(DOCUMENT_HASH, documentHashNode.textValue());
            }
            final JsonNode documentCategoryId = fieldData.get(CATEGORY_ID);
            if (documentCategoryId != null) {
                sanitisedData.put(CATEGORY_ID, documentCategoryId.textValue());
            }
            final JsonNode documentUploadTimeStamp = fieldData.get(UPLOAD_TIMESTAMP);
            if (documentUploadTimeStamp != null) {
                sanitisedData.put(UPLOAD_TIMESTAMP, documentUploadTimeStamp.textValue());
            }

            validateDocumentFilename(fieldTypeDefinition, document);
            sanitisedData.put(DOCUMENT_FILENAME, document.getOriginalDocumentName());
            return sanitisedData;
        }
    }

    private Document retrieveDocument(FieldTypeDefinition fieldTypeDefinition, String documentUrl) {
        // TODO: Remove this feature flag once all services have migrated to CDAM.
        // At that point, dm-store will only allow CDAM to call its APIs directly,
        // and data-store should always go through case-doc-am-api.
        if (applicationParams.isDocumentSanitiserCaseDocAMEnable()) {
            return retrieveDocumentFromCaseDocAM(fieldTypeDefinition, documentUrl);
        }
        return documentManagementRestClient.getDocument(fieldTypeDefinition, documentUrl);
    }

    private Document retrieveDocumentFromCaseDocAM(FieldTypeDefinition fieldTypeDefinition, String documentUrl) {
        try {
            final URI uri = new URI(documentUrl);
            final String documentUrlPath = uri.getPath();
            final String caseDocumentAmEndpoint = applicationParams.getCaseDocumentAmUrl() + "/cases" + documentUrlPath;
            return documentManagementRestClient.getDocument(fieldTypeDefinition, caseDocumentAmEndpoint);
        } catch (URISyntaxException | NullPointerException e) {
            throw new ServiceException("Invalid document URL format: " + documentUrl, e);
        }
    }

    private void validateBinaryLink(FieldTypeDefinition fieldTypeDefinition, Binary binary) {
        if (binary == null || StringUtils.isBlank(binary.getHref())) {
            LOG.error(String.format(
                "Cannot sanitize document for the Case Field Type:%s, Case Field Type Id:%s because of document binary "
                    + "url missing",
                fieldTypeDefinition.getType(), fieldTypeDefinition.getId()));
            throw new ValidationException(String.format(
                "Cannot sanitize document for the Case Field Type:%s, Case Field Type Id:%s because of document binary "
                    + "url missing",
                fieldTypeDefinition.getType(), fieldTypeDefinition.getId()));

        }
    }

    private void validateDocumentFilename(FieldTypeDefinition fieldTypeDefinition, Document document) {
        if (StringUtils.isBlank(document.getOriginalDocumentName())) {
            LOG.error(String.format(
                "Cannot sanitize document for the Case Field Type:%s, Case Field Type Id:%s because of document "
                    + "filename missing",
                fieldTypeDefinition.getType(), fieldTypeDefinition.getId()));
            throw new ValidationException(String.format(
                "Cannot sanitize document for the Case Field Type:%s, Case Field Type Id:%s because of document "
                    + "filename missing",
                fieldTypeDefinition.getType(), fieldTypeDefinition.getId()));

        }
    }

}
