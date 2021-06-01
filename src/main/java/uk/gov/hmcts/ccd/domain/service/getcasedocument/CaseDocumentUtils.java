package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Named
public class CaseDocumentUtils {
    public static final String DOCUMENT_URL = "document_url";
    public static final String DOCUMENT_HASH = "hashToken";// TODO: replace hashToken to "document_hash";

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

    public Set<String> getTamperedHashes(@NonNull final Map<String, String> preCallbackHashes,
                                         @NonNull final Map<String, String> postCallbackHashes) {
        return CollectionUtils.setsIntersection(
            preCallbackHashes.keySet(),
            postCallbackHashes.keySet()
        );
    }

    public List<DocumentHashToken> buildDocumentHashToken(@NonNull final Map<String, String> preCallbackHashes,
                                                          @NonNull final Map<String, String> postCallbackHashes) {
        final Map<String, String> combinedHashes = CollectionUtils.mapsUnion(preCallbackHashes, postCallbackHashes);

        return combinedHashes.entrySet().stream()
            .map(x -> DocumentHashToken.builder()
                .id(x.getKey())
                .hashToken(x.getValue())
                .build())
            .collect(Collectors.toUnmodifiableList());
    }

    public List<JsonNode> getViolatingDocuments(@NonNull final List<JsonNode> preCallbackDocumentNodes,
                                                @NonNull final List<JsonNode> postCallbackDocumentNodes) {

        final List<String> preCallbackDocumentKeys = getPreCallbackDocumentKeys(preCallbackDocumentNodes);

        final List<JsonNode> filterPostCallbackDocumentNodes = filterPostCallbackDocuments(
            preCallbackDocumentKeys,
            postCallbackDocumentNodes
        );

        final List<JsonNode> combinedDocumentNodes = CollectionUtils.listsUnion(
            preCallbackDocumentNodes,
            filterPostCallbackDocumentNodes
        );

        return combinedDocumentNodes.stream()
            .filter(x -> x.get(DOCUMENT_HASH) == null)
            .collect(Collectors.toUnmodifiableList());
    }

    private List<String> getPreCallbackDocumentKeys(final List<JsonNode> documentNodes) {
        return documentNodes.stream()
            .map(x -> x.get(DOCUMENT_URL).textValue())
            .collect(Collectors.toUnmodifiableList());
    }

    List<JsonNode> filterPostCallbackDocuments(final List<String> preCallbackDocumentKeys,
                                               final List<JsonNode> documentNodes) {
        return documentNodes.stream()
            .filter(x -> !preCallbackDocumentKeys.contains(x.get(DOCUMENT_URL).textValue()))
            .collect(Collectors.toUnmodifiableList());
    }

}
