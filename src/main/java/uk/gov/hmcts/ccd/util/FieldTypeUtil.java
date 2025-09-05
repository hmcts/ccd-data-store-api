package uk.gov.hmcts.ccd.util;

import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.domain.model.search.DataType;
import uk.gov.hmcts.ccd.domain.model.search.Field;

@UtilityClass
public class FieldTypeUtil {

    private static final Logger LOG = LoggerFactory.getLogger(FieldTypeUtil.class);

    public static DataType getDataTypeFromField(final Field field) {
        if (field != null && field.getType() != null) {
            LOG.info("Field Type : {}", field.getType());
            if (field.getType().getCollectionFieldTypeDefinition() != null
                && field.getType().getCollectionFieldTypeDefinition().isCollectionFieldType()) {
                return DataType.COLLECTION;
            } else {
                if (field.getType().getCollectionFieldTypeDefinition() != null
                    && field.getType() != null && field.getType().isComplexFieldType()) {
                    return DataType.COMPLEX;
                }
            }
        }
        return null;
    }
}
