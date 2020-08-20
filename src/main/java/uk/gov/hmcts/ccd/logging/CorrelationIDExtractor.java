package uk.gov.hmcts.ccd.logging;

public interface CorrelationIDExtractor <T>{

    String getCorrelationID(T resource);
}
