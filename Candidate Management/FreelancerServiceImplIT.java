package com.resourceservice.service.impl;

import com.jober.utilsservice.constant.ResponseMessageConstant;
import com.jober.utilsservice.utils.modelCustom.Coordinates;
import com.jober.utilsservice.utils.modelCustom.Paging;
import com.jober.utilsservice.utils.modelCustom.Response;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import com.resourceservice.dto.request.JobParamDTO;
import com.resourceservice.dto.response.OrganizationDetailResponse;
import com.resourceservice.model.Freelancer;
import com.resourceservice.model.Organization;
import com.resourceservice.model.UserCommon;
import com.resourceservice.repository.FreelancerRepo;
import com.resourceservice.repository.OrganizationRepo;
import com.resourceservice.repository.UserCommonRepo;
import com.resourceservice.dto.UserCommonDTO;
import com.resourceservice.interceptor.BearerTokenWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.jober.utilsservice.constant.Constant.INACTIVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * ============================================================================
 *  Integration Tests cho {@link FreelancerServiceImpl} (user-service).
 *  ----------------------------------------------------------------------------
 *  Phạm vi: chỉ chứa các TC `Standard` cần verify DB state thật
 *           (CheckDB=Y, Rollback=Y). TC `Exception` ở UT
 *           ({@link FreelancerServiceImplTest}).
 *
 *  TC chuyển sang IT (4 cases tiêu biểu):
 *    - TC_FL_008  deleteByIds_success                       (bulk update INACTIVE)
 *    - TC_FL_012  getListFreelancer_hasData                 (JPQL paging)
 *    - TC_FL_024  getFreelancerByUserIdAndJobDefaultId_existing (composite lookup)
 *    - TC_FL_042  getOrganizationDetail_found               (org real lookup)
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
@SuppressWarnings("unchecked")
class FreelancerServiceImplIT {

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

    @Autowired private FreelancerServiceImpl service;
    @Autowired private FreelancerRepo freelancerRepo;
    @Autowired private OrganizationRepo organizationRepo;
    @Autowired private UserCommonRepo userCommonRepo;
    @MockBean private CacheManagerService cacheManagerService;
    @MockBean private BearerTokenWrapper tokenWrapper;

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private UserCommon persistedUser(String suffix) {
        UserCommon user = new UserCommon();
        user.setPhone("0902" + suffix);
        user.setName("User " + suffix);
        user.setEmail("u" + suffix + "@x.com");
        user.setCreationDate(LocalDateTime.now());
        return userCommonRepo.save(user);
    }

    private Freelancer persistedFreelancer(UserCommon user, Long jobDefaultId) {
        Freelancer freelancer = new Freelancer();
        freelancer.setName("Freelancer " + user.getId());
        freelancer.setPhone("0911" + user.getId());
        freelancer.setStatus(1);
        freelancer.setUserCommon(user);
        if (jobDefaultId != null) {
            freelancer.setJobDefaultId(jobDefaultId);
        }
        freelancer.setCreationDate(LocalDateTime.now());
        freelancer.setUpdatedate(LocalDateTime.now());
        return (Freelancer) freelancerRepo.save(freelancer);
    }

    private Organization persistedOrganization(String name) {
        Organization org = new Organization();
        org.setName(name);
        org.setCreationDate(LocalDateTime.now());
        return organizationRepo.save(org);
    }

    // ============================================================
    //  TEST CASES
    // ============================================================

    /**
     * TC_FL_008 - deleteByIds - Standard
     * Mục tiêu: Bulk delete (soft) freelancer hợp lệ → reload từ DB,
     *           status phải = INACTIVE.
     * CheckDB: Y. Rollback: Y.
     */
    @Test
    void TC_FL_008_deleteByIds_success() {
        UserCommon user1 = persistedUser("8a");
        UserCommon user2 = persistedUser("8b");
        Freelancer f1 = persistedFreelancer(user1, 10L);
        Freelancer f2 = persistedFreelancer(user2, 11L);
        List<Long> ids = Arrays.asList(f1.getId(), f2.getId());

        ResponseEntity<Response> response = service.deleteByIds(ids);

        assertEquals(ResponseMessageConstant.UPDATED, response.getBody().getCode());
        Freelancer reloaded1 = (Freelancer) freelancerRepo.findById(f1.getId()).orElseThrow();
        Freelancer reloaded2 = (Freelancer) freelancerRepo.findById(f2.getId()).orElseThrow();
        assertEquals(INACTIVE, reloaded1.getStatus(), "f1.status phai bi update thanh INACTIVE");
        assertEquals(INACTIVE, reloaded2.getStatus(), "f2.status phai bi update thanh INACTIVE");
    }

    /**
     * TC_FL_012 - getListFreelancer - Standard
     * Mục tiêu: Khi DB có ít nhất 1 freelancer hợp lệ, JPQL query phải trả 200
     *           (không exception). Verify pipeline EntityManager + native query
     *           hoạt động trên DB thật (pgvector/pg15).
     * CheckDB: Y. Rollback: Y.
     */
    @Test
    void TC_FL_012_getListFreelancer_hasData() {
        UserCommon user = persistedUser("012");
        persistedFreelancer(user, 10L);

        JobParamDTO dto = new JobParamDTO();
        dto.setPaging(new Paging(1, 10));

        ResponseEntity<ResponseObject> response = service.getListFreelancer(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    /**
     * TC_FL_024 - getFreelancerByUserIdAndJobDefaultId - Standard
     * Mục tiêu: Khi tồn tại freelancer (userId+jobDefaultId), service phải trả
     *           200 từ query DB thật.
     * CheckDB: Y. Rollback: Y.
     *
     * Note: BearerTokenWrapper trong Spring context cần resolve uid; nếu chưa có
     *       TestSecurityConfig, có thể cần @MockBean BearerTokenWrapper.
     */
    @Test
    void TC_FL_024_getFreelancerByUserIdAndJobDefaultId_existing() {
        UserCommon user = persistedUser("024");
        persistedFreelancer(user, 10L);

        UserCommonDTO userCommonDTO = new UserCommonDTO();
        userCommonDTO.setId(user.getId());
        when(tokenWrapper.getUid()).thenReturn(user.getId());
        when(cacheManagerService.getUser(user.getId())).thenReturn(userCommonDTO);

        ResponseEntity<ResponseObject> response = service.getFreelancerByUserIdAndJobDefaultId(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    /**
     * TC_FL_042 - getOrganizationDetail - Standard
     * Mục tiêu: Khi organization tồn tại trong DB, service phải trả response
     *           với organization.name khớp giá trị đã seed.
     * CheckDB: Y. Rollback: Y.
     */
    @Test
    void TC_FL_042_getOrganizationDetail_found() {
        Organization org = persistedOrganization("OrgA");

        OrganizationDetailResponse response = service.getOrganizationDetail(org.getId(), 0, 10);

        assertNotNull(response);
        assertNotNull(response.getOrganization());
        assertEquals("OrgA", response.getOrganization().getName());
    }
}
