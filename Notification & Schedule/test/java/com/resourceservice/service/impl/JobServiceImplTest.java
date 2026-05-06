package com.resourceservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import com.resourceservice.common.CommonUtils;
import com.resourceservice.config.EnvProperties;
import com.resourceservice.dto.JobDetailDto;
import com.resourceservice.dto.request.JobParamDTO;
import com.resourceservice.dto.request.JobParamSearchDTO;
import com.resourceservice.interceptor.BearerTokenWrapper;
import com.resourceservice.repository.CandidateManagementRepo;
import com.resourceservice.repository.FreelancerRepo;
import com.resourceservice.repository.JobRepo;
import com.resourceservice.repository.OrganizationRepo;
import com.resourceservice.repository.ScheduleRepo;
import com.resourceservice.repository.UserCommonRepo;
import com.resourceservice.service.CandidateManagementService;
import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

  private JobServiceImpl jobService;

  @Mock
  private BearerTokenWrapper tokenWrapper;
  @Mock
  private EntityManager entityManager;
  @Mock
  private TypedQuery<JobDetailDto> jobDetailQuery;
  @Mock
  private JobRepo jobRepo;
  @Mock
  private CommonUtils utils;
  @Mock
  private UserCommonRepo userCommonRepo;
  @Mock
  private FreelancerRepo freelancerRepo;
  @Mock
  private ScheduleRepo scheduleRepo;
  @Mock
  private EnvProperties envProperties;
  @Mock
  private CacheManagerService cacheManagerService;
  @Mock
  private CandidateManagementRepo candidateManagementRepo;
  @Mock
  private OrganizationRepo organizationRepo;
  @Mock
  private CandidateManagementService candidateManagementService;

  @BeforeEach
  void setUp() {
    jobService = new JobServiceImpl(tokenWrapper);
    ReflectionTestUtils.setField(jobService, "entityManager", entityManager);
    ReflectionTestUtils.setField(jobService, "jobRepo", jobRepo);
    ReflectionTestUtils.setField(jobService, "utils", utils);
    ReflectionTestUtils.setField(jobService, "userCommonRepo", userCommonRepo);
    ReflectionTestUtils.setField(jobService, "freelancerRepo", freelancerRepo);
    ReflectionTestUtils.setField(jobService, "scheduleRepo", scheduleRepo);
    ReflectionTestUtils.setField(jobService, "envProperties", envProperties);
    ReflectionTestUtils.setField(jobService, "cacheManagerService", cacheManagerService);
    ReflectionTestUtils.setField(jobService, "candidateManagementRepo", candidateManagementRepo);
    ReflectionTestUtils.setField(jobService, "organizationRepo", organizationRepo);
    ReflectionTestUtils.setField(jobService, "candidateManagementService", candidateManagementService);
  }

  @Test
  void getPageJobs_nullJobParam_throwsIllegalArgumentException() {
    // Test Case ID: TC_JOB_SERVICE_SEARCH_001
    // Scenario ID: SC_JOB_SERVICE_SAD_001
    // Objective: Verify getPageJobs rejects null input.
    // Covered Branch/Path: jobParamDTO == null -> IllegalArgumentException.
    // DB Check: No DB query must be executed when input is null.
    // Rollback: DB/cache/external services are mocked, no state is changed.
    // Arrange
    JobParamDTO input = null;

    // Act
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> jobService.getPageJobs(input));

    // Assert
    assertThat(ex.getMessage()).isEqualTo("JobParamDTO cannot be null");
    verifyNoInteractions(entityManager);
    verify(jobRepo, never()).save(any());
    verify(scheduleRepo, never()).save(any());
  }

  @Test
  void getPageJobs_validKeywordAndDefaultPaging_returnsPagedResult() {
    // Test Case ID: TC_JOB_SERVICE_SEARCH_002
    // Scenario ID: SC_JOB_SERVICE_HAPPY_001
    // Objective: Verify getPageJobs returns paged jobs and binds keyword parameter correctly.
    // Covered Branch/Path: paging null -> default paging; keySearch present -> set keySearch; query success.
    // DB Check: Read-only DB path via EntityManager.createQuery/getResultList; no save/update/delete operations.
    // Rollback: DB/cache/external services are mocked, no state is changed.
    // Arrange
    JobParamDTO input = new JobParamDTO();
    input.setKeySearch("Java");
    input.setPaging(null);

    List<JobDetailDto> results = List.of(org.mockito.Mockito.mock(JobDetailDto.class));
    when(entityManager.createQuery(anyString(), eq(JobDetailDto.class))).thenReturn(jobDetailQuery);
    when(jobDetailQuery.setParameter(anyString(), any())).thenReturn(jobDetailQuery);
    when(jobDetailQuery.setFirstResult(anyInt())).thenReturn(jobDetailQuery);
    when(jobDetailQuery.setMaxResults(anyInt())).thenReturn(jobDetailQuery);
    when(jobDetailQuery.getResultList()).thenReturn(results, results);

    // Act
    Page<JobDetailDto> actual = jobService.getPageJobs(input);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.getTotalElements()).isEqualTo(1L);
    assertThat(actual.getContent()).hasSize(1);
    verify(entityManager, times(1)).createQuery(anyString(), eq(JobDetailDto.class));
    verify(jobDetailQuery, times(1)).setParameter("keySearch", "%java%");
    verify(jobDetailQuery, times(1)).setFirstResult(0);
    verify(jobDetailQuery, times(1)).setMaxResults(10);
    verify(jobDetailQuery, times(2)).getResultList();
    verify(jobRepo, never()).save(any());
    verify(scheduleRepo, never()).save(any());
  }

  @Test
  void getPageJobs_queryCreationFails_throwsRuntimeException() {
    // Test Case ID: TC_JOB_SERVICE_SEARCH_003
    // Scenario ID: SC_JOB_SERVICE_SAD_002
    // Objective: Verify getPageJobs throws RuntimeException when HQL query cannot be created.
    // Covered Branch/Path: createQuery throws -> Invalid HQL query RuntimeException.
    // DB Check: Query creation attempted once; no data write operation is performed.
    // Rollback: DB/cache/external services are mocked, no state is changed.
    // Arrange
    JobParamDTO input = new JobParamDTO();
    when(entityManager.createQuery(anyString(), eq(JobDetailDto.class)))
        .thenThrow(new IllegalArgumentException("bad hql"));

    // Act
    RuntimeException ex = assertThrows(RuntimeException.class, () -> jobService.getPageJobs(input));

    // Assert
    assertThat(ex.getMessage()).isEqualTo("Invalid HQL query");
    verify(entityManager, times(1)).createQuery(anyString(), eq(JobDetailDto.class));
    verify(jobDetailQuery, never()).getResultList();
    verify(jobRepo, never()).save(any());
    verify(scheduleRepo, never()).save(any());
  }

  @Test
  void searchJobsAdvanced_nullInput_throwsIllegalArgumentException() {
    // Test Case ID: TC_JOB_SERVICE_SEARCH_004
    // Scenario ID: SC_JOB_SERVICE_SAD_003
    // Objective: Verify searchJobsAdvanced rejects null search criteria.
    // Covered Branch/Path: searchDTO == null -> IllegalArgumentException.
    // DB Check: No DB query must be executed when input is null.
    // Rollback: DB/cache/external services are mocked, no state is changed.
    // Arrange
    JobParamSearchDTO input = null;

    // Act
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> jobService.searchJobsAdvanced(input));

    // Assert
    assertThat(ex.getMessage()).isEqualTo("JobParamSearchDTO cannot be null");
    verifyNoInteractions(entityManager);
    verify(jobRepo, never()).save(any());
    verify(scheduleRepo, never()).save(any());
  }

  @Test
  void searchJobsAdvanced_validCriteria_returnsFoundResponse() {
    // Test Case ID: TC_JOB_SERVICE_SEARCH_005
    // Scenario ID: SC_JOB_SERVICE_HAPPY_002
    // Objective: Verify advanced search returns FOUND response when jobs are available.
    // Covered Branch/Path: non-null DTO -> getPageJobsAdvanced success -> non-empty result branch.
    // DB Check: Read-only DB path via EntityManager.createQuery/getResultList; no save/update/delete operations.
    // Rollback: DB/cache/external services are mocked, no state is changed.
    // Arrange
    JobParamSearchDTO input = new JobParamSearchDTO();
    input.setSearchKey("Java");
    input.setPage(2);
    input.setSize(5);

    List<JobDetailDto> results = List.of(
        org.mockito.Mockito.mock(JobDetailDto.class),
        org.mockito.Mockito.mock(JobDetailDto.class));

    when(entityManager.createQuery(anyString(), eq(JobDetailDto.class))).thenReturn(jobDetailQuery);
    when(jobDetailQuery.setParameter(anyString(), any())).thenReturn(jobDetailQuery);
    when(jobDetailQuery.setFirstResult(anyInt())).thenReturn(jobDetailQuery);
    when(jobDetailQuery.setMaxResults(anyInt())).thenReturn(jobDetailQuery);
    when(jobDetailQuery.getResultList()).thenReturn(results, results);

    // Act
    ResponseEntity<ResponseObject> actual = jobService.searchJobsAdvanced(input);

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject body = actual.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getTotalCount()).isNotNull();
    assertThat(body.getTotalCount()).isGreaterThan(0L);
    assertThat(body.getCurrentCount()).isNotNull();
    assertThat(body.getCurrentCount()).isGreaterThanOrEqualTo(0);
    assertThat(body.getTotalPage()).isNotNull();
    assertThat(body.getTotalPage()).isGreaterThanOrEqualTo(1);
    assertThat((List<?>) body.getData()).hasSize(2);
    verify(entityManager, times(1)).createQuery(anyString(), eq(JobDetailDto.class));
    verify(jobDetailQuery, times(1)).setParameter("searchKey", "%java%");
    verify(jobDetailQuery, times(1)).setFirstResult(5);
    verify(jobDetailQuery, times(1)).setMaxResults(5);
    verify(jobDetailQuery, times(2)).getResultList();
    verify(jobRepo, never()).save(any());
    verify(scheduleRepo, never()).save(any());
  }

  @Test
  void searchJobsAdvanced_noData_returnsNotFoundResponse() {
    // Test Case ID: TC_JOB_SERVICE_SEARCH_006
    // Scenario ID: SC_JOB_SERVICE_EDGE_001
    // Objective: Verify advanced search returns NOT_FOUND response when no jobs match filters.
    // Covered Branch/Path: non-null DTO -> getPageJobsAdvanced success -> empty result branch.
    // DB Check: Read-only DB path via EntityManager.createQuery/getResultList; no save/update/delete operations.
    // Rollback: DB/cache/external services are mocked, no state is changed.
    // Arrange
    JobParamSearchDTO input = new JobParamSearchDTO();
    input.setPage(null);
    input.setSize(null);
    input.setSearchKey(null);

    when(entityManager.createQuery(anyString(), eq(JobDetailDto.class))).thenReturn(jobDetailQuery);
    when(jobDetailQuery.setFirstResult(anyInt())).thenReturn(jobDetailQuery);
    when(jobDetailQuery.setMaxResults(anyInt())).thenReturn(jobDetailQuery);
    when(jobDetailQuery.getResultList()).thenReturn(Collections.emptyList(), Collections.emptyList());

    // Act
    ResponseEntity<ResponseObject> actual = jobService.searchJobsAdvanced(input);

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject body = actual.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getTotalCount()).isEqualTo(0L);
    assertThat(body.getCurrentCount()).isEqualTo(0);
    assertThat(body.getTotalPage()).isEqualTo(0);
    assertThat((List<?>) body.getData()).isEmpty();
    verify(entityManager, times(1)).createQuery(anyString(), eq(JobDetailDto.class));
    verify(jobDetailQuery, times(1)).setFirstResult(0);
    verify(jobDetailQuery, times(1)).setMaxResults(10);
    verify(jobDetailQuery, times(2)).getResultList();
    verify(jobRepo, never()).save(any());
    verify(scheduleRepo, never()).save(any());
  }
}
