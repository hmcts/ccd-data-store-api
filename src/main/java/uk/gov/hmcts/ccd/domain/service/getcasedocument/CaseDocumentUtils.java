package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
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

    public List<DocumentHashToken> buildDocumentHashToken(final Map<String, String> preCallbackHashes,
                                                          final Map<String, String> postCallbackHashes) {
        final Map<String, String> combinedHashes = CollectionUtils.mapsUnion(preCallbackHashes, postCallbackHashes);

        return combinedHashes.entrySet().stream()
            .map(x -> DocumentHashToken.builder()
                .id(x.getKey())
                .hashToken(x.getValue())
                .build())
            .collect(Collectors.toUnmodifiableList());
    }

}
