package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import java.util.Properties;
import java.util.regex.Pattern;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.hibernate.query.NativeQuery;
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverter;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

public interface SupplementaryDataQueryBuilder {

    Query build(EntityManager entityManager,
                String caseReference,
                String fieldPath,
                Object fieldValue);

    SupplementaryDataOperation operationType();

    default void setCommonProperties(Query query,
                                     String caseReference,
                                     String fieldPath,
                                     Object fieldValue) {
        String key = fieldPath.replaceAll(Pattern.quote("."), ",");
        query.setParameter("leaf_node_key", "{" + key + "}");
        query.setParameter("value", fieldValue);
        query.setParameter("reference", caseReference);
        query.unwrap(NativeQuery.class)
            .addScalar("supplementary_data", SupplementaryDataUserType.CUSTOM_TYPE);
    }

    default String requestedDataToJson(String fieldPath, Object fieldValue) {
        PropertiesToJsonConverter propertiesMapper = new PropertiesToJsonConverter();
        Properties properties = new Properties();
        properties.put(fieldPath, fieldValue);
        return propertiesMapper.convertToJson(properties);
    }

    default String requestedDataJsonForPath(String fieldPath, Object fieldValue, String pathToMatch) {
        String jsonString = requestedDataToJson(fieldPath, fieldValue);
        DocumentContext context = JsonPath.parse(jsonString);

        try {
            Object value = context.read("$." + pathToMatch, Object.class);
            return jsonNodeToString(value);
        } catch (PathNotFoundException e) {
            throw new ServiceException(String.format("Path %s is not found", pathToMatch));
        }
    }

    default String jsonNodeToString(Object data) {
        try {
            ObjectMapper objectMapper = JsonMapper.builderWithJackson2Defaults().build();
            return objectMapper.writeValueAsString(data);
        } catch (JacksonException e) {
            throw new ServiceException("Unable to map object to JSON string", e);
        }
    }
}
