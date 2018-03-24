package uk.gov.hmcts.ccd.domain.types;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.MethodInvokingBean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;

import javax.inject.Inject;

@Configuration
class BaseTypeConfiguration {

    @Inject
    BaseTypeConfiguration(
        @Qualifier(DefaultCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository)
        throws BaseTypeConfigurationException {
        final MethodInvokingBean invokingBean = new MethodInvokingBean();
        invokingBean.setStaticMethod("uk.gov.hmcts.ccd.domain.types.BaseType.setCaseDefinitionRepository");
        invokingBean.setArguments(caseDefinitionRepository);
        try {
            invokingBean.prepare();
            invokingBean.invoke();
        } catch (Exception e) {
            throw new BaseTypeConfigurationException(e);
        }
    }
}
