package com.resourceservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import com.resourceservice.dto.request.JobParamDTO;
import com.resourceservice.dto.request.JobParamSearchDTO;
import com.resourceservice.dto.request.getRecommendDTO;
import com.resourceservice.service.JobService;
import com.resourceservice.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class JobCtrlTest {

  @InjectMocks
  private JobCtrl jobCtrl;

  @Mock
  private JobService jobService;

  @Mock
  private RecommendationService recommendationService;

  @Test
  void getListJobsCommon_validRequest_delegatesToService() {
    // Test Case ID: TC_JOB_CTRL_SEARCH_001
    // Scenario ID: SC_JOB_CTRL_HAPPY_001
    // Objective: Verify /_search delegates JobParamDTO to jobService.getListJobs.
    // Covered Branch/Path: Controller receives search request -> delegates service call.
    // DB Check: No direct DB access in controller; only service delegation is verified.
    // Rollback: Not applicable.
    // Arrange
    JobParamDTO input = new JobParamDTO();
    ResponseEntity<ResponseObject> expected = ResponseEntity.ok(new ResponseObject());
    when(jobService.getListJobs(input)).thenReturn(expected);

    // Act
    ResponseEntity<ResponseObject> actual = jobCtrl.getListJobsCommon(input);

    // Assert
    assertThat(actual).isSameAs(expected);
    verify(jobService).getListJobs(input);
    verifyNoMoreInteractions(jobService);
  }

  @Test
  void getListJobsCommonV2_withPaging_delegatesParsedValues() {
    // Test Case ID: TC_JOB_CTRL_SEARCH_002
    // Scenario ID: SC_JOB_CTRL_HAPPY_002
    // Objective: Verify /_search_v2 parses userId and passes provided paging values.
    // Covered Branch/Path: Paging exists -> use provided page/size -> delegate getListJobsV2.
    // DB Check: No direct DB access in controller; only service delegation is verified.
    // Rollback: Not applicable.
    // Arrange
    getRecommendDTO input = new getRecommendDTO();
    input.setUserId(" 123 ");
    getRecommendDTO.Paging paging = new getRecommendDTO.Paging();
    paging.setPage(3);
    paging.setSize(15);
    input.setPaging(paging);

    ResponseEntity<ResponseObject> expected = ResponseEntity.ok(new ResponseObject());
    when(jobService.getListJobsV2(123L, 3, 15)).thenReturn(expected);

    // Act
    ResponseEntity<ResponseObject> actual = jobCtrl.getListJobsCommon(input);

    // Assert
    assertThat(actual).isSameAs(expected);
    verify(jobService).getListJobsV2(123L, 3, 15);
    verifyNoMoreInteractions(jobService);
  }

  @Test
  void getListJobsCommonV2_withoutPaging_usesDefaultPaging() {
    // Test Case ID: TC_JOB_CTRL_SEARCH_003
    // Scenario ID: SC_JOB_CTRL_EDGE_001
    // Objective: Verify /_search_v2 uses default page=0 and size=10 when paging is null.
    // Covered Branch/Path: Paging null -> default values -> delegate getListJobsV2.
    // DB Check: No direct DB access in controller; only service delegation is verified.
    // Rollback: Not applicable.
    // Arrange
    getRecommendDTO input = new getRecommendDTO();
    input.setUserId("88");
    input.setPaging(null);

    ResponseEntity<ResponseObject> expected = ResponseEntity.ok(new ResponseObject());
    when(jobService.getListJobsV2(88L, 0, 10)).thenReturn(expected);

    // Act
    ResponseEntity<ResponseObject> actual = jobCtrl.getListJobsCommon(input);

    // Assert
    assertThat(actual).isSameAs(expected);
    verify(jobService).getListJobsV2(88L, 0, 10);
    verifyNoMoreInteractions(jobService);
  }

  @Test
  void searchJobsAdvanced_validRequest_delegatesToService() {
    // Test Case ID: TC_JOB_CTRL_SEARCH_004
    // Scenario ID: SC_JOB_CTRL_HAPPY_003
    // Objective: Verify /_search_advanced delegates JobParamSearchDTO to service.
    // Covered Branch/Path: Advanced search request -> direct delegation to searchJobsAdvanced.
    // DB Check: No direct DB access in controller; only service delegation is verified.
    // Rollback: Not applicable.
    // Arrange
    JobParamSearchDTO input = new JobParamSearchDTO();
    ResponseEntity<ResponseObject> expected = ResponseEntity.ok(new ResponseObject());
    when(jobService.searchJobsAdvanced(input)).thenReturn(expected);

    // Act
    ResponseEntity<ResponseObject> actual = jobCtrl.searchJobsAdvanced(input);

    // Assert
    assertThat(actual).isSameAs(expected);
    verify(jobService).searchJobsAdvanced(input);
    verifyNoMoreInteractions(jobService);
  }
}
