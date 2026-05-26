package com.nhom611.jobsvc.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Component
public class MongoMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MongoMigration.class);
    private final MongoTemplate mongoTemplate;

    public MongoMigration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        // Set empty attachments array for proposals missing the field (migration runs only once)
        Query query = new Query(Criteria.where("attachments").exists(false));
        Update update = new Update().set("attachments", List.of());
        var result = mongoTemplate.updateMulti(query, update, "proposals");
        if (result.getModifiedCount() > 0) {
            log.info("MongoDB migration: Updated {} proposals to have empty attachments array", result.getModifiedCount());
        } else {
            log.info("MongoDB migration: No proposals needed migration (all already have attachments field)");
        }
    }
}

