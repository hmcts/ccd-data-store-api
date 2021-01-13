package uk.gov.hmcts.ccd.domain.enablingcondition.jexl;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.enablingcondition.EnablingConditionConverter;
import uk.gov.hmcts.ccd.domain.enablingcondition.EnablingConditionParser;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

@Component
@Qualifier("jexl")
public class JexlEnablingConditionParser implements EnablingConditionParser {

    private static final Logger LOG = LoggerFactory.getLogger(JexlEnablingConditionParser.class);

    private final JexlEngine engine;

    private final ObjectMapper objectMapper;

    private EnablingConditionConverter enablingConditionConverter;

    @Inject
    public JexlEnablingConditionParser(EnablingConditionConverter enablingConditionConverter) {
        this(enablingConditionConverter, new ObjectMapper());
    }

    protected JexlEnablingConditionParser(EnablingConditionConverter enablingConditionConverter,
                                          ObjectMapper objectMapper) {
        this.enablingConditionConverter = enablingConditionConverter;
        this.engine = new JexlBuilder().create();
        this.objectMapper = objectMapper;
    }

    @Override
    public Boolean evaluate(String enablingCondition, Map<String, JsonNode> caseEventData) {
        try {
            String expression = this.enablingConditionConverter.convert(enablingCondition);
            if (expression != null) {
                JexlScript expressionScript = engine.createScript(expression);
                Map<String, Object> data = retrieveContextData(caseEventData, expressionScript.getVariables());
                return (Boolean) expressionScript.execute(new MapContext(data));
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
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
            .map(list -> String.join(".", list))
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
