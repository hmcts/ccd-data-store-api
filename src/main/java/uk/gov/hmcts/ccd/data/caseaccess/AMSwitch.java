package uk.gov.hmcts.ccd.data.caseaccess;

import com.google.common.collect.Maps;
import uk.gov.hmcts.ccd.ApplicationParams;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;

@Named
@Singleton
public class AMSwitch {

    public static final String AM_MODE = "am";
    public static final String CCD_MODE = "ccd";
    public static final String BOTH_MODE = "both";
    private final Map<String, String> caseTypesToWriteModes;
    private final Map<String, String> caseTypesToReadModes;

    public AMSwitch(final ApplicationParams applicationParams) {
        this.caseTypesToWriteModes = Maps.newHashMap();
        this.caseTypesToReadModes = Maps.newHashMap();
        applicationParams.getWriteToCCDCaseTypesOnly().forEach(caseType -> {
            caseTypesToWriteModes.put(caseType.toUpperCase(), CCD_MODE);
        });
        applicationParams.getWriteToAMCaseTypesOnly().forEach(caseType -> {
            caseTypesToWriteModes.put(caseType.toUpperCase(), AM_MODE);
        });
        applicationParams.getWriteToBothCaseTypes().forEach(caseType -> {
            caseTypesToWriteModes.put(caseType.toUpperCase(), BOTH_MODE);
        });
        applicationParams.getReadFromCCDCaseTypes().forEach(caseType -> {
            caseTypesToReadModes.put(caseType.toUpperCase(), CCD_MODE);
        });
        applicationParams.getReadFromAMCaseTypes().forEach(caseType -> {
            caseTypesToReadModes.put(caseType.toUpperCase(), AM_MODE);
        });
    }

    public boolean isWriteAccessManagementWithCCD(final String caseTypeId) {
        String mode = caseTypesToWriteModes.getOrDefault(caseTypeId.toUpperCase(), CCD_MODE);
        return mode.equals(BOTH_MODE) || mode.equals(CCD_MODE);
    }

    public boolean isWriteAccessManagementWithAM(final String caseTypeId) {
        String mode = caseTypesToWriteModes.getOrDefault(caseTypeId.toUpperCase(), CCD_MODE);
        return mode.equals(BOTH_MODE) || mode.equals(AM_MODE);
    }

    public boolean isReadAccessManagementWithCCD(final String caseTypeId) {
        return caseTypesToReadModes.getOrDefault(caseTypeId.toUpperCase(), CCD_MODE).equals(CCD_MODE);
    }

    public boolean isReadAccessManagementWithAM(final String caseTypeId) {
        return caseTypesToReadModes.getOrDefault(caseTypeId.toUpperCase(), CCD_MODE).equals(AM_MODE);
    }

}
