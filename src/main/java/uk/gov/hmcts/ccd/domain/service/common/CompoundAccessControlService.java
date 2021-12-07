package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import static java.util.Spliterators.spliteratorUnknownSize;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_DELETE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.VALUE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.hasAccessControlList;

@Service
public class CompoundAccessControlService {

    private static final Logger LOG = LoggerFactory.getLogger(CompoundAccessControlService.class);
    public static final String A_CHILD_OF_HAS_DATA_DELETE_WITHOUT_DELETE_ACL = "A child {} of {} has been deleted but"
        + " no Delete ACL";
    public static final String A_CHILD_OF_HAS_DATA_UPDATE_WITHOUT_UPDATE_ACL = "A child {} of {} has data update "
        + "without Update ACL";
    public static final String SIMPLE_CHILD_OF_HAS_DATA_UPDATE_BUT_NO_UPDATE_ACL = "Simple child {} of {} has data "
        + "update but no Update ACL";
    public static final String A_CHILD_HAS_DATA_UPDATE_WITHOUT_UPDATE_ACL = "A child of {} has data updated "
        + "without Update ACL";

    public boolean hasAccessForAction(final JsonNode newData,
                                      final JsonNode existingData,
                                      final CaseFieldDefinition caseFieldDefinition,
                                      final Set<AccessProfile> accessProfiles) {
        if (!itemAddedAndHasCreateAccess(newData, caseFieldDefinition, accessProfiles)) {
            return false;
        }
        if (!itemDeletedAndHasDeleteAccess(existingData, newData, caseFieldDefinition, accessProfiles)) {
            return false;
        }
        return itemUpdatedAndHasUpdateAccess(existingData, newData, caseFieldDefinition, accessProfiles);
    }

    private boolean itemAddedAndHasCreateAccess(JsonNode newData, CaseFieldDefinition caseFieldDefinition,
                                                Set<AccessProfile> accessProfiles) {
        if (caseFieldDefinition.isCollectionFieldType()) {
            final JsonNode jsonNode = newData.get(caseFieldDefinition.getId());
            boolean containsNewItem = containsNewCollectionItem(jsonNode);
            return (!containsNewItem
                || hasAccessControlList(accessProfiles, caseFieldDefinition.getAccessControlLists(), CAN_CREATE))
                && !isCreateDenied(jsonNode, caseFieldDefinition, accessProfiles);
        } else {
            return !isCreateDenied(newData.get(caseFieldDefinition.getId()), caseFieldDefinition, accessProfiles);
        }
    }

    private boolean isCreateDenied(JsonNode data, CaseFieldDefinition caseFieldDefinition,
                                   Set<AccessProfile> accessProfiles) {
        if (caseFieldDefinition.isCollectionFieldType() && containsNewCollectionItem(data)
            && !hasAccessControlList(accessProfiles, caseFieldDefinition.getAccessControlLists(), CAN_CREATE)) {
            return true;
        }
        boolean createDenied = false;
        if (caseFieldDefinition.isCollectionFieldType() && data.size() > 0) {
            createDenied = StreamSupport.stream(data.spliterator(), false).anyMatch(
                node -> isCreateDeniedForAnyChildNode(caseFieldDefinition, accessProfiles, node));
        } else if (caseFieldDefinition.isComplexFieldType() && data != null) {
            createDenied = isCreateDeniedForAnyChildNode(caseFieldDefinition, accessProfiles, data);
        }
        return createDenied;
    }

    private boolean isCreateDeniedForAnyChildNode(final CaseFieldDefinition caseFieldDefinition,
                                                  final Set<AccessProfile> accessProfiles,
                                                  final JsonNode node) {
        boolean createDeniedForAnyChild = false;
        if (caseFieldDefinition.isCollectionFieldType()) {
            for (CaseFieldDefinition field : caseFieldDefinition.getFieldTypeDefinition()
                .getCollectionFieldTypeDefinition().getComplexFields()) {
                if (field.isCompoundFieldType() && node.get(VALUE) != null && node.get(VALUE).get(field.getId()) != null
                    && isCreateDenied(node.get(VALUE).get(field.getId()), field, accessProfiles)) {
                    LOG.info("Child {} of {} has new data and no create access", field.getId(),
                        caseFieldDefinition.getId());
                    createDeniedForAnyChild = true;
                    break;
                }
            }
        } else { //caseField is Complex
            for (CaseFieldDefinition field : caseFieldDefinition.getFieldTypeDefinition().getComplexFields()) {
                if (field.isCompoundFieldType() && node.get(field.getId()) != null
                    && isCreateDenied(node.get(field.getId()), field, accessProfiles)) {
                    LOG.info("Child {} has new data and no create access", field.getId());
                    createDeniedForAnyChild = true;
                    break;
                }
            }
        }
        return createDeniedForAnyChild;
    }

    private boolean itemDeletedAndHasDeleteAccess(JsonNode existingData,
                                                  JsonNode newData,
                                                  CaseFieldDefinition caseFieldDefinition,
                                                  Set<AccessProfile> accessProfiles) {
        boolean containsDeletedItem = caseFieldDefinition.isCollectionFieldType() && StreamSupport
            .stream(spliteratorUnknownSize(existingData.get(caseFieldDefinition.getId()).elements(),
                Spliterator.ORDERED), false)
            .anyMatch(oldItem -> itemMissing(oldItem, newData.get(caseFieldDefinition.getId())));

        return (!containsDeletedItem
            || hasAccessControlList(accessProfiles, caseFieldDefinition.getAccessControlLists(), CAN_DELETE))
            && !isDeleteDeniedForChildren(existingData.get(caseFieldDefinition.getId()),
            newData.get(caseFieldDefinition.getId()),
            caseFieldDefinition,
            accessProfiles);
    }

    private boolean itemMissing(JsonNode oldItem, JsonNode newValue) {
        boolean itemExists =
            StreamSupport.stream(spliteratorUnknownSize(newValue.elements(), Spliterator.ORDERED), false)
                .anyMatch(newItem -> !isNullId(newItem) && newItem.get("id").equals(oldItem.get("id")));
        return !itemExists;
    }

    private boolean isDeleteDeniedForChildren(final JsonNode existingData,
                                              final JsonNode newData,
                                              final CaseFieldDefinition caseFieldDefinition,
                                              final Set<AccessProfile> userRoles) {
        boolean deleteDenied = false;
        if (caseFieldDefinition.isCollectionFieldType() && existingData.size() > 0) {
            deleteDenied = StreamSupport
                .stream(existingData.spliterator(), false)
                .anyMatch(existingNode -> isCurrentNodeOrAnyChildNodeDeletedWithoutAccess(newData, caseFieldDefinition,
                    userRoles, existingNode));
        } else {
            deleteDenied = isAnyChildOfComplexNodeDeletedWithoutAccess(existingData, newData, caseFieldDefinition,
                userRoles);
        }
        return deleteDenied;
    }

    private boolean isAnyChildOfComplexNodeDeletedWithoutAccess(final JsonNode existingData,
                                                                final JsonNode newData,
                                                                final CaseFieldDefinition caseFieldDefinition,
                                                                final Set<AccessProfile> accessProfiles) {
        boolean deleteDeniedForAnyChildNode = false;
        if (existingData.isObject() && existingData.size() > 0 && newData != null
            && newData.isObject() && newData.size() > 0) {
            for (CaseFieldDefinition field : caseFieldDefinition.getFieldTypeDefinition().getComplexFields()) {
                if (field.isCompoundFieldType()
                    && existingData.get(field.getId()) != null
                    && newData.get(field.getId()) != null
                    && isDeleteDeniedForChildren(existingData.get(field.getId()),
                    newData.get(field.getId()), field, accessProfiles)) {
                    deleteDeniedForAnyChildNode = true;
                    LOG.info(A_CHILD_OF_HAS_DATA_DELETE_WITHOUT_DELETE_ACL, field.getId(), caseFieldDefinition.getId());
                    break;
                }
            }
        }
        return deleteDeniedForAnyChildNode;
    }

    private boolean isCurrentNodeOrAnyChildNodeDeletedWithoutAccess(final JsonNode newData,
                                                                    final CaseFieldDefinition caseFieldDefinition,
                                                                    final Set<AccessProfile> accessProfiles,
                                                                    final JsonNode existingNode) {
        boolean currentNodeOrAnyChildNodeDeletedWithoutAccess = false;
        Optional<JsonNode> correspondingNewNode = findCorrespondingNode(newData, existingNode.get("id"));
        if (correspondingNewNode.isPresent()) {
            JsonNode newNode = correspondingNewNode.get();
            for (CaseFieldDefinition field : caseFieldDefinition.getFieldTypeDefinition()
                .getCollectionFieldTypeDefinition().getComplexFields()) {
                if (field.isCompoundFieldType() && existingNode.get(VALUE) != null
                    && existingNode.get(VALUE).get(field.getId()) != null
                    && isDeleteDeniedForChildren(existingNode.get(VALUE).get(field.getId()),
                    newNode.get(VALUE).get(field.getId()), field, accessProfiles)) {
                    currentNodeOrAnyChildNodeDeletedWithoutAccess = true;
                    LOG.info(A_CHILD_OF_HAS_DATA_DELETE_WITHOUT_DELETE_ACL, field.getId(), caseFieldDefinition.getId());
                    break;
                }
            }
        } else {
            if (!hasAccessControlList(accessProfiles, caseFieldDefinition.getAccessControlLists(), CAN_DELETE)) {
                LOG.info("A child {} item has been deleted but no Delete ACL", caseFieldDefinition.getId());
                currentNodeOrAnyChildNodeDeletedWithoutAccess = true;
            }
        }
        return currentNodeOrAnyChildNodeDeletedWithoutAccess;
    }

    private boolean itemUpdatedAndHasUpdateAccess(JsonNode existingData,
                                                  JsonNode newData,
                                                  CaseFieldDefinition caseFieldDefinition,
                                                  Set<AccessProfile> accessProfiles) {
        if (caseFieldDefinition.isCollectionFieldType()) {
            boolean containsUpdatedItem = StreamSupport.stream(
                spliteratorUnknownSize(existingData.get(caseFieldDefinition.getId()).elements(), Spliterator.ORDERED),
                false)
                .anyMatch(oldItem ->
                    itemUpdated(oldItem, newData.get(caseFieldDefinition.getId()),
                        caseFieldDefinition, accessProfiles));
            return !containsUpdatedItem;
        } else {
            return !isUpdateDeniedForCaseField(existingData.get(caseFieldDefinition.getId()),
                newData.get(caseFieldDefinition.getId()), caseFieldDefinition, accessProfiles);
        }
    }

    private boolean itemUpdated(JsonNode oldItem, JsonNode newValue, CaseFieldDefinition caseFieldDefinition,
                                Set<AccessProfile> accessProfiles) {
        return StreamSupport.stream(spliteratorUnknownSize(newValue.elements(), Spliterator.ORDERED), false)
            .anyMatch(newItem -> {
                boolean itemExists = !isNullId(newItem) && newItem.get("id").equals(oldItem.get("id"));
                if (itemExists) {
                    return !newItem.equals(oldItem) && isUpdateDeniedForCaseField(oldItem, newItem,
                        caseFieldDefinition, accessProfiles);
                }
                return false;
            });
    }

    private boolean isUpdateDeniedForCaseField(final JsonNode oldItem,
                                               final JsonNode newItem,
                                               final CaseFieldDefinition caseFieldDefinition,
                                               final Set<AccessProfile> accessProfiles) {
        boolean updateDenied = false;
        if (caseFieldDefinition.isCollectionFieldType()) {
            if (oldItem.isObject() && oldItem.get(VALUE) != null && newItem.isObject() && newItem.get(VALUE) != null) {
                updateDenied = checkCollectionNodesForUpdate(caseFieldDefinition, oldItem, newItem, accessProfiles);
            } else if (oldItem.isArray() && oldItem.size() > 0 && newItem.isArray() && newItem.size() > 0) {
                updateDenied = isUpdateDeniedForAnyChildNode(oldItem, newItem, caseFieldDefinition, accessProfiles);
            }
        } else {
            if (oldItem.isObject() && oldItem.size() > 0 && newItem.isObject() && newItem.size() > 0) {
                updateDenied = checkComplexNodesForUpdate(caseFieldDefinition, oldItem, newItem, accessProfiles);
            }
        }
        return updateDenied;
    }

    private boolean isUpdateDeniedForAnyChildNode(final JsonNode oldItem,
                                                  final JsonNode newItem,
                                                  final CaseFieldDefinition caseFieldDefinition,
                                                  final Set<AccessProfile> accessProfiles) {
        boolean updateDenied = false;
        for (JsonNode oldNode : oldItem) {
            Optional<JsonNode> optionalCorrespondingNode = findCorrespondingNode(newItem, oldNode.get("id"));
            if (optionalCorrespondingNode.isPresent()) {
                JsonNode newNode = optionalCorrespondingNode.get();
                updateDenied = checkCollectionNodesForUpdate(caseFieldDefinition, oldNode, newNode, accessProfiles);
            }
            if (updateDenied) {
                break;
            }
        }
        return updateDenied;
    }

    private boolean checkCollectionNodesForUpdate(final CaseFieldDefinition caseFieldDefinition,
                                                  final JsonNode oldNode,
                                                  final JsonNode newNode,
                                                  final Set<AccessProfile> accessProfiles) {
        boolean updateDenied = false;
        if (!caseFieldDefinition.getFieldTypeDefinition().getCollectionFieldTypeDefinition().isComplexFieldType()
            && !oldNode.get(VALUE).equals(newNode.get(VALUE))
            && !hasAccessControlList(accessProfiles, caseFieldDefinition.getAccessControlLists(), CAN_UPDATE)) {
            updateDenied = true;
            LOG.info(A_CHILD_HAS_DATA_UPDATE_WITHOUT_UPDATE_ACL, caseFieldDefinition.getId());
        } else {
            for (CaseFieldDefinition field : caseFieldDefinition.getFieldTypeDefinition()
                .getCollectionFieldTypeDefinition().getComplexFields()) {
                if (!field.isCompoundFieldType() && oldNode.get(VALUE).get(field.getId()) != null
                    && !oldNode.get(VALUE).get(field.getId()).equals(newNode.get(VALUE).get(field.getId()))
                    && !hasAccessControlList(accessProfiles, field.getAccessControlLists(), CAN_UPDATE)) {
                    updateDenied = true;
                    LOG.info(SIMPLE_CHILD_OF_HAS_DATA_UPDATE_BUT_NO_UPDATE_ACL, field.getId(),
                        caseFieldDefinition.getId());
                } else if (field.isCompoundFieldType() && oldNode.get(VALUE).get(field.getId()) != null
                    && newNode.get(VALUE).get(field.getId()) != null
                    && isUpdateDeniedForCaseField(oldNode.get(VALUE).get(field.getId()),
                    newNode.get(VALUE).get(field.getId()), field, accessProfiles)) {
                    updateDenied = true;
                    LOG.info(A_CHILD_OF_HAS_DATA_UPDATE_WITHOUT_UPDATE_ACL, field.getId(), caseFieldDefinition.getId());
                }
                if (updateDenied) {
                    break;
                }
            }
        }
        return updateDenied;
    }

    private boolean checkComplexNodesForUpdate(final CaseFieldDefinition caseFieldDefinition,
                                               final JsonNode oldNode,
                                               final JsonNode newNode,
                                               final Set<AccessProfile> accessProfiles) {
        boolean updateDenied = false;
        for (CaseFieldDefinition field : caseFieldDefinition.getFieldTypeDefinition().getComplexFields()) {
            if (!field.isCompoundFieldType()
                && isSimpleFieldValueReallyUpdated(oldNode, newNode, field)
                && !hasAccessControlList(accessProfiles, field.getAccessControlLists(), CAN_UPDATE)) {
                updateDenied = true;
                LOG.info(SIMPLE_CHILD_OF_HAS_DATA_UPDATE_BUT_NO_UPDATE_ACL, field.getId(), caseFieldDefinition.getId());
            } else if (field.isCompoundFieldType() && oldNode.get(field.getId()) != null
                && newNode.get(field.getId()) != null
                && isUpdateDeniedForCaseField(oldNode.get(field.getId()), newNode.get(field.getId()), field,
                accessProfiles)) {
                updateDenied = true;
                LOG.info(A_CHILD_OF_HAS_DATA_UPDATE_WITHOUT_UPDATE_ACL, field.getId(), caseFieldDefinition.getId());
            }
            if (updateDenied) {
                break;
            }
        }
        return updateDenied;
    }

    private boolean isSimpleFieldValueReallyUpdated(final JsonNode oldNode, final JsonNode newNode,
                                                    final CaseFieldDefinition field) {
        if (oldNode.get(field.getId()) == null && (newNode.get(field.getId()) == null
            || newNode.get(field.getId()).isNull())) {
            // If both null, then nothing changed
            // We mark fields (and subfields) with no UPDATE as READONLY, this causes "null" value to be submitted
            // when the old value is null and new value is sent as "null" due to the above requirement,
            // this mustn't be interpreted as an update
            return false;
        }
        if (oldNode.get(field.getId()) == null) {
            return true;
        }
        return !oldNode.get(field.getId()).equals(newNode.get(field.getId()));
    }

    private Optional<JsonNode> findCorrespondingNode(final JsonNode newItem, final JsonNode id) {
        if (newItem == null) {
            return Optional.empty();
        }
        return StreamSupport.stream(spliteratorUnknownSize(newItem.elements(), Spliterator.ORDERED), false)
            .filter(node -> node.get("id") != null && node.get("id").equals(id))
            .findFirst();
    }

    private boolean containsNewCollectionItem(JsonNode data) {
        return StreamSupport.stream(spliteratorUnknownSize(data.elements(), Spliterator.ORDERED), false)
            .anyMatch(this::isNullId);
    }

    private boolean isNullId(JsonNode newItem) {
        return newItem.get("id") == null
            || newItem.get("id").equals(NullNode.getInstance())
            || newItem.get("id").asText().equalsIgnoreCase("null");
    }
}
