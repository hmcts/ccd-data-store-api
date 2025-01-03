package uk.gov.hmcts.ccd.domain.service.casefileview;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.vavr.Tuple2;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Named;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static uk.gov.hmcts.ccd.config.JacksonUtils.MAPPER;

@Named
public class FileViewDocumentService {
    private static final String ID = "id";

    static final Function<String, String> DOCUMENT_VALUE_NODE_FUNCTION = input -> {
        final String result = Arrays.stream(input.split("\\."))
            .map(fragment -> isNumeric(fragment) ? String.format("[%s].value", fragment) : fragment)
            .collect(Collectors.joining(".")).replaceAll("\\.\\[", "[");
        return String.format("$.%s", result);
    };

    static final Function<String, List<Tuple2<Integer, Integer>>> DOCUMENT_ID_NODE_FUNCTION = input -> {
        final List<Integer> arrayIndices = Pattern.compile(Pattern.quote("["))
            .matcher(input)
            .results()
            .map(MatchResult::start)
            .collect(Collectors.toList());

        final List<Integer> valueIndices = Pattern.compile(Pattern.quote("value"))
            .matcher(input)
            .results()
            .map(MatchResult::start)
            .collect(Collectors.toList());

        if (arrayIndices.size() != valueIndices.size()) {
            throw new RuntimeException(String.format("Invalid document path: %s", input));
        }

        return IntStream.range(0, valueIndices.size())
            .mapToObj(i -> new Tuple2<>(arrayIndices.get(i), valueIndices.get(i)))
            .collect(Collectors.toUnmodifiableList());
    };

    public Tuple2<String, Map<String, String>> getDocumentNode(@NonNull final String dotNotationPath,
                                                               @NonNull final Map<String, JsonNode> caseData) {
        final String documentPath = isBlank(dotNotationPath)
            ? dotNotationPath
            : DOCUMENT_VALUE_NODE_FUNCTION.apply(dotNotationPath);

        final DocumentContext documentContext = parseCaseData(caseData);

        final Map<String, String> documentNode = documentContext.read(documentPath);

        final StringBuilder sb = new StringBuilder();

        final List<Tuple2<Integer, Integer>> indices = DOCUMENT_ID_NODE_FUNCTION.apply(documentPath);

        int idx = 0;
        for (Tuple2<Integer, Integer> index : indices) {
            final String substring = documentPath.substring(idx, index._1);
            sb.append(substring);
            idx = index._2;
            final String idPath = documentPath.substring(0, index._2) + ID;
            final String nodeId = documentContext.read(idPath);
            sb.append(String.format("[%s].", nodeId));
        }

        sb.append(documentPath.contains("]")
            ? documentPath.substring(documentPath.lastIndexOf("]") + 2)
            : documentPath);
        final String result = sb.toString();

        return new Tuple2<>(result.substring(2).replaceAll(".value", ""), documentNode);
    }

    @SneakyThrows
    private DocumentContext parseCaseData(final Map<String, JsonNode> caseData) {
        final String jsonString = MAPPER.writeValueAsString(caseData);
        return JsonPath.parse(jsonString);
    }

}
