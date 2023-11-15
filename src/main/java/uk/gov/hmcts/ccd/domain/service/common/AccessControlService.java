package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewActionableEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterUtil;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageComplexFieldOverride;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageField;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField.MANDATORY;
import static uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField.OPTIONAL;
import static uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField.READONLY;
import static uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterCollectionOptions.ALLOW_DELETE;
import static uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterCollectionOptions.ALLOW_INSERT;
import static uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterCollectionOptions.ALLOW_UPDATE;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;

public interface AccessControlService {

    Logger LOG = LoggerFactory.getLogger(AccessControlService.class);

    Predicate<AccessControlList> CAN_CREATE = AccessControlList::isCreate;
    Predicate<AccessControlList> CAN_UPDATE = AccessControlList::isUpdate;
    Predicate<AccessControlList> CAN_READ = AccessControlList::isRead;
    Predicate<AccessControlList> CAN_DELETE = AccessControlList::isDelete;
    String NO_CASE_TYPE_FOUND = "No case type found";
    String NO_CASE_TYPE_FOUND_DETAILS = "Unable to find the case type, please try a search or "
        + "return to the case list overview page.";
    String NO_CASE_STATE_FOUND = "Invalid event";
    String NO_EVENT_FOUND = "No event found";
    String NO_FIELD_FOUND = "No field found";
    String NO_ROLE_FOUND = "User doesn't have required access for {}={}. userRoles={}, {}={}";
    String VALUE = "value";
    String ALL = "*";

    static boolean hasAccess(Set<String> userRoles,
                             Predicate<AccessControlList> criteria,
                             List<AccessControlList> accessControlLists) {
        return accessControlLists != null && accessControlLists
            .stream()
            .filter(acls -> userRoles.contains(acls.getAccessProfile()))
            .anyMatch(criteria);
    }

    boolean canAccessCaseTypeWithCriteria(CaseTypeDefinition caseType,
                                          Set<AccessProfile> accessProfiles,
                                          Predicate<AccessControlList> criteria);

    boolean canAccessCaseStateWithCriteria(String caseState,
                                           CaseTypeDefinition caseType,
                                           Set<AccessProfile> accessProfiles,
                                           Predicate<AccessControlList> criteria);

    boolean canAccessCaseEventWithCriteria(String eventId,
                                           List<CaseEventDefinition> caseEventDefinitions,
                                           Set<AccessProfile> accessProfiles,
                                           Predicate<AccessControlList> criteria);

    boolean canAccessCaseFieldsWithCriteria(JsonNode caseFields,
                                            List<CaseFieldDefinition> caseFieldDefinitions,
                                            Set<AccessProfile> accessProfiles,
                                            Predicate<AccessControlList> criteria);

    boolean canAccessCaseViewFieldWithCriteria(CommonField caseViewField,
                                               Set<AccessProfile> accessProfiles,
                                               Predicate<AccessControlList> criteria);

    boolean canAccessCaseFieldsForUpsert(JsonNode newData,
                                         JsonNode existingData,
                                         List<CaseFieldDefinition> caseFieldDefinitions,
                                         Set<AccessProfile> accessProfiles);

    CaseUpdateViewEvent setReadOnlyOnCaseViewFieldsIfNoAccess(String caseTypeId, String caseReference, String eventId,
                                                              CaseUpdateViewEvent caseEventTrigger,
                                                              List<CaseFieldDefinition> caseFieldDefinitions,
                                                              Set<AccessProfile> accessProfiles,
                                                              Predicate<AccessControlList> access);

    CaseUpdateViewEvent updateCollectionDisplayContextParameterByAccess(CaseUpdateViewEvent
                                                                            caseEventTrigger,
                                                                        Set<AccessProfile> accessProfiles);

    JsonNode filterCaseFieldsByAccess(JsonNode caseFields,
                                      List<CaseFieldDefinition> caseFieldDefinitions,
                                      Set<AccessProfile> accessProfiles,
                                      Predicate<AccessControlList> access,
                                      boolean isClassification);

    List<CaseFieldDefinition> filterCaseFieldsByAccess(List<CaseFieldDefinition> caseFieldDefinitions,
                                                       Set<AccessProfile> accessProfiles,
                                                       Predicate<AccessControlList> access);

    CaseUpdateViewEvent filterCaseViewFieldsByAccess(CaseUpdateViewEvent caseEventTrigger,
                                                     List<CaseFieldDefinition> caseFieldDefinitions,
                                                     Set<AccessProfile> accessProfiles,
                                                     Predicate<AccessControlList> access);

    List<AuditEvent> filterCaseAuditEventsByReadAccess(List<AuditEvent> auditEvents,
                                                       List<CaseEventDefinition> caseEventDefinitions,
                                                       Set<AccessProfile> accessProfiles);

    List<CaseStateDefinition> filterCaseStatesByAccess(CaseTypeDefinition caseType,
                                                       Set<AccessProfile> accessProfiles,
                                                       Predicate<AccessControlList> access);

    List<CaseStateDefinition> filterCaseStatesByAccess(List<CaseStateDefinition> caseStates,
                                                       Set<AccessProfile> accessProfiles,
                                                       Predicate<AccessControlList> access);

    List<CaseEventDefinition> filterCaseEventsByAccess(CaseTypeDefinition caseTypeDefinition,
                                                       Set<AccessProfile> accessProfiles,
                                                       Predicate<AccessControlList> access);

    CaseViewActionableEvent[] filterCaseViewTriggersByCreateAccess(
        CaseViewActionableEvent[] caseViewTriggers,
        List<CaseEventDefinition> caseEventDefinitions,
        Set<AccessProfile> accessProfiles);

    default Optional<CaseFieldDefinition> findCaseFieldAndVerifyHasAccess(
        String fieldName, List<CaseFieldDefinition> caseFieldDefinitions,
        Set<AccessProfile> accessProfiles, Predicate<AccessControlList> access) {
        return caseFieldDefinitions.stream().filter(caseField ->
            caseField.getId().equals(fieldName)
                && hasAccessControlList(accessProfiles, access, caseField.getAccessControlLists())).findFirst();
    }

    default JsonNode filterChildrenUsingJsonNode(CaseFieldDefinition caseField,
                                                 JsonNode jsonNode,
                                                 Set<AccessProfile> accessProfiles,
                                                 Predicate<AccessControlList> access,
                                                 boolean isClassification) {
        if (caseField.isCompoundFieldType()) {
            caseField.getFieldTypeDefinition().getChildren().forEach(childField -> {
                if (!hasAccessControlList(accessProfiles, access, childField.getAccessControlLists())) {
                    locateAndRemoveChildNode(caseField, jsonNode, childField);
                } else {
                    if (childField.isCollectionFieldType()) {
                        traverseAndFilterCollectionChildField(caseField, jsonNode, accessProfiles, access,
                            isClassification, childField);
                    } else if (childField.isComplexFieldType()) {
                        traverseAndFilterComplexChildField(caseField, jsonNode, accessProfiles, access,
                            isClassification, childField);
                    }
                }
            });
        }
        return jsonNode;
    }

    default void traverseAndFilterComplexChildField(CaseFieldDefinition caseField,
                                                    JsonNode jsonNode,
                                                    Set<AccessProfile> accessProfiles,
                                                    Predicate<AccessControlList> access,
                                                    boolean isClassification,
                                                    CaseFieldDefinition childField) {
        if (caseField.isCollectionFieldType() && jsonNode.isArray()) {
            jsonNode.forEach(caseFieldValueJsonNode -> {
                if (caseFieldValueJsonNode.get(VALUE).get(childField.getId()) != null) {
                    filterChildrenUsingJsonNode(childField, caseFieldValueJsonNode.get(VALUE).get(childField.getId()),
                        accessProfiles, access, isClassification);
                }
            });
        } else {
            filterChildrenUsingJsonNode(childField, jsonNode.path(childField.getId()), accessProfiles, access,
                isClassification);
        }
    }

    default void traverseAndFilterCollectionChildField(CaseFieldDefinition caseField,
                                                       JsonNode jsonNode,
                                                       Set<AccessProfile> accessProfiles,
                                                       Predicate<AccessControlList> access,
                                                       boolean isClassification,
                                                       CaseFieldDefinition childField) {
        if (caseField.isCollectionFieldType() && jsonNode.isArray()) {
            jsonNode.forEach(caseFieldValueJsonNode -> {
                if (caseFieldValueJsonNode.get(VALUE).get(childField.getId()) != null) {
                    caseFieldValueJsonNode.get(VALUE).get(childField.getId()).forEach(childFieldValueJsonNode ->
                        filterChildrenUsingJsonNode(childField, childFieldValueJsonNode.get(VALUE),
                            accessProfiles, access,
                            isClassification));
                }
            });
        } else {
            jsonNode.path(childField.getId()).forEach(childJsonNode ->
                filterChildrenUsingJsonNode(childField, childJsonNode.get(VALUE),
                    accessProfiles, access, isClassification));
        }
    }

    default void locateAndRemoveChildNode(CaseFieldDefinition caseField, JsonNode jsonNode,
                                          CaseFieldDefinition childField) {
        if (caseField.isCollectionFieldType() && jsonNode.isArray()) {
            jsonNode.forEach(jsonNode1 -> ((ObjectNode) jsonNode1.get(VALUE)).remove(childField.getId()));
        } else {
            // We are getting class com.fasterxml.jackson.databind.node.MissingNode cannot be cast to class
            // com.fasterxml.jackson.databind.node.ObjectNode exception when childField.getId() is null, so this
            // is added to check if the value exists.
            if (jsonNode instanceof ObjectNode) {
                ((ObjectNode) jsonNode).remove(childField.getId());
            } else {
                LOG.debug("Logging below details as we are getting com.fasterxml.jackson.databind.node.MissingNode "
                    + " cannot be cast to class com.fasterxml.jackson.databind.node.ObjectNode");
                LOG.info("Can not find field with caseFieldId={}, accessControlList={}",
                    caseField.getId(), caseField.getAccessControlLists());
            }
        }
    }

    default void setChildrenCollectionDisplayContextParameter(List<CaseFieldDefinition> caseFields,
                                                              Set<AccessProfile> accessProfiles) {
        caseFields.stream().filter(CommonField::isCollectionFieldType)
            .forEach(childField ->
                childField.setDisplayContextParameter(generateDisplayContextParameter(accessProfiles, childField)));

        caseFields.forEach(childField ->
            setChildrenCollectionDisplayContextParameter(childField.getFieldTypeDefinition().getChildren(),
                accessProfiles));
    }

    default String generateDisplayContextParameter(Set<AccessProfile> accessProfiles, CommonField field) {
        List<String> collectionAccess = new ArrayList<>();
        if (hasAccessControlList(accessProfiles, CAN_CREATE, field.getAccessControlLists())) {
            collectionAccess.add(ALLOW_INSERT.getOption());
        }
        if (hasAccessControlList(accessProfiles, CAN_DELETE, field.getAccessControlLists())) {
            collectionAccess.add(ALLOW_DELETE.getOption());
        }
        if (hasAccessControlList(accessProfiles, CAN_UPDATE, field.getAccessControlLists())) {
            collectionAccess.add(ALLOW_UPDATE.getOption());
        }

        return DisplayContextParameterUtil.updateCollectionDisplayContextParameter(field.getDisplayContextParameter(),
            collectionAccess);
    }

    default boolean shouldRemoveCaseViewFieldIfNoReadAccess(final String caseTypeId,
                                                            String caseReference,
                                                            String eventId,
                                                            boolean isMultipartyFixEnabled,
                                                            List<String> multipartyCaseTypes,
                                                            List<String> multipartyEvents,
                                                            String caseViewFieldId,
                                                            CaseFieldDefinition field,
                                                            Set<AccessProfile> accessProfiles) {
        boolean isMultipartyCaseTypePresent = multipartyCaseTypes
            .stream().anyMatch(caseType -> caseType.equals(ALL) || caseType.equalsIgnoreCase(caseTypeId));
        boolean isMultipartyEventPresent = multipartyEvents
            .stream().anyMatch(event -> event.equals(ALL) || event.equalsIgnoreCase(eventId));

        if (isMultipartyFixEnabled
            && isMultipartyCaseTypePresent && isMultipartyEventPresent
            && (field == null || !hasAccessControlList(accessProfiles, CAN_READ, field.getAccessControlLists()))) {
            LOG.debug("Case view field {} has been removed for case {} and event {} as part of multiparty fix",
                caseViewFieldId, caseReference, eventId);
            return true;
        }

        return false;
    }

    default void setChildrenAsReadOnlyIfNoAccess(final String caseTypeId,
                                                 final String caseReference,
                                                 final String eventId,
                                                 boolean isMultipartyFixEnabled,
                                                 List<String> multipartyCaseTypes,
                                                 List<String> multipartyEvents,
                                                 List<WizardPage> wizardPages,
                                                 String rootFieldId,
                                                 CaseFieldDefinition caseField,
                                                 Predicate<AccessControlList> access,
                                                 Set<AccessProfile> accessProfiles,
                                                 CommonField caseViewField) {
        if (caseField.isCompoundFieldType()) {
            caseField.getFieldTypeDefinition().getChildren().forEach(childField -> {
                boolean shouldRemoveCaseViewField = false;

                if (!hasAccessControlList(accessProfiles, access, childField.getAccessControlLists())) {
                    CommonField childCaseViewField = findNestedField(caseViewField, childField.getId());
                    childCaseViewField.setDisplayContext(READONLY);

                    shouldRemoveCaseViewField = shouldRemoveCaseViewFieldIfNoReadAccess(caseTypeId, caseReference,
                        eventId, isMultipartyFixEnabled, multipartyCaseTypes, multipartyEvents,
                        childCaseViewField.getId(), childField, accessProfiles);
                    if (shouldRemoveCaseViewField) {
                        caseViewField.getFieldTypeDefinition().getChildren().remove(childCaseViewField);
                    }

                    Optional<WizardPageField> optionalWizardPageField = getWizardPageField(wizardPages, rootFieldId);
                    optionalWizardPageField.ifPresent(wizardPageField ->
                        setOverrideAsReadOnlyIfNotReadOnly(wizardPageField, rootFieldId, childField));
                }

                if (!shouldRemoveCaseViewField && childField.isCompoundFieldType()) {
                    setChildrenAsReadOnlyIfNoAccess(
                        caseTypeId,
                        caseReference,
                        eventId,
                        isMultipartyFixEnabled,
                        multipartyCaseTypes,
                        multipartyEvents,
                        wizardPages,
                        rootFieldId,
                        childField,
                        access,
                        accessProfiles,
                        findNestedField(caseViewField, childField.getId())
                    );
                }
            });
        }
    }

    default void setOverrideAsReadOnlyIfNotReadOnly(WizardPageField wizardPageField,
                                                    String rootFieldId,
                                                    CaseFieldDefinition field) {
        final Optional<WizardPageComplexFieldOverride> fieldOverrideOptional =
            getWizardPageComplexFieldOverride(wizardPageField, rootFieldId, field);
        if (fieldOverrideOptional.isPresent()) {
            WizardPageComplexFieldOverride override = fieldOverrideOptional.get();
            if (MANDATORY.equalsIgnoreCase(override.getDisplayContext())
                || OPTIONAL.equalsIgnoreCase(override.getDisplayContext())) {
                override.setDisplayContext(READONLY);
            }
        }
    }

    default Optional<WizardPageComplexFieldOverride> getWizardPageComplexFieldOverride(
        WizardPageField wizardPageField,
        String rootFieldId,
        CaseFieldDefinition field) {
        return wizardPageField.getComplexFieldOverrides()
            .stream()
            .filter(wpcfo -> wpcfo.getComplexFieldElementId().startsWith(rootFieldId)
                && wpcfo.getComplexFieldElementId().contains("." + field.getId()))
            .findFirst();
    }

    default Optional<WizardPageField> getWizardPageField(List<WizardPage> wizardPages, String rootFieldId) {
        return wizardPages.stream()
            .filter(wizardPage -> wizardPage.getWizardPageFields().stream().anyMatch(wizardPageField ->
                wizardPageField.getCaseFieldId().equalsIgnoreCase(rootFieldId)))
            .map(wizardPage -> wizardPage.getWizardPageFields().stream().filter(wizardPageField ->
                wizardPageField.getCaseFieldId().equalsIgnoreCase(rootFieldId)).findFirst().get())
            .findFirst();
    }

    default Optional<CaseFieldDefinition> findCaseField(List<CaseFieldDefinition> caseFieldDefinitions,
                                                        String caseViewFieldId) {
        return caseFieldDefinitions.stream()
            .filter(caseField -> caseField.getId().equals(caseViewFieldId))
            .findAny();
    }

    default Optional<CaseViewField> findCaseViewField(List<CaseViewField> caseFieldDefinitions,
                                                      String caseViewFieldId) {
        return caseFieldDefinitions.stream()
            .filter(caseField -> caseField.getId().equals(caseViewFieldId))
            .findAny();
    }

    default void filterChildren(CaseFieldDefinition caseField, CommonField caseViewField,
                                Set<AccessProfile> accessProfiles,
                                Predicate<AccessControlList> access) {
        if (!hasAccessControlList(accessProfiles, access, caseField.getAccessControlLists())) {
            locateAndRemoveCaseField(caseField, caseViewField);
        } else if (caseField.isCompoundFieldType()) {
            caseField.getFieldTypeDefinition().getChildren().forEach(childField -> {
                if (!hasAccessControlList(accessProfiles, access, childField.getAccessControlLists())) {
                    locateAndRemoveChildField(findNestedField(caseViewField, caseField.getId()), childField,
                        caseField.isCollectionFieldType());
                } else if (childField.isCompoundFieldType()) {
                    traverseAndFilterCompoundChildField(findNestedField(caseViewField, caseField.getId()),
                        accessProfiles, access, childField);
                }
            });
        }
    }

    default void traverseAndFilterCompoundChildField(CommonField caseViewField,
                                                     Set<AccessProfile> accessProfiles,
                                                     Predicate<AccessControlList> access,
                                                     CaseFieldDefinition childField) {
        if (childField.isCollectionFieldType()) {
            childField.getFieldTypeDefinition().getCollectionFieldTypeDefinition()
                .getComplexFields().forEach(subField ->
                filterChildren(subField, findNestedField(caseViewField, childField.getId()), accessProfiles, access));
        } else if (childField.isComplexFieldType()) {
            childField.getFieldTypeDefinition().getComplexFields().forEach(subField ->
                filterChildren(subField, findNestedField(caseViewField, childField.getId()), accessProfiles, access));
        }
    }

    default CommonField findNestedField(CommonField caseViewField, String childFieldId) {
        return caseViewField.getComplexFieldNestedField(childFieldId)
            .orElseThrow(() -> new BadRequestException(format("CaseViewField %s has no nested elements with code %s.",
                caseViewField.getId(), childFieldId)));
    }

    default void locateAndRemoveChildField(CommonField caseViewField,
                                           CaseFieldDefinition childField,
                                           boolean isCollection) {
        if (isCollection) {
            caseViewField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getComplexFields().remove(
                findNestedField(caseViewField, childField.getId()));
        } else {
            caseViewField.getFieldTypeDefinition().getComplexFields()
                .remove(findNestedField(caseViewField, childField.getId()));
        }
    }

    default void locateAndRemoveCaseField(CaseFieldDefinition caseField, CommonField caseViewField) {
        if (caseViewField.isCollectionFieldType()) {
            caseViewField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getComplexFields().remove(
                findNestedField(caseViewField, caseField.getId()));
        } else {
            caseViewField.getFieldTypeDefinition().getComplexFields()
                .remove(findNestedField(caseViewField, caseField.getId()));
        }
    }

    default List<WizardPage> filterWizardPageFields(CaseUpdateViewEvent caseEventTrigger,
                                                    List<String> filteredCaseFieldIds) {
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
                        return toBeRemovedField.isEmpty();
                    })
                    .map(wizardPageField -> {
                        if (!wizardPageField.getComplexFieldOverrides().isEmpty()) {
                            wizardPageField.setComplexFieldOverrides(
                                filterMissingOverrides(wizardPageField.getComplexFieldOverrides(),
                                    wizardPageField.getCaseFieldId(), caseEventTrigger)
                            );
                        }
                        return wizardPageField;
                    })
                    .collect(toList()));
                return wizardPage;
            })
            .collect(toList());
    }

    default List<WizardPageComplexFieldOverride> filterMissingOverrides(List<WizardPageComplexFieldOverride> overrides,
                                                                        String fieldId,
                                                                        CaseUpdateViewEvent caseEventTrigger) {
        return overrides
            .stream()
            .filter(o -> {
                Optional<CaseViewField> optionalCaseViewField = findCaseViewField(caseEventTrigger.getCaseFields(),
                                                                                  fieldId);
                return optionalCaseViewField
                    .map(caseViewField ->
                        caseViewField.getComplexFieldNestedField(o.getComplexFieldElementId()
                            .replace(fieldId + ".", "")).isPresent())
                    .orElse(false);
            })
            .collect(toList());
    }

    default CaseFieldDefinition checkIfChildFilteringRequired(CaseFieldDefinition caseField,
                                                              Set<AccessProfile> accessProfiles,
                                                              Predicate<AccessControlList> access) {
        return (caseField.isCompoundFieldType() && !caseField.getComplexACLs().isEmpty())
            ? determineFieldTypeAndCheckChildAccess(caseField, accessProfiles, access)
            : caseField;
    }

    default CaseFieldDefinition determineFieldTypeAndCheckChildAccess(CaseFieldDefinition caseField,
                                                                      Set<AccessProfile> accessProfiles,
                                                                      Predicate<AccessControlList> access) {
        if (caseField.getFieldTypeDefinition().getType().equalsIgnoreCase(COMPLEX)) {
            caseField
                .getFieldTypeDefinition()
                .setComplexFields(checkSubFieldsAccess(caseField, accessProfiles, access));
        } else {
            caseField.getFieldTypeDefinition().getCollectionFieldTypeDefinition()
                .setComplexFields(checkSubFieldsAccess(caseField, accessProfiles, access));
        }
        return caseField;
    }

    default List<CaseFieldDefinition> checkSubFieldsAccess(CaseFieldDefinition caseField,
                                                           Set<AccessProfile> accessProfiles,
                                                           Predicate<AccessControlList> access) {
        return caseField.getFieldTypeDefinition()
            .getChildren()
            .stream()
            .filter(childField -> hasAccessControlList(accessProfiles, access, childField.getAccessControlLists()))
            .map(subField -> subField.isCompoundFieldType()
                ? determineFieldTypeAndCheckChildAccess(subField, accessProfiles, access) : subField)
            .collect(toList());
    }

    default Optional<CaseEventDefinition> getCaseEventById(List<CaseEventDefinition> caseEventDefinitions,
                                                           CaseViewActionableEvent caseViewTrigger) {
        return caseEventDefinitions
            .stream()
            .filter(event -> hasEqualIds(caseViewTrigger, event))
            .findAny();
    }

    default boolean hasEqualIds(CaseViewActionableEvent caseViewTrigger, CaseEventDefinition event) {
        return event.getId().equals(caseViewTrigger.getId());
    }

    default List<AccessControlList> getCaseEventAcls(List<CaseEventDefinition> caseEventDefinitions, String eventId) {
        return caseEventDefinitions
            .stream()
            .filter(caseEventDef ->
                nonNull(caseEventDef.getAccessControlLists()) && caseEventDef.getId().equals(eventId))
            .map(CaseEventDefinition::getAccessControlLists)
            .findAny().orElse(newArrayList());
    }

    default boolean hasCaseEventWithAccess(Set<AccessProfile> accessProfiles, AuditEvent auditEvent,
                                           List<CaseEventDefinition> caseEventDefinitions) {

        return caseEventDefinitions
            .stream()
            .anyMatch(caseEventDefinition ->
                auditEvent.getEventId().equals(caseEventDefinition.getId())
                    && hasAccessControlList(accessProfiles,
                    CAN_READ,
                    caseEventDefinition.getAccessControlLists()));
    }

    default Stream<String> getStream(JsonNode newData) {
        return StreamSupport.stream(spliteratorUnknownSize(newData.fieldNames(), Spliterator.ORDERED), false);
    }

    default boolean hasCaseEventAccess(String eventId,
                                       List<CaseEventDefinition> caseEventDefinitions,
                                       Set<AccessProfile> accessProfiles,
                                       Predicate<AccessControlList> criteria) {
        Optional<CaseEventDefinition> matchedEvent = caseEventDefinitions.stream()
                .filter(caseEvent -> caseEvent.getId().equals(eventId))
                .findFirst();
        if (matchedEvent.isEmpty()) {
            LOG.error("No matching caseEvent={} in caseEventDefinitions", eventId);
            return false;
        } else if (hasAccessControlList(accessProfiles, criteria, matchedEvent.get().getAccessControlLists())) {
            return true;
        }

        LOG.error(NO_ROLE_FOUND, "caseEvent",
                eventId,
                extractAccessProfileNames(accessProfiles),
                "caseEventACL",
                getCaseEventAcls(caseEventDefinitions, eventId));
        return false;
    }

    default Optional<CaseFieldDefinition> getCaseFieldType(List<CaseFieldDefinition> caseFieldDefinitions,
                                                           String fieldName) {
        return caseFieldDefinitions
            .stream()
            .filter(caseField -> nonNull(caseField.getAccessControlLists()) && caseField.getId().equals(fieldName))
            .findAny();
    }

    default List<AccessControlList> getCaseFieldAcls(List<CaseFieldDefinition> caseFieldDefinitions, String fieldName) {
        return caseFieldDefinitions
            .stream()
            .filter(caseField -> nonNull(caseField.getAccessControlLists()) && caseField.getId().equals(fieldName))
            .map(CaseFieldDefinition::getAccessControlLists)
            .findAny().orElse(newArrayList());
    }

    default boolean hasAccessControlList(Set<AccessProfile> accessProfiles,
                                         Predicate<AccessControlList> criteria,
                                         List<AccessControlList> accessControlLists) {
        // scoop out access control roles based on user roles
        // intersect and make sure we have access for given criteria
        return hasAccessControlList(accessProfiles, accessControlLists, criteria);
    }

    static boolean hasAccessControlList(Set<AccessProfile> accessProfiles,
                                        List<AccessControlList> accessControlLists,
                                        Predicate<AccessControlList> criteria) {
        Set<String> accessProfileNames = extractAccessProfileNames(accessProfiles);
        return accessControlLists != null && accessControlLists
            .stream()
            .filter(acls -> accessProfileNames.contains(acls.getAccessProfile()))
            .anyMatch(criteria);
    }

    static Set<String> extractAccessProfileNames(Set<AccessProfile> accessProfiles) {
        return accessProfiles.stream()
            .map(AccessProfile::getAccessProfile)
            .collect(Collectors.toSet());
    }

}
