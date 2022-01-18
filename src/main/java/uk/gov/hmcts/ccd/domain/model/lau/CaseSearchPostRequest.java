package uk.gov.hmcts.ccd.domain.model.lau;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Setter
@Getter
public class CaseSearchPostRequest implements Serializable {

    public static final long serialVersionUID = 432973322;

    private SearchLog searchLog;

    public CaseSearchPostRequest(final SearchLog searchLog) {
        this.searchLog = searchLog;
    }
}
