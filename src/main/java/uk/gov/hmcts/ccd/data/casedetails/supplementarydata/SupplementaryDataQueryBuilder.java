package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmihalcea.hibernate.type.json.JsonNodeBinaryType;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.hibernate.query.NativeQuery;

public interface SupplementaryDataQueryBuilder {

    ObjectMapper mapper = new ObjectMapper();

    SupplementaryDataProcessor dataProcessor = new SupplementaryDataProcessor();

    List<Query> buildQueries(EntityManager entityManager, String caseReference, Map<String, Object> requestData);

    Operation operationType();

    default String requestJson(Object supplementaryData) {
        try {
            return mapper.writeValueAsString(supplementaryData);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    default void setCommonProperties(Query query,
                                     Map<String, Object> requestData,
                                     String caseReference,
                                     String leafNodeKey,
                                     Object leafNodeValue) {
        query.setParameter("leaf_node_key", "{" + leafNodeKey + "}");
        query.setParameter("value", leafNodeValue);
        query.setParameter("json_value", requestJson(requestData));
        query.setParameter("current_time",  LocalDateTime.now(ZoneOffset.UTC));
        query.setParameter("reference", caseReference);
        query.unwrap(NativeQuery.class)
            .addScalar("supplementary_data", JsonNodeBinaryType.INSTANCE);
    }
}
