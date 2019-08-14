package uk.gov.hmcts.ccd.data.caseaccess;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.InvalidPropertyException;
import uk.gov.hmcts.ccd.ApplicationParams;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

@Named
@Singleton
public class AMSwitch {

    private static final String AM_MODE = "am";
    private static final String CCD_MODE = "ccd";
    private static final String BOTH_MODE = "both";
    private final Map<String, String> caseTypesToWriteModes;
    private final Map<String, String> caseTypesToReadModes;

    public AMSwitch(final ApplicationParams applicationParams) {
        this.caseTypesToWriteModes = Maps.newHashMap();
        this.caseTypesToReadModes = Maps.newHashMap();
        List<String> writeDuplicates = Lists.newArrayList(applicationParams.getWriteToCCDCaseTypesOnly());
        applicationParams.getWriteToCCDCaseTypesOnly().stream().filter(StringUtils::isNotEmpty).forEach(caseType -> caseTypesToWriteModes.put(caseType.toUpperCase(), CCD_MODE));
        applicationParams.getWriteToAMCaseTypesOnly().stream().filter(StringUtils::isNotEmpty).forEach(caseType -> {
            consumeOrThrow(writeDuplicates, caseTypesToWriteModes, "ccd.am.write.to_am_only", caseType, AM_MODE);
        });
        applicationParams.getWriteToBothCaseTypes().stream().filter(StringUtils::isNotEmpty).forEach(caseType -> {
            consumeOrThrow(writeDuplicates, caseTypesToWriteModes,"ccd.am.write.to_both", caseType, BOTH_MODE);
        });
        List<String> readDuplicates = Lists.newArrayList(applicationParams.getReadFromCCDCaseTypes());
        applicationParams.getReadFromCCDCaseTypes().stream().filter(StringUtils::isNotEmpty).forEach(caseType -> caseTypesToReadModes.put(caseType.toUpperCase(), CCD_MODE));
        applicationParams.getReadFromAMCaseTypes().stream().filter(StringUtils::isNotEmpty).forEach(caseType -> {
            consumeOrThrow(readDuplicates, caseTypesToReadModes, "ccd.am.read.from_am", caseType, AM_MODE);
        });
    }

    private void consumeOrThrow(final List<String> duplicates, Map<String, String> caseTypesToModes, String property, final String caseType, final String mode) {
        if (duplicates.contains(caseType)) {
            throw new InvalidPropertyException(ApplicationParams.class, property, "Duplicate case type configurations detected for Access Management persistence switches.");
        } else {
            duplicates.add(caseType);
            caseTypesToModes.put(caseType.toUpperCase(), mode);
        }
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
