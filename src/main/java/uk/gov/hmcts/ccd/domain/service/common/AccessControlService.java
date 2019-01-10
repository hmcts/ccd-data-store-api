package uk.gov.hmcts.ccd.domain.service.common;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COLLECTION;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

@Service
public class AccessControlService {

    private static final Logger LOG = LoggerFactory.getLogger(AccessControlService.class);
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    public static final Predicate<AccessControlList> CAN_CREATE = AccessControlList::isCreate;
    public static final Predicate<AccessControlList> CAN_UPDATE = AccessControlList::isUpdate;
    public static final Predicate<AccessControlList> CAN_READ = AccessControlList::isRead;
    public static final Predicate<AccessControlList> CAN_DELETE = AccessControlList::isDelete;
    public static final String NO_CASE_TYPE_FOUND = "No case type found";
    public static final String NO_CASE_TYPE_FOUND_DETAILS = "Unable to find the case type, please try a search or return to the case list overview page.";
    public static final String NO_CASE_STATE_FOUND = "Invalid event";
    public static final String NO_EVENT_FOUND = "No event found";
    public static final String NO_FIELD_FOUND = "No field found";

    public boolean canAccessCaseTypeWithCriteria(final CaseType caseType,
                                                 final Set<String> userRoles,
                                                 final Predicate<AccessControlList> criteria) {
        boolean hasAccess = caseType != null
            && hasAccessControlList(userRoles, criteria, caseType.getAccessControlLists());

        if (!hasAccess) {
            LOG.debug("No relevant case type access for caseTypeACLs={}, userRoles={}",
                caseType != null ? caseType.getAccessControlLists() : newArrayList(),
                userRoles);
        }

        return hasAccess;
    }

    public boolean canAccessCaseStateWithCriteria(final String caseState,
                                                  final CaseType caseType,
                                                  final Set<String> userRoles,
                                                  final Predicate<AccessControlList> criteria) {
        boolean hasAccess = hasAccessControlList(userRoles, criteria, caseType.getStates()
            .stream()
            .filter(cState -> cState.getId().equalsIgnoreCase(caseState))
            .map(CaseState::getAccessControlLists)
            .flatMap(Collection::stream)
            .collect(toList()));

        if (!hasAccess) {
            LOG.debug("No relevant case state access for caseState= {}, caseTypeACLs={}, userRoles={}",
                caseState,
                caseType.getAccessControlLists(),
                userRoles);
        }
        return hasAccess;
    }

    public boolean canAccessCaseEventWithCriteria(final String eventId,
                                                  final List<CaseEvent> caseEventDefinitions,
                                                  final Set<String> userRoles,
                                                  final Predicate<AccessControlList> criteria) {
        boolean hasAccess = hasCaseEventAccess(eventId, caseEventDefinitions, userRoles, criteria);
        if (!hasAccess) {
            LOG.debug("No relevant event access for eventId={}, eventAcls={}, userRoles={}",
                eventId,
                getCaseEventAcls(caseEventDefinitions, eventId),
                userRoles);
        }
        return hasAccess;
    }

    public boolean canAccessCaseFieldsWithCriteria(final JsonNode caseFields,
                                                   final List<CaseField> caseFieldDefinitions,
                                                   final Set<String> userRoles,
                                                   final Predicate<AccessControlList> criteria) {
        if (caseFields != null) {
            final Iterator<String> fieldNames = caseFields.fieldNames();
            while (fieldNames.hasNext()) {
                if (!hasCaseFieldAccess(caseFieldDefinitions, userRoles, criteria, fieldNames.next())) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean canAccessCaseViewFieldWithCriteria(final CaseViewField caseViewField,
                                                      final Set<String> userRoles,
                                                      final Predicate<AccessControlList> criteria) {
        return hasAccessControlList(userRoles, criteria, caseViewField.getAccessControlLists());
    }

    public boolean canAccessCaseFieldsForUpsert(final JsonNode newData,
                                                final JsonNode existingData,
                                                final List<CaseField> caseFieldDefinitions,
                                                final Set<String> userRoles) {
        if (newData != null) {
            final boolean noAccessGranted = getStream(newData)
                .anyMatch(newFieldName -> {
                    if (existingData.has(newFieldName)) {
                        return !valueDifferentAndHasUpdateAccess(newData,
                            existingData,
                            newFieldName,
                            caseFieldDefinitions,
                            userRoles);
                    } else {
                        return !hasCreateAccess(newFieldName,
                            caseFieldDefinitions,
                            userRoles);
                    }
                });
            return !noAccessGranted;
        }
        return true;
    }

    public JsonNode filterCaseFieldsByAccess(final JsonNode caseFields,
                                             final List<CaseField> caseFieldDefinitions,
                                             final Set<String> userRoles,
                                             final Predicate<AccessControlList> access) {
        JsonNode filteredCaseFields = JSON_NODE_FACTORY.objectNode();
        if (caseFields != null) {
            final Iterator<String> fieldNames = caseFields.fieldNames();
            while (fieldNames.hasNext()) {
                final String fieldName = fieldNames.next();
                for (CaseField caseField : caseFieldDefinitions) {
                    if (caseField.getId().equals(fieldName)
                        && hasAccessControlList(userRoles, access, caseField.getAccessControlLists())) {
                        ((ObjectNode) filteredCaseFields).set(fieldName, caseFields.get(fieldName));
                    }
                }
            }
        }
        return filteredCaseFields;
    }

    public CaseEventTrigger setReadOnlyOnCaseViewFieldsIfNoAccess(final CaseEventTrigger caseEventTrigger,
                                                                  final List<CaseField> caseFieldDefinitions,
                                                                  final Set<String> userRoles,
                                                                  final Predicate<AccessControlList> access) {
        caseEventTrigger.getCaseFields().stream()
            .forEach(caseViewField -> {
                Optional<CaseField> caseFieldOpt = caseFieldDefinitions.stream()
                    .filter(caseField -> caseField.getId().equals(caseViewField.getId()))
                    .findAny();

                if (caseFieldOpt.isPresent()) {
                    if (!hasAccessControlList(userRoles, access, caseFieldOpt.get().getAccessControlLists())) {
                        caseViewField.setDisplayContext("READONLY");
                    }
                } else {
                    caseViewField.setDisplayContext("READONLY");
                }
            });
        return caseEventTrigger;
    }

    public CaseEventTrigger filterCaseViewFieldsByAccess(final CaseEventTrigger caseEventTrigger,
                                                           final List<CaseField> caseFieldDefinitions,
                                                           final Set<String> userRoles,
                                                           final Predicate<AccessControlList> access) {
        List<String> filteredCaseFieldIds = new ArrayList<>();
        caseEventTrigger.setCaseFields(caseEventTrigger.getCaseFields()
            .stream()
            .filter(caseViewField -> {
                Optional<CaseField> caseFieldOpt = caseFieldDefinitions.stream()
                    .filter(caseField -> caseField.getId().equals(caseViewField.getId()))
                    .findAny();

                if (caseFieldOpt.isPresent()) {
                    if (!hasAccessControlList(userRoles, access, caseFieldOpt.get().getAccessControlLists())) {
                        filteredCaseFieldIds.add(caseViewField.getId());
                        return false;
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

    private List<WizardPage> filterWizardPageFields(CaseEventTrigger caseEventTrigger, List<String> filteredCaseFieldIds) {
        return caseEventTrigger.getWizardPages()
            .stream()
            .map(wizardPage -> {
                wizardPage.setWizardPageFields(wizardPage.getWizardPageFields()
                    .stream()
                    .filter(wizardPageField -> {
                        final Optional<String> toBeRemovedField = filteredCaseFieldIds
                            .stream()
                            .filter(id -> id.equalsIgnoreCase(wizardPageField.getCaseFieldId()))
                            .findAny();
                        return !toBeRemovedField.isPresent();
                    })
                    .collect(toList()));
                return wizardPage;
            })
            .collect(toList());
    }

    public List<AuditEvent> filterCaseAuditEventsByReadAccess(final List<AuditEvent> auditEvents,
                                                              final List<CaseEvent> caseEventDefinitions,
                                                              final Set<String> userRoles) {
        List<AuditEvent> filteredAuditEvents = newArrayList();
        if (auditEvents != null) {
            filteredAuditEvents = auditEvents
                .stream()
                .filter(auditEvent -> hasCaseEventWithAccess(userRoles, auditEvent, caseEventDefinitions))
                .collect(toList());

        }
        return filteredAuditEvents;
    }

    public List<CaseState> filterCaseStatesByAccess(final List<CaseState> caseStateDefinitions,
                                                    final Set<String> userRoles,
                                                    final Predicate<AccessControlList> access) {
        return caseStateDefinitions
            .stream()
            .filter(caseState -> hasAccessControlList(userRoles,
                access,
                caseState.getAccessControlLists()))
            .collect(toList());
    }

    public List<CaseEvent> filterCaseEventsByAccess(final List<CaseEvent> caseEventDefinitions,
                                                     final Set<String> userRoles,
                                                     final Predicate<AccessControlList> access) {
        return caseEventDefinitions
                .stream()
                .filter(caseEvent -> hasAccessControlList(userRoles,
                                                          access,
                                                          caseEvent.getAccessControlLists()))
                .collect(toList());
    }

    public CaseViewTrigger[] filterCaseViewTriggersByCreateAccess(final CaseViewTrigger[] caseViewTriggers,
                                                                  final List<CaseEvent> caseEventDefinitions,
                                                                  final Set<String> userRoles) {
        return stream(caseViewTriggers)
            .filter(caseViewTrigger -> hasAccessControlList(userRoles,
                                                            CAN_CREATE,
                                                            getCaseEventById(caseEventDefinitions, caseViewTrigger)
                                                                .map(CaseEvent::getAccessControlLists)
                                                                .orElse(newArrayList()))
            )
            .toArray(CaseViewTrigger[]::new);
    }

    public List<CaseField> filterCaseFieldsByAccess(final List<CaseField> caseFieldDefinitions,
                                                    final Set<String> userRoles,
                                                    final Predicate<AccessControlList> access) {
        List<CaseField> filteredCaseFields = newArrayList();
        if (caseFieldDefinitions != null) {
            filteredCaseFields = caseFieldDefinitions
                .stream()
                .filter(caseField -> caseField.isMetadata() || hasAccessControlList(userRoles,
                                                                                    access,
                                                                                    caseField.getAccessControlLists()))
                .collect(toList());

        }
        return filteredCaseFields;
    }

    private Optional<CaseEvent> getCaseEventById(List<CaseEvent> caseEventDefinitions, CaseViewTrigger caseViewTrigger) {
        return caseEventDefinitions
            .stream()
            .filter(event -> hasEqualIds(caseViewTrigger, event))
            .findAny();
    }

    private boolean hasEqualIds(CaseViewTrigger caseViewTrigger, CaseEvent event) {
        return event.getId().equals(caseViewTrigger.getId());
    }

    private List<AccessControlList> getCaseEventAcls(List<CaseEvent> caseEventDefinitions, String eventId) {
        return caseEventDefinitions
            .stream()
            .filter(caseEventDef -> nonNull(caseEventDef.getAccessControlLists()) && caseEventDef.getId().equals(eventId))
            .map(CaseEvent::getAccessControlLists)
            .findAny().orElse(newArrayList());
    }


    private boolean hasCaseEventWithAccess(Set<String> userRoles, AuditEvent auditEvent, List<CaseEvent> caseEventDefinitions) {

        return caseEventDefinitions
            .stream()
            .anyMatch(caseEventDefinition ->
                auditEvent.getEventId().equals(caseEventDefinition.getId())
                    && hasAccessControlList(userRoles,
                    CAN_READ,
                    caseEventDefinition.getAccessControlLists()));
    }

    private boolean hasCreateAccess(String newFieldName, final List<CaseField> caseFieldDefinitions, final Set<String> userRoles) {
        return hasCaseFieldAccess(caseFieldDefinitions,
            userRoles,
            CAN_CREATE,
            newFieldName);
    }

    private boolean valueDifferentAndHasUpdateAccess(JsonNode newData, JsonNode existingData, String newFieldName, final List<CaseField> caseFieldDefinitions, final Set<String> userRoles) {
        if (existingData.get(newFieldName).equals(newData.get(newFieldName))) {
            return true;
        }
        Optional<FieldType> fieldType = getCaseFieldType(caseFieldDefinitions, newFieldName);
        if (!fieldType.isPresent()) {
            return false;
        }
        if (!fieldType.get().getType().equals(COLLECTION)) {
            return hasCaseFieldAccess(caseFieldDefinitions, userRoles, CAN_UPDATE, newFieldName);
        } else {
            return hasAccessForCollectionItemAction(newData, existingData, newFieldName, caseFieldDefinitions, userRoles);
        }
    }

    private boolean hasAccessForCollectionItemAction(JsonNode newData, JsonNode existingData, String newFieldName, List<CaseField> caseFieldDefinitions, Set<String> userRoles) {
        if (!itemAddedAndHasCreateAccess(newData, newFieldName, caseFieldDefinitions, userRoles)) {
            return false;
        }
        if (!itemDeletedAndHasDeleteAccess(existingData, newData, newFieldName, caseFieldDefinitions, userRoles)) {
            return false;
        }
        return itemUpdatedAndHasUpdateAccess(existingData, newData, newFieldName, caseFieldDefinitions, userRoles);
    }

    private boolean itemDeletedAndHasDeleteAccess(JsonNode existingData, JsonNode newData, String newFieldName,
                                                  List<CaseField> caseFieldDefinitions, Set<String> userRoles) {
        boolean containsDeletedItem = StreamSupport
            .stream(spliteratorUnknownSize(existingData.get(newFieldName).elements(), Spliterator.ORDERED), false)
            .anyMatch(oldItem -> itemMissing(oldItem, newData.get(newFieldName)));

        return !containsDeletedItem || hasCaseFieldAccess(caseFieldDefinitions, userRoles, CAN_DELETE, newFieldName);
    }

    private boolean itemMissing(JsonNode oldItem, JsonNode newValue) {
        boolean itemExists = StreamSupport.stream(spliteratorUnknownSize(newValue.elements(), Spliterator.ORDERED), false)
            .anyMatch(newItem -> !isNullId(newItem) && newItem.get("id").equals(oldItem.get("id")));
        return !itemExists;
    }
    
    private boolean itemUpdatedAndHasUpdateAccess(JsonNode existingData, JsonNode newData, String newFieldName,
                                                  List<CaseField> caseFieldDefinitions, Set<String> userRoles) {
        boolean containsUpdatedItem = StreamSupport
            .stream(spliteratorUnknownSize(existingData.get(newFieldName).elements(), Spliterator.ORDERED), false)
            .anyMatch(oldItem -> itemUpdated(oldItem, newData.get(newFieldName)));

        return !containsUpdatedItem || hasCaseFieldAccess(caseFieldDefinitions, userRoles, CAN_UPDATE, newFieldName);
    }

    private boolean itemUpdated(JsonNode oldItem, JsonNode newValue) {
        return StreamSupport.stream(spliteratorUnknownSize(newValue.elements(), Spliterator.ORDERED), false)
            .anyMatch(newItem -> {
                boolean itemExists = !isNullId(newItem) && newItem.get("id").equals(oldItem.get("id"));
                if (itemExists) {
                    return !newItem.equals(oldItem);
                }
                return false;
            });
    }

    private boolean itemAddedAndHasCreateAccess(JsonNode newData, String newFieldName, List<CaseField> caseFieldDefinitions, Set<String> userRoles) {
        JsonNode newValue = newData.get(newFieldName);
        boolean containsNewItem = StreamSupport.stream(spliteratorUnknownSize(newValue.elements(), Spliterator.ORDERED), false)
            .anyMatch(this::isNullId);
        return !containsNewItem || hasCreateAccess(newFieldName, caseFieldDefinitions, userRoles);
    }

    private boolean isNullId(JsonNode newItem) {
        return newItem.get("id") == null
            || newItem.get("id").equals(NullNode.getInstance())
            || newItem.get("id").asText().equalsIgnoreCase("null");
    }

    private Stream<String> getStream(JsonNode newData) {
        return StreamSupport.stream(
            spliteratorUnknownSize(
                newData.fieldNames(), Spliterator.ORDERED),
            false);
    }

    private boolean hasCaseEventAccess(String eventId, List<CaseEvent> caseEventDefinitions, Set<String> userRoles, Predicate<AccessControlList> criteria) {
        for (CaseEvent caseEvent : caseEventDefinitions) {
            if (caseEvent.getId().equals(eventId)
                && hasAccessControlList(userRoles, criteria, caseEvent.getAccessControlLists())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCaseFieldAccess(List<CaseField> caseFieldDefinitions, Set<String> userRoles, Predicate<AccessControlList> criteria, String fieldName) {
        if (caseFieldDefinitions.isEmpty()) {
            return true;
        }
        for (CaseField caseField : caseFieldDefinitions) {
            if (caseField.getId().equals(fieldName)
                && hasAccessControlList(userRoles, criteria, caseField.getAccessControlLists())) {
                return true;
            }
        }
        LOG.debug(
            "Field names do not match or no relevant field access for fieldName={}, caseFieldDefinitions={}, userRoles={}",
            fieldName,
            getCaseFieldAcls(caseFieldDefinitions, fieldName),
            userRoles);
        return false;
    }

    private Optional<FieldType> getCaseFieldType(List<CaseField> caseFieldDefinitions, String fieldName) {
        return caseFieldDefinitions
            .stream()
            .filter(caseField -> nonNull(caseField.getAccessControlLists()) && caseField.getId().equals(fieldName))
            .map(CaseField::getFieldType)
            .findAny();
    }

    private List<AccessControlList> getCaseFieldAcls(List<CaseField> caseFieldDefinitions, String fieldName) {
        return caseFieldDefinitions
            .stream()
            .filter(caseField -> nonNull(caseField.getAccessControlLists()) && caseField.getId().equals(fieldName))
            .map(CaseField::getAccessControlLists)
            .findAny().orElse(newArrayList());
    }

    private boolean hasAccessControlList(Set<String> userRoles, Predicate<AccessControlList> criteria, List<AccessControlList> accessControlLists) {
        // scoop out access control roles based on user roles
        // intersect and make sure we have access for given criteria
        return accessControlLists != null && accessControlLists
            .stream()
            .filter(acls -> userRoles.contains(acls.getRole()))
            .anyMatch(criteria);
    }

}
