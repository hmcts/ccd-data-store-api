package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTrigger;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterUtil;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageComplexFieldOverride;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageField;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField.MANDATORY;
import static uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField.OPTIONAL;
import static uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField.READONLY;
import static uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterCollectionOptions.ALLOW_DELETE;
import static uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterCollectionOptions.ALLOW_INSERT;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COMPLEX;

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
    public static final String VALUE = "value";

    private final CompoundAccessControlService compoundAccessControlService;

    public AccessControlService(final CompoundAccessControlService compoundAccessControlService) {
        this.compoundAccessControlService = compoundAccessControlService;
    }


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

    public boolean canAccessCaseViewFieldWithCriteria(final CommonField caseViewField,
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
                        return !valueDifferentAndHasUpdateAccess(newData, existingData, newFieldName, caseFieldDefinitions, userRoles);
                    } else {
                        return !hasCaseFieldAccess(caseFieldDefinitions, userRoles, CAN_CREATE, newFieldName);
                    }
                });
            return !noAccessGranted;
        }
        return true;
    }

    public JsonNode filterCaseFieldsByAccess(final JsonNode caseFields, final List<CaseField> caseFieldDefinitions,
                                             final Set<String> userRoles, final Predicate<AccessControlList> access,
                                             boolean isClassification) {
        ObjectNode filteredCaseFields = JSON_NODE_FACTORY.objectNode();
        getStream(caseFields).forEach(
            fieldName -> findCaseFieldAndVerifyHasAccess(fieldName, caseFieldDefinitions, userRoles, access)
                .ifPresent(caseField -> {
                    if (isEmpty(caseField.getComplexACLs())) {
                        filteredCaseFields.set(fieldName, caseFields.get(fieldName));
                    } else if (!isClassification) {
                        filteredCaseFields.set(fieldName, filterChildren(caseField, caseFields.get(fieldName), userRoles, access, isClassification));
                    }
                })
        );
        return filteredCaseFields;
    }

    private Optional<CaseField> findCaseFieldAndVerifyHasAccess(final String fieldName, final List<CaseField> caseFieldDefinitions, final Set<String> userRoles, final Predicate<AccessControlList> access) {
        return caseFieldDefinitions.stream().filter(caseField -> caseField.getId().equals(fieldName) && hasAccessControlList(userRoles, access, caseField.getAccessControlLists())).findFirst();
    }

    private JsonNode filterChildren(final CaseField caseField, final JsonNode jsonNode, final Set<String> userRoles,
                                    final Predicate<AccessControlList> access, boolean isClassification) {
        if (caseField.isCompoundFieldType()) {
            caseField.getFieldType().getChildren().stream().forEach(childField -> {
                if (!hasAccessControlList(userRoles, access, childField.getAccessControlLists())) {
                    locateAndRemoveChildNode(caseField, jsonNode, childField);
                } else {
                    if (childField.isCollectionFieldType()) {
                        traverseAndFilterCollectionChildField(caseField, jsonNode, userRoles, access, isClassification, childField);
                    } else if (childField.isComplexFieldType()) {
                        traverseAndFilterComplexChildField(caseField, jsonNode, userRoles, access, isClassification, childField);
                    }
                }
            });
        }
        return jsonNode;
    }

    private void traverseAndFilterComplexChildField(final CaseField caseField, final JsonNode jsonNode, final Set<String> userRoles, final Predicate<AccessControlList> access, final boolean isClassification, final CaseField childField) {
        if (caseField.isCollectionFieldType() && jsonNode.isArray()) {
            jsonNode.forEach(caseFieldValueJsonNode -> {
                if (caseFieldValueJsonNode.get(VALUE).get(childField.getId()) != null) {
                    filterChildren(childField, caseFieldValueJsonNode.get(VALUE).get(childField.getId()), userRoles, access, isClassification);
                }
            });
        } else {
            filterChildren(childField, jsonNode.path(childField.getId()), userRoles, access, isClassification);
        }
    }

    private void traverseAndFilterCollectionChildField(final CaseField caseField, final JsonNode jsonNode, final Set<String> userRoles, final Predicate<AccessControlList> access, final boolean isClassification, final CaseField childField) {
        if (caseField.isCollectionFieldType() && jsonNode.isArray()) {
            jsonNode.forEach(caseFieldValueJsonNode -> {
                if (caseFieldValueJsonNode.get(VALUE).get(childField.getId()) != null) {
                    caseFieldValueJsonNode.get(VALUE).get(childField.getId()).forEach(childFieldValueJsonNode -> filterChildren(childField, childFieldValueJsonNode.get(VALUE), userRoles, access, isClassification));
                }
            });
        } else {
            jsonNode.path(childField.getId()).forEach(childJsonNode -> filterChildren(childField, childJsonNode.get(VALUE), userRoles, access, isClassification));
        }
    }

    private void locateAndRemoveChildNode(final CaseField caseField, final JsonNode jsonNode, final CaseField childField) {
        if (caseField.isCollectionFieldType() && jsonNode.isArray()) {
            jsonNode.forEach(jsonNode1 -> ((ObjectNode) jsonNode1.get(VALUE)).remove(childField.getId()));
        } else {
            ((ObjectNode) jsonNode).remove(childField.getId());
        }
    }

    public CaseEventTrigger setReadOnlyOnCaseViewFieldsIfNoAccess(final CaseEventTrigger caseEventTrigger,
                                                                  final List<CaseField> caseFieldDefinitions,
                                                                  final Set<String> userRoles,
                                                                  final Predicate<AccessControlList> access) {
        caseEventTrigger.getCaseFields().stream()
            .forEach(caseViewField -> {
                Optional<CaseField> caseFieldOpt = findCaseField(caseFieldDefinitions, caseViewField.getId());

                if (caseFieldOpt.isPresent()) {
                    CaseField field = caseFieldOpt.get();
                    if (!hasAccessControlList(userRoles, access, field.getAccessControlLists())) {
                        caseViewField.setDisplayContext(READONLY);
                    }
                    if (field.isCompoundFieldType()) {
                        setChildrenAsReadOnlyIfNoAccess(caseEventTrigger.getWizardPages(), field.getId(), field, access, userRoles, caseViewField);
                    }
                } else {
                    caseViewField.setDisplayContext(READONLY);
                }
            });
        return caseEventTrigger;
    }

    public CaseEventTrigger updateCollectionDisplayContextParameterByAccess(final CaseEventTrigger caseEventTrigger,
                                                                            final Set<String> userRoles) {
        caseEventTrigger.getCaseFields().stream().filter(CommonField::isCollectionFieldType)
            .forEach(caseViewField -> caseViewField.setDisplayContextParameter(generateDisplayContextParamer(userRoles, caseViewField)));

        caseEventTrigger.getCaseFields().forEach(caseViewField ->
            setChildrenCollectionDisplayContextParameter(caseViewField.getFieldType().getChildren(), userRoles));

        return caseEventTrigger;
    }

    private void setChildrenCollectionDisplayContextParameter(final List<CaseField> caseFields,
                                                              final Set<String> userRoles) {
        caseFields.stream().filter(CommonField::isCollectionFieldType)
            .forEach(childField -> childField.setDisplayContextParameter(generateDisplayContextParamer(userRoles, childField)));

        caseFields.forEach(childField -> {
            setChildrenCollectionDisplayContextParameter(childField.getFieldType().getChildren(), userRoles);
        });
    }

    private String generateDisplayContextParamer(Set<String> userRoles, CommonField field) {
        List<String> collectionAccess = new ArrayList<>();
        if (hasAccessControlList(userRoles, CAN_CREATE, field.getAccessControlLists())) {
            collectionAccess.add(ALLOW_INSERT.getOption());
        }
        if (hasAccessControlList(userRoles, CAN_DELETE, field.getAccessControlLists())) {
            collectionAccess.add(ALLOW_DELETE.getOption());
        }
        if (hasAccessControlList(userRoles, CAN_UPDATE, field.getAccessControlLists())) {
            collectionAccess.add(ALLOW_INSERT.getOption());
            collectionAccess.add(ALLOW_DELETE.getOption());
        }

        return DisplayContextParameterUtil.updateCollectionDisplayContextParameter(field.getDisplayContextParameter(), collectionAccess);
    }

    private void setChildrenAsReadOnlyIfNoAccess(final List<WizardPage> wizardPages, final String rootFieldId, final CaseField caseField, final Predicate<AccessControlList> access, final Set<String> userRoles, final CommonField caseViewField) {
        if (caseField.isCompoundFieldType()) {
            caseField.getFieldType().getChildren().stream().forEach(childField -> {
                if (!hasAccessControlList(userRoles, access, childField.getAccessControlLists())) {
                    findNestedField(caseViewField, childField.getId()).setDisplayContext(READONLY);
                    Optional<WizardPageField> optionalWizardPageField = getWizardPageField(wizardPages, rootFieldId);
                    if (optionalWizardPageField.isPresent()) {
                        setOverrideAsReadOnlyIfNotReadOnly(optionalWizardPageField.get(), rootFieldId, childField);
                    }
                }
                if (childField.isCompoundFieldType()) {
                    setChildrenAsReadOnlyIfNoAccess(wizardPages, rootFieldId, childField, access, userRoles, findNestedField(caseViewField, childField.getId()));
                }
            });
        }
    }

    private void setOverrideAsReadOnlyIfNotReadOnly(final WizardPageField wizardPageField, final String rootFieldId, final CaseField field) {
        final Optional<WizardPageComplexFieldOverride> fieldOverrideOptional = getWizardPageComplexFieldOverride(wizardPageField, rootFieldId, field);
        if (fieldOverrideOptional.isPresent()) {
            WizardPageComplexFieldOverride override = fieldOverrideOptional.get();
            if (MANDATORY.equalsIgnoreCase(override.getDisplayContext()) || OPTIONAL.equalsIgnoreCase(override.getDisplayContext())) {
                override.setDisplayContext(READONLY);
            }
        }
    }

    private Optional<WizardPageComplexFieldOverride> getWizardPageComplexFieldOverride(final WizardPageField wizardPageField, final String rootFieldId, final CaseField field) {
        return wizardPageField.getComplexFieldOverrides()
                .stream()
                .filter(wpcfo -> wpcfo.getComplexFieldElementId().startsWith(rootFieldId)
                    && wpcfo.getComplexFieldElementId().contains("." + field.getId()))
                .findFirst();
    }

    private Optional<WizardPageField> getWizardPageField(final List<WizardPage> wizardPages, final String rootFieldId) {
        return wizardPages.stream()
            .filter(wizardPage -> wizardPage.getWizardPageFields().stream().anyMatch(wizardPageField -> wizardPageField.getCaseFieldId().equalsIgnoreCase(rootFieldId)))
            .map(wizardPage -> wizardPage.getWizardPageFields().stream().filter(wizardPageField -> wizardPageField.getCaseFieldId().equalsIgnoreCase(rootFieldId)).findFirst().get())
            .findFirst();
    }

    public CaseEventTrigger filterCaseViewFieldsByAccess(final CaseEventTrigger caseEventTrigger,
                                                         final List<CaseField> caseFieldDefinitions,
                                                         final Set<String> userRoles,
                                                         final Predicate<AccessControlList> access) {
        List<String> filteredCaseFieldIds = new ArrayList<>();
        caseEventTrigger.setCaseFields(caseEventTrigger.getCaseFields()
            .stream()
            .filter(caseViewField -> {
                Optional<CaseField> caseFieldOpt = findCaseField(caseFieldDefinitions, caseViewField.getId());

                if (caseFieldOpt.isPresent()) {
                    CaseField cf = caseFieldOpt.get();
                    if (!hasAccessControlList(userRoles, access, cf.getAccessControlLists())) {
                        filteredCaseFieldIds.add(caseViewField.getId());
                        return false;
                    }
                    if (!isEmpty(cf.getComplexACLs())) {
                        cf.getFieldType().getChildren().stream().forEach(caseField -> filterChildren(caseField, caseViewField, userRoles, access));
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

    private Optional<CaseField> findCaseField(final List<CaseField> caseFieldDefinitions, final String caseViewFieldId) {
        return caseFieldDefinitions.stream()
            .filter(caseField -> caseField.getId().equals(caseViewFieldId))
            .findAny();
    }

    private Optional<CaseViewField> findCaseViewField(final List<CaseViewField> caseFieldDefinitions, final String caseViewFieldId) {
        return caseFieldDefinitions.stream()
            .filter(caseField -> caseField.getId().equals(caseViewFieldId))
            .findAny();
    }

    private void filterChildren(final CaseField caseField, CommonField caseViewField,
                                final Set<String> userRoles,
                                final Predicate<AccessControlList> access) {
        if (!hasAccessControlList(userRoles, access, caseField.getAccessControlLists())) {
            locateAndRemoveCaseField(caseField, caseViewField);
        } else if (caseField.isCompoundFieldType()) {
            caseField.getFieldType().getChildren().stream().forEach(childField -> {
                if (!hasAccessControlList(userRoles, access, childField.getAccessControlLists())) {
                    locateAndRemoveChildField(findNestedField(caseViewField, caseField.getId()), childField, caseField.isCollectionFieldType());
                } else if (childField.isCompoundFieldType()) {
                    traverseAndFilterCompoundChildField(findNestedField(caseViewField, caseField.getId()), userRoles, access, childField);
                }
            });
        }
    }

    private void traverseAndFilterCompoundChildField(final CommonField caseViewField, final Set<String> userRoles, final Predicate<AccessControlList> access, final CaseField childField) {
        if (childField.isCollectionFieldType()) {
            childField.getFieldType().getCollectionFieldType().getComplexFields().forEach(subField -> filterChildren(subField, findNestedField(caseViewField, childField.getId()), userRoles, access));
        } else if (childField.isComplexFieldType()) {
            childField.getFieldType().getComplexFields().forEach(subField -> filterChildren(subField, findNestedField(caseViewField, childField.getId()), userRoles, access));
        }
    }

    private CommonField findNestedField(final CommonField caseViewField, final String childFieldId) {
        return caseViewField.getComplexFieldNestedField(childFieldId)
            .orElseThrow(() -> new BadRequestException(format("CaseViewField %s has no nested elements with code %s.", caseViewField.getId(), childFieldId)));
    }

    private void locateAndRemoveChildField(final CommonField caseViewField, final CaseField childField, final boolean isCollection) {
        if (isCollection) {
            caseViewField.getFieldType().getCollectionFieldType().getComplexFields().remove(findNestedField(caseViewField, childField.getId()));
        } else {
            caseViewField.getFieldType().getComplexFields().remove(findNestedField(caseViewField, childField.getId()));
        }
    }

    private void locateAndRemoveCaseField(final CaseField caseField, final CommonField caseViewField) {
        if (caseViewField.isCollectionFieldType()) {
            caseViewField.getFieldType().getCollectionFieldType().getComplexFields().remove(findNestedField(caseViewField, caseField.getId()));
        } else {
            caseViewField.getFieldType().getComplexFields().remove(findNestedField(caseViewField, caseField.getId()));
        }
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
                    .map(wizardPageField -> {
                        if (!wizardPageField.getComplexFieldOverrides().isEmpty()) {
                            wizardPageField.setComplexFieldOverrides(filterMissingOverrides(wizardPageField.getComplexFieldOverrides(), wizardPageField.getCaseFieldId(), caseEventTrigger));
                        }
                        return wizardPageField;
                    })
                    .collect(toList()));
                return wizardPage;
            })
            .collect(toList());
    }

    private List<WizardPageComplexFieldOverride> filterMissingOverrides(List<WizardPageComplexFieldOverride> overrides, String fieldId, final CaseEventTrigger caseEventTrigger) {
        return overrides
            .stream()
            .filter(o -> {
                Optional<CaseViewField> optionalCaseViewField = findCaseViewField(caseEventTrigger.getCaseFields(), fieldId);
                if (optionalCaseViewField.isPresent()) {
                    return optionalCaseViewField.get().getComplexFieldNestedField(o.getComplexFieldElementId().replace(fieldId + ".", "")).isPresent();
                } else
                    return false;
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
                .map(caseField -> checkIfChildFilteringRequired(caseField, userRoles, access))
                .collect(toList());

        }
        return filteredCaseFields;
    }

    private CaseField checkIfChildFilteringRequired(final CaseField caseField, final Set<String> userRoles, final Predicate<AccessControlList> access) {
        return (caseField.isCompoundFieldType() && !caseField.getComplexACLs().isEmpty()) ? determineFieldTypeAndCheckChildAccess(caseField, userRoles, access) : caseField;
    }

    private CaseField determineFieldTypeAndCheckChildAccess(final CaseField caseField, final Set<String> userRoles, final Predicate<AccessControlList> access) {
        if (caseField.getFieldType().getType().equalsIgnoreCase(COMPLEX)) {
            caseField.getFieldType().setComplexFields(checkSubFieldsAccess(caseField, userRoles, access));
        } else {
            caseField.getFieldType().getCollectionFieldType().setComplexFields(checkSubFieldsAccess(caseField, userRoles, access));
        }
        return caseField;
    }

    private List<CaseField> checkSubFieldsAccess(CaseField caseField, final Set<String> userRoles, final Predicate<AccessControlList> access) {
        return caseField.getFieldType()
            .getChildren()
            .stream()
            .filter(childField -> hasAccessControlList(userRoles, access, childField.getAccessControlLists()))
            .map(subField -> subField.isCompoundFieldType() ? determineFieldTypeAndCheckChildAccess(subField, userRoles, access) : subField)
            .collect(toList());
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

    private boolean valueDifferentAndHasUpdateAccess(JsonNode newData, JsonNode existingData, String newFieldName, final List<CaseField> caseFieldDefinitions, final Set<String> userRoles) {
        if (existingData.get(newFieldName).equals(newData.get(newFieldName))) {
            return true;
        }
        Optional<CaseField> fieldOptional = getCaseFieldType(caseFieldDefinitions, newFieldName);
        if (fieldOptional.isPresent()) {
            CaseField caseField = fieldOptional.get();
            if (!caseField.isCompoundFieldType()) {
                return hasCaseFieldAccess(caseFieldDefinitions, userRoles, CAN_UPDATE, newFieldName);
            } else {
                return compoundAccessControlService.hasAccessForAction(newData, existingData, caseField, userRoles);
            }
        } else {
            LOG.error("Data submitted for unknown field '{}'", newFieldName);
            return false;
        }
    }

    private Stream<String> getStream(JsonNode newData) {
        return StreamSupport.stream(spliteratorUnknownSize(newData.fieldNames(), Spliterator.ORDERED), false);
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

    static boolean hasCaseFieldAccess(List<CaseField> caseFieldDefinitions, Set<String> userRoles, Predicate<AccessControlList> criteria, String fieldName) {
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

    private Optional<CaseField> getCaseFieldType(List<CaseField> caseFieldDefinitions, String fieldName) {
        return caseFieldDefinitions
            .stream()
            .filter(caseField -> nonNull(caseField.getAccessControlLists()) && caseField.getId().equals(fieldName))
            .findAny();
    }

    static List<AccessControlList> getCaseFieldAcls(List<CaseField> caseFieldDefinitions, String fieldName) {
        return caseFieldDefinitions
            .stream()
            .filter(caseField -> nonNull(caseField.getAccessControlLists()) && caseField.getId().equals(fieldName))
            .map(CaseField::getAccessControlLists)
            .findAny().orElse(newArrayList());
    }

    static boolean hasAccessControlList(Set<String> userRoles, Predicate<AccessControlList> criteria, List<AccessControlList> accessControlLists) {
        // scoop out access control roles based on user roles
        // intersect and make sure we have access for given criteria
        return accessControlLists != null && accessControlLists
            .stream()
            .filter(acls -> userRoles.contains(acls.getRole()))
            .anyMatch(criteria);
    }
}
