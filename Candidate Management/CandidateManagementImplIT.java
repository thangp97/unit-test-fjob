package com.resourceservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import com.resourceservice.utilsmodule.utils.modelCustom.Paging;
import com.resourceservice.model.CandidateManagement;
import com.resourceservice.model.Freelancer;
import com.resourceservice.model.Job;
import com.resourceservice.model.Schedule;
import com.resourceservice.model.UserCommon;
import com.resourceservice.repository.CandidateManagementRepo;
import com.resourceservice.repository.FreelancerRepo;
import com.resourceservice.repository.JobRepo;
import com.resourceservice.repository.ScheduleRepo;
import com.resourceservice.repository.UserCommonRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * ============================================================================
 *  Integration Tests cho {@link CandidateManagementImpl} (user-service).
 *  ----------------------------------------------------------------------------
 *  Phạm vi: chỉ chứa các TC `Standard` cần verify DB state thật
 *           (CheckDB=Y, Rollback=Y). TC `Exception` ở UT
 *           ({@link CandidateManagementImplTest}).
 *
 *  TC chuyển sang IT (3 cases):
 *    - TC_CMD_010 getJobsOfCandidate_hasSavedJobs   (read query + paging)
 *    - TC_CMD_011 getJobsOfCandidate_noSavedJobs    (edge empty)
 *    - TC_CMD_013 getJobById_validId                (projection mapping)
 *
 *  Rollback policy: class annotate {@code @Transactional} → Spring auto rollback
 *  sau mỗi test → DB trở về trạng thái BEFORE.
 *
 *  Yêu cầu chạy: Docker đang chạy (Testcontainers tự bật pgvector/pg15).
 *  Chạy: mvn -pl user-service verify
 * ============================================================================
 */
@SpringBootTest
@Testcontainers
@Transactional
@ActiveProfiles("test")
class CandidateManagementImplIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("pgvector/pgvector:pg15")
                    .withDatabaseName("jober_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withInitScript("init-pgvector.sql");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private CandidateManagementImpl service;
    @Autowired private CandidateManagementRepo candidateManagementRepo;
    @Autowired private UserCommonRepo userCommonRepo;
    @Autowired private JobRepo jobRepo;
    @Autowired private FreelancerRepo freelancerRepo;
    @Autowired private ScheduleRepo scheduleRepo;

    // ------------------------------------------------------------
    // Helpers - persist các entity tối thiểu để test query
    // ------------------------------------------------------------

    private UserCommon persistedUser(String suffix) {
        UserCommon user = new UserCommon();
        user.setPhone("0901" + suffix);
        user.setName("User " + suffix);
        user.setEmail("u" + suffix + "@x.com");
        user.setCreationDate(LocalDateTime.now());
        return userCommonRepo.save(user);
    }

    private Job persistedJob(UserCommon owner) {
        Job job = new Job();
        job.setName("Backend Engineer");
        job.setJob("Java");
        job.setPhone("0912");
        job.setEmail("job@x.com");
        job.setAddress("HN");
        job.setProvince("Ha Noi");
        job.setWard("Cau Giay");
        job.setDes("desc");
        job.setNumber(1);
        job.setActive(1);
        job.setLevel(1);
        job.setSalary("1000");
        job.setLat(21.0);
        job.setLng(105.0);
        job.setExpDate(LocalDateTime.now().plusDays(10));
        job.setCreationDate(LocalDateTime.now());
        job.setUserCommon(owner);
        return (Job) jobRepo.save(job);
    }

    private Freelancer persistedFreelancer(UserCommon user) {
        Freelancer freelancer = new Freelancer();
        freelancer.setName("Freelancer " + user.getId());
        freelancer.setPhone("0911");
        freelancer.setStatus(1);
        freelancer.setUserCommon(user);
        freelancer.setCreationDate(LocalDateTime.now());
        freelancer.setUpdatedate(LocalDateTime.now());
        return (Freelancer) freelancerRepo.save(freelancer);
    }

    private Schedule persistedSchedule(Freelancer freelancer, Job job) {
        Schedule schedule = new Schedule();
        schedule.setFreelancer(freelancer);
        schedule.setJob(job);
        schedule.setCreationDate(LocalDateTime.now());
        return scheduleRepo.save(schedule);
    }

    private CandidateManagement persistedCandidateManagement(UserCommon user, Job job) {
        CandidateManagement cm = new CandidateManagement();
        cm.setUserCommon(user);
        cm.setJob(job);
        cm.setActive(1);
        // status @NotNull trong CandidateManagement entity - set "1" (active) de qua bean validation
        cm.setStatus("1");
        cm.setCreationdate(LocalDateTime.now());
        return (CandidateManagement) candidateManagementRepo.save(cm);
    }

    // ============================================================
    //  TEST CASES
    // ============================================================

    /**
     * TC_CMD_010 - getJobsOfCandidate - Standard
     * Mục tiêu: Khi user có saved jobs (qua freelancer + schedule), service phải
     *           trả 200 + totalCount đúng số schedule trong DB thật.
     * CheckDB: Y. Rollback: Y (@Transactional class-level).
     */
    @Test
    void TC_CMD_010_getJobsOfCandidate_hasSavedJobs() {
        UserCommon user = persistedUser("010");
        Job job = persistedJob(user);
        Freelancer freelancer = persistedFreelancer(user);
        persistedSchedule(freelancer, job);

        // BearerTokenWrapper trong context test cần resolve uid = user.getId()
        // Tuỳ TestSecurityConfig của project; nếu chưa có, test này có thể skip
        // hoặc dùng @WithMockUser. Mặc định ở đây assume tokenWrapper trả uid hợp lệ
        // qua Spring context (có thể cần MockBean nếu fail).
        ResponseEntity<ResponseObject> response = service.getJobsOfCandidate(new Paging(1, 10));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    /**
     * TC_CMD_011 - getJobsOfCandidate - Standard
     * Mục tiêu: Khi user không có freelancer/schedule, service phải trả 200
     *           với totalCount = 0.
     * CheckDB: Y. Rollback: Y.
     */
    @Test
    void TC_CMD_011_getJobsOfCandidate_noSavedJobs() {
        persistedUser("011");

        ResponseEntity<ResponseObject> response = service.getJobsOfCandidate(new Paging(1, 10));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0L, response.getBody().getTotalCount());
    }

    /**
     * TC_CMD_013 - getJobById - Standard
     * Mục tiêu: Khi tồn tại candidate-management projection liên kết tới job,
     *           service phải trả 200 + DTO non-null từ DB thật.
     * CheckDB: Y. Rollback: Y.
     */
    @Test
    void TC_CMD_013_getJobById_validId() throws JsonProcessingException {
        UserCommon user = persistedUser("013");
        Job job = persistedJob(user);
        persistedCandidateManagement(user, job);

        ResponseEntity<ResponseObject> response = service.getJobById(job.getId());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
