package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import lombok.Data;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class CaseAccessMetadata {

    public static final String ACCESS_GRANTED = "[ACCESS_GRANTED]";
    public static final String ACCESS_PROCESS = "[ACCESS_PROCESS]";
    public static final String ACCESS_GRANTED_LABEL = "Access Granted";
    public static final String ACCESS_PROCESS_LABEL = "Access Process";

    private static final String COMMA_DELIMITER = ",";

    private List<GrantType> accessGrants;
    private AccessProcess accessProcess;

    public String getAccessGrantsString() {
        if (accessGrants == null) {
            return null;
        }
        return accessGrants.stream()
            .map(Enum::name)
            .distinct()
            .sorted()
            .collect(Collectors.joining(COMMA_DELIMITER));
    }

    public String getAccessProcessString() {
        if (accessProcess == null) {
            return null;
        }
        return accessProcess.name();
    }
}
