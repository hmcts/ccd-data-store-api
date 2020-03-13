package uk.gov.hmcts.ccd.domain.service.common;

import static java.util.Spliterators.spliteratorUnknownSize;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_DELETE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.VALUE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.hasAccessControlList;

import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CompoundAccessControlService {

    private static final Logger LOG = LoggerFactory.getLogger(CompoundAccessControlService.class);
    public static final String A_CHILD_OF_HAS_DATA_DETELE_WITHOUT_DELETE_ACL = "A child {} of {} has been deleted but no Delete ACL";
    public static final String A_CHILD_OF_HAS_DATA_UPDATE_WITHOUT_UPDATE_ACL = "A child {} of {} has data update without Update ACL";
    public static final String SIMPLE_CHILD_OF_HAS_DATA_UPDATE_BUT_NO_UPDATE_ACL = "Simple child {} of {} has data update but no Update ACL";

    public boolean hasAccessForAction(final JsonNode newData, final JsonNode existingData, final CaseField caseField, final Set<String> userRoles) {
        if (!itemAddedAndHasCreateAccess(newData, caseField, userRoles)) {
            return false;
        }
        if (!itemDeletedAndHasDeleteAccess(existingData, newData, caseField, userRoles)) {
            return false;
        }
        return itemUpdatedAndHasUpdateAccess(existingData, newData, caseField, userRoles);
    }

    private boolean itemAddedAndHasCreateAccess(JsonNode newData, CaseField caseField, Set<String> userRoles) {
        if (caseField.isCollectionFieldType()) {
            final JsonNode jsonNode = newData.get(caseField.getId());
            boolean containsNewItem = containsNewCollectionItem(jsonNode);
            return (!containsNewItem || hasAccessControlList(userRoles, CAN_CREATE, caseField.getAccessControlLists()))
                && !isCreateDenied(jsonNode, caseField, userRoles);
        } else {
            return !isCreateDenied(newData.get(caseField.getId()), caseField, userRoles);
        }
    }

    private boolean isCreateDenied(JsonNode data, CaseField caseField, Set<String> userRoles) {
        if (caseField.isCollectionFieldType() && containsNewCollectionItem(data)
            && !hasAccessControlList(userRoles, CAN_CREATE, caseField.getAccessControlLists())) {
            return true;
        }
        boolean createDenied = false;
        if (caseField.isCollectionFieldType() && data.size() > 0) {
            createDenied = StreamSupport.stream(data.spliterator(), false).anyMatch(node -> isCreateDeniedForAnyChildNode(caseField, userRoles, node));
        } else if (caseField.isComplexFieldType() && data != null) {
            createDenied = isCreateDeniedForAnyChildNode(caseField, userRoles, data);
        }
        return createDenied;
    }

    private boolean isCreateDeniedForAnyChildNode(final CaseField caseField, final Set<String> userRoles, final JsonNode node) {
        boolean createDeniedForAnyChild = false;
        if (caseField.isCollectionFieldType()) {
            for (CaseField field : caseField.getFieldType().getCollectionFieldType().getComplexFields()) {
                if (field.isCompoundFieldType() && node.get(VALUE) != null && node.get(VALUE).get(field.getId()) != null
                    && isCreateDenied(node.get(VALUE).get(field.getId()), field, userRoles)) {
                    LOG.info("Child {} of {} has new data and no create access", field.getId(), caseField.getId());
                    createDeniedForAnyChild = true;
                    break;
                }
            }
        } else { //caseField is Complex
            for (CaseField field : caseField.getFieldType().getComplexFields()) {
                if (field.isCompoundFieldType() && node.get(field.getId()) != null && isCreateDenied(node.get(field.getId()), field, userRoles)) {
                    LOG.info("Child {} has new data and no create access", field.getId());
                    createDeniedForAnyChild = true;
                    break;
                }
            }
        }
        return createDeniedForAnyChild;
    }

    private boolean itemDeletedAndHasDeleteAccess(JsonNode existingData, JsonNode newData, CaseField caseField, Set<String> userRoles) {
        boolean containsDeletedItem = caseField.isCollectionFieldType() && StreamSupport
            .stream(spliteratorUnknownSize(existingData.get(caseField.getId()).elements(), Spliterator.ORDERED), false)
            .anyMatch(oldItem -> itemMissing(oldItem, newData.get(caseField.getId())));

        return (!containsDeletedItem || hasAccessControlList(userRoles, CAN_DELETE, caseField.getAccessControlLists()))
            && !isDeleteDeniedForChildren(existingData.get(caseField.getId()), newData.get(caseField.getId()), caseField, userRoles);
    }

    private boolean itemMissing(JsonNode oldItem, JsonNode newValue) {
        boolean itemExists = StreamSupport.stream(spliteratorUnknownSize(newValue.elements(), Spliterator.ORDERED), false)
            .anyMatch(newItem -> !isNullId(newItem) && newItem.get("id").equals(oldItem.get("id")));
        return !itemExists;
    }

    private boolean isDeleteDeniedForChildren(final JsonNode existingData, final JsonNode newData, final CaseField caseField, final Set<String> userRoles) {
        boolean deleteDenied = false;
        if (caseField.isCollectionFieldType() && existingData.size() > 0) {
            deleteDenied = StreamSupport
                .stream(existingData.spliterator(), false)
                .anyMatch(existingNode -> isCurrentNodeOrAnyChildNodeDeletedWithoutAccess(newData, caseField, userRoles, existingNode));
        } else {
            deleteDenied = isAnyChildOfComplexNodeDeletedWithoutAccess(existingData, newData, caseField, userRoles);
        }
        return deleteDenied;
    }

    private boolean isAnyChildOfComplexNodeDeletedWithoutAccess(final JsonNode existingData, final JsonNode newData, final CaseField caseField, final Set<String> userRoles) {
        boolean deleteDeniedForAnyChildNode = false;
        if (existingData.isObject() && existingData.size() > 0 && newData != null && newData.isObject() && newData.size() > 0) {
            for (CaseField field : caseField.getFieldType().getComplexFields()) {
                if (field.isCompoundFieldType() && existingData.get(field.getId()) != null && newData.get(field.getId()) != null
                    && isDeleteDeniedForChildren(existingData.get(field.getId()), newData.get(field.getId()), field, userRoles)) {
                    deleteDeniedForAnyChildNode = true;
                    LOG.info(A_CHILD_OF_HAS_DATA_DETELE_WITHOUT_DELETE_ACL, field.getId(), caseField.getId());
                    break;
                }
            }
        }
        return deleteDeniedForAnyChildNode;
    }

    private boolean isCurrentNodeOrAnyChildNodeDeletedWithoutAccess(final JsonNode newData, final CaseField caseField, final Set<String> userRoles, final JsonNode existingNode) {
        boolean currentNodeOrAnyChildNodeDeletedWithoutAccess = false;
        Optional<JsonNode> correspondingNewNode = findCorrespondingNode(newData, existingNode.get("id"));
        if (correspondingNewNode.isPresent()) {
            JsonNode newNode = correspondingNewNode.get();
            for (CaseField field : caseField.getFieldType().getCollectionFieldType().getComplexFields()) {
                if (field.isCompoundFieldType() && existingNode.get(VALUE) != null && existingNode.get(VALUE).get(field.getId()) != null
                    && isDeleteDeniedForChildren(existingNode.get(VALUE).get(field.getId()), newNode.get(VALUE).get(field.getId()), field, userRoles)) {
                    currentNodeOrAnyChildNodeDeletedWithoutAccess = true;
                    LOG.info(A_CHILD_OF_HAS_DATA_DETELE_WITHOUT_DELETE_ACL, field.getId(), caseField.getId());
                    break;
                }
            }
        } else {
            if (!hasAccessControlList(userRoles, CAN_DELETE, caseField.getAccessControlLists())) {
                LOG.info("A child {} item has been deleted but no Delete ACL", caseField.getId());
                currentNodeOrAnyChildNodeDeletedWithoutAccess = true;
            }
        }
        return currentNodeOrAnyChildNodeDeletedWithoutAccess;
    }

    private boolean itemUpdatedAndHasUpdateAccess(JsonNode existingData, JsonNode newData, CaseField caseField, Set<String> userRoles) {
        if (caseField.isCollectionFieldType()) {
            boolean containsUpdatedItem = StreamSupport.stream(spliteratorUnknownSize(existingData.get(caseField.getId()).elements(), Spliterator.ORDERED), false)
                .anyMatch(oldItem -> itemUpdated(oldItem, newData.get(caseField.getId()), caseField, userRoles));
            return !containsUpdatedItem;
        } else {
            return !isUpdateDeniedForCaseField(existingData.get(caseField.getId()), newData.get(caseField.getId()), caseField, userRoles);
        }
    }

    private boolean itemUpdated(JsonNode oldItem, JsonNode newValue, CaseField caseField, Set<String> userRoles) {
        return StreamSupport.stream(spliteratorUnknownSize(newValue.elements(), Spliterator.ORDERED), false)
            .anyMatch(newItem -> {
                boolean itemExists = !isNullId(newItem) && newItem.get("id").equals(oldItem.get("id"));
                if (itemExists) {
                    return !newItem.equals(oldItem) && isUpdateDeniedForCaseField(oldItem, newItem, caseField, userRoles);
                }
                return false;
            });
    }

    private boolean isUpdateDeniedForCaseField(final JsonNode oldItem, final JsonNode newItem, final CaseField caseField, Set<String> userRoles) {
        boolean updateDenied = false;
        if (caseField.isCollectionFieldType()) {
            if (oldItem.isObject() && oldItem.get(VALUE) != null && newItem.isObject() && newItem.get(VALUE) != null) {
                updateDenied = checkCollectionNodesForUpdate(caseField, oldItem, newItem, userRoles);
            } else if (oldItem.isArray() && oldItem.size() > 0 && newItem.isArray() && newItem.size() > 0) {
                updateDenied = isUpdateDeniedForAnyChildNode(oldItem, newItem, caseField, userRoles);
            }
        } else {
            if (oldItem.isObject() && oldItem.size() > 0 && newItem.isObject() && newItem.size() > 0) {
                updateDenied = checkComplexNodesForUpdate(caseField, oldItem, newItem, userRoles);
            }
        }
        return updateDenied;
    }

    private boolean isUpdateDeniedForAnyChildNode(final JsonNode oldItem, final JsonNode newItem, final CaseField caseField, final Set<String> userRoles) {
        boolean updateDenied = false;
        for (JsonNode oldNode : oldItem) {
            Optional<JsonNode> optionalCorrespondingNode = findCorrespondingNode(newItem, oldNode.get("id"));
            if (optionalCorrespondingNode.isPresent()) {
                JsonNode newNode = optionalCorrespondingNode.get();
                updateDenied = checkCollectionNodesForUpdate(caseField, oldNode, newNode, userRoles);
            }
            if (updateDenied)
                break;
        }
        return updateDenied;
    }

    private boolean checkCollectionNodesForUpdate(final CaseField caseField, final JsonNode oldNode, final JsonNode newNode, Set<String> userRoles) {
        boolean udateDenied = false;
        for (CaseField field : caseField.getFieldType().getCollectionFieldType().getComplexFields()) {
            if (!field.isCompoundFieldType() && oldNode.get(VALUE).get(field.getId()) != null
                && !oldNode.get(VALUE).get(field.getId()).equals(newNode.get(VALUE).get(field.getId()))
                && !hasAccessControlList(userRoles, CAN_UPDATE, field.getAccessControlLists())) {
                udateDenied = true;
                LOG.info(SIMPLE_CHILD_OF_HAS_DATA_UPDATE_BUT_NO_UPDATE_ACL, field.getId(), caseField.getId());
            } else if (field.isCompoundFieldType() && oldNode.get(VALUE).get(field.getId()) != null && newNode.get(VALUE).get(field.getId()) != null
                && isUpdateDeniedForCaseField(oldNode.get(VALUE).get(field.getId()), newNode.get(VALUE).get(field.getId()), field, userRoles)) {
                udateDenied = true;
                LOG.info(A_CHILD_OF_HAS_DATA_UPDATE_WITHOUT_UPDATE_ACL, field.getId(), caseField.getId());
            }
            if (udateDenied) {
                break;
            }
        }
        return udateDenied;
    }

    private boolean checkComplexNodesForUpdate(final CaseField caseField, final JsonNode oldNode, final JsonNode newNode, Set<String> userRoles) {
        boolean updateDenied = false;
        for (CaseField field : caseField.getFieldType().getComplexFields()) {
            if (!field.isCompoundFieldType()
                && isSimpleFieldValueReallyUpdated(oldNode, newNode, field)
                && !hasAccessControlList(userRoles, CAN_UPDATE, field.getAccessControlLists())) {
                updateDenied = true;
                LOG.info(SIMPLE_CHILD_OF_HAS_DATA_UPDATE_BUT_NO_UPDATE_ACL, field.getId(), caseField.getId());
            } else if (field.isCompoundFieldType() && oldNode.get(field.getId()) != null && newNode.get(field.getId()) != null
                && isUpdateDeniedForCaseField(oldNode.get(field.getId()), newNode.get(field.getId()), field, userRoles)) {
                updateDenied = true;
                LOG.info(A_CHILD_OF_HAS_DATA_UPDATE_WITHOUT_UPDATE_ACL, field.getId(), caseField.getId());
            }
            if (updateDenied) {
                break;
            }
        }
        return updateDenied;
    }

    private boolean isSimpleFieldValueReallyUpdated(final JsonNode oldNode, final JsonNode newNode, final CaseField field) {
        if (oldNode.get(field.getId()) == null && (newNode.get(field.getId()) == null || newNode.get(field.getId()).isNull())) {
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
