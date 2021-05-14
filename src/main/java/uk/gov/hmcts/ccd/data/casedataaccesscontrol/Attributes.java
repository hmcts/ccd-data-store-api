package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
public class Attributes implements Serializable {

    private List<String> jurisdiction;
    private List<String> caseType;
    private List<String> caseId;
    private List<String> region;
    private List<String> location;
    private List<String> contractType;
}
