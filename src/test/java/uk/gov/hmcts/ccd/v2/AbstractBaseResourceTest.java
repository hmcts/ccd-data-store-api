package uk.gov.hmcts.ccd.v2;

public abstract class AbstractBaseResourceTest {

    protected String getActualPath(String actualUrl) {
        String actualPath = actualUrl;
        if (actualUrl.contains("://localhost")) {
            actualPath = actualUrl.substring(actualUrl.indexOf("/", actualUrl.indexOf("//") + 2));
        }
        return actualPath;
    }

}
