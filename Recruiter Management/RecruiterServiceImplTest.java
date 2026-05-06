package com.resourceservice.service.impl;

import com.jober.utilsservice.model.PageableModel;
import com.jober.utilsservice.utils.modelCustom.Paging;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import com.resourceservice.exception.CommonException;
import com.resourceservice.interceptor.BearerTokenWrapper;
import com.resourceservice.model.Job;
import com.resourceservice.model.RecruiterManagement;
import com.resourceservice.model.UserCommon;
import com.resourceservice.model.projection.FreelancerProjection;
import com.resourceservice.model.projection.JobProjection;
import com.resourceservice.repository.FreelancerRepo;
import com.resourceservice.repository.JobRepo;
import com.resourceservice.repository.OrganizationRepo;
import com.resourceservice.repository.RecruiterManagementRepo;
import com.resourceservice.repository.UserCommonRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.jober.utilsservice.constant.Constant.NULL_CODE;
import static com.jober.utilsservice.constant.Constant.SUCCESS_CODE;
import static com.jober.utilsservice.constant.ResponseMessageConstant.CREATED;
import static com.jober.utilsservice.constant.ResponseMessageConstant.FAILED;
import static com.jober.utilsservice.constant.ResponseMessageConstant.FOUND;
import static com.jober.utilsservice.constant.ResponseMessageConstant.NOT_FOUND;
import static com.jober.utilsservice.constant.ResponseMessageConstant.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ============================================================================
 * Unit Tests cho RecruiterServiceImpl (user-service).
 * ----------------------------------------------------------------------------
 * Scope UT: addNewRecruiter, addNewCandidate, updateStatusCandidate,
 * updateNoteRecruiterManagement, listPost, listPostCSV, findAppliedCandidate,
 * getOrganizationName. getRecommendedCandidates out of scope.
 * Rollback: N (mock thuan, khong cham DB that).
 * ============================================================================
 */
@ExtendWith(MockitoExtension.class)
class RecruiterServiceImplTest {

    @Mock private JobRepo jobRepo;
    @Mock private OrganizationRepo organizationRepo;
    @Mock private RecruiterManagementRepo<RecruiterManagement, Long> repo;
    @Mock private UserCommonRepo userCommonRepo;
    @Mock private FreelancerRepo freelancerRepo;
    @Mock private BearerTokenWrapper tokenWrapper;

    private RecruiterServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RecruiterServiceImpl(tokenWrapper);
        ReflectionTestUtils.setField(service, "jobRepo", jobRepo);
        ReflectionTestUtils.setField(service, "organizationRepo", organizationRepo);
        ReflectionTestUtils.setField(service, "repo", repo);
        ReflectionTestUtils.setField(service, "userCommonRepo", userCommonRepo);
        ReflectionTestUtils.setField(service, "freelancerRepo", freelancerRepo);
    }

    /**
     * TC_REC_001 - addNewRecruiter - Standard
     * Muc tieu: Khi save thanh cong, service tra CREATED va data khong null.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_REC_001_addNewRecruiter_success() {
        RecruiterManagement input = new RecruiterManagement();
        RecruiterManagement saved = new RecruiterManagement();
        saved.setId(1L);
        when(repo.save(input)).thenReturn(saved);

        ResponseEntity<ResponseObject> response = service.addNewRecruiter(input);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(CREATED, response.getBody().getCode());
        assertEquals(saved, response.getBody().getData());
        verify(repo, times(1)).save(input);
    }

    /**
     * TC_REC_002 - addNewRecruiter - Standard
     * Muc tieu: Khi repo.save tra null, service tra NOT_MODIFIED.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_REC_002_addNewRecruiter_saveFail() {
        RecruiterManagement input = new RecruiterManagement();
        when(repo.save(input)).thenReturn(null);

        ResponseEntity<ResponseObject> response = service.addNewRecruiter(input);

        assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
        assertEquals(FAILED, response.getBody().getCode());
        verify(repo, times(1)).save(input);
    }

    /**
     * TC_REC_003 - addNewCandidate - Standard
     * Muc tieu: Khi user ton tai, service tao recruiter-management va tra SUCCESS.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_REC_003_addNewCandidate_success() {
        UserCommon user = new UserCommon();
        user.setId(1L);
        when(userCommonRepo.findById(1L)).thenReturn(Optional.of(user));

        ResponseEntity<String> response = service.addNewCandidate(1L, 10L, "x");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(SUCCESS, response.getBody());

        ArgumentCaptor<RecruiterManagement> captor = ArgumentCaptor.forClass(RecruiterManagement.class);
        verify(repo, times(1)).save(captor.capture());
        RecruiterManagement actual = captor.getValue();
        assertEquals(1L, actual.getUserCommon().getId());
        assertEquals(10L, actual.getFreelancerid());
        assertEquals("1", actual.getStatus());
        assertEquals("0", actual.getNote());
    }

    /**
     * TC_REC_004 - addNewCandidate - Exception
     * Muc tieu: Khi user khong ton tai, service nem CommonException.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_REC_004_addNewCandidate_userNotFound() {
        when(userCommonRepo.findById(1L)).thenReturn(Optional.empty());
        assertThrows(CommonException.class, () -> service.addNewCandidate(1L, 10L, "x"));
    }

    /**
     * TC_REC_005 - addNewCandidate - Exception
     * Muc tieu: Khi repo.save gap loi, service nem CommonException.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_REC_005_addNewCandidate_repoException() {
        UserCommon user = new UserCommon();
        user.setId(1L);
        when(userCommonRepo.findById(1L)).thenReturn(Optional.of(user));
        when(repo.save(any(RecruiterManagement.class))).thenThrow(new RuntimeException("db"));

        assertThrows(CommonException.class, () -> service.addNewCandidate(1L, 10L, "x"));
    }

    /**
     * TC_REC_006 - updateStatusCandidate - Standard
     * Muc tieu: Khi record ton tai, service cap nhat status va tra SUCCESS.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_REC_006_updateStatusCandidate_success() {
        RecruiterManagement existing = new RecruiterManagement();
        existing.setStatus("0");
        when(repo.findByUserAndFreelancer(1L, 10L)).thenReturn(Optional.of(existing));

        ResponseEntity<String> response = service.updateStatusCandidate(1L, 10L, "2");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(SUCCESS, response.getBody());
        assertEquals("2", existing.getStatus());
        verify(repo, times(1)).save(any());
    }

    /**
     * TC_REC_007 - updateStatusCandidate - Standard
     * Muc tieu: Khi record khong ton tai, service khong save va tra SUCCESS.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_REC_007_updateStatusCandidate_notFound() {
        when(repo.findByUserAndFreelancer(1L, 10L)).thenReturn(Optional.empty());

        ResponseEntity<String> response = service.updateStatusCandidate(1L, 10L, "2");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(SUCCESS, response.getBody());
        verify(repo, never()).save(any());
    }

    /**
     * TC_REC_008 - updateStatusCandidate - Exception
     * Muc tieu: Khi repo.findByUserAndFreelancer throw, service nem CommonException.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_REC_008_updateStatusCandidate_exception() {
        when(repo.findByUserAndFreelancer(1L, 10L)).thenThrow(new RuntimeException("db"));
        assertThrows(CommonException.class, () -> service.updateStatusCandidate(1L, 10L, "2"));
    }

    /**
     * TC_REC_009 - updateNoteRecruiterManagement - Standard
     * Muc tieu: Khi record ton tai, service cap nhat note va tra SUCCESS.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_REC_009_updateNoteRecruiterManagement_success() {
        RecruiterManagement existing = new RecruiterManagement();
        existing.setNote("0");
        when(repo.findByUserAndFreelancer(1L, 10L)).thenReturn(Optional.of(existing));

        ResponseEntity<String> response = service.updateNoteRecruiterManagement(1L, 10L, "shortlist");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(SUCCESS, response.getBody());
        assertEquals("shortlist", existing.getNote());
        verify(repo, times(1)).save(any());
    }

    /**
     * TC_REC_010 - updateNoteRecruiterManagement - Standard
     * Muc tieu: Khi record khong ton tai, service khong save va tra SUCCESS.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_REC_010_updateNoteRecruiterManagement_notFound() {
        when(repo.findByUserAndFreelancer(1L, 10L)).thenReturn(Optional.empty());

        ResponseEntity<String> response = service.updateNoteRecruiterManagement(1L, 10L, "shortlist");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(SUCCESS, response.getBody());
        verify(repo, never()).save(any());
    }

    /**
     * TC_REC_011 - updateNoteRecruiterManagement - Exception
     * Muc tieu: Khi repo.findByUserAndFreelancer throw, service nem CommonException.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_REC_011_updateNoteRecruiterManagement_exception() {
        when(repo.findByUserAndFreelancer(1L, 10L)).thenThrow(new RuntimeException("db"));
        assertThrows(CommonException.class, () -> service.updateNoteRecruiterManagement(1L, 10L, "shortlist"));
    }

    /**
     * TC_REC_012 - listPost - Standard
     * Muc tieu: Khi co data, service tra FOUND va paging hop le.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_REC_012_listPost_hasData() {
        when(tokenWrapper.getUid()).thenReturn(1L);
        JobProjection projection = Mockito.mock(JobProjection.class);
        Page<JobProjection> page = new PageImpl<>(Collections.singletonList(projection), PageRequest.of(0, 10), 1);
        when(jobRepo.findJobsByUserId(eq(1L), any(Pageable.class))).thenReturn(page);

        PageableModel pageableModel = new PageableModel();
        pageableModel.setPage(1);
        pageableModel.setSize(10);

        ResponseEntity<ResponseObject> response = service.listPost(pageableModel);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(FOUND, response.getBody().getStatus());
        assertEquals(SUCCESS_CODE, response.getBody().getCode());
    }

    /**
     * TC_REC_013 - listPost - Standard
     * Muc tieu: Khi page rong, service tra NOT_FOUND va list rong.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_REC_013_listPost_noData() {
        when(tokenWrapper.getUid()).thenReturn(1L);
        Page<JobProjection> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(jobRepo.findJobsByUserId(eq(1L), any(Pageable.class))).thenReturn(page);

        PageableModel pageableModel = new PageableModel();
        pageableModel.setPage(1);
        pageableModel.setSize(10);

        ResponseEntity<ResponseObject> response = service.listPost(pageableModel);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(NOT_FOUND, response.getBody().getStatus());
        assertEquals(SUCCESS_CODE, response.getBody().getCode());
    }

    /**
     * TC_REC_014 - listPost - Standard
     * Muc tieu: Khi repo throw exception, service tra ERROR + NOT_FOUND status.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_REC_014_listPost_exception() {
        when(tokenWrapper.getUid()).thenReturn(1L);
        when(jobRepo.findJobsByUserId(eq(1L), any(Pageable.class))).thenThrow(new RuntimeException("db"));

        PageableModel pageableModel = new PageableModel();
        pageableModel.setPage(1);
        pageableModel.setSize(10);

        ResponseEntity<ResponseObject> response = service.listPost(pageableModel);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(com.jober.utilsservice.constant.Constant.ERROR, response.getBody().getStatus());
        assertEquals(NULL_CODE, response.getBody().getCode());
    }

    /**
     * TC_REC_015 - listPostCSV - Standard
     * Muc tieu: Khi co job, CSV phai co header va data.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_REC_015_listPostCSV_hasData() {
        Job job = new Job();
        job.setId(1L);
        job.setName("Post A");
        job.setJob("Dev");
        job.setSalary("1000");
        job.setDes("desc");
        job.setAddress("HN");
        job.setCv("cv.pdf");
        job.setActive(1);
        job.setCreationDate(LocalDateTime.now());
        job.setExpDate(LocalDateTime.now().plusDays(1));

        when(jobRepo.findByIds(Arrays.asList(1L))).thenReturn(Collections.singletonList(job));

        StringWriter writer = new StringWriter();
        service.listPostCSV(writer, Arrays.asList(1L));

        String output = writer.toString();
        assertTrue(output.contains("ID"));
        assertTrue(output.contains("Post A"));
    }

    /**
     * TC_REC_016 - listPostCSV - Standard
     * Muc tieu: Khi listJobId rong, CSV van co header.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_REC_016_listPostCSV_empty() {
        when(jobRepo.findByIds(Collections.emptyList())).thenReturn(Collections.emptyList());

        StringWriter writer = new StringWriter();
        service.listPostCSV(writer, Collections.emptyList());

        String output = writer.toString();
        assertTrue(output.contains("ID"));
    }

    /**
     * TC_REC_017 - listPostCSV - Standard
     * Muc tieu: Khi repo throw, method khong nem exception.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_REC_017_listPostCSV_exception() {
        when(jobRepo.findByIds(Collections.emptyList())).thenThrow(new RuntimeException("db"));

        StringWriter writer = new StringWriter();
        service.listPostCSV(writer, Collections.emptyList());

        assertTrue(writer.toString().isEmpty() || writer.toString().contains("ID"));
    }

    /**
     * TC_REC_018 - findAppliedCandidate - Standard
     * Muc tieu: Khi co data, service tra FOUND.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_REC_018_findAppliedCandidate_hasData() {
        when(tokenWrapper.getUid()).thenReturn(1L);
        FreelancerProjection projection = Mockito.mock(FreelancerProjection.class);
        Page<FreelancerProjection> page = new PageImpl<>(Collections.singletonList(projection), PageRequest.of(0, 10), 1);
        when(freelancerRepo.findAppliedCandidate(eq(1L), any(Pageable.class))).thenReturn(page);

        ResponseEntity<ResponseObject> response = service.findAppliedCandidate(new Paging(1, 10));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(FOUND, response.getBody().getStatus());
        assertEquals(SUCCESS_CODE, response.getBody().getCode());
    }

    /**
     * TC_REC_019 - findAppliedCandidate - Standard
     * Muc tieu: Khi repo tra null, service tra NOT_FOUND.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_REC_019_findAppliedCandidate_nullPage() {
        when(tokenWrapper.getUid()).thenReturn(1L);
        when(freelancerRepo.findAppliedCandidate(eq(1L), any(Pageable.class))).thenReturn(null);

        ResponseEntity<ResponseObject> response = service.findAppliedCandidate(new Paging(1, 10));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(NOT_FOUND, response.getBody().getStatus());
        assertEquals(SUCCESS, response.getBody().getCode());
    }

    /**
     * TC_REC_020 - getOrganizationName - Standard
     * Muc tieu: Khi co organization, service tra map id->name.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_REC_020_getOrganizationName_hasData() {
        List<Object[]> resultList = new ArrayList<>();
        resultList.add(new Object[]{1L, "OrgA"});
        resultList.add(new Object[]{2L, "OrgB"});
        when(organizationRepo.findAllOrganizationIdAndName()).thenReturn(resultList);

        ResponseEntity<ResponseObject> response = service.getOrganizationName();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(FOUND, response.getBody().getStatus());
        assertEquals("SUCCESS_CODE", response.getBody().getCode());
        Map<Long, String> actualMap = (Map<Long, String>) response.getBody().getData();
        assertEquals("OrgA", actualMap.get(1L));
        assertEquals("OrgB", actualMap.get(2L));
    }

    /**
     * TC_REC_021 - getOrganizationName - Standard
     * Muc tieu: Khi khong co organization, service tra NOT_FOUND.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_REC_021_getOrganizationName_noData() {
        when(organizationRepo.findAllOrganizationIdAndName()).thenReturn(Collections.emptyList());

        ResponseEntity<ResponseObject> response = service.getOrganizationName();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(NOT_FOUND, response.getBody().getStatus());
        assertEquals(SUCCESS, response.getBody().getCode());
    }
}
