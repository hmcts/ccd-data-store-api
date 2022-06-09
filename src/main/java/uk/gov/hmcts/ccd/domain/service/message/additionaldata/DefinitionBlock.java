package uk.gov.hmcts.ccd.domain.service.message.additionaldata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

@Data
@AllArgsConstructor
@Builder
public class DefinitionBlock {

    private String originalId;
    private String type;
    private String subtype;
    private Map<String, DefinitionBlock> typeDef;

    public static final Map<String, DefinitionBlock> DOCUMENT_TYPE_DEFINITION;

    private static final String DOCUMENT_URL = "document_url";
    private static final String DOCUMENT_BINARY_URL = "document_binary_url";
    private static final String DOCUMENT_FILENAME = "document_filename";
    private static final String CATEGORY_ID = "category_id";
    private static final String UPLOAD_TIMESTAMP = "upload_timestamp";

    static {
        DOCUMENT_TYPE_DEFINITION = Collections.unmodifiableMap(documentTypeDefinitionTemplate());
    }

    private static Map<String, DefinitionBlock> documentTypeDefinitionTemplate() {
        DefinitionBlockBuilder textFieldBuilder = builder().type("SimpleText").subtype("Text");
        DefinitionBlockBuilder dateFieldBuilder = builder().type("SimpleDateTime").subtype("DateTime");

        Map<String, DefinitionBlock> documentTypeDef = newHashMap();
        documentTypeDef.put(DOCUMENT_URL, textFieldBuilder.originalId(DOCUMENT_URL).build());
        documentTypeDef.put(DOCUMENT_BINARY_URL, textFieldBuilder.originalId(DOCUMENT_BINARY_URL).build());
        documentTypeDef.put(DOCUMENT_FILENAME, textFieldBuilder.originalId(DOCUMENT_FILENAME).build());
        documentTypeDef.put(CATEGORY_ID, textFieldBuilder.originalId(CATEGORY_ID).build());
        documentTypeDef.put(UPLOAD_TIMESTAMP, dateFieldBuilder.originalId(UPLOAD_TIMESTAMP).build());

        return documentTypeDef;
    }
}
