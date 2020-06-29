package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("increment")
public class IncrementSupplementaryDataQueryBuilder implements SupplementaryDataQueryBuilder {

    private static final String INC_UPDATE_QUERY = "UPDATE case_data SET "
        + "supplementary_data= (CASE"
        + "        WHEN COALESCE(supplementary_data, '{}') = '{}' "
        + "        THEN COALESCE(supplementary_data, '{}') || :json_value\\:\\:jsonb"
        + "        WHEN jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), :node_path) IS NULL "
        + "        THEN jsonb_set(COALESCE(supplementary_data, '{}'), :leaf_node_key, :value\\:\\:TEXT\\:\\:jsonb)"
        + "        WHEN jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), :node_path) IS NOT NULL"
        + "        THEN jsonb_set(COALESCE(supplementary_data, '{}'), :leaf_node_key,"
        + "             GREATEST((jsonb_extract_path_text(supplementary_data, :node_path)\\:\\:INT + :value), 0) \\:\\:TEXT\\:\\:jsonb, false)"
        + "    END), "
        + "supplementary_data_last_modified = :current_time "
        + "WHERE reference = :reference";

    @Override
    public List<Query> buildQueries(EntityManager entityManager, String caseReference, Map<String, Object> requestData) {
        Map<String, Object> leafNodes = dataProcessor.accessLeafNodes(requestData);
        return leafNodes.entrySet().stream().map(entry -> {
            Query query = entityManager.createNativeQuery(INC_UPDATE_QUERY);
            setCommonProperties(query, requestData, caseReference, entry.getKey(), entry.getValue());
            query.setParameter("node_path", Arrays.asList(entry.getKey().split(",")));
            return query;
        }).collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    public Operation operationType() {
        return Operation.INC;
    }
}
