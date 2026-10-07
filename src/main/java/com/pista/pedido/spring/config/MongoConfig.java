package com.pista.pedido.spring.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.pista.pedido.domain.service.UnidadeDeTrabalho;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String uri;

    @Value("${pedido.mongo.database:pm_pedido}")
    private String database;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    protected void configureClientSettings(MongoClientSettings.Builder builder) {
        builder.applyConnectionString(new ConnectionString(uri));
    }

    @Override
    protected boolean autoIndexCreation() {
        return true;
    }

    // Transações no MongoDB exigem replica set (o Atlas já é)
    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }

    @Bean
    public UnidadeDeTrabalho unidadeDeTrabalho(MongoTransactionManager transactionManager) {
        return new MongoUnidadeDeTrabalho(transactionManager);
    }
}
