package uk.gov.hmcts.ccd.data.caseaccess;

import com.google.common.collect.Maps;
import uk.gov.hmcts.ccd.ApplicationParams;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;

@Named
@Singleton
public class AMSwitch {

    public static final String AM_TYPE = "am";
    public static final String CCD_TYPE = "ccd";
    public static final String BOTH_TYPE = "both";
    private final Map<String, String> caseTypesToWriteModes;
    private final Map<String, String> caseTypesToReadModes;

    public AMSwitch(final ApplicationParams applicationParams) {
        this.caseTypesToWriteModes = Maps.newHashMap();
        this.caseTypesToReadModes = Maps.newHashMap();
        applicationParams.getWriteToCCDCaseTypesOnly().forEach(caseType -> {
            caseTypesToWriteModes.put(caseType, CCD_TYPE);
        });
        applicationParams.getWriteToAMCaseTypesOnly().forEach(caseType -> {
            caseTypesToWriteModes.put(caseType, AM_TYPE);
        });
        applicationParams.getWriteToBothCaseTypes().forEach(caseType -> {
            caseTypesToWriteModes.put(caseType, BOTH_TYPE);
        });
        applicationParams.getReadFromCCDCaseTypes().forEach(caseType -> {
            caseTypesToReadModes.put(caseType, CCD_TYPE);
        });
        applicationParams.getReadFromAMCaseTypes().forEach(caseType -> {
            caseTypesToReadModes.put(caseType, AM_TYPE);
        });
    }

    public boolean isWriteAccessManagementWithCCD(final String caseTypeId) {
        String mode = caseTypesToWriteModes.get(caseTypeId);
        return mode.equals(BOTH_TYPE) || mode.equals(CCD_TYPE);
    }

    public boolean isWriteAccessManagementWithAM(final String caseTypeId) {
        String mode = caseTypesToWriteModes.get(caseTypeId);
        return mode.equals(BOTH_TYPE) || mode.equals(AM_TYPE);
    }

    public boolean isReadAccessManagementWithCCD(final String caseTypeId) {
        return caseTypesToReadModes.get(caseTypeId).equals(CCD_TYPE);
    }

    public boolean isReadAccessManagementWithAM(final String caseTypeId) {
        return caseTypesToReadModes.get(caseTypeId).equals(AM_TYPE);
    }

}
