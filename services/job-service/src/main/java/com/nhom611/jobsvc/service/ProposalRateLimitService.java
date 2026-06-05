package com.nhom611.jobsvc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class ProposalRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(ProposalRateLimitService.class);

    private static final Duration WINDOW = Duration.ofHours(12);
    private static final long MAX_SUBMISSIONS = 50;
    private static final String USER_KEY_PREFIX = "proposal:rate-limit:user:";
    private static final Duration IP_WINDOW = Duration.ofMinutes(5);
    private static final long IP_MAX_SUBMISSIONS = 10;
    private static final String IP_KEY_PREFIX = "proposal:rate-limit:ip:";

    private static final String LUA_CHECK_AND_ADD = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local windowStart = tonumber(ARGV[2])
            local maxAllowed = tonumber(ARGV[3])
            local ttlSeconds = tonumber(ARGV[4])
            local member = ARGV[5]

            redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart)
            local currentCount = redis.call('ZCARD', key)
            if currentCount >= maxAllowed then
                return -1
            end

            redis.call('ZADD', key, now, member)
            redis.call('EXPIRE', key, ttlSeconds)
            return currentCount + 1
            """;

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;

    public ProposalRateLimitService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.rateLimitScript = new DefaultRedisScript<>();
        this.rateLimitScript.setScriptText(LUA_CHECK_AND_ADD);
        this.rateLimitScript.setResultType(Long.class);
    }

    public void enforceFreelancerLimit(String freelancerId) {
        enforceLimit(
                USER_KEY_PREFIX + freelancerId,
                WINDOW,
                MAX_SUBMISSIONS,
                "You can submit at most 50 proposals within 12 hours"
        );
    }

    public void enforceClientIpLimit(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return;
        }

        enforceLimit(
                IP_KEY_PREFIX + clientIp,
                IP_WINDOW,
                IP_MAX_SUBMISSIONS,
                "Too many proposal submissions from your network. Please try again later"
        );
    }

    private void enforceLimit(String key, Duration window, long maxAllowed, String errorMessage) {
        try {
            long nowMillis = System.currentTimeMillis();
            long windowStart = nowMillis - window.toMillis();
            String member = nowMillis + ":" + UUID.randomUUID();

            Long result = stringRedisTemplate.execute(
                    rateLimitScript,
                    List.of(key),
                    String.valueOf(nowMillis),
                    String.valueOf(windowStart),
                    String.valueOf(maxAllowed),
                    String.valueOf(window.toSeconds() + 60),
                    member
            );

            if (result == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Rate limit check failed");
            }

            if (result < 0) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, errorMessage);
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Redis rate limit check failed for key={}", key, ex);
            // Fail open to avoid blocking legitimate submissions if Redis is temporarily unavailable.
        }
    }
}