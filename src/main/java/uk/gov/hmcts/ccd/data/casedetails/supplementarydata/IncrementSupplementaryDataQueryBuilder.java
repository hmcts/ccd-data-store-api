package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.domain.service.common.DefaultObjectMapperService;

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

    private DefaultObjectMapperService defaultObjectMapperService;

    @Autowired
    public IncrementSupplementaryDataQueryBuilder(DefaultObjectMapperService defaultObjectMapperService) {
        this.defaultObjectMapperService = defaultObjectMapperService;
    }

    @Override
    public List<Query> buildQueryForEachSupplementaryDataProperty(EntityManager entityManager,
                                                                  String caseReference,
                                                                  SupplementaryDataUpdateRequest updateRequest) {
        Optional<Map<String, Object>> requestData = updateRequest.getOperationData(operationType());
        if (requestData.isPresent()) {
            Map<String, Object> leafNodes = updateRequest.getUpdateOperationProperties(operationType());
            return leafNodes.entrySet().stream().map(entry -> {
                Query query = entityManager.createNativeQuery(INC_UPDATE_QUERY);
                setCommonProperties(query, caseReference, entry.getKey(), entry.getValue());
                String jsonValue = updateRequest.requestedDataToJson(operationType());
                query.setParameter("json_value", jsonValue);
                query.setParameter("node_path", Arrays.asList(entry.getKey().split(",")));
                return query;
            }).collect(Collectors.toCollection(LinkedList::new));
        }
        return Lists.newArrayList();
    }

    @Override
    public SupplementaryDataOperation operationType() {
        return SupplementaryDataOperation.INC;
    }
}
