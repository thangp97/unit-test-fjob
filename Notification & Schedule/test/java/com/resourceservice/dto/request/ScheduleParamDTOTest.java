package com.resourceservice.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ScheduleParamDTOTest {

  @Test
  void getCurrentWeekStartDate_returnsMondayAtStartOfDay() {
    // Test Case ID: TC_SCHEDULE_PARAM_DTO_001
    // Scenario ID: SC_SCHEDULE_PARAM_DTO_HAPPY_001
    // Objective: Verify week-start helper returns Monday 00:00:00.000000000.
    // Covered Branch/Path: Static helper path for computing current-week start boundary.
    // DB Check: No DB interaction (pure date-time computation).
    // Rollback: Not applicable.

    // Arrange
    DayOfWeek expectedDay = DayOfWeek.MONDAY;

    // Act
    LocalDateTime actual = ScheduleParamDTO.getCurrentWeekStartDate();

    // Assert
    assertThat(actual.getDayOfWeek()).isEqualTo(expectedDay);
    assertThat(actual.getHour()).isEqualTo(0);
    assertThat(actual.getMinute()).isEqualTo(0);
    assertThat(actual.getSecond()).isEqualTo(0);
    assertThat(actual.getNano()).isEqualTo(0);
  }

  @Test
  void getCurrentWeekEndDate_returnsSundayAtEndOfDay() {
    // Test Case ID: TC_SCHEDULE_PARAM_DTO_002
    // Scenario ID: SC_SCHEDULE_PARAM_DTO_HAPPY_002
    // Objective: Verify week-end helper returns Sunday 23:59:59.999999999.
    // Covered Branch/Path: Static helper path for computing current-week end boundary.
    // DB Check: No DB interaction (pure date-time computation).
    // Rollback: Not applicable.

    // Arrange
    DayOfWeek expectedDay = DayOfWeek.SUNDAY;

    // Act
    LocalDateTime actual = ScheduleParamDTO.getCurrentWeekEndDate();

    // Assert
    assertThat(actual.getDayOfWeek()).isEqualTo(expectedDay);
    assertThat(actual.getHour()).isEqualTo(23);
    assertThat(actual.getMinute()).isEqualTo(59);
    assertThat(actual.getSecond()).isEqualTo(59);
    assertThat(actual.getNano()).isEqualTo(999999999);
  }

  @Test
  void constructor_defaultDateRange_isValidWeekRange() {
    // Test Case ID: TC_SCHEDULE_PARAM_DTO_003
    // Scenario ID: SC_SCHEDULE_PARAM_DTO_EDGE_001
    // Objective: Verify default startDate/endDate initialized by DTO form a valid current-week range.
    // Covered Branch/Path: Field initialization path invoking both static helper methods.
    // DB Check: No DB interaction (pure date-time computation).
    // Rollback: Not applicable.

    // Arrange
    ScheduleParamDTO input = new ScheduleParamDTO();

    // Act
    LocalDateTime startDate = input.getStartDate();
    LocalDateTime endDate = input.getEndDate();

    // Assert
    assertThat(startDate).isNotNull();
    assertThat(endDate).isNotNull();
    assertThat(startDate.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    assertThat(endDate.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
    assertThat(startDate).isBeforeOrEqualTo(endDate);
  }
}

