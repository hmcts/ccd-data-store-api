package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import org.jooq.lambda.tuple.Tuple2;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Named
public class CaseDocumentUtils {
    private static final Function<List<Tuple2<String, String>>,
        Function<List<Tuple2<String, String>>, List<Tuple2<String, String>>>> FILTER = x -> y -> {
            List<Tuple2<String, String>> a = new ArrayList<>(x);
            List<Tuple2<String, String>> b = new ArrayList<>(y);

            return a.removeAll(b) ? Collections.unmodifiableList(a) : x;
        };

    public static final String DOCUMENT_URL = "document_url";
    public static final String DOCUMENT_HASH = "hashToken";// TODO: replace hashToken to "document_hash";

    public List<Tuple2<String, String>> findDocumentsHashes(@NonNull final Map<String, JsonNode> data) {
        final List<JsonNode> documentNodes = findDocumentNodes(data);

        return documentNodes.stream()
            .map(x -> new Tuple2<>(
                x.get(DOCUMENT_URL).textValue(),
                Optional.ofNullable(x.get(DOCUMENT_HASH)).map(JsonNode::textValue).orElse(null))
            )
            .collect(Collectors.toUnmodifiableList());
    }

    public List<JsonNode> findDocumentNodes(@NonNull final Map<String, JsonNode> data) {
        return data.values().stream()
            .map(x -> x.findParents(DOCUMENT_URL))
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public Set<String> getTamperedHashes(@NonNull final List<Tuple2<String, String>> preCallbackHashes,
                                         @NonNull final List<Tuple2<String, String>> postCallbackHashes) {
        final Set<String> h1 = preCallbackHashes.stream().map(x -> x.v1).collect(Collectors.toUnmodifiableSet());
        final Set<String> h2 = postCallbackHashes.stream().map(x -> x.v1).collect(Collectors.toUnmodifiableSet());

        return CollectionUtils.setsIntersection(h1, h2);
    }

    public List<DocumentHashToken> buildDocumentHashToken(
        @NonNull final List<Tuple2<String, String>> preCallbackHashes,
        @NonNull final List<Tuple2<String, String>> postCallbackHashes
    ) {

        final List<Tuple2<String, String>> filtered = FILTER.apply(postCallbackHashes).apply(preCallbackHashes);

        final List<Tuple2<String, String>> combinedHashes = CollectionUtils.listsUnion(preCallbackHashes, filtered);

        return combinedHashes.stream()
            .map(x -> DocumentHashToken.builder()
                .id(x.v1)
                .hashToken(x.v2)
                .build())
            .collect(Collectors.toUnmodifiableList());
    }

    public List<DocumentHashToken> getViolatingDocuments(@NonNull final List<DocumentHashToken> documentHashes) {
        return documentHashes.stream()
            .filter(x -> x.getHashToken() == null)
            .collect(Collectors.toUnmodifiableList());
    }

}
