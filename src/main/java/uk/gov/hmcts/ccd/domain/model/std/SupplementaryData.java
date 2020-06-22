package uk.gov.hmcts.ccd.domain.model.std;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SupplementaryData {

    private Map<String, Object> supplementaryData;
}
