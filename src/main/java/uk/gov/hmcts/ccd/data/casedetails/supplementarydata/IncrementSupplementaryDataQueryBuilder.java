package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import java.util.Arrays;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("increment")
public class IncrementSupplementaryDataQueryBuilder implements SupplementaryDataQueryBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(IncrementSupplementaryDataQueryBuilder.class);

    private static final String INC_UPDATE_QUERY = "UPDATE case_data SET "
        + "supplementary_data= (CASE"
        + "   WHEN COALESCE(supplementary_data, '{}') = '{}' "
        + "   THEN COALESCE(supplementary_data, '{}') || :json_value\\:\\:jsonb"
        + "   WHEN jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), :node_path) IS NULL "
        + "   THEN jsonb_set(COALESCE(supplementary_data, '{}'), :leaf_node_key, (:value)\\:\\:TEXT\\:\\:jsonb)"
        + "   WHEN jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), :node_path) IS NOT NULL"
        + "   THEN jsonb_set(COALESCE(supplementary_data, '{}'), :leaf_node_key,"
        + "   (jsonb_extract_path_text(supplementary_data, :node_path)\\:\\:INT + :value) \\:\\:TEXT\\:\\:jsonb, false)"
        + "   END) "
        + "WHERE reference = :reference";

    private static final String INC_UPDATE_QUERY_TEXT = "UPDATE case_data SET "
        + "supplementary_data= (CASE"
        + "   WHEN COALESCE(supplementary_data, '{}') = '{}' "
        + "   THEN COALESCE(supplementary_data, '{}') || :json_value\\:\\:jsonb"
        + "   WHEN jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), :node_path) IS NULL "
        + "   THEN jsonb_set(COALESCE(supplementary_data, '{}'), :leaf_node_key, to_jsonb(:value\\:\\:TEXT))"
        + "   WHEN jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), :node_path) IS NOT NULL"
        + "   THEN jsonb_set(COALESCE(supplementary_data, '{}'), :leaf_node_key,"
        + "   (jsonb_extract_path_text(supplementary_data, :node_path)\\:\\:INT + :value) \\:\\:TEXT\\:\\:jsonb, false)"
        + "   END) "
        + "WHERE reference = :reference";

    @Override
    public Query build(EntityManager entityManager,
                       String caseReference,
                       String fieldPath,
                       Object fieldValue) {
        Query query = entityManager.createNativeQuery(getIncUpdateQuery(fieldValue));
        setCommonProperties(query, caseReference, fieldPath, fieldValue);
        String jsonValue = requestedDataToJson(fieldPath, fieldValue);
        LOG.info("inc json_value {}", jsonValue);
        query.setParameter("json_value", jsonValue);
        query.setParameter("node_path", Arrays.asList(fieldPath.split(Pattern.quote("."))));
        LOG.info("inc leaf_node_key {}, node_path {}, value {}, reference {}",
            fieldPath.replaceAll(Pattern.quote("."), ","),
            Arrays.asList(fieldPath.split(Pattern.quote("."))),
            fieldValue.toString(),
            caseReference);
        return query;
    }

    private String getIncUpdateQuery(Object fieldValue) {
        if (fieldValue instanceof String) {
            return INC_UPDATE_QUERY_TEXT;
        }
        return INC_UPDATE_QUERY;
    }

    @Override
    public SupplementaryDataOperation operationType() {
        return SupplementaryDataOperation.INC;
    }
}
