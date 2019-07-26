package uk.gov.hmcts.ccd.data.caseaccess;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

public class SwitchableCaseUserRepository {

    public static final String AM_TYPE = "am";
    public static final String CCD_TYPE = "ccd";
    private final Map<String, CaseUserRepository> caseUserRepositoryTypes;
    private final Multimap<String, String> caseTypesToWriteModes;
    private final Map<String, String> caseTypesToReadModes;

    private final Multimap<String, CaseUserRepository> writeCaseTypeToCaseUserRepository;
    private final Map<String, CaseUserRepository> readCaseTypeToCaseUserRepository;

    public SwitchableCaseUserRepository() {
        this.caseUserRepositoryTypes = Maps.newHashMap();
        this.caseTypesToWriteModes = ArrayListMultimap.create();
        this.caseTypesToReadModes = Maps.newHashMap();
        this.writeCaseTypeToCaseUserRepository = ArrayListMultimap.create();
        this.readCaseTypeToCaseUserRepository = Maps.newHashMap();
    }

    // set up section

    @Autowired
    public void setCaseUserRepositoryTypes(List<CaseUserRepository> caseUserRepositoryTypes) {
        caseUserRepositoryTypes.forEach(caseUserRepository -> this.caseUserRepositoryTypes.put(caseUserRepository.getType(), caseUserRepository));
    }

    public void updateWriteToAMCaseUserRepositoryForCaseType(String caseType) {
        writeCaseTypeToCaseUserRepository.put(caseType, caseUserRepositoryTypes.get(AM_TYPE));
        caseTypesToWriteModes.put(caseType, AM_TYPE);
    }

    public void updateWriteToCCDCaseUserRepositoryForCaseType(String caseType) {
        writeCaseTypeToCaseUserRepository.put(caseType, caseUserRepositoryTypes.get(CCD_TYPE));
        caseTypesToWriteModes.put(caseType, CCD_TYPE);
    }

    public void updateWriteToBothCaseUserRepositoryForCaseType(String caseType) {
        writeCaseTypeToCaseUserRepository.put(caseType, caseUserRepositoryTypes.get(AM_TYPE));
        writeCaseTypeToCaseUserRepository.put(caseType, caseUserRepositoryTypes.get(CCD_TYPE));
        caseTypesToWriteModes.put(caseType, AM_TYPE);
        caseTypesToWriteModes.put(caseType, CCD_TYPE);
    }

    public void updateReadFromAMCaseUserRepositoryForCaseType(String caseType) {
        readCaseTypeToCaseUserRepository.put(caseType, caseUserRepositoryTypes.get(AM_TYPE));
        caseTypesToReadModes.put(caseType, AM_TYPE);
    }

    public void updateReadFromCCDCaseUserRepositoryForCaseType(String caseType) {
        readCaseTypeToCaseUserRepository.put(caseType, caseUserRepositoryTypes.get(CCD_TYPE));
        caseTypesToReadModes.put(caseType, CCD_TYPE);
    }

    // API section

    public List<String> getWriteModeForCaseType(String caseType) {
        return (List<String>)caseTypesToWriteModes.get(caseType);
    }

    public String getReadModeForCaseType(String caseType) {
        return caseTypesToReadModes.get(caseType);
    }

    public List<CaseUserRepository> forWriting(String caseTypeId) {
        return (List<CaseUserRepository>)this.writeCaseTypeToCaseUserRepository.get(caseTypeId);
    }

    public CaseUserRepository forReading(String caseTypeId) {
        return this.readCaseTypeToCaseUserRepository.getOrDefault(caseTypeId, this.caseUserRepositoryTypes.get("ccd"));
    }

    public List<CaseUserRepository> forReading() {
        return (List<CaseUserRepository>) caseUserRepositoryTypes.values();
    }

}
