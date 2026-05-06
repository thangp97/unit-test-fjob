package com.resourceservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import com.resourceservice.dto.request.ScheduleParamDTO;
import com.resourceservice.service.ScheduleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class RecruiterControllerTest {

  @InjectMocks
  private RecruiterController recruiterController;

  @Mock
  private ScheduleService scheduleService;

  @Test
  void listCandidateSelected_validRequest_delegatesToScheduleService() {
    // Test Case ID: TC_RECRUITER_CTRL_SCHEDULE_001
    // Scenario ID: SC_RECRUITER_CTRL_HAPPY_001
    // Objective: Verify selected-candidate endpoint delegates to scheduleService.getScheduleByStatus.
    // Covered Branch/Path: Recruiter selected-candidate request -> direct delegation to schedule service.
    // DB Check: No direct DB access in controller; service delegation only.
    // Rollback: Not applicable.

    // Arrange
    ScheduleParamDTO input = new ScheduleParamDTO();
    ResponseEntity<ResponseObject> expected = ResponseEntity.ok(new ResponseObject());
    when(scheduleService.getScheduleByStatus(input)).thenReturn(expected);

    // Act
    ResponseEntity<ResponseObject> actual = recruiterController.listCandidateSelected(input);

    // Assert
    assertThat(actual).isSameAs(expected);
    verify(scheduleService).getScheduleByStatus(input);
    verifyNoMoreInteractions(scheduleService);
  }

  @Test
  void listCandidateSelected_nullRequest_stillDelegatesToScheduleService() {
    // Test Case ID: TC_RECRUITER_CTRL_SCHEDULE_002
    // Scenario ID: SC_RECRUITER_CTRL_EDGE_001
    // Objective: Verify controller keeps pass-through behavior when request body is null.
    // Covered Branch/Path: Null body -> direct delegation to schedule service without local validation.
    // DB Check: No direct DB access in controller; service delegation only.
    // Rollback: Not applicable.

    // Arrange
    ResponseEntity<ResponseObject> expected = ResponseEntity.ok(new ResponseObject());
    when(scheduleService.getScheduleByStatus(null)).thenReturn(expected);

    // Act
    ResponseEntity<ResponseObject> actual = recruiterController.listCandidateSelected(null);

    // Assert
    assertThat(actual).isSameAs(expected);
    verify(scheduleService).getScheduleByStatus(null);
    verifyNoMoreInteractions(scheduleService);
  }

  @Test
  void listCandidateSelected_serviceThrowsException_propagatesException() {
    // Test Case ID: TC_RECRUITER_CTRL_SCHEDULE_003
    // Scenario ID: SC_RECRUITER_CTRL_SAD_001
    // Objective: Verify controller propagates service exception for selected-candidate endpoint.
    // Covered Branch/Path: scheduleService throws RuntimeException -> exception propagates to caller.
    // DB Check: No direct DB access in controller; service delegation only.
    // Rollback: Not applicable.

    // Arrange
    when(scheduleService.getScheduleByStatus(null)).thenThrow(new RuntimeException("service error"));

    // Act
    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> recruiterController.listCandidateSelected(null));

    // Assert
    assertThat(ex.getMessage()).isEqualTo("service error");
    verify(scheduleService).getScheduleByStatus(null);
    verifyNoMoreInteractions(scheduleService);
  }
}
