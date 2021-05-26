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
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewActionableEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
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


    private final CompoundAccessControlService compoundAccessControlService;

    public AccessControlServiceImpl(final CompoundAccessControlService compoundAccessControlService) {
        this.compoundAccessControlService = compoundAccessControlService;
    }

    @Override
    public boolean canAccessCaseTypeWithCriteria(final CaseTypeDefinition caseType,
                                                 final Set<String> userRoles,
                                                 final Predicate<AccessControlList> criteria) {
        boolean hasAccess = caseType != null
            && hasAccessControlList(userRoles, criteria, getAccessControlList(caseType));

        if (!hasAccess) {
            LOG.debug("No relevant case type access for caseType={}, caseTypeACLs={}, userRoles={}",
                     caseType != null ? caseType.getId() : "",
                     getAccessControlList(caseType),
                     userRoles);
        }

        return hasAccess;
    }

    @Override
    public boolean canAccessCaseStateWithCriteria(final String caseState,
                                                  final CaseTypeDefinition caseType,
                                                  final Set<String> userRoles,
                                                  final Predicate<AccessControlList> criteria) {

        List<AccessControlList> stateACLs = caseType.getStates()
            .stream()
            .filter(cState -> cState.getId().equalsIgnoreCase(caseState))
            .map(cState -> getAccessControlList(caseType, cState))
            .flatMap(Collection::stream)
            .collect(toList());

        boolean hasAccess = hasAccessControlList(userRoles, criteria, stateACLs);

        if (!hasAccess) {
            LOG.debug("No relevant case state access for caseState={}, caseStateACL={}, userRoles={}",
                     caseState,
                     stateACLs,
                     userRoles);
        }
        return hasAccess;
    }

    @Override
    public boolean canAccessCaseEventWithCriteria(final String eventId,
                                                  final List<CaseEventDefinition> caseEventDefinitions,
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

    @Override
    public boolean canAccessCaseFieldsWithCriteria(final JsonNode caseFields,
                                                   final List<CaseFieldDefinition> caseFieldDefinitions,
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

    @Override
    public boolean canAccessCaseViewFieldWithCriteria(final CommonField caseViewField,
                                                      final Set<String> userRoles,
                                                      final Predicate<AccessControlList> criteria) {
        return hasAccessControlList(userRoles, criteria, getAccessControlList(caseViewField));
    }

    @Override
    public boolean canAccessCaseFieldsForUpsert(final JsonNode newData,
                                                final JsonNode existingData,
                                                final List<CaseFieldDefinition> caseFieldDefinitions,
                                                final Set<String> userRoles) {
        if (newData != null) {
            final boolean noAccessGranted = getStream(newData)
                .anyMatch(newFieldName -> {
                    if (existingData.has(newFieldName)) {
                        return !valueDifferentAndHasUpdateAccess(newData, existingData, newFieldName,
                            caseFieldDefinitions, userRoles);
                    } else {
                        return !hasCaseFieldAccess(caseFieldDefinitions, userRoles, CAN_CREATE, newFieldName);
                    }
                });
            return !noAccessGranted;
        }
        return true;
    }

    @Override
    public JsonNode filterCaseFieldsByAccess(final JsonNode caseFields,
                                             final List<CaseFieldDefinition> caseFieldDefinitions,
                                             final Set<String> userRoles,
                                             final Predicate<AccessControlList> access,
                                             boolean isClassification) {
        ObjectNode filteredCaseFields = JSON_NODE_FACTORY.objectNode();
        getStream(caseFields).forEach(
            fieldName -> findCaseFieldAndVerifyHasAccess(fieldName, caseFieldDefinitions, userRoles, access)
                .ifPresent(caseField -> {
                    if (isEmpty(getAccessControlList(caseField))) {
                        filteredCaseFields.set(fieldName, caseFields.get(fieldName));
                    } else if (!isClassification) {
                        filteredCaseFields.set(
                            fieldName,
                            filterChildrenUsingJsonNode(caseField, caseFields.get(fieldName), userRoles,
                                access, isClassification)
                        );
                    }
                })
        );
        return filteredCaseFields;
    }

    @Override
    public List<CaseFieldDefinition> filterCaseFieldsByAccess(final List<CaseFieldDefinition> caseFieldDefinitions,
                                                              final Set<String> userRoles,
                                                              final Predicate<AccessControlList> access) {
        List<CaseFieldDefinition> filteredCaseFields = newArrayList();
        if (caseFieldDefinitions != null) {
            filteredCaseFields = caseFieldDefinitions
                .stream()
                .filter(caseField -> caseField.isMetadata() || hasAccessControlList(userRoles,
                    access,
                    getAccessControlList(caseField)))
                .map(caseField -> checkIfChildFilteringRequired(caseField, userRoles, access))
                .collect(toList());

        }
        return filteredCaseFields;
    }

    @Override
    public CaseUpdateViewEvent setReadOnlyOnCaseViewFieldsIfNoAccess(
        final CaseUpdateViewEvent caseEventTrigger,
        final List<CaseFieldDefinition> caseFieldDefinitions,
        final Set<String> userRoles,
        final Predicate<AccessControlList> access) {
        caseEventTrigger.getCaseFields().stream()
            .forEach(caseViewField -> {
                Optional<CaseFieldDefinition> caseFieldOpt = findCaseField(caseFieldDefinitions, caseViewField.getId());

                if (caseFieldOpt.isPresent()) {
                    CaseFieldDefinition field = caseFieldOpt.get();
                    if (!hasAccessControlList(userRoles, access, getAccessControlList(field))) {
                        caseViewField.setDisplayContext(READONLY);
                    }
                    if (field.isCompoundFieldType()) {
                        setChildrenAsReadOnlyIfNoAccess(caseEventTrigger.getWizardPages(), field.getId(), field,
                            access, userRoles, caseViewField);
                    }
                } else {
                    caseViewField.setDisplayContext(READONLY);
                }
            });
        return caseEventTrigger;
    }

    @Override
    public CaseUpdateViewEvent updateCollectionDisplayContextParameterByAccess(final CaseUpdateViewEvent
                                                                                   caseEventTrigger,
                                                                               final Set<String> userRoles) {
        caseEventTrigger.getCaseFields().stream().filter(CommonField::isCollectionFieldType)
            .forEach(caseViewField ->
                caseViewField.setDisplayContextParameter(generateDisplayContextParamer(userRoles, caseViewField)));

        caseEventTrigger.getCaseFields().forEach(caseViewField ->
            setChildrenCollectionDisplayContextParameter(caseViewField.getFieldTypeDefinition().getChildren(),
                                                        userRoles));

        return caseEventTrigger;
    }

    @Override
    public CaseUpdateViewEvent filterCaseViewFieldsByAccess(final CaseUpdateViewEvent caseEventTrigger,
                                                            final List<CaseFieldDefinition> caseFieldDefinitions,
                                                            final Set<String> userRoles,
                                                            final Predicate<AccessControlList> access) {
        List<String> filteredCaseFieldIds = new ArrayList<>();
        caseEventTrigger.setCaseFields(caseEventTrigger.getCaseFields()
            .stream()
            .filter(caseViewField -> {
                Optional<CaseFieldDefinition> caseFieldOpt = findCaseField(caseFieldDefinitions, caseViewField.getId());

                if (caseFieldOpt.isPresent()) {
                    CaseFieldDefinition cf = caseFieldOpt.get();
                    if (!hasAccessControlList(userRoles, access, getAccessControlList(cf))) {
                        filteredCaseFieldIds.add(caseViewField.getId());
                        return false;
                    }
                    if (!isEmpty(cf.getComplexACLs())) {
                        cf.getFieldTypeDefinition().getChildren().stream().forEach(caseField ->
                            filterChildren(caseField, caseViewField, userRoles, access));
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

    @Override
    public List<CaseStateDefinition> filterCaseStatesByAccess(CaseTypeDefinition caseType,
                                                              final Set<String> userRoles,
                                                              final Predicate<AccessControlList> access) {
        return caseType.getStates()
            .stream()
            .filter(caseState -> hasAccessControlList(userRoles,
                access,
                getAccessControlList(caseType, caseState)))
            .collect(toList());
    }

    @Override
    public List<CaseEventDefinition> filterCaseEventsByAccess(CaseTypeDefinition caseTypeDefinition, final Set<String> userRoles,
                                                              final Predicate<AccessControlList> access) {
        return caseTypeDefinition.getEvents()
            .stream()
            .filter(caseEvent -> hasAccessControlList(userRoles,
                access,
                getAccessControlList(caseEvent)))
            .collect(toList());
    }

    @Override
    public CaseViewActionableEvent[] filterCaseViewTriggersByCreateAccess(
        final CaseViewActionableEvent[] caseViewTriggers,
        final List<CaseEventDefinition> caseEventDefinitions,
        final Set<String> userRoles) {
        return stream(caseViewTriggers)
            .filter(caseViewTrigger -> hasAccessControlList(userRoles,
                CAN_CREATE,
                getCaseEventById(caseEventDefinitions, caseViewTrigger)
                    .map(eventDef -> getAccessControlList(eventDef))
                    .orElse(newArrayList()))
            )
            .toArray(CaseViewActionableEvent[]::new);
    }

    private boolean hasCaseFieldAccess(List<CaseFieldDefinition> caseFieldDefinitions,
                                       Set<String> userRoles,
                                       Predicate<AccessControlList> criteria,
                                       String fieldName) {
        if (caseFieldDefinitions.isEmpty()) {
            return true;
        }
        for (CaseFieldDefinition caseField : caseFieldDefinitions) {
            if (caseField.getId().equals(fieldName)
                && hasAccessControlList(userRoles, criteria, getAccessControlList(caseField))) {
                return true;
            }
        }
        AccessControlServiceImpl.LOG.debug(
            "Field names do not match or no relevant field access for fieldName={}, "
                + "caseFieldDefinitions={}, userRoles={}",
            fieldName,
            getCaseFieldAcls(caseFieldDefinitions, fieldName),
            userRoles);
        return false;
    }

    private boolean valueDifferentAndHasUpdateAccess(JsonNode newData,
                                                     JsonNode existingData,
                                                     String newFieldName,
                                                     List<CaseFieldDefinition> caseFieldDefinitions,
                                                     Set<String> userRoles) {
        if (existingData.get(newFieldName).equals(newData.get(newFieldName))) {
            return true;
        }
        Optional<CaseFieldDefinition> fieldOptional = getCaseFieldType(caseFieldDefinitions, newFieldName);
        if (fieldOptional.isPresent()) {
            CaseFieldDefinition caseField = fieldOptional.get();
            if (!caseField.isCompoundFieldType()) {
                return hasCaseFieldAccess(caseFieldDefinitions, userRoles, CAN_UPDATE, newFieldName);
            } else {
                return compoundAccessControlService.hasAccessForAction(newData, existingData, caseField, userRoles);
            }
        } else {
            AccessControlServiceImpl.LOG.error("Data submitted for unknown field '{}'", newFieldName);
            return false;
        }
    }

}
