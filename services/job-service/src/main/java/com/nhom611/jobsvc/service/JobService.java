package com.nhom611.jobsvc.service;

import com.nhom611.jobsvc.domain.BudgetType;
import com.nhom611.jobsvc.domain.Job;
import com.nhom611.jobsvc.domain.JobStatus;
import com.nhom611.jobsvc.dto.JobDtos;
import com.nhom611.jobsvc.repository.JobRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final MongoTemplate mongoTemplate;

    public JobService(JobRepository jobRepository, MongoTemplate mongoTemplate) {
        this.jobRepository = jobRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public JobDtos.JobDetailResponse createJob(String employerId, JobDtos.CreateJobRequest req) {
        Instant now = Instant.now();

        Job job = new Job();
        job.setEmployerId(employerId);
        job.setTitle(req.title().trim());
        job.setDescription(req.description().trim());
        job.setRequiredSkills(normalizeSkills(req.requiredSkills()));
        job.setBudgetType(req.budgetType());
        job.setDeadline(req.deadline());
        job.setStatus(JobStatus.OPEN);
        job.setProposalCount(0);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);

        validateBudgetForCreate(req);
        applyBudget(job, req.budgetType(), req.fixedBudget(), req.hourlyRate(), req.estimatedHours());
        validateDeadline(job.getDeadline());

        Job saved = jobRepository.save(job);
        return toDetail(saved);
    }

    public Page<JobDtos.JobListItemResponse> listPublicJobs(
            String q,
            List<String> skills,
            BudgetType budgetType,
            BigDecimal minBudget,
            BigDecimal maxBudget,
            Instant deadlineBefore,
            JobDtos.Sort sort,
            Pageable pageable
    ) {
        Query query = buildPublicSearchQuery(q, skills, budgetType, minBudget, maxBudget, deadlineBefore);

        if (q == null || q.isBlank()) {
            query.with(resolveSort(sort, budgetType));
        }

        Query countQuery = Query.of(query).limit(-1).skip(-1);
        long total = mongoTemplate.count(countQuery, Job.class);

        query.with(pageable);
        List<Job> items = mongoTemplate.find(query, Job.class);
        List<JobDtos.JobListItemResponse> mapped = items.stream().map(JobService::toListItem).toList();

        return new PageImpl<>(mapped, pageable, total);
    }

    public JobDtos.JobDetailResponse getJob(String jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        return toDetail(job);
    }

    public Page<JobDtos.JobListItemResponse> listEmployerJobs(String employerId, JobStatus status, Pageable pageable) {
        Page<Job> page = (status == null)
                ? jobRepository.findByEmployerId(employerId, pageable)
                : jobRepository.findByEmployerIdAndStatus(employerId, status, pageable);

        return page.map(JobService::toListItem);
    }

    public JobDtos.JobDetailResponse updateJob(String employerId, String jobId, JobDtos.UpdateJobRequest req) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        requireOwner(employerId, job);
        if (job.getStatus() != JobStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only OPEN jobs can be edited");
        }

        if (req.title() != null) {
            job.setTitle(req.title().trim());
        }
        if (req.description() != null) {
            job.setDescription(req.description().trim());
        }
        if (req.requiredSkills() != null) {
            job.setRequiredSkills(normalizeSkills(req.requiredSkills()));
        }
        if (req.deadline() != null) {
            job.setDeadline(req.deadline());
        }
        validateDeadline(job.getDeadline());

        applyBudgetUpdate(job, req);

        job.setUpdatedAt(Instant.now());
        Job saved = jobRepository.save(job);
        return toDetail(saved);
    }

    public JobDtos.JobDetailResponse closeJob(String employerId, String jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        requireOwner(employerId, job);

        if (job.getStatus() != JobStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only OPEN jobs can be closed");
        }

        job.setStatus(JobStatus.CANCELLED);
        job.setUpdatedAt(Instant.now());
        Job saved = jobRepository.save(job);
        return toDetail(saved);
    }

    private static void validateBudgetForCreate(JobDtos.CreateJobRequest req) {
        if (req.budgetType() == BudgetType.FIXED) {
            if (req.fixedBudget() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fixedBudget is required for FIXED jobs");
            }
            if (req.hourlyRate() != null || req.estimatedHours() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hourlyRate/estimatedHours not allowed for FIXED jobs");
            }
        } else if (req.budgetType() == BudgetType.HOURLY) {
            if (req.hourlyRate() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hourlyRate is required for HOURLY jobs");
            }
            if (req.estimatedHours() == null || req.estimatedHours() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "estimatedHours must be > 0 for HOURLY jobs");
            }
            if (req.fixedBudget() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fixedBudget not allowed for HOURLY jobs");
            }
        }
    }

    private static void applyBudget(Job job, BudgetType budgetType, BigDecimal fixedBudget, BigDecimal hourlyRate, Integer estimatedHours) {
        job.setBudgetType(budgetType);

        if (budgetType == BudgetType.FIXED) {
            job.setFixedBudget(fixedBudget);
            job.setHourlyRate(null);
            job.setEstimatedHours(null);
        } else if (budgetType == BudgetType.HOURLY) {
            job.setFixedBudget(null);
            job.setHourlyRate(hourlyRate);
            job.setEstimatedHours(estimatedHours);
        }
    }

    private void applyBudgetUpdate(Job job, JobDtos.UpdateJobRequest req) {
        BudgetType requestedType = req.budgetType();
        BudgetType effectiveType = requestedType != null ? requestedType : job.getBudgetType();

        if (effectiveType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "budgetType is required");
        }

        if (requestedType == BudgetType.FIXED) {
            if (req.fixedBudget() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fixedBudget is required when changing budgetType to FIXED");
            }
            if (req.hourlyRate() != null || req.estimatedHours() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hourlyRate/estimatedHours not allowed for FIXED jobs");
            }
            applyBudget(job, BudgetType.FIXED, req.fixedBudget(), null, null);
            return;
        }

        if (requestedType == BudgetType.HOURLY) {
            if (req.hourlyRate() == null || req.estimatedHours() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hourlyRate and estimatedHours are required when changing budgetType to HOURLY");
            }
            if (req.estimatedHours() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "estimatedHours must be > 0 for HOURLY jobs");
            }
            if (req.fixedBudget() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fixedBudget not allowed for HOURLY jobs");
            }
            applyBudget(job, BudgetType.HOURLY, null, req.hourlyRate(), req.estimatedHours());
            return;
        }

        // budgetType not changed; allow updating compatible fields
        if (effectiveType == BudgetType.FIXED) {
            if (req.hourlyRate() != null || req.estimatedHours() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hourlyRate/estimatedHours not allowed for FIXED jobs");
            }
            if (req.fixedBudget() != null) {
                applyBudget(job, BudgetType.FIXED, req.fixedBudget(), null, null);
            }
            return;
        }

        if (effectiveType == BudgetType.HOURLY) {
            if (req.fixedBudget() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fixedBudget not allowed for HOURLY jobs");
            }
            BigDecimal hourlyRate = req.hourlyRate() != null ? req.hourlyRate() : job.getHourlyRate();
            Integer estimatedHours = req.estimatedHours() != null ? req.estimatedHours() : job.getEstimatedHours();

            if (hourlyRate == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hourlyRate is required for HOURLY jobs");
            }
            if (estimatedHours == null || estimatedHours <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "estimatedHours must be > 0 for HOURLY jobs");
            }
            applyBudget(job, BudgetType.HOURLY, null, hourlyRate, estimatedHours);
        }
    }

    private static void validateDeadline(Instant deadline) {
        if (deadline == null) {
            return;
        }
        if (deadline.isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deadline must be in the future");
        }
    }

    private static List<String> normalizeSkills(List<String> skills) {
        if (skills == null) {
            return List.of();
        }

        return skills.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .limit(10)
                .toList();
    }

    private static void requireOwner(String employerId, Job job) {
        if (employerId == null || employerId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing user identity");
        }
        if (!employerId.equals(job.getEmployerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not job owner");
        }
    }

    private Query buildPublicSearchQuery(
            String q,
            List<String> skills,
            BudgetType budgetType,
            BigDecimal minBudget,
            BigDecimal maxBudget,
            Instant deadlineBefore
    ) {
        Query query;

        boolean hasQ = q != null && !q.isBlank();
        if (hasQ) {
            TextCriteria text = TextCriteria.forDefaultLanguage().matching(q.trim());
            query = TextQuery.queryText(text).sortByScore();
        } else {
            query = new Query();
        }

        List<Criteria> and = new ArrayList<>();
        and.add(Criteria.where("status").is(JobStatus.OPEN));

        if (skills != null && !skills.isEmpty()) {
            and.add(Criteria.where("requiredSkills").in(normalizeSkills(skills)));
        }

        if (deadlineBefore != null) {
            and.add(Criteria.where("deadline").lte(deadlineBefore));
        }

        if (budgetType != null) {
            and.add(Criteria.where("budgetType").is(budgetType));

            if (budgetType == BudgetType.FIXED) {
                if (minBudget != null) {
                    and.add(Criteria.where("fixedBudget").gte(minBudget));
                }
                if (maxBudget != null) {
                    and.add(Criteria.where("fixedBudget").lte(maxBudget));
                }
            } else if (budgetType == BudgetType.HOURLY) {
                if (minBudget != null) {
                    and.add(Criteria.where("hourlyRate").gte(minBudget));
                }
                if (maxBudget != null) {
                    and.add(Criteria.where("hourlyRate").lte(maxBudget));
                }
            }
        } else if (minBudget != null || maxBudget != null) {
            List<Criteria> or = new ArrayList<>();

            Criteria fixed = Criteria.where("budgetType").is(BudgetType.FIXED);
            if (minBudget != null) {
                fixed = fixed.and("fixedBudget").gte(minBudget);
            }
            if (maxBudget != null) {
                fixed = fixed.and("fixedBudget").lte(maxBudget);
            }
            or.add(fixed);

            Criteria hourly = Criteria.where("budgetType").is(BudgetType.HOURLY);
            if (minBudget != null) {
                hourly = hourly.and("hourlyRate").gte(minBudget);
            }
            if (maxBudget != null) {
                hourly = hourly.and("hourlyRate").lte(maxBudget);
            }
            or.add(hourly);

            and.add(new Criteria().orOperator(or.toArray(new Criteria[0])));
        }

        if (!and.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(and.toArray(new Criteria[0])));
        }

        return query;
    }

    private static Sort resolveSort(JobDtos.Sort sort, BudgetType budgetType) {
        if (sort == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        if (sort == JobDtos.Sort.BUDGET_ASC || sort == JobDtos.Sort.BUDGET_DESC) {
            Sort.Direction dir = (sort == JobDtos.Sort.BUDGET_ASC) ? Sort.Direction.ASC : Sort.Direction.DESC;

            if (budgetType == BudgetType.FIXED) {
                return Sort.by(dir, "fixedBudget").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            }
            if (budgetType == BudgetType.HOURLY) {
                return Sort.by(dir, "hourlyRate").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            }

            // Budget sort without a budgetType is ambiguous; fall back to newest.
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private static JobDtos.JobListItemResponse toListItem(Job job) {
        return new JobDtos.JobListItemResponse(
                job.getId(),
                job.getEmployerId(),
                job.getTitle(),
                job.getRequiredSkills(),
                job.getBudgetType(),
                job.getFixedBudget(),
                job.getHourlyRate(),
                job.getEstimatedHours(),
                job.getDeadline(),
                job.getStatus(),
                job.getProposalCount(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }

    private static JobDtos.JobDetailResponse toDetail(Job job) {
        return new JobDtos.JobDetailResponse(
                job.getId(),
                job.getEmployerId(),
                job.getTitle(),
                job.getDescription(),
                job.getRequiredSkills(),
                job.getBudgetType(),
                job.getFixedBudget(),
                job.getHourlyRate(),
                job.getEstimatedHours(),
                job.getDeadline(),
                job.getStatus(),
                job.getProposalCount(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
