package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewActionableEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField.READONLY;

@Service
@ConditionalOnProperty(name = "enable-attribute-based-access-control", havingValue = "false", matchIfMissing = true)
public class AccessControlServiceImpl implements AccessControlService {

    private static final Logger LOG = LoggerFactory.getLogger(AccessControlServiceImpl.class);
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    private final ApplicationParams applicationParams;

    private final CompoundAccessControlService compoundAccessControlService;

    public AccessControlServiceImpl(final ApplicationParams applicationParams,
                                    final CompoundAccessControlService compoundAccessControlService) {
        this.applicationParams = applicationParams;
        this.compoundAccessControlService = compoundAccessControlService;
    }

    @Override
    public boolean canAccessCaseTypeWithCriteria(final CaseTypeDefinition caseType,
                                                 final Set<AccessProfile> accessProfiles,
                                                 final Predicate<AccessControlList> criteria) {
        if (caseType == null) {
            return false;
        }

        boolean hasAccess = hasAccessControlList(accessProfiles, criteria, caseType.getAccessControlLists());
        if (!hasAccess) {
            LOG.debug(NO_ROLE_FOUND, "caseType",
                    caseType.getId(),
                    AccessControlService.extractAccessProfileNames(accessProfiles),
                    "caseTypeACL",
                    caseType.getAccessControlLists());
        }

        return hasAccess;
    }

    @Override
    public boolean canAccessCaseStateWithCriteria(final String caseState,
                                                  final CaseTypeDefinition caseType,
                                                  final Set<AccessProfile> accessProfiles,
                                                  final Predicate<AccessControlList> criteria) {

        List<AccessControlList> stateACLs = caseType.getStates()
            .stream()
            .filter(cState -> cState.getId().equalsIgnoreCase(caseState))
            .map(CaseStateDefinition::getAccessControlLists)
            .flatMap(Collection::stream)
            .collect(toList());

        boolean hasAccess = hasAccessControlList(accessProfiles, criteria, stateACLs);

        if (!hasAccess) {
            LOG.debug(NO_ROLE_FOUND, "caseState",
                    caseState,
                    AccessControlService.extractAccessProfileNames(accessProfiles),
                    "caseStateACL",
                    stateACLs);
        }
        return hasAccess;
    }

    @Override
    public boolean canAccessCaseEventWithCriteria(final String eventId,
                                                  final List<CaseEventDefinition> caseEventDefinitions,
                                                  final Set<AccessProfile> accessProfiles,
                                                  final Predicate<AccessControlList> criteria) {
        return hasCaseEventAccess(eventId, caseEventDefinitions, accessProfiles, criteria);
    }

    @Override
    public boolean canAccessCaseFieldsWithCriteria(final JsonNode caseFields,
                                                   final List<CaseFieldDefinition> caseFieldDefinitions,
                                                   final Set<AccessProfile> accessProfiles,
                                                   final Predicate<AccessControlList> criteria) {
        if (caseFields != null) {
            final Iterator<String> fieldNames = caseFields.fieldNames();
            while (fieldNames.hasNext()) {
                if (!hasCaseFieldAccess(caseFieldDefinitions, accessProfiles, criteria, fieldNames.next())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean canAccessCaseViewFieldWithCriteria(final CommonField caseViewField,
                                                      final Set<AccessProfile> accessProfiles,
                                                      final Predicate<AccessControlList> criteria) {
        if (!hasAccessControlList(accessProfiles, criteria, caseViewField.getAccessControlLists())) {
            LOG.debug(NO_ROLE_FOUND, "caseField",
                    caseViewField.getId(),
                    AccessControlService.extractAccessProfileNames(accessProfiles),
                    "caseFieldACL",
                    caseViewField.getAccessControlLists());
            return false;
        }
        return true;
    }

    @Override
    public boolean canAccessCaseFieldsForUpsert(final JsonNode newData,
                                                final JsonNode existingData,
                                                final List<CaseFieldDefinition> caseFieldDefinitions,
                                                final Set<AccessProfile> accessProfiles) {
        if (newData != null) {
            List<String> errors = new ArrayList<>();
            final boolean noAccessGranted = getStream(newData)
                .anyMatch(newFieldName -> {
                    if (existingData.has(newFieldName)) {
                        boolean result = !valueDifferentAndHasUpdateAccess(newData, existingData, newFieldName,
                                caseFieldDefinitions, accessProfiles);
                        if (result) {
                            errors.add(newFieldName);
                        }
                        return result;
                    } else {
                        boolean result = !hasCaseFieldAccess(caseFieldDefinitions,
                                accessProfiles, CAN_CREATE, newFieldName);
                        if (result) {
                            errors.add(newFieldName);
                        }
                        return result;
                    }
                });
            if (noAccessGranted) {
                String listString = String.join(", ", errors);
                LOG.info("Fields have no access {}", listString);
            }
            return !noAccessGranted;
        }
        return true;
    }

    @Override
    public JsonNode filterCaseFieldsByAccess(final JsonNode caseFields,
                                             final List<CaseFieldDefinition> caseFieldDefinitions,
                                             final Set<AccessProfile> accessProfiles,
                                             final Predicate<AccessControlList> access,
                                             boolean isClassification) {
        ObjectNode filteredCaseFields = JSON_NODE_FACTORY.objectNode();
        getStream(caseFields).forEach(
            fieldName -> findCaseFieldAndVerifyHasAccess(fieldName, caseFieldDefinitions, accessProfiles, access)
                .ifPresent(caseField -> {
                    if (isEmpty(caseField.getComplexACLs())) {
                        filteredCaseFields.set(fieldName, caseFields.get(fieldName));
                    } else if (!isClassification) {
                        filteredCaseFields.set(
                            fieldName,
                            filterChildrenUsingJsonNode(caseField, caseFields.get(fieldName), accessProfiles,
                                access, isClassification)
                        );
                    }
                })
        );
        return filteredCaseFields;
    }

    @Override
    public List<CaseFieldDefinition> filterCaseFieldsByAccess(final List<CaseFieldDefinition> caseFieldDefinitions,
                                                              final Set<AccessProfile> accessProfiles,
                                                              final Predicate<AccessControlList> access) {
        List<CaseFieldDefinition> filteredCaseFields = newArrayList();
        if (caseFieldDefinitions != null) {
            filteredCaseFields = caseFieldDefinitions
                .stream()
                .filter(caseField -> caseField.isMetadata() || hasAccessControlList(accessProfiles,
                    access,
                    caseField.getAccessControlLists()))
                .map(caseField -> checkIfChildFilteringRequired(caseField, accessProfiles, access))
                .collect(toList());

        }
        return filteredCaseFields;
    }

    @Override
    public CaseUpdateViewEvent setReadOnlyOnCaseViewFieldsIfNoAccess(final String caseTypeId,
        final String caseReference, final String eventId, final CaseUpdateViewEvent caseEventTrigger,
        final List<CaseFieldDefinition> caseFieldDefinitions, final Set<AccessProfile> accessProfiles,
        final Predicate<AccessControlList> access) {

        List<String> filteredCaseFieldIds = new ArrayList<>();

        boolean isMultipartyFixEnabled = applicationParams.isMultipartyFixEnabled();
        List<String> multipartyCaseTypes = applicationParams.getMultipartyCaseTypes();
        List<String> multipartyEvents = applicationParams.getMultipartyEvents();

        caseEventTrigger.getCaseFields()
            .forEach(caseViewField -> {
                String caseViewFieldId = caseViewField.getId();
                Optional<CaseFieldDefinition> caseFieldOpt = findCaseField(caseFieldDefinitions, caseViewFieldId);

                if (caseFieldOpt.isPresent()) {
                    CaseFieldDefinition field = caseFieldOpt.get();
                    if (!hasAccessControlList(accessProfiles, access, field.getAccessControlLists())) {
                        caseViewField.setDisplayContext(READONLY);
                        if (shouldRemoveCaseViewFieldIfNoReadAccess(caseTypeId, caseReference, eventId,
                            isMultipartyFixEnabled, multipartyCaseTypes, multipartyEvents, caseViewFieldId,
                            field, accessProfiles)) {
                            filteredCaseFieldIds.add(caseViewField.getId());
                            return;
                        }
                    }

                    if (field.isCompoundFieldType()) {
                        setChildrenAsReadOnlyIfNoAccess(caseTypeId, caseReference, eventId, isMultipartyFixEnabled,
                            multipartyCaseTypes, multipartyEvents, caseEventTrigger.getWizardPages(), field.getId(),
                            field, access, accessProfiles, caseViewField);
                    }
                } else {
                    caseViewField.setDisplayContext(READONLY);
                    if (shouldRemoveCaseViewFieldIfNoReadAccess(caseTypeId, caseReference, eventId,
                        isMultipartyFixEnabled, multipartyCaseTypes, multipartyEvents, caseViewFieldId,
                        null, accessProfiles)) {
                        filteredCaseFieldIds.add(caseViewField.getId());
                    }
                }
            });

        removeCaseViewFieldIfNoReadAccess(caseEventTrigger, filteredCaseFieldIds);
        return caseEventTrigger;
    }

    @Override
    public CaseUpdateViewEvent updateCollectionDisplayContextParameterByAccess(CaseUpdateViewEvent caseEventTrigger,
                                                                               Set<AccessProfile> accessProfiles) {
        caseEventTrigger.getCaseFields().stream().filter(CommonField::isCollectionFieldType)
            .forEach(caseViewField ->
                caseViewField.setDisplayContextParameter(generateDisplayContextParameter(accessProfiles,
                    caseViewField)));

        caseEventTrigger.getCaseFields().forEach(caseViewField ->
            setChildrenCollectionDisplayContextParameter(caseViewField.getFieldTypeDefinition().getChildren(),
                accessProfiles));

        return caseEventTrigger;
    }

    @Override
    public CaseUpdateViewEvent filterCaseViewFieldsByAccess(final CaseUpdateViewEvent caseEventTrigger,
                                                            final List<CaseFieldDefinition> caseFieldDefinitions,
                                                            final Set<AccessProfile> accessProfiles,
                                                            final Predicate<AccessControlList> access) {
        List<String> filteredCaseFieldIds = new ArrayList<>();
        caseEventTrigger.setCaseFields(caseEventTrigger.getCaseFields()
            .stream()
            .filter(caseViewField -> {
                Optional<CaseFieldDefinition> caseFieldOpt = findCaseField(caseFieldDefinitions, caseViewField.getId());

                if (caseFieldOpt.isPresent()) {
                    CaseFieldDefinition cf = caseFieldOpt.get();
                    if (!hasAccessControlList(accessProfiles, access, cf.getAccessControlLists())) {
                        filteredCaseFieldIds.add(caseViewField.getId());
                        return false;
                    }
                    if (!isEmpty(cf.getComplexACLs())) {
                        cf.getFieldTypeDefinition().getChildren().forEach(caseField ->
                            filterChildren(caseField, caseViewField, accessProfiles, access));
                    }
                } else {
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList())
        );
        caseEventTrigger.setWizardPages(filterWizardPageFields(caseEventTrigger, filteredCaseFieldIds));
        return caseEventTrigger;
    }

    @Override
    public List<AuditEvent> filterCaseAuditEventsByReadAccess(final List<AuditEvent> auditEvents,
                                                              final List<CaseEventDefinition> caseEventDefinitions,
                                                              final Set<AccessProfile> accessProfiles) {
        List<AuditEvent> filteredAuditEvents = newArrayList();
        if (auditEvents != null) {
            filteredAuditEvents = auditEvents
                .stream()
                .filter(auditEvent -> hasCaseEventWithAccess(accessProfiles, auditEvent, caseEventDefinitions))
                .collect(toList());

        }
        return filteredAuditEvents;
    }

    @Override
    public List<CaseStateDefinition> filterCaseStatesByAccess(CaseTypeDefinition caseType,
                                                              final Set<AccessProfile> accessProfiles,
                                                              final Predicate<AccessControlList> access) {
        return caseType.getStates()
            .stream()
            .filter(caseState -> hasAccessControlList(accessProfiles,
                access,
                caseState.getAccessControlLists()))
            .collect(toList());
    }

    @Override
    public List<CaseStateDefinition> filterCaseStatesByAccess(List<CaseStateDefinition> caseStates,
                                                       Set<AccessProfile> accessProfiles,
                                                       Predicate<AccessControlList> access) {
        return caseStates
            .stream()
            .filter(caseState -> hasAccessControlList(accessProfiles,
                access,
                caseState.getAccessControlLists()))
            .collect(toList());
    }

    @Override
    public List<CaseEventDefinition> filterCaseEventsByAccess(CaseTypeDefinition caseTypeDefinition,
                                                              final Set<AccessProfile> accessProfiles,
                                                              final Predicate<AccessControlList> access) {
        return caseTypeDefinition.getEvents()
            .stream()
            .filter(caseEvent -> hasAccessControlList(accessProfiles,
                access,
                caseEvent.getAccessControlLists()))
            .collect(toList());
    }

    @Override
    public CaseViewActionableEvent[] filterCaseViewTriggersByCreateAccess(
        final CaseViewActionableEvent[] caseViewTriggers,
        final List<CaseEventDefinition> caseEventDefinitions,
        final Set<AccessProfile> accessProfiles) {
        return stream(caseViewTriggers)
            .filter(caseViewTrigger -> hasAccessControlList(accessProfiles,
                CAN_CREATE,
                getCaseEventById(caseEventDefinitions, caseViewTrigger)
                    .map(CaseEventDefinition::getAccessControlLists)
                    .orElse(newArrayList()))
            )
            .toArray(CaseViewActionableEvent[]::new);
    }

    private void removeCaseViewFieldIfNoReadAccess(CaseUpdateViewEvent caseEventTrigger,
                                                   List<String> filteredCaseFieldIds) {
        caseEventTrigger.getCaseFields()
            .removeIf(caseViewField -> filteredCaseFieldIds.contains(caseViewField.getId()));
        caseEventTrigger.setWizardPages(
            filterWizardPageFields(caseEventTrigger, filteredCaseFieldIds));
    }

    private boolean hasCaseFieldAccess(List<CaseFieldDefinition> caseFieldDefinitions,
                                       Set<AccessProfile> accessProfiles,
                                       Predicate<AccessControlList> criteria,
                                       String fieldName) {
        if (caseFieldDefinitions.isEmpty()) {
            return true;
        }

        Optional<CaseFieldDefinition> matchedField = caseFieldDefinitions.stream()
                .filter(caseField -> caseField.getId().equals(fieldName))
                .findFirst();
        if (matchedField.isEmpty()) {
            LOG.error("No matching caseField={} in caseFieldDefinitions", fieldName);
            return false;
        } else if (hasAccessControlList(accessProfiles, criteria, matchedField.get().getAccessControlLists())) {
            return true;
        }
        LOG.error(NO_ROLE_FOUND, "caseField",
                fieldName,
                AccessControlService.extractAccessProfileNames(accessProfiles),
                "caseFieldACL",
                getCaseFieldAcls(caseFieldDefinitions, fieldName));
        return false;
    }

    private boolean valueDifferentAndHasUpdateAccess(JsonNode newData,
                                                     JsonNode existingData,
                                                     String newFieldName,
                                                     List<CaseFieldDefinition> caseFieldDefinitions,
                                                     Set<AccessProfile> accessProfiles) {
        if (existingData.get(newFieldName).equals(newData.get(newFieldName))) {
            return true;
        }
        Optional<CaseFieldDefinition> fieldOptional = getCaseFieldType(caseFieldDefinitions, newFieldName);
        if (fieldOptional.isPresent()) {
            CaseFieldDefinition caseField = fieldOptional.get();
            if (!caseField.isCompoundFieldType()) {
                return hasCaseFieldAccess(caseFieldDefinitions, accessProfiles, CAN_UPDATE, newFieldName);
            } else {
                return compoundAccessControlService.hasAccessForAction(newData,
                    existingData,
                    caseField,
                    accessProfiles);
            }
        } else {
            AccessControlServiceImpl.LOG.error("Data submitted for unknown field '{}'", newFieldName);
            return false;
        }
    }

}
