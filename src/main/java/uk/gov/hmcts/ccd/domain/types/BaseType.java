package uk.gov.hmcts.ccd.domain.types;

import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import java.util.HashMap;
import java.util.Map;

public class BaseType {
    private static final Map<String, BaseType> BASE_TYPES = new HashMap<>();
    private static CaseDefinitionRepository caseDefinitionRepository;
    private static Boolean initialised = Boolean.FALSE;

    private final String type;
    private final String regularExpression;

    public BaseType(final FieldTypeDefinition fieldTypeDefinition) {
        type = fieldTypeDefinition.getType();
        regularExpression = fieldTypeDefinition.getRegularExpression();
    }

    public static void setCaseDefinitionRepository(@Qualifier(DefaultCaseDefinitionRepository.QUALIFIER)
                                                   final CaseDefinitionRepository caseDefinitionRepository) {
        BaseType.caseDefinitionRepository = caseDefinitionRepository;
    }

    /**
     * This needs to be lazily initialised as it would prevent startup of the application otherwise.
     */
    public static void initialise() {
        BaseType.caseDefinitionRepository.getBaseTypes()
            .forEach(fieldType -> BaseType.register(new BaseType(fieldType)));
        BaseType.initialised = Boolean.TRUE;
    }

    public static void register(final BaseType baseType) {
        BASE_TYPES.put(baseType.getType().toUpperCase(), baseType);
    }

    public static BaseType get(final String type) {
        if (!BaseType.initialised) {
            BaseType.initialise();
        }

        return BASE_TYPES.get(type.toUpperCase());
    }

    public static Boolean contains(final String type) {
        if (!BaseType.initialised) {
            BaseType.initialise();
        }

        return BASE_TYPES.keySet().contains(type.toUpperCase());
    }

    public String getType() {
        return type;
    }

    String getRegularExpression() {
        return regularExpression;
    }
}
