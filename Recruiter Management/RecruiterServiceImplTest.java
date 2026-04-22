package com.resourceservice.service.impl;

import com.jober.utilsservice.model.PageableModel;
import com.jober.utilsservice.utils.modelCustom.Paging;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import com.resourceservice.dto.MyPostDto;
import com.resourceservice.dto.request.FreelancerStatsRequest;
import com.resourceservice.dto.request.FreelancerStatsResponse;
import com.resourceservice.exception.CommonException;
import com.resourceservice.interceptor.BearerTokenWrapper;
import com.resourceservice.model.Job;
import com.resourceservice.model.RecruiterManagement;
import com.resourceservice.model.UserCommon;
import com.resourceservice.model.projection.FreelancerProjection;
import com.resourceservice.model.projection.JobProjection;
import com.resourceservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.persistence.EntityManager;
import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecruiterServiceImplTest {

    @Mock
    private JobRepo jobRepo;

    @Mock
    private OrganizationRepo organizationRepo;

    @Mock
    private RecruiterManagementRepo repo;

    @Mock
    private UserCommonRepo userCommonRepo;

    @Mock
    private FreelancerRepo freelancerRepo;

    @Mock
    private BearerTokenWrapper tokenWrapper;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private RecruiterServiceImpl recruiterService;

    // ==================== TC_RS_001 ====================
    @Test
    @DisplayName("TC_RS_001: addNewRecruiter - Happy path - save succeeds")
    void testAddNewRecruiter_Success() {
        RecruiterManagement entity = RecruiterManagement.builder()
                .comment("test").freelancerid(1L).build();
        RecruiterManagement saved = RecruiterManagement.builder()
                .id(1L).comment("test").freelancerid(1L).build();

        when(repo.save(any(RecruiterManagement.class))).thenReturn(saved);

        ResponseEntity<ResponseObject> response = recruiterService.addNewRecruiter(entity);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(repo, times(1)).save(entity);
    }

    // ==================== TC_RS_002 ====================
    @Test
    @DisplayName("TC_RS_002: addNewRecruiter - Edge case - save returns null")
    void testAddNewRecruiter_SaveReturnsNull() {
        RecruiterManagement entity = RecruiterManagement.builder().build();

        when(repo.save(any(RecruiterManagement.class))).thenReturn(null);

        ResponseEntity<ResponseObject> response = recruiterService.addNewRecruiter(entity);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
    }

    // ==================== TC_RS_003 ====================
    @Test
    @DisplayName("TC_RS_003: addNewCandidate - Happy path - add candidate successfully")
    void testAddNewCandidate_Success() {
        UserCommon user = UserCommon.builder().id(1L).build();
        when(userCommonRepo.findById(1L)).thenReturn(Optional.of(user));
        when(repo.save(any(RecruiterManagement.class))).thenReturn(new RecruiterManagement());

        ResponseEntity<String> response = recruiterService.addNewCandidate(1L, 10L, "test");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(repo, times(1)).save(any(RecruiterManagement.class));
    }

    // ==================== TC_RS_004 ====================
    @Test
    @DisplayName("TC_RS_004: addNewCandidate - Exception - user not found")
    void testAddNewCandidate_UserNotFound() {
        when(userCommonRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CommonException.class, () ->
                recruiterService.addNewCandidate(999L, 10L, "n"));
    }

    // ==================== TC_RS_005 ====================
    @Test
    @DisplayName("TC_RS_005: addNewCandidate - Exception - repo.save() throws")
    void testAddNewCandidate_SaveThrows() {
        UserCommon user = UserCommon.builder().id(1L).build();
        when(userCommonRepo.findById(1L)).thenReturn(Optional.of(user));
        when(repo.save(any(RecruiterManagement.class))).thenThrow(new RuntimeException("DB error"));

        assertThrows(CommonException.class, () ->
                recruiterService.addNewCandidate(1L, 10L, "n"));
    }

    // ==================== TC_RS_006 ====================
    @Test
    @DisplayName("TC_RS_006: updateStatusCandidate - Happy path - update status")
    void testUpdateStatusCandidate_Success() {
        RecruiterManagement entity = RecruiterManagement.builder()
                .id(1L).status("1").build();

        when(repo.findByUserAndFreelancer(1L, 10L)).thenReturn(Optional.of(entity));
        when(repo.save(any())).thenReturn(entity);

        ResponseEntity<String> response = recruiterService.updateStatusCandidate(1L, 10L, "2");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("2", entity.getStatus());
    }

    // ==================== TC_RS_007 ====================
    @Test
    @DisplayName("TC_RS_007: updateStatusCandidate - Edge case - entity not found")
    void testUpdateStatusCandidate_EntityNotFound() {
        when(repo.findByUserAndFreelancer(1L, 99L)).thenReturn(Optional.empty());
        // save(Optional.empty()) might throw or cause issue
        when(repo.save(any())).thenThrow(new RuntimeException("Cannot save empty"));

        assertThrows(CommonException.class, () ->
                recruiterService.updateStatusCandidate(1L, 99L, "2"));
    }

    // ==================== TC_RS_008 ====================
    @Test
    @DisplayName("TC_RS_008: updateStatusCandidate - Exception path")
    void testUpdateStatusCandidate_ExceptionThrown() {
        when(repo.findByUserAndFreelancer(anyLong(), anyLong()))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(CommonException.class, () ->
                recruiterService.updateStatusCandidate(1L, 10L, "2"));
    }

    // ==================== TC_RS_009 ====================
    @Test
    @DisplayName("TC_RS_009: updateNoteRecruiterManagement - Happy path")
    void testUpdateNoteRecruiterManagement_Success() {
        RecruiterManagement entity = RecruiterManagement.builder()
                .id(1L).note("old").build();

        when(repo.findByUserAndFreelancer(1L, 10L)).thenReturn(Optional.of(entity));
        when(repo.save(any())).thenReturn(entity);

        ResponseEntity response = recruiterService.updateNoteRecruiterManagement(1L, 10L, "important");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("important", entity.getNote());
    }

    // ==================== TC_RS_010 ====================
    @Test
    @DisplayName("TC_RS_010: updateNoteRecruiterManagement - Edge case - entity not found")
    void testUpdateNoteRecruiterManagement_EntityNotFound() {
        when(repo.findByUserAndFreelancer(1L, 99L)).thenReturn(Optional.empty());
        when(repo.save(any())).thenThrow(new RuntimeException("Cannot save empty"));

        assertThrows(CommonException.class, () ->
                recruiterService.updateNoteRecruiterManagement(1L, 99L, "n"));
    }

    // ==================== TC_RS_011 ====================
    @Test
    @DisplayName("TC_RS_011: updateNoteRecruiterManagement - Exception path")
    void testUpdateNoteRecruiterManagement_Exception() {
        when(repo.findByUserAndFreelancer(anyLong(), anyLong()))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(CommonException.class, () ->
                recruiterService.updateNoteRecruiterManagement(1L, 10L, "note"));
    }

    // ==================== TC_RS_012 ====================
    @Test
    @DisplayName("TC_RS_012: listPost - Happy path - jobs found")
    void testListPost_JobsFound() {
        PageableModel pageableModel = new PageableModel();
        pageableModel.setPage(1);
        pageableModel.setSize(10);

        when(tokenWrapper.getUid()).thenReturn(1L);

        JobProjection projection = mock(JobProjection.class);
        Page<JobProjection> jobPage = new PageImpl<>(
                List.of(projection, projection, projection),
                PageRequest.of(0, 10), 3);

        when(jobRepo.findJobsByUserId(eq(1L), any(Pageable.class))).thenReturn(jobPage);

        ResponseEntity response = recruiterService.listPost(pageableModel);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ==================== TC_RS_013 ====================
    @Test
    @DisplayName("TC_RS_013: listPost - Edge case - empty results")
    void testListPost_EmptyResults() {
        PageableModel pageableModel = new PageableModel();
        pageableModel.setPage(1);
        pageableModel.setSize(10);

        when(tokenWrapper.getUid()).thenReturn(1L);

        Page<JobProjection> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(jobRepo.findJobsByUserId(eq(1L), any(Pageable.class))).thenReturn(emptyPage);

        ResponseEntity response = recruiterService.listPost(pageableModel);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ==================== TC_RS_014 ====================
    @Test
    @DisplayName("TC_RS_014: listPost - Boundary - page=1 (first page)")
    void testListPost_FirstPage() {
        PageableModel pageableModel = new PageableModel();
        pageableModel.setPage(1);
        pageableModel.setSize(5);

        when(tokenWrapper.getUid()).thenReturn(1L);

        JobProjection projection = mock(JobProjection.class);
        Page<JobProjection> jobPage = new PageImpl<>(
                List.of(projection), PageRequest.of(0, 5), 1);

        when(jobRepo.findJobsByUserId(eq(1L), any(Pageable.class))).thenReturn(jobPage);

        ResponseEntity response = recruiterService.listPost(pageableModel);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Verify PageRequest.of(0, 5) was used internally (page-1)
        verify(jobRepo).findJobsByUserId(eq(1L), eq(PageRequest.of(0, 5)));
    }

    // ==================== TC_RS_015 ====================
    @Test
    @DisplayName("TC_RS_015: listPost - Exception path")
    void testListPost_Exception() {
        PageableModel pageableModel = new PageableModel();
        pageableModel.setPage(1);
        pageableModel.setSize(10);

        when(tokenWrapper.getUid()).thenReturn(1L);
        when(jobRepo.findJobsByUserId(anyLong(), any(Pageable.class)))
                .thenThrow(new RuntimeException("DB error"));

        ResponseEntity response = recruiterService.listPost(pageableModel);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ==================== TC_RS_016 ====================
    @Test
    @DisplayName("TC_RS_016: listPostCSV - Happy path - export CSV with jobs")
    void testListPostCSV_WithJobs() {
        Writer writer = new StringWriter();
        List<Long> listJobId = List.of(1L, 2L);

        Job job1 = Job.builder().id(1L).name("Job1").job("Dev").salary("1000")
                .des("desc").address("HCM").cv("cv1").active(1)
                .creationDate(LocalDateTime.now()).expDate(LocalDateTime.now().plusDays(30))
                .build();
        Job job2 = Job.builder().id(2L).name("Job2").job("QA").salary("2000")
                .des("desc2").address("HN").cv("cv2").active(1)
                .creationDate(LocalDateTime.now()).expDate(LocalDateTime.now().plusDays(30))
                .build();

        when(jobRepo.findByIds(listJobId)).thenReturn(List.of(job1, job2));

        recruiterService.listPostCSV(writer, listJobId);

        String csv = writer.toString();
        assertTrue(csv.contains("ID"));
        assertTrue(csv.contains("Name"));
        assertTrue(csv.contains("Job1"));
        assertTrue(csv.contains("Job2"));
    }

    // ==================== TC_RS_017 ====================
    @Test
    @DisplayName("TC_RS_017: listPostCSV - Edge case - empty job list")
    void testListPostCSV_EmptyList() {
        Writer writer = new StringWriter();
        List<Long> listJobId = Collections.emptyList();

        when(jobRepo.findByIds(listJobId)).thenReturn(Collections.emptyList());

        recruiterService.listPostCSV(writer, listJobId);

        String csv = writer.toString();
        assertTrue(csv.contains("ID")); // Header should still be present
    }

    // ==================== TC_RS_018 ====================
    @Test
    @DisplayName("TC_RS_018: listPostCSV - Edge case - job with null fields")
    void testListPostCSV_JobWithNullFields() {
        Writer writer = new StringWriter();
        List<Long> listJobId = List.of(1L);

        Job job = Job.builder().id(1L).name(null).job(null).salary(null)
                .des(null).address(null).cv(null).active(null)
                .creationDate(null).expDate(null).build();

        when(jobRepo.findByIds(listJobId)).thenReturn(List.of(job));

        // Should not throw exception
        assertDoesNotThrow(() -> recruiterService.listPostCSV(writer, listJobId));
    }

    // ==================== TC_RS_019 ====================
    @Test
    @DisplayName("TC_RS_019: findAppliedCandidate - Happy path - candidates found")
    void testFindAppliedCandidate_Found() {
        Paging paging = new Paging(1, 10);

        when(tokenWrapper.getUid()).thenReturn(1L);

        FreelancerProjection projection = mock(FreelancerProjection.class);
        Page<FreelancerProjection> page = new PageImpl<>(
                List.of(projection), PageRequest.of(0, 10), 1);

        when(freelancerRepo.findAppliedCandidate(eq(1L), any(Pageable.class))).thenReturn(page);

        ResponseEntity<ResponseObject> response = recruiterService.findAppliedCandidate(paging);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ==================== TC_RS_020 ====================
    @Test
    @DisplayName("TC_RS_020: findAppliedCandidate - Edge case - no applied candidates")
    void testFindAppliedCandidate_NotFound() {
        Paging paging = new Paging(1, 10);

        when(tokenWrapper.getUid()).thenReturn(1L);
        when(freelancerRepo.findAppliedCandidate(eq(1L), any(Pageable.class))).thenReturn(null);

        ResponseEntity<ResponseObject> response = recruiterService.findAppliedCandidate(paging);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ==================== TC_RS_021 ====================
    @Test
    @DisplayName("TC_RS_021: getOrganizationName - Happy path - organizations found")
    void testGetOrganizationName_Found() {
        List<Object[]> resultList = new ArrayList<>();
        resultList.add(new Object[]{1L, "OrgA"});
        resultList.add(new Object[]{2L, "OrgB"});

        when(organizationRepo.findAllOrganizationIdAndName()).thenReturn(resultList);

        ResponseEntity<ResponseObject> response = recruiterService.getOrganizationName();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ==================== TC_RS_022 ====================
    @Test
    @DisplayName("TC_RS_022: getOrganizationName - Edge case - no organizations")
    void testGetOrganizationName_Empty() {
        when(organizationRepo.findAllOrganizationIdAndName()).thenReturn(Collections.emptyList());

        ResponseEntity<ResponseObject> response = recruiterService.getOrganizationName();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ==================== TC_RS_023 ====================
    @Test
    @DisplayName("TC_RS_023: getOrganizationName - Edge case - null result")
    void testGetOrganizationName_Null() {
        when(organizationRepo.findAllOrganizationIdAndName()).thenReturn(null);

        ResponseEntity<ResponseObject> response = recruiterService.getOrganizationName();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
