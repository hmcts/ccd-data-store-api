package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import java.util.Arrays;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("increment")
public class IncrementSupplementaryDataQueryBuilder implements SupplementaryDataQueryBuilder {
    @SuppressWarnings("checkstyle:LineLength") //don't want to break long SQL statement
    private static final String INC_UPDATE_QUERY = "UPDATE case_data SET "
            + "supplementary_data= (CASE"
            + "        WHEN COALESCE(supplementary_data, '{}') = '{}' "
            + "        THEN COALESCE(supplementary_data, '{}') || :json_value\\:\\:jsonb"
            + "        WHEN jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), :parent_path) IS NULL AND :value > 0"
            + "        THEN jsonb_insert(COALESCE(supplementary_data, '{}'), :parent_key, :json_value_insert\\:\\:jsonb)"
            + "        WHEN jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), :node_path) IS NULL "
            + "        THEN jsonb_set(COALESCE(supplementary_data, '{}'), :leaf_node_key, :value\\:\\:TEXT\\:\\:jsonb)"
            + "        WHEN jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), :node_path) IS NOT NULL "
            + "        THEN jsonb_set(COALESCE(supplementary_data, '{}'), :leaf_node_key,"
            + "             (jsonb_extract_path_text(supplementary_data, :node_path)\\:\\:INT + :value) \\:\\:TEXT\\:\\:jsonb, false)"
            + "    END) "
            + "WHERE reference = :reference";

    @Override
    public Query build(EntityManager entityManager,
                       String caseReference,
                       String fieldPath,
                       Object fieldValue) {
        Query query = entityManager.createNativeQuery(INC_UPDATE_QUERY);
        String parentKey = fieldPath.split(Pattern.quote("."))[0];
        setCommonProperties(query, caseReference, fieldPath, fieldValue);
        String jsonValue = requestedDataToJson(fieldPath, fieldValue);
        query.setParameter("json_value", jsonValue);
        String parentKeyJsonValue = requestedDataJsonForPath(fieldPath, fieldValue, parentKey);
        query.setParameter("json_value_insert", parentKeyJsonValue);
        query.setParameter("node_path", Arrays.asList(fieldPath.split(Pattern.quote("."))));
        query.setParameter("parent_key", "{" + parentKey + "}");
        query.setParameter("parent_path", Arrays.asList(parentKey));


        return query;
    }

    @Override
    public SupplementaryDataOperation operationType() {
        return SupplementaryDataOperation.INC;
    }
}
