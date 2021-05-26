package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import uk.gov.hmcts.ccd.domain.model.search.CaseDocumentsMetadata;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class CaseDocumentUtils {
    private static final String DOCUMENT_URL = "document_url";
    private static final String DOCUMENT_HASH = "hashToken";//"document_hash";

    public Map<String, String> extractDocumentsHashes(@NonNull final Map<String, JsonNode> data) {
        final List<JsonNode> documentNodes = findDocumentNodes(data);

        return documentNodes.stream()
            .filter(x -> x.hasNonNull(DOCUMENT_HASH))
            .collect(Collectors.toMap(
                node -> node.get(DOCUMENT_URL).textValue(),
                node -> node.get(DOCUMENT_HASH).textValue())
            );
    }

//    public static Map<String, JsonNode> removeDocumentsHashes(@NonNull final Map<String, JsonNode> data) {
//        final Map<String, JsonNode> dataCopy = copy(data);
//
//        removeHashes(dataCopy);
//
//        return dataCopy;
//    }

    public CaseDocumentsMetadata createDocumentMetadata(@NonNull final String reference,
                                                        @NonNull final String caseTypeId,
                                                        @NonNull final String jurisdiction,
                                                        @NonNull final List<DocumentHashToken> documentHashes) {
        return CaseDocumentsMetadata.builder()
            .caseId(reference)
            .caseTypeId(caseTypeId)
            .jurisdictionId(jurisdiction)
            .documentHashToken(documentHashes)
            .build();
    }

    public List<JsonNode> findDocumentNodes(@NonNull final Map<String, JsonNode> data) {
        return data.values().stream()
            .map(x -> x.findParents(DOCUMENT_URL))
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public Set<String> getTamperedHashes(final Map<String, String> preCallbackHashes,
                                         final Map<String, String> postCallbackHashes) {
        return CollectionUtils.setsIntersection(
            preCallbackHashes.keySet(),
            postCallbackHashes.keySet()
        );
    }

    private List<DocumentHashToken> buildDocumentHashToken(final Map<String, String> preCallbackHashes,
                                                           final Map<String, String> postCallbackHashes) {
        final Map<String, String> combinedHashes = CollectionUtils.mapsUnion(preCallbackHashes, postCallbackHashes);

        return combinedHashes.entrySet().stream()
            .map(x -> DocumentHashToken.builder().id(x.getKey()).hashToken(x.getValue()).build())
            .collect(Collectors.toUnmodifiableList());
    }

}
