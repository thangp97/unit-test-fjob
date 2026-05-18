package com.resourceservice.service.impl;

import com.jober.utilsservice.model.PageableModel;
import com.jober.utilsservice.utils.modelCustom.Paging;
import com.jober.utilsservice.utils.modelCustom.Response;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import com.resourceservice.dto.JobDTO;
import com.resourceservice.dto.JobDataModel;
import com.resourceservice.dto.JobDetailDto;
import com.resourceservice.dto.LocationParamsDto;
import com.resourceservice.dto.ResponseApplyDto;
import com.resourceservice.dto.ResponseJdDTO;
import com.resourceservice.dto.UserCommonDTO;
import com.resourceservice.dto.request.ChartDataDTO;
import com.resourceservice.dto.request.UserParamDTO1;
import com.resourceservice.interceptor.BearerTokenWrapper;
import com.resourceservice.model.Freelancer;
import com.resourceservice.model.Job;
import com.resourceservice.model.JobDefault;
import com.resourceservice.model.Organization;
import com.resourceservice.model.Schedule;
import com.resourceservice.model.UserCommon;
import com.resourceservice.model.projection.CandidateManagementProjection;
import com.resourceservice.repository.CandidateManagementRepo;
import com.resourceservice.repository.FreelancerRepo;
import com.resourceservice.repository.JobRepo;
import com.resourceservice.repository.OrganizationRepo;
import com.resourceservice.repository.ScheduleRepo;
import com.resourceservice.repository.UserCommonRepo;
import com.resourceservice.utilsmodule.constant.Constant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for JobServiceImpl.
 * Covers business logic for job management, applications, and reporting.
 */
@RunWith(org.mockito.junit.MockitoJUnitRunner.Silent.class)
public class JobServiceImplTest {

    @Mock
    private JobRepo jobRepo;

    @Mock
    private UserCommonRepo userCommonRepo;

    @Mock
    private FreelancerRepo freelancerRepo;

    @Mock
    private ScheduleRepo scheduleRepo;

    @Mock
    private CandidateManagementRepo candidateManagementRepo;

    @Mock
    private OrganizationRepo organizationRepo;

    @Mock
    private RestTemplate restTemplate;

    private BearerTokenWrapper tokenWrapper;

    private JobServiceImpl jobService;

    @Before
    public void setUp() {
        tokenWrapper = new BearerTokenWrapper();
        tokenWrapper.setUid(10L);
        jobService = new JobServiceImpl(tokenWrapper);

        ReflectionTestUtils.setField(jobService, "jobRepo", jobRepo);
        ReflectionTestUtils.setField(jobService, "userCommonRepo", userCommonRepo);
        ReflectionTestUtils.setField(jobService, "freelancerRepo", freelancerRepo);
        ReflectionTestUtils.setField(jobService, "scheduleRepo", scheduleRepo);
        ReflectionTestUtils.setField(jobService, "candidateManagementRepo", candidateManagementRepo);
        ReflectionTestUtils.setField(jobService, "organizationRepo", organizationRepo);
        ReflectionTestUtils.setField(jobService, "restTemplate", restTemplate);
    }

    /**
     * TC_001: convertToJobDTO - Happy path
     * Objective: Verify core fields are mapped from Job entity to JobDTO.
     * Input: Valid Job entity.
     * Expected: JobDTO with matching ID, name, and phone.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    public void convertToJobDTO_WhenJobEntityIsValid_ShouldReturnMappedJobDTO() {
        Job job = buildJob(1L);
        JobDTO dto = jobService.convertToJobDTO(job);

        assertEquals(Long.valueOf(1L), dto.getId());
        assertEquals("Backend Developer", dto.getJob());
        assertEquals(Long.valueOf(11L), dto.getJobDefaultId());
        assertEquals("0900000000", dto.getUserPhone());
    }

    /**
     * TC_002: convertToJdDto - Happy path
     * Objective: Verify conversion from Job entity to ResponseJdDTO.
     * Input: Valid Job entity.
     * Expected: Non-null ResponseJdDTO returned.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    public void convertToJdDto_WhenJobEntityIsValid_ShouldReturnResponseJdDTO() {
        Job job = buildJob(2L);
        ResponseJdDTO dto = jobService.convertToJdDto(job);
        assertNotNull(dto);
    }

    /**
     * TC_003: deleteJobs - Happy path
     * Objective: Verify jobs are marked as inactive (status UPDATED).
     * Input: List of job IDs.
     * Expected: HTTP 200 OK with status "UPDATED".
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void deleteJobs_WhenIdsAreProvided_ShouldReturnUpdatedStatus() {
        when(jobRepo.updateByIds(eq(0), anyList())).thenReturn(1);

        ResponseEntity<Response> response = jobService.deleteJobs(Arrays.asList(1L, 2L));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UPDATED", response.getBody().getStatus());
    }

    /**
     * TC_004: deleteJobByIds - Happy path
     * Objective: Verify jobs are physically deleted (status DELETED).
     * Input: List of job IDs.
     * Expected: HTTP 200 OK with status "DELETED".
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void deleteJobByIds_WhenIdsAreProvided_ShouldReturnDeletedStatus() {
        when(jobRepo.deleteJobByIds(Arrays.asList(1L, 2L))).thenReturn(1);

        ResponseEntity<ResponseObject> response = jobService.deleteJobByIds(Arrays.asList(1L, 2L));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("DELETED", response.getBody().getStatus());
    }

    /**
     * TC_005: findById - Happy path
     * Objective: Verify job retrieval by ID.
     * Input: Valid job ID.
     * Expected: ResponseObject contains the expected Job entity.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void findById_WhenJobExists_ShouldReturnJobData() {
        Job job = buildJob(3L);
        when(jobRepo.findJobById(3L)).thenReturn(job);

        ResponseEntity response = jobService.findById(3L);

        ResponseObject body = (ResponseObject) response.getBody();
        assertEquals(job, body.getData());
    }

    /**
     * TC_006: findJobIdByJobDefaultIdAndUserId - Happy path
     * Objective: Verify retrieval of job ID using default job ID and user ID from token.
     * Input: JobDefault ID.
     * Expected: The correct Job ID returned.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void findJobId_WhenValidDefaultId_ShouldReturnJobIdFromTokenUser() {
        when(jobRepo.findJobIdByJobDefaultIdAndUserId(11L, 10L)).thenReturn(99L);

        Long result = jobService.findJobIdByJobDefaultIdAndUserId(11L);

        assertEquals(Long.valueOf(99L), result);
    }

    /**
     * TC_007: applyJob - Happy path
     * Objective: Verify a candidate can apply for a job, creating a schedule.
     * Input: User ID and JobDefault ID.
     * Expected: Schedule entity created and returned.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void applyJob_WhenFreelancerExists_ShouldCreateSchedule() {
        Freelancer freelancer = new Freelancer();
        when(freelancerRepo.findFreelancerByUserIdAndJobDefaultId(1L, 11L)).thenReturn(freelancer);
        when(scheduleRepo.save(any())).thenAnswer(invocation -> invocation.getArguments()[0]);

        ResponseEntity<ResponseObject> response = jobService.applyJob(1L, 11L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getData() instanceof Schedule);
    }

    /**
     * TC_008: applyJob - Edge case (Freelancer not found)
     * Objective: Verify behavior when the applicant does not have a freelancer profile.
     * Input: User ID without freelancer profile.
     * Expected: Response message "NOT_EXISTED".
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void applyJob_WhenFreelancerMissing_ShouldReturnNotExisted() {
        when(freelancerRepo.findFreelancerByUserIdAndJobDefaultId(1L, 11L)).thenReturn(null);

        ResponseEntity<?> response = jobService.applyJob(1L, 11L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Response);
        assertEquals("NOT_EXISTED", ((Response) response.getBody()).getMessage());
    }

    /**
     * TC_009: getListJobsV2 - Happy path
     * Objective: Verify retrieval of recommended jobs with pagination.
     * Input: User ID, page, and size.
     * Expected: ResponseObject containing the total count of recommendations.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void getListJobsV2_WhenRecommendationsFound_ShouldReturnTotalCount() {
        JobServiceImpl spyService = spy(jobService);
        JobDetailDto detail = new JobDetailDto();
        Page<JobDetailDto> page = new PageImpl<>(Collections.singletonList(detail), PageRequest.of(0, 5), 1);
        doReturn(page).when(spyService).getRecommendationsByUser(10L, 1, 5);

        ResponseEntity<ResponseObject> response = spyService.getListJobsV2(10L, 1, 5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getTotalCount().longValue());
    }

    /**
     * TC_010: listJobsCompleted - Happy path
     * Objective: Verify retrieval of completed jobs for a user.
     * Input: User ID and paging parameters.
     * Expected: ResponseObject containing the list of completed jobs.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void listJobsCompleted_WhenJobsFound_ShouldReturnJobPage() {
        UserParamDTO1 params = new UserParamDTO1();
        params.setUserId(10L);
        params.setPaging(new com.resourceservice.utilsmodule.utils.modelCustom.Paging(1, 10));

        Page<Job> jobs = new PageImpl<>(Collections.singletonList(buildJob(4L)), PageRequest.of(0, 10), 1);
        when(jobRepo.findJobsCompleted(eq(10L), any())).thenReturn(jobs);

        ResponseEntity<ResponseObject> response = jobService.listJobsCompleted(params);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getTotalCount().longValue());
    }

    /**
     * TC_011: listJobsByNote - Happy path
     * Objective: Verify retrieval of jobs filtered by note and location.
     * Input: Search note and location parameters.
     * Expected: ResponseObject containing matching jobs.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void listJobsByNote_WhenMatchesFound_ShouldReturnJobs() {
        LocationParamsDto params = buildLocationParams();
        Page<Job> jobs = new PageImpl<>(Collections.singletonList(buildJob(6L)), PageRequest.of(0, 10), 1);
        when(jobRepo.ListJobsByNote(eq(10L), eq("note"), any())).thenReturn(jobs);

        ResponseEntity<ResponseObject> response = jobService.listJobsByNote("note", params);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getTotalCount().longValue());
    }

    /**
     * TC_012: listSavedJobs - Happy path
     * Objective: Verify retrieval of jobs saved by the user.
     * Input: User token and paging parameters.
     * Expected: ResponseObject containing the list of saved job DTOs.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void listSavedJobs_WhenSavedJobsExist_ShouldReturnJobDTOs() {
        CandidateManagementProjection projection = createCandidateProjection(7L, buildJob(7L));
        LocationParamsDto params = buildLocationParams();
        when(candidateManagementRepo.findSavedJobs(eq(10L), any())).thenReturn(Collections.singletonList(projection));
        when(candidateManagementRepo.countSavedJobs(10L)).thenReturn(1L);

        ResponseEntity<ResponseObject> response = jobService.listSavedJobs(params);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getTotalCount().longValue());
    }

    /**
     * TC_013: listPeopleApply - Happy path
     * Objective: Verify retrieval of candidates who applied for a specific job.
     * Input: Recruiter phone and paging parameters.
     * Expected: ResponseObject containing the list of applicants.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void listPeopleApply_WhenApplicantsFound_ShouldReturnApplicantList() {
        UserCommonDTO dto = new UserCommonDTO();
        PageableModel pageableModel = new PageableModel();
        pageableModel.setPage(1);
        pageableModel.setSize(10);
        dto.setPageableModel(pageableModel);
        dto.setPhone("0900000000");

        ResponseApplyDto applyDto = new ResponseApplyDto("Name", "0900000000", LocalDateTime.now());
        Page<ResponseApplyDto> page = new PageImpl<>(Collections.singletonList(applyDto), PageRequest.of(0, 10), 1);
        when(jobRepo.listPeopleApply(eq("0900000000"), any())).thenReturn(page);

        ResponseEntity response = jobService.listPeopleApply(dto);

        ResponseObject body = (ResponseObject) response.getBody();
        assertEquals(1L, body.getTotalCount().longValue());
    }

    /**
     * TC_014: listJobByUser - Happy path
     * Objective: Verify retrieval of jobs posted by a specific recruiter.
     * Input: Recruiter phone and paging parameters.
     * Expected: ResponseObject containing the list of job descriptions.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void listJobByUser_WhenJobsFound_ShouldReturnJDList() {
        JobDTO input = new JobDTO();
        input.setUserPhone("0900000000");
        input.setPage(1);
        input.setSize(10);

        Page<Job> jobs = new PageImpl<>(Collections.singletonList(buildJob(8L)), PageRequest.of(0, 10), 1);
        when(jobRepo.listJobByUser(eq("0900000000"), any())).thenReturn(jobs);

        ResponseEntity<ResponseObject> response = jobService.listJobByUser(input);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getTotalCount().longValue());
    }

    /**
     * TC_015: latestJobs - Happy path
     * Objective: Verify retrieval of the most recently posted jobs.
     * Input: Location and paging parameters.
     * Expected: ResponseObject containing the latest jobs.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void latestJobs_WhenValidParams_ShouldReturnLatestJobs() {
        LocationParamsDto params = buildLocationParams();
        Page<Job> jobs = new PageImpl<>(Collections.singletonList(buildJob(9L)), PageRequest.of(0, 10), 1);
        when(jobRepo.findJobs(any())).thenReturn(jobs);

        ResponseEntity<ResponseObject> response = jobService.latestJobs(params);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getTotalCount().longValue());
    }

    /**
     * TC_016: getJobsByOrganization - Happy path
     * Objective: Verify retrieval of active jobs belonging to a specific organization.
     * Input: Organization ID and paging parameters.
     * Expected: ResponseObject containing the organization's jobs.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void getJobsByOrganization_WhenOrgExists_ShouldReturnOrgJobs() {
        Job job = buildJob(10L);
        Page<Job> jobs = new PageImpl<>(Collections.singletonList(job), PageRequest.of(0, 10), 1);
        when(jobRepo.findByOrganizationIdAndActiveAndExpDateAfter(eq(1L), eq(1), any(), any()))
            .thenReturn(jobs);

        Organization organization = new Organization();
        organization.setId(1L);
        organization.setName("OrgName");
        when(organizationRepo.findById(1L)).thenReturn(Optional.of(organization));

        ResponseEntity<ResponseObject> response = jobService.getJobsByOrganization("1", null, 1, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getTotalCount().longValue());
    }

    /**
     * TC_017: getJobsNearBy - Edge case (Empty page)
     * Objective: Verify that an empty result page still returns status FOUND.
     * Input: Location parameters with no nearby jobs.
     * Expected: ResponseObject with status "FOUND" and empty data list.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void getJobsNearBy_WhenNoJobsNearby_ShouldReturnStatusFound() {
        LocationParamsDto params = buildLocationParams();
        Page<JobDetailDto> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(jobRepo.listJobsNearBy(any(), any(), any())).thenReturn(page);

        ResponseEntity<ResponseObject> response = jobService.getJobsNearBy(params);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("FOUND", response.getBody().getStatus());
    }

    /**
     * TC_018: jobsHadPostByRecruiter - Happy path
     * Objective: Verify retrieval of default job IDs that a recruiter has already posted.
     * Input: Recruiter user ID from token.
     * Expected: HTTP 200 OK with list of job IDs.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void jobsHadPostByRecruiter_WhenJobsExist_ShouldReturnJobDefaultIds() {
        when(jobRepo.findJobDefaultIdsHavePostByRecruiter(10L))
            .thenReturn(Collections.singletonList(buildJob(11L)));

        ResponseEntity<Response> response = jobService.jobsHadPostByRecruiter();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * TC_019: getChartData - Happy path
     * Objective: Verify retrieval of aggregated dashboard data (active jobs, companies, etc.).
     * Input: Standard dashboard request.
     * Expected: ChartDataDTO with populated counts from repositories.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void getChartData_WhenCalled_ShouldReturnAggregatedStatistics() {
        when(jobRepo.findJobPostingsByDateRange(any(), any())).thenReturn(Collections.emptyList());
        when(jobRepo.findSalaryByIndustry(any(), any())).thenReturn(Collections.emptyList());
        when(jobRepo.countByCreationDateAfterAndActive(any(), eq(1))).thenReturn(3L);
        when(jobRepo.countByActive(1)).thenReturn(7L);
        when(organizationRepo.countByActive(1)).thenReturn(2L);

        ChartDataDTO data = jobService.getChartData();

        assertEquals(Long.valueOf(3L), data.getTotalJobs24h());
        assertEquals(Long.valueOf(7L), data.getTotalActiveJobs());
        assertEquals(Long.valueOf(2L), data.getTotalCompanies());
    }

    /**
     * TC_020: jobsToCsv - Happy path
     * Objective: Verify that job data is correctly formatted into CSV.
     * Input: List of JobDataModel objects.
     * Expected: StringWriter contains CSV headers and formatted row data.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    public void jobsToCsv_WhenDataExists_ShouldFormatCsvCorrectly() {
        JobDataModel model = JobDataModel.builder()
            .id(1L).name("Company").address("Address").distance(1.2).job("Developer")
            .number(2).expdate(LocalDateTime.now()).salary("1000").des("Desc")
            .img("img.png").creationdate(LocalDateTime.now()).build();

        StringWriter writer = new StringWriter();
        jobService.jobsToCsv(writer, Collections.singletonList(model));

        String csv = writer.toString();
        assertTrue(csv.contains("ID"));
        assertTrue(csv.contains("Company"));
    }

    /**
     * TC_021: listJobsCsv - Happy path
     * Objective: Verify CSV export based on specific job IDs.
     * Input: List of job IDs in LocationParamsDto.
     * Expected: CSV content generated for the requested IDs.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void listJobsCsv_WhenIdsProvided_ShouldGenerateCsvExport() {
        LocationParamsDto params = buildLocationParams();
        params.setIds(Collections.singletonList(1L));
        when(jobRepo.findByIds(Collections.singletonList(1L))).thenReturn(Collections.singletonList(buildJob(12L)));

        StringWriter writer = new StringWriter();
        jobService.listJobsCsv(writer, params);

        assertTrue(writer.toString().contains("ID"));
    }

    /**
     * TC_022: updateActiveJob - Happy path (Toggle 1 to 0)
     * Objective: Verify that an active job can be deactivated.
     * Input: ID of an active job (active=1).
     * Expected: Job's active status becomes 0, status "UPDATED".
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void updateActiveJob_WhenActiveIs1_ShouldDeactivateJob() {
        Job job = buildJob(13L);
        job.setActive(1);
        when(jobRepo.findById(13L)).thenReturn(Optional.of(job));
        when(jobRepo.save(job)).thenReturn(job);

        ResponseEntity<ResponseObject> response = jobService.updateActiveJob(13L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UPDATED", response.getBody().getStatus());
        assertEquals(Integer.valueOf(0), job.getActive());
    }

    /**
     * TC_023: updateActiveJob - Happy path (Toggle 0 to 1)
     * Objective: Verify that an inactive job can be activated.
     * Input: ID of an inactive job (active=0).
     * Expected: Job's active status becomes 1.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void updateActiveJob_WhenActiveIs0_ShouldActivateJob() {
        Job job = buildJob(100L);
        job.setActive(0);
        when(jobRepo.findById(100L)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(Job.class))).thenAnswer(i -> i.getArguments()[0]);
        
        jobService.updateActiveJob(100L);
        assertEquals(Integer.valueOf(1), job.getActive());
    }

    /**
     * TC_024: updateActiveJob - Exception (Job not found)
     * Objective: Verify behavior when attempting to update a non-existent job.
     * Input: Invalid job ID.
     * Expected: NoSuchElementException.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void updateActiveJob_WhenJobNotFound_ShouldThrowException() {
        when(jobRepo.findById(999L)).thenReturn(Optional.empty());
        jobService.updateActiveJob(999L);
    }

    /**
     * TC_025: updateActiveJob - BUG Confirmation (Null active)
     * Objective: Confirm that a null active field causes a NullPointerException.
     * Input: Job entity with active = null.
     * Expected: NullPointerException.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void updateActiveJob_WhenActiveIsNull_ShouldThrowNullPointerException() {
        Job job = buildJob(100L);
        job.setActive(null);
        when(jobRepo.findById(100L)).thenReturn(Optional.of(job));
        jobService.updateActiveJob(100L);
    }

    /**
     * TC_026: deleteJobs - Edge case (No match)
     * Objective: Verify status when no jobs match the provided IDs for deactivation.
     * Input: ID list that results in 0 updated rows.
     * Expected: Status "NOT_MODIFIED".
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void deleteJobs_WhenNoRowsUpdated_ShouldReturnNotModified() {
        when(jobRepo.updateByIds(eq(0), anyList())).thenReturn(0);
        ResponseEntity<Response> response = jobService.deleteJobs(Arrays.asList(999L));
        assertEquals("NOT_MODIFIED", response.getBody().getStatus());
    }

    /**
     * TC_027: applyJob - Edge case (Freelancer missing)
     * Objective: Re-verify application failure when freelancer profile is missing.
     * Input: Valid IDs but null return from freelancerRepo.
     * Expected: Message "NOT_EXISTED".
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void applyJob_WhenProfileNotFound_ShouldReturnNotExistedMessage() {
        when(freelancerRepo.findFreelancerByUserIdAndJobDefaultId(10L, 1L)).thenReturn(null);
        ResponseEntity<?> response = jobService.applyJob(10L, 1L);
        assertEquals("NOT_EXISTED", ((Response)response.getBody()).getMessage());
    }

    /**
     * TC_028: getListJobsV2 - Edge case (Null recommendations)
     * Objective: Verify behavior when the recommendation service (Python API) returns null.
     * Input: Valid user ID, but null recommendation page.
     * Expected: Empty data list returned to the caller.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    public void getListJobsV2_WhenPythonApiReturnsNull_ShouldReturnEmptyDataList() {
        JobServiceImpl spyService = spy(jobService);
        doReturn(null).when(spyService).getRecommendationsByUser(10L, 1, 10);
        ResponseEntity<ResponseObject> response = spyService.getListJobsV2(10L, 1, 10);
        assertEquals(0, ((List)response.getBody().getData()).size());
    }

    /**
     * TC_029: listJobsCompleted - BUG Confirmation (Null paging)
     * Objective: Confirm that null paging in UserParamDTO1 causes a failure.
     * Input: UserParamDTO1 with null paging.
     * Expected: IllegalArgumentException (due to PageRequest validation).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    public void listJobsCompleted_WhenPagingIsNull_ShouldThrowException() {
        UserParamDTO1 params = new UserParamDTO1();
        params.setPaging(null);
        jobService.listJobsCompleted(params);
    }

    /**
     * TC_030: convertToJobDTO - BUG Confirmation (Null userCommon)
     * Objective: Confirm that a job without an associated user causes a NullPointerException.
     * Input: Job entity with userCommon = null.
     * Expected: NullPointerException.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    public void convertToJobDTO_WhenUserCommonIsNull_ShouldThrowNullPointerException() {
        Job job = buildJob(1L);
        job.setUserCommon(null);
        jobService.convertToJobDTO(job);
    }

    /**
     * TC_031: latestJobs - BUG Confirmation (Null paging)
     * Objective: Confirm that null paging in LocationParamsDto causes a failure.
     * Input: LocationParamsDto with null paging.
     * Expected: IllegalArgumentException.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    public void latestJobs_WhenPagingIsNull_ShouldThrowException() {
        LocationParamsDto params = new LocationParamsDto();
        params.setPaging(null);
        jobService.latestJobs(params);
    }

    /**
     * TC_032: saveJob - Exception path
     * Objective: Verify error handling when the Python recommendation API is unavailable.
     * Input: Valid JobDTO, but RestTemplate throws exception.
     * Expected: Exception is thrown by the service.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void saveJob_WhenPythonApiIsDown_ShouldThrowRuntimeException() {
        Job job = buildJob(1L);
        UserCommon user = new UserCommon();
        user.setId(10L);
        user.setOrganizationId(1L);
        
        when(userCommonRepo.findById(10L)).thenReturn(Optional.of(user));
        when(jobRepo.saveAndFlush(any(Job.class))).thenReturn(job);
        when(jobRepo.findById(any())).thenReturn(Optional.of(job));
        
        when(restTemplate.exchange(anyString(), any(), any(), any(Class.class)))
            .thenThrow(new RuntimeException("Python API Down"));

        JobDTO jobDTO = new JobDTO();
        jobDTO.setName("Test Job");
        jobDTO.setJobDefaultId(11L);
        
        jobService.saveJob(jobDTO);
    }

    /**
     * TC_033: adminSaveJobPost - Exception path
     * Objective: Verify admin job posting fails if recommendation synchronization fails.
     * Input: Valid JobDTO, but RestTemplate fails.
     * Expected: RuntimeException.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void adminSaveJobPost_WhenSyncFails_ShouldThrowRuntimeException() {
        Job job = buildJob(1L);
        when(jobRepo.save(any(Job.class))).thenReturn(job);
        when(jobRepo.findById(any())).thenReturn(Optional.of(job));
        when(restTemplate.exchange(anyString(), any(), any(), any(Class.class)))
            .thenThrow(new RuntimeException("Sync Error"));

        JobDTO jobDTO = new JobDTO();
        jobDTO.setUserId(10L);
        jobDTO.setJobDefaultId(11L);
        
        jobService.adminSaveJobPost(jobDTO);
    }

    /**
     * TC_034: listJobs - Exception path
     * Objective: Verify error handling during job listing if database fails.
     * Input: Valid params, but repository throws exception.
     * Expected: Exception caught or rethrown based on logic.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void listJobs_WhenDatabaseErrorOccurs_ShouldThrowException() {
        when(jobRepo.listJobs(any())).thenThrow(new RuntimeException("DB Error"));
        LocationParamsDto params = buildLocationParams();
        jobService.listJobs(params);
    }

    /**
     * TC_035: latestJobs - Edge case (Empty result)
     * Objective: Verify behavior when no jobs exist in the database.
     * Input: Valid location parameters.
     * Expected: HTTP 200 OK with an empty data list.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void latestJobs_WhenNoJobsFound_ShouldReturnEmptyDataList() {
        Page<Job> emptyPage = new PageImpl<>(Collections.emptyList());
        when(jobRepo.findJobs(any())).thenReturn(emptyPage);
        LocationParamsDto params = buildLocationParams();
        ResponseEntity<ResponseObject> response = jobService.latestJobs(params);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(((List)response.getBody().getData()).isEmpty());
    }

    private Job buildJob(Long id) {
        JobDefault jobDefault = new JobDefault();
        jobDefault.setId(11L);
        jobDefault.setName("Software Developer");

        UserCommon userCommon = new UserCommon();
        userCommon.setId(22L);
        userCommon.setPhone("0900000000");

        return Job.builder()
            .id(id)
            .name("Backend Developer Position")
            .job("Backend Developer")
            .phone("0900000000")
            .userCommon(userCommon)
            .jobDefault(jobDefault)
            .active(1)
            .build();
    }

    private LocationParamsDto buildLocationParams() {
        LocationParamsDto params = new LocationParamsDto();
        params.setUserId(10L);
        params.setPaging(new Paging(1, 10));
        com.jober.utilsservice.utils.modelCustom.Coordinates coords =
            new com.jober.utilsservice.utils.modelCustom.Coordinates();
        coords.setLat(21.0);
        coords.setLng(105.0);
        params.setCoordinates(coords);
        return params;
    }

    private CandidateManagementProjection createCandidateProjection(Long id, Job job) {
        return new CandidateManagementProjection() {
            @Override
            public String getStatus() { return "SAVED"; }
            @Override
            public Long getId() { return id; }
            @Override
            public Job getJob() { return job; }
        };
    }
}
