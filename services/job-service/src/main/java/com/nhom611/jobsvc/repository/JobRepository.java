package com.nhom611.jobsvc.repository;

import com.nhom611.jobsvc.domain.Job;
import com.nhom611.jobsvc.domain.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface JobRepository extends MongoRepository<Job, String> {

    Page<Job> findByEmployerId(String employerId, Pageable pageable);

    Page<Job> findByEmployerIdAndStatus(String employerId, JobStatus status, Pageable pageable);
}
