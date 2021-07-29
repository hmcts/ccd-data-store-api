package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;

import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.JURISDICTION_FIELD_COL;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.REFERENCE_FIELD_COL;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.SECURITY_CLASSIFICATION_FIELD_COL;

public interface GrantTypeESQueryBuilder {

    BoolQueryBuilder createQuery(List<RoleAssignment> roleAssignments);

    default BoolQueryBuilder createClassification(Stream<RoleAssignment> roleAssignmentStream) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        Set<String> classifications = roleAssignmentStream
            .map(roleAssignment -> roleAssignment.getClassification())
            .filter(classification -> StringUtils.isNotBlank(classification))
            .collect(Collectors.toSet());

        if (classifications.size() > 0) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(SECURITY_CLASSIFICATION_FIELD_COL, classifications));
        }
        return boolQueryBuilder;
    }

    default void addJurisdictions(Supplier<Stream<RoleAssignment>> streamSupplier,
                                  BoolQueryBuilder boolQueryBuilder) {
        Set<String> jurisdictions = streamSupplier.get()
            .map(roleAssignment -> roleAssignment.getAttributes().getJurisdiction().orElse(""))
            .filter(jurisdiction -> jurisdiction.length() > 0)
            .collect(Collectors.toSet());

        if (jurisdictions.size() > 0) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(JURISDICTION_FIELD_COL, jurisdictions));
        }
    }

    default void addCaseReferences(Supplier<Stream<RoleAssignment>> streamSupplier,
                                   BoolQueryBuilder boolQueryBuilder) {
        Set<String> caseReferences = streamSupplier.get()
            .map(roleAssignment -> roleAssignment.getAttributes().getCaseId().orElse(""))
            .filter(caseId -> caseId.length() > 0)
            .collect(Collectors.toSet());

        if (caseReferences.size() > 0) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(REFERENCE_FIELD_COL, caseReferences));
        }
    }
}
