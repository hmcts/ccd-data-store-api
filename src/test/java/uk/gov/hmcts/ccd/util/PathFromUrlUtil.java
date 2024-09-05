package uk.gov.hmcts.ccd.util;

public class PathFromUrlUtil {

    public static String getActualPath(String actualUrl) {
        String actualPath = actualUrl;
        if (actualUrl.contains("://localhost")) {
            actualPath = actualUrl.substring(actualUrl.indexOf("/", actualUrl.indexOf("//") + 2));
        }
        return actualPath;
    }

}
