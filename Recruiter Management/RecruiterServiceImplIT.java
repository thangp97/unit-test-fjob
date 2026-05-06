package com.resourceservice.service.impl;

import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import com.resourceservice.interceptor.BearerTokenWrapper;
import com.resourceservice.model.Organization;
import com.resourceservice.model.RecruiterManagement;
import com.resourceservice.model.UserCommon;
import com.resourceservice.repository.OrganizationRepo;
import com.resourceservice.repository.RecruiterManagementRepo;
import com.resourceservice.repository.UserCommonRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import java.util.Map;

import static com.jober.utilsservice.constant.ResponseMessageConstant.CREATED;
import static com.jober.utilsservice.constant.ResponseMessageConstant.FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * ============================================================================
 * Integration Tests cho RecruiterServiceImpl (user-service).
 * ----------------------------------------------------------------------------
 * Scope IT: addNewRecruiter (persist DB), getOrganizationName (read DB).
 * Rollback: Y (@Transactional class-level).
 * ============================================================================
 */
@SpringBootTest
@Testcontainers
@Transactional
@ActiveProfiles("test")
class RecruiterServiceImplIT {

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

    @Autowired private RecruiterServiceImpl service;
    @Autowired private RecruiterManagementRepo recruiterManagementRepo;
    @Autowired private UserCommonRepo userCommonRepo;
    @Autowired private OrganizationRepo organizationRepo;
    @MockBean private BearerTokenWrapper tokenWrapper;

    private UserCommon persistedUser(String suffix) {
        UserCommon user = new UserCommon();
        user.setPhone("0903" + suffix);
        user.setName("User " + suffix);
        user.setEmail("u" + suffix + "@x.com");
        user.setCreationDate(LocalDateTime.now());
        user.setActive(1);
        return userCommonRepo.save(user);
    }

    private Organization persistedOrganization(String name) {
        Organization org = new Organization();
        org.setName(name);
        org.setCreationDate(LocalDateTime.now());
        org.setActive(1);
        return organizationRepo.save(org);
    }

    /**
     * TC_REC_023 - addNewRecruiter - Standard
     * Muc tieu: Verify save tren DB that va record duoc persist.
     * CheckDB: Y
     * Rollback: Y
     */
    @Test
    void TC_REC_023_addNewRecruiter_persisted() {
        UserCommon user = persistedUser("rec1");
        RecruiterManagement input = new RecruiterManagement();
        input.setUserCommon(user);
        input.setFreelancerid(10L);
        input.setStatus("1");
        input.setNote("0");
        input.setCreationdate(LocalDateTime.now());
        input.setUpdatedate(LocalDateTime.now());
        input.setActive(1);

        ResponseEntity<ResponseObject> response = service.addNewRecruiter(input);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(CREATED, response.getBody().getCode());
        RecruiterManagement saved = (RecruiterManagement) response.getBody().getData();
        assertNotNull(saved);
        assertNotNull(recruiterManagementRepo.findById(saved.getId()).orElse(null));
    }

    /**
     * TC_REC_024 - getOrganizationName - Standard
     * Muc tieu: Verify query tra map tu DB that.
     * CheckDB: Y
     * Rollback: Y
     */
    @Test
    void TC_REC_024_getOrganizationName_found() {
        Organization org = persistedOrganization("OrgIT");

        ResponseEntity<ResponseObject> response = service.getOrganizationName();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(FOUND, response.getBody().getStatus());
        Map<Long, String> actualMap = (Map<Long, String>) response.getBody().getData();
        assertEquals("OrgIT", actualMap.get(org.getId()));
    }
}
