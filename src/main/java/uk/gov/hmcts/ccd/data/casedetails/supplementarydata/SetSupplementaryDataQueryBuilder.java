package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import java.util.Arrays;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("set")
public class SetSupplementaryDataQueryBuilder implements SupplementaryDataQueryBuilder {

    private static final String SET_UPDATE_QUERY = "UPDATE case_data SET "
        + "supplementary_data= (CASE"
        + "        WHEN COALESCE(supplementary_data, '{}') = '{}' "
        + "        THEN COALESCE(supplementary_data, '{}') || :json_value\\:\\:jsonb"
        + "        WHEN jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), :parent_path) IS NOT NULL "
        + "        THEN jsonb_set(COALESCE(supplementary_data, '{}'), :leaf_node_key, :value\\:\\:TEXT\\:\\:jsonb)"
        + "        WHEN jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), :parent_path) IS NULL"
        + "        THEN jsonb_insert(COALESCE(supplementary_data, '{}'), :parent_key, :json_value_insert\\:\\:jsonb)"
        + "    END), "
        + "supplementary_data_last_modified = :current_time "
        + "WHERE reference = :reference";

    @Override
    public Query build(EntityManager entityManager,
                       String caseReference,
                       String fieldPath,
                       Object fieldValue) {
        Query query = entityManager.createNativeQuery(SET_UPDATE_QUERY);
        setCommonProperties(query, caseReference, fieldPath, fieldValue);
        String parentKey = fieldPath.split(Pattern.quote("."))[0];
        String jsonValue = requestedDataToJson(fieldPath, fieldValue);
        query.setParameter("json_value", jsonValue);
        String parentKeyJsonValue = requestedDataJsonForPath(fieldPath, fieldValue, parentKey);
        query.setParameter("json_value_insert", parentKeyJsonValue);
        query.setParameter("parent_path", Arrays.asList(parentKey));
        query.setParameter("parent_key", "{" + parentKey + "}");
        return query;
    }

    @Override
    public SupplementaryDataOperation operationType() {
        return SupplementaryDataOperation.SET;
    }
}
