package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.ElasticSearchCaseDetailsDTO;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface CaseDetailsMapper {

    String EMPTY_MAP = "java(new java.util.HashMap<>())";
    Pattern DATE_TIME_FRACTION_PATTERN = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2})\\.(\\d{3})\\d+(Z|[+-]\\d{2}:\\d{2})?$"
    );

    @Mapping(source = "data", target = "data", defaultExpression = EMPTY_MAP)
    @Mapping(source = "dataClassification", target = "dataClassification", defaultExpression = EMPTY_MAP)
    CaseDetails dtoToCaseDetails(ElasticSearchCaseDetailsDTO caseDetailsDTO);

    List<CaseDetails> dtosToCaseDetailsList(List<ElasticSearchCaseDetailsDTO> caseDetailsDTOs);

    @AfterMapping
    default void truncateDateTimeInCaseData(@MappingTarget CaseDetails target) {
        if (target.getCreatedDate() != null) {
            target.setCreatedDate(target.getCreatedDate().truncatedTo(ChronoUnit.MILLIS));
        }
        if (target.getLastModified() != null) {
            target.setLastModified(target.getLastModified().truncatedTo(ChronoUnit.MILLIS));
        }
        if (target.getLastStateModifiedDate() != null) {
            target.setLastStateModifiedDate(target.getLastStateModifiedDate().truncatedTo(ChronoUnit.MILLIS));
        }

        Map<String, JsonNode> data = target.getData();
        if (data == null || data.isEmpty()) {
            return;
        }
        data.replaceAll((key, value) -> truncateDateTimes(value));
    }

    default JsonNode truncateDateTimes(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isTextual()) {
            return new TextNode(truncateFractionIfPresent(node.asText()));
        }
        if (node.isArray()) {
            ArrayNode result = JsonNodeFactory.instance.arrayNode();
            for (JsonNode item : node) {
                result.add(truncateDateTimes(item));
            }
            return result;
        }
        if (node.isObject()) {
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                result.set(entry.getKey(), truncateDateTimes(entry.getValue()));
            }
            return result;
        }
        return node;
    }

    default String truncateFractionIfPresent(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = DATE_TIME_FRACTION_PATTERN.matcher(value);
        if (matcher.matches()) {
            String suffix = matcher.group(3) == null ? "" : matcher.group(3);
            return matcher.group(1) + "." + matcher.group(2) + suffix;
        }
        return value;
    }
}
