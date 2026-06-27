package com.example.CampusFlowServer.global.config;

import javax.sql.DataSource;
import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class JdbcBatchConfiguration extends DefaultBatchConfiguration {

    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;

    public JdbcBatchConfiguration(
        DataSource dataSource,
        PlatformTransactionManager transactionManager
    ) {
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
    }

    @Bean
    @Override
    public JobRepository jobRepository() {
        JdbcJobRepositoryFactoryBean factory = new JdbcJobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager);
        try {
            factory.afterPropertiesSet();
            return factory.getObject();
        } catch (Exception exception) {
            throw new BatchConfigurationException(
                "Unable to configure the JDBC job repository",
                exception
            );
        }
    }

    @Override
    protected PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }
}
