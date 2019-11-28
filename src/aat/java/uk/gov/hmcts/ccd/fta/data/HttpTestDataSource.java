package uk.gov.hmcts.ccd.fta.data;

public interface HttpTestDataSource {

    HttpTestData getDataForTestCall(String testDataId);

}
