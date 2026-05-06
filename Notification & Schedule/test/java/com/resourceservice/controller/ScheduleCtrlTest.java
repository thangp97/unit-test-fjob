package com.resourceservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.jober.utilsservice.utils.modelCustom.Response;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import com.resourceservice.dto.request.ScheduleInputDTO;
import com.resourceservice.dto.request.ScheduleParamDTO;
import com.resourceservice.dto.request.ScheduleRqDTO;
import com.resourceservice.dto.request.StatusRequest;
import com.resourceservice.repository.ScheduleRepo;
import com.resourceservice.service.ScheduleService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ScheduleCtrlTest {

  @InjectMocks
  private ScheduleCtrl scheduleCtrl;

  @Mock
  private ScheduleService scheduleService;

  @Mock
  private ScheduleRepo scheduleRepo;

  @Test
  void saveSchedule_validRequest_delegatesToService() {
    // Test Case ID: TC_SCHEDULE_CTRL_001
    // Scenario ID: SC_SCHEDULE_CTRL_HAPPY_001
    // Objective: Verify saveSchedule delegates request to scheduleService.saveSchedule.
    // Covered Branch/Path: Controller receives valid DTO -> delegates to service -> returns service response.
    // DB Check: No direct DB access in controller; service delegation only.
    // Rollback: Not applicable.

    // Arrange
    ScheduleRqDTO input = new ScheduleRqDTO();
    ResponseEntity<ResponseObject> expected = ResponseEntity.ok(new ResponseObject());
    when(scheduleService.saveSchedule(input)).thenReturn(expected);

    // Act
    ResponseEntity<ResponseObject> actual = scheduleCtrl.saveSchedule(input);

    // Assert
    assertThat(actual).isSameAs(expected);
    verify(scheduleService).saveSchedule(input);
    verifyNoMoreInteractions(scheduleService);
  }

  @Test
  void getScheduleById_validId_delegatesToService() {
    // Test Case ID: TC_SCHEDULE_CTRL_002
    // Scenario ID: SC_SCHEDULE_CTRL_HAPPY_002
    // Objective: Verify getScheduleById delegates id to scheduleService.getScheduleById.
    // Covered Branch/Path: Controller receives id -> delegates to service -> returns service response.
    // DB Check: No direct DB access in controller; service delegation only.
    // Rollback: Not applicable.

    // Arrange
    ResponseEntity<ResponseObject> expected = ResponseEntity.ok(new ResponseObject());
    when(scheduleService.getScheduleById(100L)).thenReturn(expected);

    // Act
    ResponseEntity<ResponseObject> actual = scheduleCtrl.getScheduleById(100L);

    // Assert
    assertThat(actual).isSameAs(expected);
    verify(scheduleService).getScheduleById(100L);
    verifyNoMoreInteractions(scheduleService);
  }

  @Test
  void getScheduleByStatus_validRequest_delegatesToService() {
    // Test Case ID: TC_SCHEDULE_CTRL_003
    // Scenario ID: SC_SCHEDULE_CTRL_HAPPY_003
    // Objective: Verify getScheduleByStatus delegates request to scheduleService.getScheduleByStatus.
    // Covered Branch/Path: Controller receives status filter -> delegates to service -> returns service response.
    // DB Check: No direct DB access in controller; service delegation only.
    // Rollback: Not applicable.

    // Arrange
    ScheduleParamDTO input = new ScheduleParamDTO();
    ResponseEntity<ResponseObject> expected = ResponseEntity.ok(new ResponseObject());
    when(scheduleService.getScheduleByStatus(input)).thenReturn(expected);

    // Act
    ResponseEntity<ResponseObject> actual = scheduleCtrl.getScheduleByStatus(input);

    // Assert
    assertThat(actual).isSameAs(expected);
    verify(scheduleService).getScheduleByStatus(input);
    verifyNoMoreInteractions(scheduleService);
  }

  @Test
  void getCalendar_validRequest_delegatesToService() {
    // Test Case ID: TC_SCHEDULE_CTRL_004
    // Scenario ID: SC_SCHEDULE_CTRL_HAPPY_004
    // Objective: Verify getCalendar delegates request to scheduleService.getCalendar.
    // Covered Branch/Path: Controller receives calendar filter -> delegates to service -> returns service response.
    // DB Check: No direct DB access in controller; service delegation only.
    // Rollback: Not applicable.

    // Arrange
    ScheduleInputDTO input = new ScheduleInputDTO();
    ResponseEntity<ResponseObject> expected = ResponseEntity.ok(new ResponseObject());
    when(scheduleService.getCalendar(input)).thenReturn(expected);

    // Act
    ResponseEntity<ResponseObject> actual = scheduleCtrl.getCalendar(input);

    // Assert
    assertThat(actual).isSameAs(expected);
    verify(scheduleService).getCalendar(input);
    verifyNoMoreInteractions(scheduleService);
  }

  @Test
  void deleteByIds_validIds_delegatesToService() {
    // Test Case ID: TC_SCHEDULE_CTRL_005
    // Scenario ID: SC_SCHEDULE_CTRL_HAPPY_005
    // Objective: Verify delete endpoint delegates ids to scheduleService.deleteByIds.
    // Covered Branch/Path: Controller receives id list -> delegates to service -> returns service response.
    // DB Check: No direct DB access in controller; service delegation only.
    // Rollback: Not applicable.

    // Arrange
    List<Long> ids = List.of(1L, 2L);
    ResponseEntity<Response> expected = ResponseEntity.ok(new Response());
    when(scheduleService.deleteByIds(ids)).thenReturn(expected);

    // Act
    ResponseEntity<Response> actual = scheduleCtrl.getCalendar(ids);

    // Assert
    assertThat(actual).isSameAs(expected);
    verify(scheduleService).deleteByIds(ids);
    verifyNoMoreInteractions(scheduleService);
  }

  @Test
  void getApplicationStatus_validRequest_delegatesToService() {
    // Test Case ID: TC_SCHEDULE_CTRL_006
    // Scenario ID: SC_SCHEDULE_CTRL_HAPPY_006
    // Objective: Verify getApplicationStatus delegates request to scheduleService.getApplicationStatus.
    // Covered Branch/Path: Controller receives status list -> delegates to service -> returns service response.
    // DB Check: No direct DB access in controller; service delegation only.
    // Rollback: Not applicable.

    // Arrange
    StatusRequest input = new StatusRequest(List.of("SELECTED"));
    ResponseEntity<ResponseObject> expected = ResponseEntity.ok(new ResponseObject());
    when(scheduleService.getApplicationStatus(input)).thenReturn(expected);

    // Act
    ResponseEntity<ResponseObject> actual = scheduleCtrl.getApplicationStatus(input);

    // Assert
    assertThat(actual).isSameAs(expected);
    verify(scheduleService).getApplicationStatus(input);
    verifyNoMoreInteractions(scheduleService);
  }

  @Test
  void getScheduleById_serviceThrowsException_propagatesException() {
    // Test Case ID: TC_SCHEDULE_CTRL_007
    // Scenario ID: SC_SCHEDULE_CTRL_SAD_001
    // Objective: Verify controller does not swallow service exception for getScheduleById.
    // Covered Branch/Path: Service throws RuntimeException -> exception propagates to caller.
    // DB Check: No direct DB access in controller; service delegation only.
    // Rollback: Not applicable.

    // Arrange
    when(scheduleService.getScheduleById(500L)).thenThrow(new RuntimeException("service error"));

    // Act
    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> scheduleCtrl.getScheduleById(500L));

    // Assert
    assertThat(ex.getMessage()).isEqualTo("service error");
    verify(scheduleService).getScheduleById(500L);
    verifyNoMoreInteractions(scheduleService);
  }
}
