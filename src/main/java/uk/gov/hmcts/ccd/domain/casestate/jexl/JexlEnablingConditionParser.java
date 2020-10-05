package uk.gov.hmcts.ccd.domain.casestate.jexl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.casestate.EnablingConditionFormatter;
import uk.gov.hmcts.ccd.domain.casestate.EnablingConditionParser;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

@Component
@Qualifier("jexl")
public class JexlEnablingConditionParser implements EnablingConditionParser {

    private final JexlEngine engine;

    private final ObjectMapper objectMapper;

    private EnablingConditionFormatter enablingConditionFormatter;

    @Inject
    public JexlEnablingConditionParser(EnablingConditionFormatter enablingConditionFormatter) {
        this.enablingConditionFormatter = enablingConditionFormatter;
        this.engine = new JexlBuilder().create();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Boolean evaluate(String enablingCondition, Map<String, JsonNode> caseEventData) {
        String expression = this.enablingConditionFormatter.format(enablingCondition);
        if (expression != null) {
            JexlScript expressionScript = engine.createScript(expression);
            Map<String, Object> data = retrieveContextData(caseEventData, expressionScript.getVariables());
            return (Boolean) expressionScript.execute(new MapContext(data));
        }
        return false;
    }

    private Map<String, Object> retrieveContextData(Map<String, JsonNode> caseEventData,
                                                    Set<List<String>> variableLists) {
        Set<String> variables = getVariableNames(variableLists);
        Map<String, Object> contextData = new HashMap<>();
        DocumentContext context = JsonPath.parse(caseDataToJsonString(caseEventData));
        for (String variable : variables) {
            Optional<Object> value = getValueFromContext(context, variable);
            if (value.isPresent()) {
                contextData.put(variable, value.get());
            }
        }
        return contextData;
    }

    private Set<String> getVariableNames(Set<List<String>> variableLists) {
        return variableLists
            .stream()
            .map(list -> list
                .stream()
                .collect(Collectors.joining(".")))
            .collect(Collectors.toSet());
    }

    private String caseDataToJsonString(Map<String, JsonNode> caseData) {
        try {
            return this.objectMapper.writeValueAsString(caseData);
        } catch (JsonProcessingException e) {
            throw new ServiceException("Unable to convert case data to JSON string", e);
        }
    }

    private Optional<Object> getValueFromContext(DocumentContext context, String variable) {
        try {
            return Optional.of(context.read(JsonPath.compile("$." + variable), Object.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
