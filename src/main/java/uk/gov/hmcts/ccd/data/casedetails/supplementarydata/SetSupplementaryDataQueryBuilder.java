package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import java.util.List;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("set")
public class SetSupplementaryDataQueryBuilder implements SupplementaryDataQueryBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(SetSupplementaryDataQueryBuilder.class);

    private static final String SET_UPDATE_QUERY = "UPDATE case_data SET "
        + "supplementary_data= (CASE"
        + "        WHEN COALESCE(supplementary_data, '{}') = '{}' "
        + "        THEN COALESCE(supplementary_data, '{}') || :json_value\\:\\:jsonb"
        + "        WHEN jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), :parent_path) IS NOT NULL "
        + "        THEN jsonb_set(COALESCE(supplementary_data, '{}'), :leaf_node_key, :value\\:\\:TEXT\\:\\:jsonb)"
        + "        WHEN jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), :parent_path) IS NULL"
        + "        THEN jsonb_insert(COALESCE(supplementary_data, '{}'), :parent_key, :json_value_insert\\:\\:jsonb)"
        + "    END) "
        + "WHERE reference = :reference";

    private static final String SET_UPDATE_QUERY_TEXT = "UPDATE case_data SET "
        + "supplementary_data= (CASE"
        + "        WHEN COALESCE(supplementary_data, '{}') = '{}' "
        + "        THEN COALESCE(supplementary_data, '{}') || :json_value\\:\\:jsonb"
        + "        WHEN jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), :parent_path) IS NOT NULL "
        + "        THEN jsonb_set(COALESCE(supplementary_data, '{}'), :leaf_node_key, to_jsonb(:value\\:\\:TEXT))"
        + "        WHEN jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), :parent_path) IS NULL"
        + "        THEN jsonb_insert(COALESCE(supplementary_data, '{}'), :parent_key, :json_value_insert\\:\\:jsonb)"
        + "    END) "
        + "WHERE reference = :reference";

    @Override
    public Query build(EntityManager entityManager,
                       String caseReference,
                       String fieldPath,
                       Object fieldValue) {
        Query query = entityManager.createNativeQuery(getSetUpdateQuery(fieldValue));
        setCommonProperties(query, caseReference, fieldPath, fieldValue);
        String parentKey = fieldPath.split(Pattern.quote("."))[0];
        String jsonValue = requestedDataToJson(fieldPath, fieldValue);
        query.setParameter("json_value", jsonValue);
        LOG.info("set json_value {}", jsonValue);
        String parentKeyJsonValue = requestedDataJsonForPath(fieldPath, fieldValue, parentKey);
        query.setParameter("json_value_insert", parentKeyJsonValue);
        query.setParameter("parent_path", List.of(parentKey));
        query.setParameter("parent_key", "{" + parentKey + "}");
        LOG.info("set leaf_node_key {}, json_value_insert {}, parent_path {}, parent_key {}, value {}, reference {}",
            fieldPath.replaceAll(Pattern.quote("."), ","),
            parentKeyJsonValue,
            List.of(parentKey),
            "{" + parentKey + "}",
            fieldValue.toString(),
            caseReference);
        return query;
    }

    private String getSetUpdateQuery(Object fieldValue) {
        if (fieldValue instanceof  String) {
            return SET_UPDATE_QUERY_TEXT;
        }
        return SET_UPDATE_QUERY;
    }

    @Override
    public SupplementaryDataOperation operationType() {
        return SupplementaryDataOperation.SET;
    }
}
