package com.resourceservice.service.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.resourceservice.dto.JobDTO;
import com.resourceservice.dto.JobDetailDto;
import com.resourceservice.dto.request.JobParamDTO;
import com.resourceservice.dto.request.JobParamSearchDTO;
import com.resourceservice.interceptor.BearerTokenWrapper;
import com.resourceservice.model.Job;
import com.resourceservice.model.JobDefault;
import com.resourceservice.model.Organization;
import com.resourceservice.model.UserCommon;
import com.resourceservice.repository.JobRepo;
import com.resourceservice.repository.JobDefaultRepo;
import com.resourceservice.repository.OrganizationRepo;
import com.resourceservice.repository.UserCommonRepo;
import com.jober.utilsservice.utils.modelCustom.Paging;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Integration tests for JobServiceImpl.
 * Verifies end-to-end flows including database persistence and external service integration.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
public class JobServiceImplIntegrationTest {

    @Autowired
    private JobServiceImpl jobService;

    @Autowired
    private JobRepo jobRepo;

    @Autowired
    private UserCommonRepo userCommonRepo;

    @Autowired
    private OrganizationRepo organizationRepo;

    @Autowired
    private JobDefaultRepo jobDefaultRepo;

    @MockBean
    private BearerTokenWrapper tokenWrapper;

    private WireMockServer wireMockServer;

    private UserCommon testUser;
    private Organization testOrg;
    private JobDefault testJobDefault;

    @Autowired
    private javax.persistence.EntityManager entityManager;

    @Before
    public void setup() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8089));
        wireMockServer.start();
        configureFor("localhost", 8089);

        // Initialize database state
        try {
            entityManager.createNativeQuery("CREATE EXTENSION IF NOT EXISTS vector").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE job DROP COLUMN IF EXISTS embedding").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE job ADD COLUMN embedding vector(3)").executeUpdate();
        } catch (Exception e) {
            // Log or ignore if extension already exists
        }

        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        
        testOrg = Organization.builder()
                .name("Test Org " + uniqueId)
                .active(1)
                .build();
        testOrg = organizationRepo.save(testOrg);

        testUser = UserCommon.builder()
                .phone("0123" + uniqueId)
                .organizationId(testOrg.getId())
                .active(1)
                .build();
        testUser = userCommonRepo.save(testUser);

        testJobDefault = JobDefault.builder()
                .name("Software Engineer " + uniqueId)
                .build();
        testJobDefault = jobDefaultRepo.save(testJobDefault);

        Mockito.when(tokenWrapper.getUid()).thenReturn(testUser.getId());
    }

    @After
    public void tearDown() {
        wireMockServer.stop();
    }

    /**
     * TC_036: saveJob - Happy path
     * Objective: Verify that a new job can be created and persisted in the database.
     * Input: JobDTO with required fields.
     * Expected: HTTP 200 OK and job entry exists in the database.
     * CheckDB: Y (Verifies persistence in 'job' table)
     * Rollback: Y (@Transactional)
     */
    @Test
    public void saveJob_WhenValidData_ShouldPersistInDatabase() {
        stubFor(post(urlEqualTo("/encode-job"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"ok\", \"embedding\": \"[0.1, 0.2, 0.3]\"}")));

        JobDTO jobDTO = new JobDTO();
        jobDTO.setName("Java Developer Integration");
        jobDTO.setJobDefaultId(testJobDefault.getId());
        jobDTO.setSalary("1000");
        jobDTO.setProvince("Hanoi");
        jobDTO.setLat(10.0);
        jobDTO.setLng(106.0);

        ResponseEntity<ResponseObject> response = jobService.saveJob(jobDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(jobRepo.findAll().stream().anyMatch(j -> "Java Developer Integration".equals(((Job) j).getName())));
    }

    /**
     * TC_037: adminSaveJobPost - Happy path
     * Objective: Verify that an admin can create a job posting for any user.
     * Input: JobDTO including specific user ID and organization.
     * Expected: HTTP 200 OK and data is saved.
     * CheckDB: Y (Verifies persistence)
     * Rollback: Y (@Transactional)
     */
    @Test
    public void adminSaveJobPost_WhenAdminInputIsValid_ShouldSaveJob() {
        stubFor(post(urlEqualTo("/encode-job"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"ok\", \"embedding\": \"[0.1, 0.2, 0.3]\"}")));

        JobDTO jobDTO = new JobDTO();
        jobDTO.setName("Admin Posted Job");
        jobDTO.setJobDefaultId(testJobDefault.getId());
        jobDTO.setUserId(testUser.getId());
        jobDTO.setCompanyName(testOrg.getName());
        jobDTO.setSalary("2000");
        jobDTO.setLat(10.0);
        jobDTO.setLng(106.0);

        ResponseEntity<ResponseObject> response = jobService.adminSaveJobPost(jobDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getData());
    }

    /**
     * TC_038: getPageJobs - Happy path
     * Objective: Verify retrieval of paginated job list with data integration.
     * Input: JobParamDTO with paging.
     * Expected: Page of JobDetailDto matching existing database records.
     * CheckDB: Y (Verifies retrieval from 'job' table)
     * Rollback: Y (@Transactional)
     */
    @Test
    public void getPageJobs_WhenJobsExist_ShouldReturnPaginatedList() {
        Job job = Job.builder()
                .name("Integration Job 1")
                .jobDefault(testJobDefault)
                .expDate(LocalDateTime.now().plusDays(10))
                .active(1)
                .lat(10.0).lng(106.0).userCommon(testUser).build();
        jobRepo.save(job);

        JobParamDTO params = new JobParamDTO();
        params.setPaging(new Paging(1, 10));

        Page<JobDetailDto> result = jobService.getPageJobs(params);

        assertFalse(result.isEmpty());
        assertTrue(result.getContent().stream().anyMatch(j -> "Integration Job 1".equals(j.getName())));
    }

    /**
     * TC_039: getCountPageJob - Happy path
     * Objective: Verify total count of jobs matching search criteria.
     * Input: JobParamDTO criteria.
     * Expected: Long count value >= 1.
     * CheckDB: Y (Verifies count query)
     * Rollback: Y (@Transactional)
     */
    @Test
    public void getCountPageJob_WhenCriteriaMatches_ShouldReturnCount() {
        Job job = Job.builder()
                .name("Count Job")
                .jobDefault(testJobDefault)
                .expDate(LocalDateTime.now().plusDays(10))
                .active(1)
                .lat(10.0).lng(106.0).userCommon(testUser).build();
        jobRepo.save(job);

        JobParamDTO params = new JobParamDTO();
        Long count = jobService.getCountPageJob(params);

        assertTrue(count >= 1);
    }

    /**
     * TC_040: getPageJobsV2 - Happy path
     * Objective: Verify retrieval of job recommendations via Python service integration.
     * Input: Valid user ID.
     * Expected: List of JobDetailDto from mocked recommendation service.
     * CheckDB: Y (Database setup for user context)
     * Rollback: Y (@Transactional)
     */
    @Test
    public void getPageJobsV2_WhenPythonServiceResponds_ShouldReturnRecommendations() {
        stubFor(post(urlEqualTo("/recommend"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\": 1, \"name\": \"Recommended Job\"}]")));

        List<JobDetailDto> result = jobService.getPageJobsV2(testUser.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Recommended Job", result.get(0).getName());
    }

    /**
     * TC_041: getRecommendationsByUser - Happy path
     * Objective: Verify paginated recommendations from external service.
     * Input: User ID and paging parameters.
     * Expected: Page of JobDetailDto.
     * CheckDB: Y
     * Rollback: Y (@Transactional)
     */
    @Test
    public void getRecommendationsByUser_WhenCalled_ShouldReturnPaginatedRecommendations() {
        stubFor(post(urlMatching("/recommend_by_user.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\": 1, \"name\": \"User Recommendation\"}]")));

        Page<JobDetailDto> result = jobService.getRecommendationsByUser(testUser.getId(), 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("User Recommendation", result.getContent().get(0).getName());
    }

    /**
     * TC_042: searchJobsAdvanced - Happy path
     * Objective: Verify advanced search filters including salary range and keywords.
     * Input: JobParamSearchDTO with keyword and salary range.
     * Expected: List of matching job details.
     * CheckDB: Y (Complex filter query)
     * Rollback: Y (@Transactional)
     */
    @Test
    public void searchJobsAdvanced_WhenFiltersApplied_ShouldReturnFilteredJobs() {
        Job job = Job.builder()
                .name("Specialist Java Integration")
                .jobDefault(testJobDefault)
                .expDate(LocalDateTime.now().plusDays(10))
                .active(1)
                .salary("1500")
                .lat(10.0).lng(106.0).userCommon(testUser).build();
        jobRepo.save(job);

        JobParamSearchDTO searchDTO = new JobParamSearchDTO();
        searchDTO.setSearchKey("Specialist");
        searchDTO.setMinSalary("1000");
        searchDTO.setMaxSalary("2000");

        ResponseEntity<ResponseObject> response = jobService.searchJobsAdvanced(searchDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<JobDetailDto> data = (List<JobDetailDto>) response.getBody().getData();
        assertTrue(data.stream().anyMatch(j -> "Specialist Java Integration".equals(j.getName())));
    }

    /**
     * TC_043: getListJobs - Happy path
     * Objective: Verify basic job listing with pagination.
     * Input: JobParamDTO with paging.
     * Expected: ResponseEntity containing list of jobs.
     * CheckDB: Y
     * Rollback: Y (@Transactional)
     */
    @Test
    public void getListJobs_WhenPagingIsValid_ShouldReturnJobList() {
        Job job = Job.builder()
                .name("List Test Job")
                .jobDefault(testJobDefault)
                .expDate(LocalDateTime.now().plusDays(10))
                .active(1)
                .lat(10.0).lng(106.0).userCommon(testUser).build();
        jobRepo.save(job);

        JobParamDTO params = new JobParamDTO();
        params.setPaging(new Paging(1, 10));

        ResponseEntity<ResponseObject> response = jobService.getListJobs(params);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getData());
    }

    /**
     * TC_044: searchJobsAdvanced - MinSalary only
     * Objective: Verify that the search correctly filters by minimum salary.
     * Input: JobParamSearchDTO with only minSalary.
     * Expected: Jobs with salary >= minSalary are returned.
     * CheckDB: Y
     * Rollback: Y (@Transactional)
     */
    @Test
    public void searchJobsAdvanced_WhenMinSalaryProvided_ShouldFilterCorrectly() {
        Job job = Job.builder()
                .name("High Salary Job")
                .jobDefault(testJobDefault)
                .expDate(LocalDateTime.now().plusDays(10))
                .active(1)
                .salary("5000")
                .lat(10.0).lng(106.0).userCommon(testUser).build();
        jobRepo.save(job);

        JobParamSearchDTO searchDTO = new JobParamSearchDTO();
        searchDTO.setMinSalary("4000");

        ResponseEntity<ResponseObject> response = jobService.searchJobsAdvanced(searchDTO);
        List<JobDetailDto> data = (List<JobDetailDto>) response.getBody().getData();
        assertTrue(data.stream().anyMatch(j -> "High Salary Job".equals(j.getName())));
    }

    /**
     * TC_045: searchJobsAdvanced - MaxSalary only
     * Objective: Verify that the search correctly filters by maximum salary.
     * Input: JobParamSearchDTO with only maxSalary.
     * Expected: Jobs with salary <= maxSalary are returned.
     * CheckDB: Y
     * Rollback: Y (@Transactional)
     */
    @Test
    public void searchJobsAdvanced_WhenMaxSalaryProvided_ShouldFilterCorrectly() {
        Job job = Job.builder()
                .name("Low Salary Job")
                .jobDefault(testJobDefault)
                .expDate(LocalDateTime.now().plusDays(10))
                .active(1)
                .salary("1000")
                .lat(10.0).lng(106.0).userCommon(testUser).build();
        jobRepo.save(job);

        JobParamSearchDTO searchDTO = new JobParamSearchDTO();
        searchDTO.setMaxSalary("2000");

        ResponseEntity<ResponseObject> response = jobService.searchJobsAdvanced(searchDTO);
        List<JobDetailDto> data = (List<JobDetailDto>) response.getBody().getData();
        assertTrue(data.stream().anyMatch(j -> "Low Salary Job".equals(j.getName())));
    }

    /**
     * TC_046: searchJobsAdvanced - Combinatorial filters
     * Objective: Verify filtering by working type, experience level, and province.
     * Input: workingType, requiredExperienceLevel, and province.
     * Expected: Only jobs matching all criteria are returned.
     * CheckDB: Y
     * Rollback: Y (@Transactional)
     */
    @Test
    public void searchJobsAdvanced_WhenMultiFiltersApplied_ShouldReturnExactMatches() {
        Job job = Job.builder()
                .name("Full Filter Job")
                .jobDefault(testJobDefault)
                .expDate(LocalDateTime.now().plusDays(10))
                .active(1)
                .workingType(1)
                .requiredExperienceLevel(2)
                .province("Danang")
                .lat(10.0).lng(106.0).userCommon(testUser).build();
        jobRepo.save(job);

        JobParamSearchDTO searchDTO = new JobParamSearchDTO();
        searchDTO.setWorkingType(1);
        searchDTO.setRequiredExperienceLevel(2);
        searchDTO.setProvince("Danang");
        searchDTO.setJobDefaultId(testJobDefault.getId());

        ResponseEntity<ResponseObject> response = jobService.searchJobsAdvanced(searchDTO);
        List<JobDetailDto> data = (List<JobDetailDto>) response.getBody().getData();
        assertTrue(data.stream().anyMatch(j -> "Full Filter Job".equals(j.getName())));
    }
}
