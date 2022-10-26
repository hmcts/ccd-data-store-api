package uk.gov.hmcts.ccd.domain.model.lau;

import com.google.common.base.Splitter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.ccd.auditlog.aop.AuditContext;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@NoArgsConstructor
@Setter
@Getter
public class SearchLog implements Serializable {

    public static final long serialVersionUID = 432973322;

    private String userId;
    private List<String> caseRefs;
    private ZonedDateTime timestamp;

    public String getTimestamp() {
        return timestamp.format(ISO_INSTANT);
    }

    public void setCaseRefs(final String caseRefs) {
        if (!isEmpty(caseRefs)) {
            this.caseRefs = Splitter.on(AuditContext.CASE_ID_SEPARATOR).trimResults().splitToList(caseRefs);
        }
    }
}
