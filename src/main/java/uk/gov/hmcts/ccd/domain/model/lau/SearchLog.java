package uk.gov.hmcts.ccd.domain.model.lau;

import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.ccd.auditlog.aop.AuditContext;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@NoArgsConstructor
@Setter
public class SearchLog implements Serializable {

    public static final long serialVersionUID = 432973322;

    private static final String ANY_SPACE_MATCH = "\\s*";

    private String userId;
    private List<String> caseRefs;
    private String timestamp;

    public SearchLog(final String userId,
        final List<String> caseRefs,
        final String timestamp) {
        this.userId = userId;
        this.caseRefs = caseRefs;
        this.timestamp = timestamp;
    }

    public void setCaseRefs(String caseRefs) {
        this.caseRefs = Arrays.asList(caseRefs.split(ANY_SPACE_MATCH + AuditContext.CASE_ID_SEPARATOR
            + ANY_SPACE_MATCH));
    }

}
