package com.nhom611.contractsvc.repository;

import com.nhom611.contractsvc.domain.Deliverable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DeliverableRepository extends MongoRepository<Deliverable, String> {
    List<Deliverable> findByMilestoneId(String milestoneId);
}
