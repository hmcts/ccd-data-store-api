package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.vladmihalcea.hibernate.type.json.JsonNodeBinaryType;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import javax.persistence.Query;
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
            .addScalar("supplementary_data", JsonNodeBinaryType.INSTANCE);
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
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new ServiceException("Unable to map object to JSON string", e);
        }
    }
}
