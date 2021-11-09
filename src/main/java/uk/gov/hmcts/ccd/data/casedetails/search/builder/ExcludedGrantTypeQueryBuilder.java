package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

@Slf4j
@Component
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class ExcludedGrantTypeQueryBuilder extends GrantTypeQueryBuilder {

    @Autowired
    public ExcludedGrantTypeQueryBuilder(AccessControlService accessControlService,
                                            CaseDataAccessControl caseDataAccessControl) {
        super(accessControlService, caseDataAccessControl);
    }

    @Override
    protected GrantType getGrantType() {
        return GrantType.EXCLUDED;
    }

    @SuppressWarnings("java:S2789")
    public String createQuery(List<RoleAssignment> roleAssignments,
                              Map<String, Object> params,
                              CaseTypeDefinition caseType) {
        Supplier<Stream<RoleAssignment>> streamSupplier = filterGrantTypeRoleAssignments(roleAssignments);
        return  getCaseReferences(streamSupplier, params);
    }

    @SuppressWarnings("java:S2789")
    private String getCaseReferences(Supplier<Stream<RoleAssignment>> streamSupplier,
                                     Map<String, Object> params) {
        Set<String> caseReferences = streamSupplier.get()
            .filter(roleAssignment -> roleAssignment.getAttributes() != null)
            .map(roleAssignment -> roleAssignment.getAttributes().getCaseId())
            .filter(Objects::nonNull)
            .map(Optional::get)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());

        if (!caseReferences.isEmpty()) {
            String paramName = "case_ids_excluded";
            params.put(paramName, caseReferences);
            return String.format(QUERY, REFERENCE, paramName);
        }
        return EMPTY;
    }
}
