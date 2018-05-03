package uk.gov.hmcts.ccd.domain.types.sanitiser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.types.sanitiser.client.DocumentManagementRestClient;
import uk.gov.hmcts.ccd.domain.types.sanitiser.document.Binary;
import uk.gov.hmcts.ccd.domain.types.sanitiser.document.Document;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class DocumentSanitiser implements Sanitiser {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentSanitiser.class);

    public static final String DOCUMENT_URL = "document_url";
    public static final String DOCUMENT_BINARY_URL = "document_binary_url";
    public static final String DOCUMENT_FILENAME = "document_filename";

    public static final String TYPE = "Document";
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    private final DocumentManagementRestClient documentManagementRestClient;

    @Inject
    public DocumentSanitiser(final DocumentManagementRestClient documentManagementRestClient) {
        this.documentManagementRestClient = documentManagementRestClient;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public JsonNode sanitise(FieldType fieldType, JsonNode fieldData) {
        final ObjectNode sanitisedData = JSON_NODE_FACTORY.objectNode();

        if ((fieldData.has(DOCUMENT_BINARY_URL)
            && fieldData.has(DOCUMENT_FILENAME))
            || fieldData.isNull()) {
            return fieldData;
        } else {
            final String documentUrl = fieldData.get(DOCUMENT_URL).textValue();

            sanitisedData.put(DOCUMENT_URL, documentUrl);
            Document document = documentManagementRestClient.getDocument(fieldType, documentUrl);

            Binary binary = document.get_links().getBinary();
            validateBinaryLink(fieldType, binary);
            sanitisedData.put(DOCUMENT_BINARY_URL, binary.getHref());
            validateDocumentFilename(fieldType, document);
            sanitisedData.put(DOCUMENT_FILENAME, document.getOriginalDocumentName());
            return sanitisedData;
        }
    }

    private void validateBinaryLink(FieldType fieldType, Binary binary) {
        if (binary == null || StringUtils.isBlank(binary.getHref())) {
            LOG.error(String.format(
                "Cannot sanitize document for the Case Field Type:%s, Case Field Type Id:%s because of document binary url missing",
                fieldType.getType(), fieldType.getId()));
            throw new ValidationException(String.format("Cannot sanitize document for the Case Field Type:%s, Case Field Type Id:%s because of document binary url missing",
                fieldType.getType(), fieldType.getId()));

        }
    }

    private void validateDocumentFilename(FieldType fieldType, Document document) {
        if (StringUtils.isBlank(document.getOriginalDocumentName())) {
            LOG.error(String.format(
                "Cannot sanitize document for the Case Field Type:%s, Case Field Type Id:%s because of document filename missing",
                fieldType.getType(), fieldType.getId()));
            throw new ValidationException(String.format("Cannot sanitize document for the Case Field Type:%s, Case Field Type Id:%s because of document filename missing",
                fieldType.getType(), fieldType.getId()));

        }
    }

}
