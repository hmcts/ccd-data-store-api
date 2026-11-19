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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    @Test
    void shouldReturnTrueWhenAccessProfilesAreNotReadOnlyWithReadFalseAndPredicateIsCreate() {
        Set<AccessProfile> accessProfiles = createAccessProfiles(false,
            ACCESS_PROFILE_1,
            ACCESS_PROFILE_2,
            ACCESS_PROFILE_3,
            ACCESS_PROFILE_4);
        List<AccessControlList> accessControlLists = createAccessControlListWithReadFalse(ACCESS_PROFILE_1,
            ACCESS_PROFILE_2,
            ACCESS_PROFILE_3,
            ACCESS_PROFILE_4);
        boolean hasAccess = attributeBasedAccessControlService
            .hasAccessControlList(accessProfiles, CAN_CREATE, accessControlLists);

        assertTrue(hasAccess);
    }

    @Test
    void shouldTreatNullReadOnlyAsNotReadOnly() {
        // AccessProfile.readOnly is a Boolean. Every production construction site sets it (AuthorisationMapper via
        // BooleanUtils.isTrue, CaseAccessService and RoleAssignmentService with a literal false), but the model
        // permits null - AccessProfile(String) leaves it unset. Auto-unboxing a null used to throw NPE here.
        Set<AccessProfile> accessProfiles = Collections.singleton(AccessProfile.builder()
            .accessProfile(ACCESS_PROFILE_1)
            .build());
        List<AccessControlList> accessControlLists = createAccessControlList(ACCESS_PROFILE_1);

        assertNull(accessProfiles.iterator().next().getReadOnly());
        assertTrue(attributeBasedAccessControlService
            .hasAccessControlList(accessProfiles, CAN_CREATE, accessControlLists));
        assertTrue(attributeBasedAccessControlService
            .hasAccessControlList(accessProfiles, CAN_DELETE, accessControlLists));
    }

    @Test
    void shouldNotMutateSuppliedAccessControlListsWhenDowngradingToReadOnly() {
        // The read-only downgrade must build new ACLs rather than edit the caller's, which are shared definition
        // objects. hasAccessControlList is also handed an immutable Stream.toList() result internally, so any
        // attempt to mutate would fail outright.
        Set<AccessProfile> accessProfiles = createAccessProfiles(true, ACCESS_PROFILE_1);
        List<AccessControlList> accessControlLists = createAccessControlList(ACCESS_PROFILE_1);
        AccessControlList supplied = accessControlLists.getFirst();

        assertFalse(attributeBasedAccessControlService
            .hasAccessControlList(accessProfiles, CAN_DELETE, accessControlLists));

        assertAll(
            () -> assertEquals(1, accessControlLists.size()),
            () -> assertSame(supplied, accessControlLists.getFirst()),
            () -> assertTrue(supplied.isCreate()),
            () -> assertTrue(supplied.isRead()),
            () -> assertTrue(supplied.isUpdate()),
            () -> assertTrue(supplied.isDelete())
        );
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
            .map(accessProfile -> AccessControlList.builder()
                .accessProfile(accessProfile)
                .create(true)
                .read(true)
                .update(true)
                .delete(true)
                .build())
            .collect(Collectors.toList());
    }

    private List<AccessControlList> createAccessControlListWithReadFalse(String... accessProfiles) {
        return Arrays.stream(accessProfiles)
            .map(accessProfile -> AccessControlList.builder()
                .accessProfile(accessProfile)
                .create(true)
                .delete(true)
                .read(false)
                .update(true)
                .build())
            .collect(Collectors.toList());
    }
}
