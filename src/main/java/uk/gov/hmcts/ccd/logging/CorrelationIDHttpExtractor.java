package uk.gov.hmcts.ccd.logging;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component("CorrelationIDHttpExtractor")
public class CorrelationIDHttpExtractor implements CorrelationIDExtractor<HttpServletRequest> {

    //Prototype
    private static final String I_SHOULD_BE_AN_PARAMETER = "correlationIDAzure";

    public String getCorrelationID(HttpServletRequest resource) {

        final String correlationID = resource.getHeader(I_SHOULD_BE_AN_PARAMETER);
        return "VALUE" + correlationID;
    }
}
