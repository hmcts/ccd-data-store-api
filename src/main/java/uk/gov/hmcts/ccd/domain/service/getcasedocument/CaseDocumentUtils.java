package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import org.jooq.lambda.tuple.Tuple2;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Named
public class CaseDocumentUtils {
    private static final Function<List<Tuple2<String, String>>,
        Function<List<Tuple2<String, String>>, List<Tuple2<String, String>>>> FILTER = x -> y -> {
            List<String> a = x.stream().map(item -> item.v1).collect(Collectors.toList());
            List<String> b = y.stream().map(item -> item.v1).collect(Collectors.toList());

            return a.removeAll(b)
                ? x.stream().filter(item -> a.contains(item.v1)).collect(Collectors.toUnmodifiableList())
                : x;
        };

    public static final String DOCUMENT_URL = "document_url";
    public static final String DOCUMENT_BINARY_URL = "document_url";
    public static final String DOCUMENT_HASH = "document_hash";
    public static final String UPLOAD_TIMESTAMP = "upload_timestamp";

    public static final String BINARY = "/binary";
    public static final String HEARING_RECORDINGS = "hearing-recordings";

    public List<Tuple2<String, String>> findDocumentsHashes(@NonNull final Map<String, JsonNode> data) {
        final List<JsonNode> documentNodes = findDocumentNodes(data);

        return documentNodes.stream()
            .map(node -> new Tuple2<>(
                getDocumentId(node),
                Optional.ofNullable(node.get(DOCUMENT_HASH)).map(JsonNode::textValue).orElse(null))
            )
            .collect(Collectors.toUnmodifiableList());
    }

    private String getDocumentId(final JsonNode jsonNode) {
        final String documentIdField = Optional.ofNullable(jsonNode.get(DOCUMENT_BINARY_URL))
            .map(JsonNode::asText)
            .orElse(jsonNode.get(DOCUMENT_URL).asText());

        final String documentId = documentIdField.contains(BINARY)
            ? documentIdField.substring(documentIdField.length() - 43, documentIdField.length() - 7)
            : documentIdField.substring(documentIdField.length() - 36);

        validateDocumentId(documentId);

        return documentId;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void validateDocumentId(final String documentId) {
        try {
            UUID.fromString(documentId);
        } catch (RuntimeException e) {
            throw new BadRequestException(String.format("The input document id %s is invalid uuid", documentId));
        }
    }

    public List<JsonNode> findDocumentNodes(@NonNull final Map<String, JsonNode> data) {
        return data.values().stream()
            .map(node -> node.findParents(DOCUMENT_URL))
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .filter(docNode -> nonHearingRecordingUrl(docNode))
            .collect(Collectors.toList());
    }

    private boolean nonHearingRecordingUrl(JsonNode documentNode) {
        return !documentNode.get(DOCUMENT_URL).asText().contains(HEARING_RECORDINGS);
    }

    public Set<String> getTamperedHashes(@NonNull final List<Tuple2<String, String>> preCallbackHashes,
                                         @NonNull final List<Tuple2<String, String>> postCallbackHashes) {
        final Set<String> preCallbackIds = preCallbackHashes.stream()
            .map(pair -> pair.v1)
            .collect(Collectors.toUnmodifiableSet());

        return postCallbackHashes.stream()
            .distinct()
            .filter(distinctPair -> preCallbackIds.contains(distinctPair.v1))
            .filter(preCallbackPair -> Objects.nonNull(preCallbackPair.v2))
            .map(tamperedPair -> tamperedPair.v1)
            .collect(Collectors.toUnmodifiableSet());
    }

    public List<DocumentHashToken> buildDocumentHashToken(
        @NonNull final List<Tuple2<String, String>> databaseDocs,
        @NonNull final List<Tuple2<String, String>> eventDocs,
        @NonNull final List<Tuple2<String, String>> postCallbackDocs
    ) {
        final List<Tuple2<String, String>> eventNewDocs = FILTER.apply(eventDocs).apply(databaseDocs);

        final List<Tuple2<String, String>> preCallbackDocs = CollectionUtils.listsUnion(
            databaseDocs,
            eventNewDocs
        );

        final List<Tuple2<String, String>> postCallbackNewDocs = FILTER.apply(postCallbackDocs).apply(preCallbackDocs);

        final List<Tuple2<String, String>> combined = CollectionUtils.listsUnion(eventNewDocs, postCallbackNewDocs);

        return combined.stream()
            .map(pair -> DocumentHashToken.builder()
                .id(pair.v1)
                .hashToken(pair.v2)
                .build())
            .collect(Collectors.toUnmodifiableList());
    }

    public List<DocumentHashToken> getViolatingDocuments(@NonNull final List<DocumentHashToken> documentHashes) {
        return documentHashes.stream()
            .filter(pair -> pair.getHashToken() == null)
            .collect(Collectors.toUnmodifiableList());
    }

}
