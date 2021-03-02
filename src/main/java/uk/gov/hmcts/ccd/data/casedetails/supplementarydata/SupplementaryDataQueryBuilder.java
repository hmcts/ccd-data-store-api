package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.vladmihalcea.hibernate.type.json.JsonNodeBinaryType;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.hibernate.query.NativeQuery;
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverter;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

public interface SupplementaryDataQueryBuilder {

    PropertiesToJsonConverter propertiesMapper = new PropertiesToJsonConverter();

    ObjectMapper objectMapper = new ObjectMapper();

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
        Properties properties = new Properties();
        properties.put(fieldPath, fieldValue);
        return propertiesMapper.convertToJson(properties);
    }

    default String requestedDataJsonForPath(String fieldPath, Object fieldValue, String pathToMatch) {
        String jsonString = requestedDataToJson(fieldPath, fieldValue);
        DocumentContext context = JsonPath.parse(jsonString);
        Object value = context.read("$." + pathToMatch, Object.class);
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ServiceException("Unable to map object to JSON string", e);
        }
    }
}
