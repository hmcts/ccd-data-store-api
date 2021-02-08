package uk.gov.hmcts.ccd.data.casedetails;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CaseDetailsKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> dataSearchParams = objectMapper.convertValue(params[1], Map.class);

        return params[0].hashCode() + getMapHashCode(dataSearchParams);
    }

    private String getMapHashCode(Map<String, String> dataSearchParams) {
        return dataSearchParams.entrySet()
            .stream()
            .map(entry -> entry.getKey() + entry.getValue())
            .collect(Collectors.joining());
    }
}
