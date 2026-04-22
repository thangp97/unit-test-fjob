package com.resourceservice.service.impl;

import com.jober.utilsservice.model.PageableModel;
import com.jober.utilsservice.utils.modelCustom.*;
import com.resourceservice.dto.*;
import com.resourceservice.dto.request.*;
import com.resourceservice.common.CommonUtils;
import com.resourceservice.config.EnvProperties;
import com.resourceservice.interceptor.BearerTokenWrapper;
import com.resourceservice.model.*;
import com.resourceservice.model.projection.CandidateManagementProjection;
import com.resourceservice.repository.*;
import com.resourceservice.service.CandidateManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock private JobRepo jobRepo;
    @Mock private CommonUtils utils;
    @Mock private UserCommonRepo userCommonRepo;
    @Mock private FreelancerRepo freelancerRepo;
    @Mock private ScheduleRepo scheduleRepo;
    @Mock private EnvProperties envProperties;
    @Mock private CacheManagerService cacheManagerService;
    @Mock private CandidateManagementRepo candidateManagementRepo;
    @Mock private OrganizationRepo organizationRepo;
    @Mock private CandidateManagementService candidateManagementService;
    @Mock private EntityManager entityManager;
    @Mock private BearerTokenWrapper tokenWrapper;

    @InjectMocks
    private JobServiceImpl jobService;

    // ---- Helper ----
    private Job buildJob(Long id, String name, Integer active) {
        UserCommon uc = UserCommon.builder().id(1L).phone("0123").sendSmsNumber("1").build();
        JobDefault jd = JobDefault.builder().id(10L).name("DefaultJob").build();
        return Job.builder().id(id).name(name).job("Dev").salary("1000").des("desc")
                .address("HCM").active(active).lat(10.0).lng(106.0)
                .creationDate(LocalDateTime.now()).expDate(LocalDateTime.now().plusDays(30))
                .userCommon(uc).jobDefault(jd).build();
    }

    // ==================== TC_JS_007 ====================
    @Test
    @DisplayName("TC_JS_007: adminSaveJobPost - null jobDTO returns BAD_REQUEST")
    void testAdminSaveJobPost_NullDto() {
        ResponseEntity<ResponseObject> resp = jobService.adminSaveJobPost(null);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // ==================== TC_JS_008 ====================
    @Test
    @DisplayName("TC_JS_008: adminSaveJobPost - null userId returns BAD_REQUEST")
    void testAdminSaveJobPost_NullUserId() {
        JobDTO dto = new JobDTO();
        dto.setUserId(null);
        dto.setJobDefaultId(1L);
        ResponseEntity<ResponseObject> resp = jobService.adminSaveJobPost(dto);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // ==================== TC_JS_009 ====================
    @Test
    @DisplayName("TC_JS_009: adminSaveJobPost - null jobDefaultId returns BAD_REQUEST")
    void testAdminSaveJobPost_NullJobDefaultId() {
        JobDTO dto = new JobDTO();
        dto.setUserId(1L);
        dto.setJobDefaultId(null);
        ResponseEntity<ResponseObject> resp = jobService.adminSaveJobPost(dto);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // ==================== TC_JS_011 ====================
    @Test
    @DisplayName("TC_JS_011: adminSaveJobPost - job already exists")
    void testAdminSaveJobPost_AlreadyExists() {
        JobDTO dto = new JobDTO();
        dto.setId(null);
        dto.setUserId(1L);
        dto.setJobDefaultId(1L);
        when(jobRepo.findJobIdByJobDefaultIdAndUserId(1L, 1L)).thenReturn(5L);

        ResponseEntity<ResponseObject> resp = jobService.adminSaveJobPost(dto);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().getStatus().contains("EXISTED"));
    }

    // ==================== TC_JS_010 ====================
    @Test
    @DisplayName("TC_JS_010: adminSaveJobPost - update non-existent job returns NOT_FOUND")
    void testAdminSaveJobPost_UpdateNotFound() {
        JobDTO dto = new JobDTO();
        dto.setId(99L);
        dto.setUserId(1L);
        dto.setJobDefaultId(1L);
        when(jobRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<ResponseObject> resp = jobService.adminSaveJobPost(dto);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    // ==================== TC_JS_012 ====================
    @Test
    @DisplayName("TC_JS_012: updateActiveJob - toggle 1→0")
    void testUpdateActiveJob_ToggleToInactive() {
        Job job = buildJob(1L, "Test", 1);
        when(jobRepo.findById(1L)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(Job.class))).thenReturn(job);

        ResponseEntity<ResponseObject> resp = jobService.updateActiveJob(1L);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(0, job.getActive());
    }

    // ==================== TC_JS_013 ====================
    @Test
    @DisplayName("TC_JS_013: updateActiveJob - toggle 0→1")
    void testUpdateActiveJob_ToggleToActive() {
        Job job = buildJob(1L, "Test", 0);
        when(jobRepo.findById(1L)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(Job.class))).thenReturn(job);

        ResponseEntity<ResponseObject> resp = jobService.updateActiveJob(1L);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, job.getActive());
    }

    // ==================== TC_JS_014 ====================
    @Test
    @DisplayName("TC_JS_014: updateActiveJob - job not found")
    void testUpdateActiveJob_NotFound() {
        when(jobRepo.findById(999L)).thenReturn(Optional.empty());
        ResponseEntity<ResponseObject> resp = jobService.updateActiveJob(999L);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

    // ==================== TC_JS_016 ====================
    @Test
    @DisplayName("TC_JS_016: deleteJobs - happy path")
    void testDeleteJobs_Success() {
        when(jobRepo.updateByIds(anyInt(), anyList())).thenReturn(3);
        ResponseEntity<Response> resp = jobService.deleteJobs(List.of(1L, 2L, 3L));
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // ==================== TC_JS_017 ====================
    @Test
    @DisplayName("TC_JS_017: deleteJobs - no jobs matched")
    void testDeleteJobs_NoMatch() {
        when(jobRepo.updateByIds(anyInt(), anyList())).thenReturn(0);
        ResponseEntity<Response> resp = jobService.deleteJobs(List.of(999L));
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // ==================== TC_JS_018 ====================
    @Test
    @DisplayName("TC_JS_018: deleteJobByIds - happy path")
    void testDeleteJobByIds_Success() {
        when(jobRepo.deleteJobByIds(anyList())).thenReturn(2);
        ResponseEntity<ResponseObject> resp = jobService.deleteJobByIds(List.of(1L, 2L));
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // ==================== TC_JS_019 ====================
    @Test
    @DisplayName("TC_JS_019: deleteJobByIds - no deletion")
    void testDeleteJobByIds_NoDeletion() {
        when(jobRepo.deleteJobByIds(anyList())).thenReturn(0);
        ResponseEntity<ResponseObject> resp = jobService.deleteJobByIds(List.of(999L));
        assertEquals(HttpStatus.NOT_MODIFIED, resp.getStatusCode());
    }

    // ==================== TC_JS_020 ====================
    @Test
    @DisplayName("TC_JS_020: applyJob - happy path")
    void testApplyJob_Success() {
        Freelancer fl = Freelancer.builder().id(1L).build();
        when(freelancerRepo.findFreelancerByUserIdAndJobDefaultId(1L, 10L)).thenReturn(fl);
        when(scheduleRepo.save(any(Schedule.class))).thenReturn(new Schedule());

        ResponseEntity<ResponseObject> resp = jobService.applyJob(1L, 10L);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // ==================== TC_JS_021 ====================
    @Test
    @DisplayName("TC_JS_021: applyJob - freelancer not found")
    void testApplyJob_FreelancerNotFound() {
        when(freelancerRepo.findFreelancerByUserIdAndJobDefaultId(1L, 99L)).thenReturn(null);
        ResponseEntity<ResponseObject> resp = jobService.applyJob(1L, 99L);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // ==================== TC_JS_022 ====================
    @Test
    @DisplayName("TC_JS_022: findById - job found")
    void testFindById_Found() {
        Job job = buildJob(1L, "Test", 1);
        when(jobRepo.findJobById(1L)).thenReturn(job);

        ResponseEntity resp = jobService.findById(1L);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // ==================== TC_JS_023 ====================
    @Test
    @DisplayName("TC_JS_023: findById - job not found")
    void testFindById_NotFound() {
        when(jobRepo.findJobById(999L)).thenReturn(null);
        ResponseEntity resp = jobService.findById(999L);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // ==================== TC_JS_024 ====================
    @Test
    @DisplayName("TC_JS_024: findJobIdByJobDefaultIdAndUserId - found")
    void testFindJobIdByJobDefaultIdAndUserId_Found() {
        when(tokenWrapper.getUid()).thenReturn(1L);
        when(jobRepo.findJobIdByJobDefaultIdAndUserId(1L, 1L)).thenReturn(5L);
        Long result = jobService.findJobIdByJobDefaultIdAndUserId(1L);
        assertEquals(5L, result);
    }

    // ==================== TC_JS_025 ====================
    @Test
    @DisplayName("TC_JS_025: findJobIdByJobDefaultIdAndUserId - not found")
    void testFindJobIdByJobDefaultIdAndUserId_NotFound() {
        when(tokenWrapper.getUid()).thenReturn(1L);
        when(jobRepo.findJobIdByJobDefaultIdAndUserId(99L, 1L)).thenReturn(null);
        Long result = jobService.findJobIdByJobDefaultIdAndUserId(99L);
        assertNull(result);
    }

    // ==================== TC_JS_037 ====================
    @Test
    @DisplayName("TC_JS_037: listJobsCompleted - happy path")
    void testListJobsCompleted_Found() {
        UserParamDTO1 params = new UserParamDTO1();
        params.setUserId(1L);
        Paging paging = new Paging(1, 10);
        params.setPaging(paging);

        Job job = buildJob(1L, "Test", 1);
        Page<Job> page = new PageImpl<>(List.of(job), PageRequest.of(0, 10), 1);
        when(jobRepo.findJobsCompleted(eq(1L), any(Pageable.class))).thenReturn(page);

        ResponseEntity<ResponseObject> resp = jobService.listJobsCompleted(params);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // ==================== TC_JS_038 ====================
    @Test
    @DisplayName("TC_JS_038: listJobsCompleted - no completed jobs")
    void testListJobsCompleted_Empty() {
        UserParamDTO1 params = new UserParamDTO1();
        params.setUserId(1L);
        Paging paging = new Paging(1, 10);
        params.setPaging(paging);

        Page<Job> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(jobRepo.findJobsCompleted(eq(1L), any(Pageable.class))).thenReturn(emptyPage);

        ResponseEntity<ResponseObject> resp = jobService.listJobsCompleted(params);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // ==================== TC_JS_053 ====================
    @Test
    @DisplayName("TC_JS_053: getJobsByOrganization - happy path with jobId exclusion")
    void testGetJobsByOrganization_Found() {
        Job job = buildJob(1L, "Job1", 1);
        Page<Job> page = new PageImpl<>(List.of(job), PageRequest.of(0, 10), 1);
        Organization org = Organization.builder().id(1L).name("Org").avatar("av").description("d").build();

        when(jobRepo.findByOrganizationIdAndIdNotAndActiveAndExpDateAfter(
                eq(1L), eq(5L), eq(1), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(page);
        when(organizationRepo.findById(1L)).thenReturn(Optional.of(org));

        ResponseEntity<ResponseObject> resp = jobService.getJobsByOrganization("1", "5", 1, 10);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // ==================== TC_JS_055 ====================
    @Test
    @DisplayName("TC_JS_055: getJobsByOrganization - invalid organizationId")
    void testGetJobsByOrganization_InvalidOrgId() {
        ResponseEntity<ResponseObject> resp = jobService.getJobsByOrganization("abc", null, 1, 10);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // ==================== TC_JS_063 ====================
    @Test
    @DisplayName("TC_JS_063: jobsHadPostByRecruiter - jobs found")
    void testJobsHadPostByRecruiter_Found() {
        when(tokenWrapper.getUid()).thenReturn(1L);
        Job job = buildJob(1L, "Test", 1);
        when(jobRepo.findJobDefaultIdsHavePostByRecruiter(1L)).thenReturn(List.of(job));

        ResponseEntity<Response> resp = jobService.jobsHadPostByRecruiter();
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // ==================== TC_JS_064 ====================
    @Test
    @DisplayName("TC_JS_064: jobsHadPostByRecruiter - no posted jobs")
    void testJobsHadPostByRecruiter_Empty() {
        when(tokenWrapper.getUid()).thenReturn(1L);
        when(jobRepo.findJobDefaultIdsHavePostByRecruiter(1L)).thenReturn(null);

        ResponseEntity<Response> resp = jobService.jobsHadPostByRecruiter();
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // ==================== TC_JS_066 ====================
    @Test
    @DisplayName("TC_JS_066: searchJobsAdvanced - null searchDTO throws")
    void testSearchJobsAdvanced_NullInput() {
        assertThrows(IllegalArgumentException.class, () -> jobService.searchJobsAdvanced(null));
    }

    // ==================== TC_JS_071 ====================
    @Test
    @DisplayName("TC_JS_071: jobsToCsv - happy path")
    void testJobsToCsv_WithData() {
        Writer writer = new StringWriter();
        JobDataModel m1 = JobDataModel.builder().id(1L).name("N1").address("A1")
                .distance(5.0).job("J1").number("2").salary("1000")
                .des("D1").img("img1").creationdate(LocalDateTime.now())
                .expdate(LocalDateTime.now().plusDays(30)).build();

        jobService.jobsToCsv(writer, List.of(m1));

        String csv = writer.toString();
        assertTrue(csv.contains("ID"));
        assertTrue(csv.contains("Company Name"));
        assertTrue(csv.contains("N1"));
    }

    // ==================== TC_JS_072 ====================
    @Test
    @DisplayName("TC_JS_072: jobsToCsv - empty list")
    void testJobsToCsv_EmptyList() {
        Writer writer = new StringWriter();
        jobService.jobsToCsv(writer, Collections.emptyList());
        String csv = writer.toString();
        assertTrue(csv.contains("ID")); // header only
    }

    // ==================== TC_JS_074 ====================
    @Test
    @DisplayName("TC_JS_074: convertToJobDTO - full mapping")
    void testConvertToJobDTO_FullMapping() {
        Job job = buildJob(1L, "TestJob", 1);
        job.setPhone("0123");
        job.setEmail("a@b.com");
        job.setLevel("Senior");
        job.setImg("img.png");
        job.setWebsite("http://test.com");
        job.setType("FT");
        job.setCv("cv.pdf");
        job.setProfit("high");
        job.setWard("W1");
        job.setProvince("P1");
        job.setRequiredExperienceLevel(3);
        job.setRequiredSkillLevel(2);
        job.setRequiredSkill("Java");

        JobDTO result = jobService.convertToJobDTO(job);

        assertEquals(1L, result.getId());
        assertEquals("TestJob", result.getName());
        assertEquals("Dev", result.getJob());
        assertEquals("1000", result.getSalary());
        assertEquals("0123", result.getPhone());
        assertEquals("a@b.com", result.getEmail());
        assertEquals(10L, result.getJobDefaultId());
    }

    // ==================== TC_JS_075 ====================
    @Test
    @DisplayName("TC_JS_075: convertToJobDTO - null userCommon throws NPE")
    void testConvertToJobDTO_NullUserCommon() {
        Job job = buildJob(1L, "Test", 1);
        job.setUserCommon(null);
        assertThrows(NullPointerException.class, () -> jobService.convertToJobDTO(job));
    }

    // ==================== TC_JS_076 ====================
    @Test
    @DisplayName("TC_JS_076: convertToJobDTO - null jobDefault throws NPE")
    void testConvertToJobDTO_NullJobDefault() {
        Job job = buildJob(1L, "Test", 1);
        job.setJobDefault(null);
        assertThrows(NullPointerException.class, () -> jobService.convertToJobDTO(job));
    }

    // ==================== TC_JS_077 ====================
    @Test
    @DisplayName("TC_JS_077: convertToJdDto - happy path")
    void testConvertToJdDto() {
        Job job = buildJob(1L, "Test", 1);
        // ObjectMapperUtil.map may need to be mocked or tested as integration
        // This test verifies no exception is thrown
        assertDoesNotThrow(() -> jobService.convertToJdDto(job));
    }
}
