package uk.gov.hmcts.ccd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;

@Configuration
@EnableTransactionManagement
public class TransactionConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionConfiguration.class);

    private void jclog(String message) {
        LOG.info("JCLOG: Info: TransactionConfiguration: " + message);
        LOG.warn("JCLOG: Warn: TransactionConfiguration: " + message);
        LOG.error("JCLOG: Error: TransactionConfiguration: " + message);
        LOG.debug("JCLOG: Debug: TransactionConfiguration: " + message);
    }

    // JC Note: DATA_STORE_TX_TIMEOUT_DEFAULT does not appear to be defined in cnp-flux-config or data-store values.yaml

    @Bean
    public PlatformTransactionManager transactionManager(final EntityManagerFactory emf,
                                                         @Value("${ccd.tx-timeout.default}") String defaultTimeout) {
        final JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        transactionManager.setDefaultTimeout(Integer.parseInt(defaultTimeout));
        jclog("getDefaultTimeout = " + transactionManager.getDefaultTimeout());
        return transactionManager;
    }

}
