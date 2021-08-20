package uk.gov.hmcts.ccd.domain.service.globalsearch;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServiceRefData {
    String serviceCode;
    String serviceShortDescription;
}
