package uk.gov.hmcts.ccd.domain.service.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_DELETE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;

class AttributeBasedAccessControlServiceTest {

    private static final String ACCESS_PROFILE_1 = "ACCESS_PROFILE_1";
    private static final String ACCESS_PROFILE_2 = "ACCESS_PROFILE_2";
    private static final String ACCESS_PROFILE_3 = "ACCESS_PROFILE_3";
    private static final String ACCESS_PROFILE_4 = "ACCESS_PROFILE_4";

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private CompoundAccessControlService compoundAccessControlService;

    @InjectMocks
    private AttributeBasedAccessControlService attributeBasedAccessControlService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldReturnFalseWhenAccessProfilesAreReadOnlyAndPredicateIsDelete() {
        Set<AccessProfile> accessProfiles = createAccessProfiles(true, ACCESS_PROFILE_1);
        List<AccessControlList> accessControlLists = createAccessControlList(ACCESS_PROFILE_1);
        boolean hasAccess = attributeBasedAccessControlService
            .hasAccessControlList(accessProfiles, CAN_DELETE, accessControlLists);

        assertFalse(hasAccess);
    }

    @Test
    void shouldReturnFalseWhenAccessProfilesAreReadOnlyAndPredicateIsDeleteAndAclsEmpty() {
        Set<AccessProfile> accessProfiles = createAccessProfiles(true, ACCESS_PROFILE_1);
        boolean hasAccess = attributeBasedAccessControlService
            .hasAccessControlList(accessProfiles, CAN_DELETE, Collections.emptyList());

        assertFalse(hasAccess);
    }

    @Test
    void shouldReturnTrueeWhenAccessProfilesAreReadOnlyAndPredicateIsRead() {
        Set<AccessProfile> accessProfiles = createAccessProfiles(true, ACCESS_PROFILE_1);
        List<AccessControlList> accessControlLists = createAccessControlList(ACCESS_PROFILE_1);
        boolean hasAccess = attributeBasedAccessControlService
            .hasAccessControlList(accessProfiles, CAN_READ, accessControlLists);

        assertTrue(hasAccess);
    }

    @Test
    void shouldReturnFalseWhenAccessProfilesAreReadOnlyAndPredicateIsUpdate() {
        Set<AccessProfile> accessProfiles = createAccessProfiles(true, ACCESS_PROFILE_1);
        List<AccessControlList> accessControlLists = createAccessControlList(ACCESS_PROFILE_1);
        boolean hasAccess = attributeBasedAccessControlService
            .hasAccessControlList(accessProfiles, CAN_UPDATE, accessControlLists);

        assertFalse(hasAccess);
    }

    @Test
    void shouldReturnFalseWhenAccessProfilesAreReadOnlyAndPredicateIsCreate() {
        Set<AccessProfile> accessProfiles = createAccessProfiles(true, ACCESS_PROFILE_1);
        List<AccessControlList> accessControlLists = createAccessControlList(ACCESS_PROFILE_1);
        boolean hasAccess = attributeBasedAccessControlService
            .hasAccessControlList(accessProfiles, CAN_CREATE, accessControlLists);

        assertFalse(hasAccess);
    }

    @Test
    void shouldReturnTrueWhenAccessProfilesAreNotReadOnlyAndPredicateIsCreate() {
        Set<AccessProfile> accessProfiles = createAccessProfiles(false,
            ACCESS_PROFILE_1,
            ACCESS_PROFILE_2,
            ACCESS_PROFILE_3,
            ACCESS_PROFILE_4);
        List<AccessControlList> accessControlLists = createAccessControlList(ACCESS_PROFILE_1,
            ACCESS_PROFILE_2,
            ACCESS_PROFILE_3,
            ACCESS_PROFILE_4);
        boolean hasAccess = attributeBasedAccessControlService
            .hasAccessControlList(accessProfiles, CAN_CREATE, accessControlLists);

        assertTrue(hasAccess);
    }

    private Set<AccessProfile> createAccessProfiles(boolean readOnly, String... accessProfiles) {
        return Arrays.stream(accessProfiles)
            .map(accessProfile -> AccessProfile.builder()
                .accessProfile(accessProfile)
                .readOnly(readOnly)
                .build())
            .collect(Collectors.toSet());
    }

    private List<AccessControlList> createAccessControlList(String... accessProfiles) {
        return Arrays.stream(accessProfiles)
            .map(accessProfile -> {
                AccessControlList accessControlList = new AccessControlList();
                accessControlList.setAccessProfile(accessProfile);
                accessControlList.setCreate(true);
                accessControlList.setDelete(true);
                accessControlList.setRead(true);
                accessControlList.setUpdate(true);
                return accessControlList;
            })
            .collect(Collectors.toList());
    }
}
