package uk.gov.hmcts.ccd.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.ccd.document.am.model.Classification;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;

import java.util.Date;
import java.util.Map;

/**
 * Jackson 3 construction metadata for the AM client 1.60.0 document model.
 *
 * <p>The published model has no no-args constructor and its Jackson 2
 * parameter-names module cannot participate in Jackson 3 deserialization.
 * Keep this mixin until the Jackson 3-ready AM client release is consumed.
 */
abstract class AmDocumentJacksonMixin {

    @JsonCreator
    AmDocumentJacksonMixin(
        @JsonProperty("classification") Classification classification,
        @JsonProperty("size") long size,
        @JsonProperty("mimeType") String mimeType,
        @JsonProperty("originalDocumentName") String originalDocumentName,
        @JsonProperty("createdOn") Date createdOn,
        @JsonProperty("modifiedOn") Date modifiedOn,
        @JsonProperty("createdBy") String createdBy,
        @JsonProperty("lastModifiedBy") String lastModifiedBy,
        @JsonProperty("ttl") Date ttl,
        @JsonProperty("hashToken") String hashToken,
        @JsonProperty("metadata") Map<String, String> metadata,
        @JsonProperty("links") Document.Links links
    ) {
    }
}
