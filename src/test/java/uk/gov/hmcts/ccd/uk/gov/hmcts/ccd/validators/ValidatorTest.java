package uk.gov.hmcts.ccd.uk.gov.hmcts.ccd.validators;

import javax.validation.ConstraintValidatorContext;

public class ValidatorTest {

    public ConstraintValidatorContext.ConstraintViolationBuilder getConstraintViolationBuilder() {

        return new ConstraintValidatorContext.ConstraintViolationBuilder() {
            @Override
            public NodeBuilderDefinedContext addNode(String name) {
                return null;
            }

            @Override
            public NodeBuilderCustomizableContext addPropertyNode(String name) {
                return null;
            }

            @Override
            public LeafNodeBuilderCustomizableContext addBeanNode() {
                return null;
            }

            @Override
            public ContainerElementNodeBuilderCustomizableContext addContainerElementNode(String name, Class<?> containerType, Integer typeArgumentIndex) {
                return null;
            }

            @Override
            public NodeBuilderDefinedContext addParameterNode(int index) {
                return null;
            }

            @Override
            public ConstraintValidatorContext addConstraintViolation() {
                return null;
            }
        };

    }
}
