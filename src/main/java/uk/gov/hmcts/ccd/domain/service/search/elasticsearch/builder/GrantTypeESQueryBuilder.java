package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;

import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.JURISDICTION_FIELD_KEYWORD_COL;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.REFERENCE_FIELD_COL;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.SECURITY_CLASSIFICATION_FIELD_COL;

public interface GrantTypeESQueryBuilder {

    BoolQueryBuilder createQuery(List<RoleAssignment> roleAssignments);

    default Optional<TermsQueryBuilder>  createClassification(Stream<RoleAssignment> roleAssignmentStream) {
        Set<String> classifications = roleAssignmentStream
            .map(roleAssignment -> roleAssignment.getClassification())
            .filter(classification -> StringUtils.isNotBlank(classification))
            .collect(Collectors.toSet());

        if (classifications.size() > 0) {
            return Optional.of(QueryBuilders.termsQuery(SECURITY_CLASSIFICATION_FIELD_COL, classifications));
        }
        return Optional.empty();
    }

    @SuppressWarnings("java:S2789")
    default Optional<TermsQueryBuilder> getJurisdictions(Supplier<Stream<RoleAssignment>> streamSupplier) {
        Set<String> jurisdictions = streamSupplier.get()
            .filter(roleAssignment -> roleAssignment.getAttributes() != null)
            .map(roleAssignment -> roleAssignment.getAttributes().getJurisdiction())
            .filter(jurisdictionOptional -> jurisdictionOptional != null)
            .map(jurisdictionOptional -> jurisdictionOptional.get())
            .filter(jurisdiction -> StringUtils.isNotBlank(jurisdiction))
            .collect(Collectors.toSet());

        if (jurisdictions.size() > 0) {
            return Optional.of(QueryBuilders.termsQuery(JURISDICTION_FIELD_KEYWORD_COL, jurisdictions));
        }
        return Optional.empty();
    }

    @SuppressWarnings("java:S2789")
    default Optional<TermsQueryBuilder> getCaseReferences(Supplier<Stream<RoleAssignment>> streamSupplier) {
        Set<String> caseReferences = streamSupplier.get()
            .filter(roleAssignment -> roleAssignment.getAttributes() != null)
            .map(roleAssignment -> roleAssignment.getAttributes().getCaseId())
            .filter(caseReferenceOptional -> caseReferenceOptional != null)
            .map(caseReferenceOptional -> caseReferenceOptional.get())
            .filter(caseReference -> StringUtils.isNotBlank(caseReference))
            .collect(Collectors.toSet());

        if (caseReferences.size() > 0) {
            return Optional.of(QueryBuilders.termsQuery(REFERENCE_FIELD_COL, caseReferences));
        }
        return Optional.empty();
    }
}
