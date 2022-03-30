package uk.gov.hmcts.ccd.domain.model.definition;

import lombok.Value;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNumeric;

@Value
public class CaseFieldMetadata {
    String path;
    String categoryId;

    private static final Function<String, String> ARRAY_EXPRESSION_FUNC = input -> Arrays.stream(input.split("\\."))
        .map(fragment -> isNumeric(fragment) ? String.format("[%s].value", fragment) : fragment)
        .collect(Collectors.joining("."));

    private static final Function<String, String> ARRAY_EXPRESSION_FUNC2 = input ->
        input.replaceAll("\\.\\[", "[");

    private static final Function<String, String> FINALISE_FUNC = input ->
        String.format("$.%s", input);

    public String getPathAsJsonPath() {
        return isBlank(path) ? path : ARRAY_EXPRESSION_FUNC
            .andThen(ARRAY_EXPRESSION_FUNC2)
            .andThen(FINALISE_FUNC)
            .apply(path);
    }

    public String getPathAsAttributePath() {
        return isBlank(path) ? path : ARRAY_EXPRESSION_FUNC.andThen(ARRAY_EXPRESSION_FUNC2).apply(path);
    }
}
