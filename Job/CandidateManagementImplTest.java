package com.resourceservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jober.utilsservice.utils.modelCustom.Response;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import com.resourceservice.utilsmodule.utils.modelCustom.Paging;
import com.resourceservice.dto.CandidateManagementDTO;
import com.resourceservice.dto.JobDTO;
import com.resourceservice.interceptor.BearerTokenWrapper;
import com.resourceservice.model.CandidateManagement;
import com.resourceservice.model.Freelancer;
import com.resourceservice.model.Job;
import com.resourceservice.model.RecruiterManagement;
import com.resourceservice.model.UserCommon;
import com.resourceservice.repository.CandidateManagementRepo;
import com.resourceservice.repository.FreelancerRepo;
import com.resourceservice.repository.JobRepo;
import com.resourceservice.repository.ScheduleRepo;
import com.resourceservice.repository.UserCommonRepo;
import com.resourceservice.utilsmodule.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ============================================================================
 *  Unit Tests cho {@link CandidateManagementImpl} (user-service).
 *  ----------------------------------------------------------------------------
 *  Phạm vi UT (Mockito mock thuần - KHÔNG đụng DB thật):
 *    - Toàn bộ TC `Exception` (per CRITICAL RULE: exception MUST be UT).
 *    - TC `Standard` chỉ cần verify logic / mock interaction.
 *  TC chuyển sang IT (xem {@link CandidateManagementImplIT}):
 *    - TC_CMD_010 getJobsOfCandidate_hasSavedJobs   (read DB query)
 *    - TC_CMD_011 getJobsOfCandidate_noSavedJobs    (read DB query)
 *    - TC_CMD_013 getJobById_validId                 (projection mapping)
 *  Rollback: N cho mọi TC ở file này (mock thuần).
 * ============================================================================
 */
@ExtendWith(MockitoExtension.class)
class CandidateManagementImplTest {

    @Mock
    private CandidateManagementRepo<CandidateManagement, Long> candidateManagementRepo;
    @Mock
    private UserCommonRepo userCommonRepo;
    @Mock
    private JobRepo<Job, Long> jobRepo;
    @Mock
    private FreelancerRepo<Freelancer, Long> freelancerRepo;
    @Mock
    private ScheduleRepo scheduleRepo;
    @Mock
    private CacheService cacheService;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private JobServiceImpl jobService;
    @Mock
    private CommunityService communityService;
    @Mock
    private BearerTokenWrapper tokenWrapper;

    private CandidateManagementImpl service;

    @BeforeEach
    void setUp() {
        service = new CandidateManagementImpl(tokenWrapper);
        ReflectionTestUtils.setField(service, "candidateManagementRepo", candidateManagementRepo);
        ReflectionTestUtils.setField(service, "userCommonRepo", userCommonRepo);
        ReflectionTestUtils.setField(service, "jobRepo", jobRepo);
        ReflectionTestUtils.setField(service, "freelancerRepo", freelancerRepo);
        ReflectionTestUtils.setField(service, "scheduleRepo", scheduleRepo);
        ReflectionTestUtils.setField(service, "cacheService", cacheService);
        ReflectionTestUtils.setField(service, "cacheManager", cacheManager);
        ReflectionTestUtils.setField(service, "jobService", jobService);
        ReflectionTestUtils.setField(service, "communityService", communityService);
    }

    // =========================================================================================
    // TC_CMD_001 -> TC_CMD_004: saveCandidate
    // =========================================================================================

    /**
     * TC_CMD_001 - saveCandidate - Standard
     * Mục tiêu: Khi save candidate hợp lệ (user + job tồn tại, chưa duplicate),
     *           service phải gọi repo.save và trả CREATED.
     * CheckDB: Y (verify mock save interaction)
     * Rollback: N
     */
    @Tag("Mock")
    @Test
    void TC_CMD_001_saveCandidate_success() {
        CandidateManagementDTO dto = new CandidateManagementDTO();
        dto.setId(null);
        dto.setUserId(1L);
        dto.setJobId(10L);

        when(tokenWrapper.getUid()).thenReturn(1L);

        // lenient() vì code thực tế hiện chưa implement đủ guard - tránh UnnecessaryStubbingException
        org.mockito.Mockito.lenient().when(userCommonRepo.findById(1L)).thenReturn(Optional.of(new UserCommon()));
        org.mockito.Mockito.lenient().when(jobRepo.findJobById(10L)).thenReturn(new Job());
        org.mockito.Mockito.lenient().when(candidateManagementRepo.findByUserAndJob(1L, 10L))
                .thenReturn(Optional.empty());

        CandidateManagement savedEntity = new CandidateManagement();
        savedEntity.setId(100L);
        savedEntity.setActive(1);
        when(candidateManagementRepo.save(any(CandidateManagement.class))).thenReturn(savedEntity);

        ResponseEntity<ResponseObject> response = service.saveCandidate(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("CREATED", response.getBody().getCode());
        verify(candidateManagementRepo, times(1)).save(any(CandidateManagement.class));
    }

    /**
     * TC_CMD_002 - saveCandidate - Standard
     * Muc tieu (SPEC): Khi da ton tai quan he user-job, service phai reject duplicate,
     *                  KHONG goi save, tra NOT_CREATED.
     *
     * Test nay se FAIL neu code-bug van con. Failure message duoi day la BUG REPORT cho dev.
     * CheckDB: Y. Rollback: N.
     */
    @Tag("Mock")
    @Test
    void TC_CMD_002_saveCandidate_duplicateRelation() {
        CandidateManagementDTO dto = new CandidateManagementDTO();
        dto.setId(null);
        dto.setUserId(1L);
        dto.setJobId(10L);

        when(tokenWrapper.getUid()).thenReturn(1L);
        org.mockito.Mockito.lenient().when(userCommonRepo.findById(1L)).thenReturn(Optional.of(new UserCommon()));
        org.mockito.Mockito.lenient().when(jobRepo.findJobById(10L)).thenReturn(new Job());
        // Da ton tai quan he user-job
        org.mockito.Mockito.lenient().when(candidateManagementRepo.findByUserAndJob(1L, 10L))
                .thenReturn(Optional.of(new RecruiterManagement()));
        org.mockito.Mockito.lenient().when(candidateManagementRepo.save(any(CandidateManagement.class)))
                .thenReturn(new CandidateManagement());

        ResponseEntity<ResponseObject> response = service.saveCandidate(dto);

        assertEquals("NOT_CREATED", response.getBody().getCode(),
                "[BUG-CMD-002] saveCandidate THIEU duplicate guard. SPEC yeu cau: goi findByUserAndJob, "
                        + "neu da ton tai thi tra NOT_CREATED va KHONG goi save. "
                        + "Hien tai code di thang vao save() khong check duplicate.");
        verify(candidateManagementRepo,
                never().description("[BUG-CMD-002] save() KHONG duoc goi khi duplicate"))
                .save(any(CandidateManagement.class));
    }

    /**
     * TC_CMD_003 - saveCandidate - Exception
     * Muc tieu (SPEC): Khi job id khong ton tai, service phai tra NOT_FOUND, khong save.
     * CheckDB: Y. Rollback: N.
     */
    @Tag("Mock")
    @Test
    void TC_CMD_003_saveCandidate_jobNotFound() {
        CandidateManagementDTO dto = new CandidateManagementDTO();
        dto.setId(null);
        dto.setUserId(1L);
        dto.setJobId(999L);

        when(tokenWrapper.getUid()).thenReturn(1L);
        org.mockito.Mockito.lenient().when(userCommonRepo.findById(1L)).thenReturn(Optional.of(new UserCommon()));
        // Job khong ton tai
        org.mockito.Mockito.lenient().when(jobRepo.findJobById(999L)).thenReturn(null);
        org.mockito.Mockito.lenient().when(candidateManagementRepo.save(any(CandidateManagement.class)))
                .thenReturn(new CandidateManagement());

        ResponseEntity<ResponseObject> response = service.saveCandidate(dto);

        assertEquals("NOT_FOUND", response.getBody().getCode(),
                "[BUG-CMD-003] saveCandidate THIEU not-found guard cho job. SPEC: goi jobRepo.findJobById, "
                        + "neu null thi tra NOT_FOUND va khong save. Hien tai code khong check job ton tai.");
        verify(candidateManagementRepo,
                never().description("[BUG-CMD-003] save() KHONG duoc goi khi job khong ton tai"))
                .save(any(CandidateManagement.class));
    }

    /**
     * TC_CMD_004 - saveCandidate - Exception
     * Muc tieu (SPEC): Khi user id khong ton tai, service phai tra NOT_FOUND, khong save.
     * CheckDB: Y. Rollback: N.
     */
    @Tag("Mock")
    @Test
    void TC_CMD_004_saveCandidate_userNotFound() {
        CandidateManagementDTO dto = new CandidateManagementDTO();
        dto.setId(null);
        dto.setUserId(999L);
        dto.setJobId(10L);

        when(tokenWrapper.getUid()).thenReturn(999L);
        // User khong ton tai
        org.mockito.Mockito.lenient().when(userCommonRepo.findById(999L)).thenReturn(Optional.empty());
        org.mockito.Mockito.lenient().when(jobRepo.findJobById(10L)).thenReturn(new Job());
        org.mockito.Mockito.lenient().when(candidateManagementRepo.save(any(CandidateManagement.class)))
                .thenReturn(new CandidateManagement());

        ResponseEntity<ResponseObject> response = service.saveCandidate(dto);

        assertEquals("NOT_FOUND", response.getBody().getCode(),
                "[BUG-CMD-004] saveCandidate THIEU not-found guard cho user. SPEC: goi userCommonRepo.findById(uid), "
                        + "neu empty thi tra NOT_FOUND va khong save. Hien tai code chi setUserId tu tokenWrapper roi save thang.");
        verify(candidateManagementRepo,
                never().description("[BUG-CMD-004] save() KHONG duoc goi khi user khong ton tai"))
                .save(any(CandidateManagement.class));
    }

    // =========================================================================================
    // TC_CMD_005 -> TC_CMD_007: updateCandidateManagement
    // =========================================================================================

    /**
     * TC_CMD_005 - updateCandidateManagement - Standard
     * Muc tieu (SPEC): Khi tim thay record, service goi save va tra UPDATED.
     * CheckDB: Y. Rollback: N.
     */
    @Tag("Mock")
    @Test
    void TC_CMD_005_updateCandidateManagement_success() {
        CandidateManagementDTO dto = new CandidateManagementDTO();
        dto.setUserId(1L);
        dto.setJobId(10L);
        dto.setNote("Update Note");

        RecruiterManagement existing = new RecruiterManagement();
        when(candidateManagementRepo.findByUserAndJob(1L, 10L)).thenReturn(Optional.of(existing));
        when(candidateManagementRepo.save(any(CandidateManagement.class))).thenReturn(new CandidateManagement());

        ResponseEntity<Response> response = service.updateCandidateManagement(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UPDATED", response.getBody().getCode(),
                "[BUG-CMD-005] Logic check Optional o source line 83 SAI: "
                        + "`Optional.of(entity).get().equals(Optional.empty()) || !Optional.of(entity).isPresent()` "
                        + "luon = false khi entity la Optional non-null -> code rot vao branch EXISTED thay vi UPDATED. "
                        + "Fix: doi thanh `entity == null || !entity.isPresent()`.");
        verify(candidateManagementRepo,
                times(1).description("[BUG-CMD-005] save() PHAI duoc goi khi update record ton tai"))
                .save(any(CandidateManagement.class));
    }

    /**
     * TC_CMD_006 - updateCandidateManagement - Exception
     * Muc tieu (SPEC): Khi khong tim thay record, service tra NOT_FOUND, KHONG goi save.
     * CheckDB: Y. Rollback: N.
     */
    @Tag("Mock")
    @Test
    void TC_CMD_006_updateCandidateManagement_invalidId() {
        CandidateManagementDTO dto = new CandidateManagementDTO();
        dto.setUserId(1L);
        dto.setJobId(10L);

        when(candidateManagementRepo.findByUserAndJob(1L, 10L)).thenReturn(Optional.empty());
        org.mockito.Mockito.lenient().when(candidateManagementRepo.save(any(CandidateManagement.class)))
                .thenReturn(new CandidateManagement());

        ResponseEntity<Response> response = service.updateCandidateManagement(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("NOT_FOUND", response.getBody().getCode(),
                "[BUG-CMD-006] updateCandidateManagement KHONG handle not-found dung. SPEC: khi entity = Optional.empty(), "
                        + "service phai tra NOT_FOUND va KHONG goi save. Hien tai do logic Optional bug, code lai goi "
                        + "save tao moi -> tra UPDATED hoac NOT_MODIFY. Fix: them branch riêng cho not-found.");
        verify(candidateManagementRepo,
                never().description("[BUG-CMD-006] save() KHONG duoc goi khi update record khong ton tai"))
                .save(any(CandidateManagement.class));
    }

    /**
     * TC_CMD_007 - updateCandidateManagement - Exception
     * Mục tiêu: Khi payload null, service phải ném NullPointerException.
     * CheckDB: N
     * Rollback: N
     */
    @Tag("Unit")
    @Test
    void TC_CMD_007_updateCandidateManagement_nullPayload() {
        assertThrows(NullPointerException.class, () -> service.updateCandidateManagement(null));
    }

    // =========================================================================================
    // TC_CMD_008 -> TC_CMD_009: deleteCandidateManagement
    // =========================================================================================

    /**
     * TC_CMD_008 - deleteCandidateManagement - Standard
     * Muc tieu (SPEC): Khi id hop le, service goi saveAll va tra DELETED.
     * CheckDB: Y. Rollback: N.
     */
    @Tag("Mock")
    @Test
    @SuppressWarnings("unchecked")
    void TC_CMD_008_deleteCandidateManagement_success() {
        when(tokenWrapper.getUid()).thenReturn(1L);
        when(jobRepo.findByIds(anyList())).thenReturn(Collections.singletonList(new Job()));

        CandidateManagement cm = new CandidateManagement();
        cm.setId(1L);
        when(candidateManagementRepo.findCandidateByUserAndJob(anyLong(), anyList()))
                .thenReturn(Collections.singletonList(cm));

        ResponseEntity<Response> response = service.deleteCandidateManagement(Arrays.asList(1L));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("DELETED", response.getBody().getCode(),
                "[BUG-CMD-008] deleteCandidateManagement tra SAI code khi xoa thanh cong. "
                        + "SPEC: phai tra DELETED. Hien tai source line 261 dat code = UPDATED. "
                        + "Fix: doi `response.setCode(UPDATED)` thanh `response.setCode(DELETED)` (va message tuong ung).");
        verify(candidateManagementRepo, times(1)).saveAll(anyList());
    }

    /**
     * TC_CMD_009 - deleteCandidateManagement - Exception
     * Muc tieu (SPEC): Khi list rong (khong tim thay record), service tra NOT_FOUND.
     * CheckDB: Y. Rollback: N.
     */
    @Tag("Mock")
    @Test
    @SuppressWarnings("unchecked")
    void TC_CMD_009_deleteCandidateManagement_notFound() {
        when(tokenWrapper.getUid()).thenReturn(1L);
        when(jobRepo.findByIds(anyList())).thenReturn(Collections.emptyList());
        when(candidateManagementRepo.findCandidateByUserAndJob(anyLong(), anyList()))
                .thenReturn(Collections.emptyList());

        ResponseEntity<Response> response = service.deleteCandidateManagement(Arrays.asList(999L));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("NOT_FOUND", response.getBody().getCode(),
                "[BUG-CMD-009] deleteCandidateManagement tra SAI code khi khong tim thay record. "
                        + "SPEC: phai tra NOT_FOUND. Hien tai source line 264-266 dat code = NOT_MODIFY. "
                        + "Fix: doi nhanh else `response.setCode(NOT_MODIFY)` thanh `response.setCode(NOT_FOUND)`.");
    }

    // =========================================================================================
    // TC_CMD_010 -> TC_CMD_011: getJobsOfCandidate -> CHUYỂN SANG IT (CandidateManagementImplIT)
    // TC_CMD_012: boundaryPaging -> Exception, GIỮ TẠI UT
    // =========================================================================================

    /**
     * TC_CMD_012 - getJobsOfCandidate - Exception
     * Mục tiêu: Khi page=0 (boundary), service phải ném IllegalArgumentException.
     * CheckDB: N
     * Rollback: N
     */
    @Tag("Unit")
    @Test
    void TC_CMD_012_getJobsOfCandidate_boundaryPaging() {
        assertThrows(IllegalArgumentException.class, () -> service.getJobsOfCandidate(new Paging(0, 10)));
    }

    // =========================================================================================
    // TC_CMD_013: getJobById_validId -> CHUYỂN SANG IT
    // TC_CMD_014, 015: GIỮ TẠI UT
    // =========================================================================================

    /**
     * TC_CMD_014 - getJobById - Exception
     * Muc tieu (SPEC): Khi job id khong ton tai, getBody().getCode() = "NOT_FOUND".
     * CheckDB: Y. Rollback: N.
     */
    @Tag("Mock")
    @Test
    void TC_CMD_014_getJobById_invalidId() throws JsonProcessingException {
        when(tokenWrapper.getUid()).thenReturn(1L);
        when(candidateManagementRepo.findSavedJob(999L, 1L)).thenReturn(Collections.emptyList());
        when(jobRepo.findJobById(999L)).thenReturn(null);

        ResponseEntity<ResponseObject> response = service.getJobById(999L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("NOT_FOUND", response.getBody().getCode(),
                "[BUG-CMD-014] getJobById dat sai thu tu argument constructor ResponseObject. "
                        + "Constructor signature: ResponseObject(status, code, message, ...). "
                        + "Hien tai source line 216 truyen `new ResponseObject(NOT_FOUND, SUCCESS_CODE, SUCCESS, ...)` "
                        + "-> field code = '200' va field status = 'NOT_FOUND'. "
                        + "SPEC: code phai = 'NOT_FOUND'. Fix: doi thanh `new ResponseObject(SUCCESS, NOT_FOUND, NOT_FOUND, ...)`.");
    }

    /**
     * TC_CMD_015 - getJobById - Standard
     * Mục tiêu: DTO mapping phải bảo toàn các trường id/name/salary/address/status.
     * CheckDB: N (pure mapping)
     * Rollback: N
     */
    @Tag("Unit")
    @Test
    void TC_CMD_015_getJobById_dataMappingIntegrity() throws JsonProcessingException {
        when(tokenWrapper.getUid()).thenReturn(1L);
        when(candidateManagementRepo.findSavedJob(11L, 1L)).thenReturn(Collections.emptyList());

        Job job = new Job();
        job.setId(11L);
        job.setOrganizationId(6L);
        when(jobRepo.findJobById(11L)).thenReturn(job);

        JobDTO dto = new JobDTO();
        dto.setId(11L);
        dto.setName("Senior SQA Engineer");
        dto.setSalary("2500$");
        dto.setAddress("Hanoi, Vietnam");
        dto.setStatus("ACTIVE");
        when(jobService.convertToJobDTO(any(Job.class))).thenReturn(dto);

        Map<String, Object> org = new HashMap<>();
        org.put("name", "Tech Corp");
        when(communityService.getOrganization(6L)).thenReturn(org);

        ResponseEntity<ResponseObject> response = service.getJobById(11L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JobDTO result = (JobDTO) response.getBody().getData();
        assertEquals(11L, result.getId());
        assertEquals("Senior SQA Engineer", result.getName());
        assertEquals("2500$", result.getSalary());
        assertEquals("Hanoi, Vietnam", result.getAddress());
        assertEquals("ACTIVE", result.getStatus());
    }
}
