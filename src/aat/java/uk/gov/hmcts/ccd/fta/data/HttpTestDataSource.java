package uk.gov.hmcts.ccd.fta.data;

public interface HttpTestDataSource {

    HttpTestData getDataForScenario(String scenarioKey);

    CaseData getCaseForScenario(String scenarioKey);

}
