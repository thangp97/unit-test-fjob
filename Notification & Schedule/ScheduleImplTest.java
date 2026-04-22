package fjob.notification.schedule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fjob.common.entity.Schedule;
import fjob.common.entity.enums.ScheduleStatus;
import fjob.notification.repository.ScheduleRepository;
import fjob.notification.service.ScheduleImpl;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test Cases for ScheduleImpl Service
 * Type: CheckDB (@SpringBootTest with @Transactional)
 * Test Case IDs: TC_SCH_001 → TC_SCH_034
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("ScheduleImpl Integration Tests")
class ScheduleImplTest {

    @Autowired
    private ScheduleImpl scheduleService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.of(2024, 5, 1, 10, 0);
        scheduleRepository.deleteAll();
    }

    // ==================== saveSchedule Tests ====================

    @Test
    @DisplayName("TC_SCH_001: Create new schedule with defaults")
    void testSaveSchedule_CreateNewSchedule() {
        // Arrange
        Schedule schedule = Schedule.builder()
                .interviewerId(1L)
                .candidateId(2L)
                .startTime(baseTime)
                .endTime(baseTime.plusHours(1))
                .jobDefaultId(5L)
                .build();

        // Act
        Schedule saved = scheduleService.saveSchedule(schedule);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(ScheduleStatus.PENDING, saved.getStatus());
        assertNotNull(saved.getCreatedDate());
        assertTrue(scheduleRepository.existsById(saved.getId()));
    }

    @Test
    @DisplayName("TC_SCH_002: Update existing schedule")
    void testSaveSchedule_UpdateExistingSchedule() {
        // Arrange
        Schedule original = Schedule.builder()
                .interviewerId(1L)
                .candidateId(2L)
                .startTime(baseTime)
                .endTime(baseTime.plusHours(1))
                .jobDefaultId(5L)
                .build();
        Schedule saved = scheduleRepository.save(original);

        // Act
        saved.setNote("Updated note");
        saved.setStartTime(baseTime.plusDays(1));
        Schedule updated = scheduleService.saveSchedule(saved);

        // Assert
        assertEquals("Updated note", updated.getNote());
        assertEquals(baseTime.plusDays(1), updated.getStartTime());
        assertNotNull(updated.getModifiedDate());
    }

    @Test
    @DisplayName("TC_SCH_003: Create schedule with all fields")
    void testSaveSchedule_CreateWithAllFields() {
        // Arrange
        Schedule schedule = Schedule.builder()
                .interviewerId(1L)
                .candidateId(2L)
                .startTime(baseTime)
                .endTime(baseTime.plusHours(1))
                .status(ScheduleStatus.APPROVED)
                .note("Interview note")
                .jobDefaultId(5L)
                .build();

        // Act
        Schedule saved = scheduleService.saveSchedule(schedule);

        // Assert
        assertEquals(ScheduleStatus.APPROVED, saved.getStatus());
        assertEquals("Interview note", saved.getNote());
        assertNotNull(saved.getId());
    }

    @Test
    @DisplayName("TC_SCH_004: Null interviewerId validation")
    void testSaveSchedule_NullInterviewerId() {
        // Arrange
        Schedule schedule = Schedule.builder()
                .interviewerId(null)
                .candidateId(2L)
                .startTime(baseTime)
                .endTime(baseTime.plusHours(1))
                .jobDefaultId(5L)
                .build();

        // Act & Assert
        assertThrows(Exception.class, () -> scheduleService.saveSchedule(schedule));
    }

    @Test
    @DisplayName("TC_SCH_005: Null candidateId validation")
    void testSaveSchedule_NullCandidateId() {
        // Arrange
        Schedule schedule = Schedule.builder()
                .interviewerId(1L)
                .candidateId(null)
                .startTime(baseTime)
                .endTime(baseTime.plusHours(1))
                .jobDefaultId(5L)
                .build();

        // Act & Assert
        assertThrows(Exception.class, () -> scheduleService.saveSchedule(schedule));
    }

    @Test
    @DisplayName("TC_SCH_006: Start time > end time validation")
    void testSaveSchedule_InvalidTimeRange() {
        // Arrange
        Schedule schedule = Schedule.builder()
                .interviewerId(1L)
                .candidateId(2L)
                .startTime(baseTime.plusHours(2))
                .endTime(baseTime)
                .jobDefaultId(5L)
                .build();

        // Act & Assert
        assertThrows(Exception.class, () -> scheduleService.saveSchedule(schedule));
    }

    @Test
    @DisplayName("TC_SCH_007: Overlapping schedule detection")
    void testSaveSchedule_OverlapDetection() {
        // Arrange - Create first schedule
        Schedule first = Schedule.builder()
                .interviewerId(1L)
                .candidateId(2L)
                .startTime(baseTime)
                .endTime(baseTime.plusHours(1))
                .jobDefaultId(5L)
                .status(ScheduleStatus.APPROVED)
                .build();
        scheduleRepository.save(first);

        // Try to create overlapping schedule
        Schedule overlap = Schedule.builder()
                .interviewerId(1L)
                .candidateId(2L)
                .startTime(baseTime.plusMinutes(30))
                .endTime(baseTime.plusHours(1).plusMinutes(30))
                .jobDefaultId(5L)
                .build();

        // Act & Assert
        assertThrows(Exception.class, () -> scheduleService.saveSchedule(overlap));
    }

    @Test
    @DisplayName("TC_SCH_008: Status transition PENDING to APPROVED")
    void testSaveSchedule_StatusTransition() {
        // Arrange
        Schedule schedule = Schedule.builder()
                .interviewerId(1L)
                .candidateId(2L)
                .startTime(baseTime)
                .endTime(baseTime.plusHours(1))
                .jobDefaultId(5L)
                .status(ScheduleStatus.PENDING)
                .build();
        Schedule saved = scheduleRepository.save(schedule);

        // Act
        saved.setStatus(ScheduleStatus.APPROVED);
        Schedule updated = scheduleService.saveSchedule(saved);

        // Assert
        assertEquals(ScheduleStatus.APPROVED, updated.getStatus());
    }

    @Test
    @DisplayName("TC_SCH_009: Save with empty note")
    void testSaveSchedule_EmptyNote() {
        // Arrange
        Schedule schedule = Schedule.builder()
                .interviewerId(1L)
                .candidateId(2L)
                .startTime(baseTime)
                .endTime(baseTime.plusHours(1))
                .jobDefaultId(5L)
                .note("")
                .build();

        // Act
        Schedule saved = scheduleService.saveSchedule(schedule);

        // Assert
        assertEquals("", saved.getNote());
    }

    // ==================== getScheduleById Tests ====================

    @Test
    @DisplayName("TC_SCH_010: Get schedule by ID - Happy path")
    void testGetScheduleById_Found() {
        // Arrange
        Schedule schedule = Schedule.builder()
                .interviewerId(1L)
                .candidateId(2L)
                .startTime(baseTime)
                .endTime(baseTime.plusHours(1))
                .jobDefaultId(5L)
                .build();
        Schedule saved = scheduleRepository.save(schedule);

        // Act
        Schedule result = scheduleService.getScheduleById(saved.getId());

        // Assert
        assertNotNull(result);
        assertEquals(saved.getId(), result.getId());
    }

    @Test
    @DisplayName("TC_SCH_011: Get schedule by ID - Not found")
    void testGetScheduleById_NotFound() {
        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> scheduleService.getScheduleById(999L));
    }

    @Test
    @DisplayName("TC_SCH_012: Get schedule - Field mapping verification")
    void testGetScheduleById_FieldMapping() {
        // Arrange
        Schedule schedule = Schedule.builder()
                .interviewerId(1L)
                .candidateId(2L)
                .startTime(baseTime)
                .endTime(baseTime.plusHours(1))
                .jobDefaultId(5L)
                .status(ScheduleStatus.APPROVED)
                .note("Test note")
                .build();
        Schedule saved = scheduleRepository.save(schedule);

        // Act
        Schedule result = scheduleService.getScheduleById(saved.getId());

        // Assert
        assertEquals("Test note", result.getNote());
        assertEquals(ScheduleStatus.APPROVED, result.getStatus());
        assertEquals(1L, result.getInterviewerId());
    }

    // ==================== getScheduleByStatus Tests ====================

    @Test
    @DisplayName("TC_SCH_013: Get PENDING schedules - Page 0")
    void testGetScheduleByStatus_FirstPage() {
        // Arrange
        for (int i = 0; i < 15; i++) {
            scheduleRepository.save(Schedule.builder()
                    .interviewerId((long) i)
                    .candidateId((long) i + 100)
                    .startTime(baseTime.plusDays(i))
                    .endTime(baseTime.plusDays(i).plusHours(1))
                    .jobDefaultId(5L)
                    .status(ScheduleStatus.PENDING)
                    .build());
        }

        // Act
        var result = scheduleService.getScheduleByStatus(ScheduleStatus.PENDING, 0, 10);

        // Assert
        assertEquals(10, result.getContent().size());
        assertTrue(result.hasNext());
    }

    @Test
    @DisplayName("TC_SCH_014: Get schedules - Last page")
    void testGetScheduleByStatus_LastPage() {
        // Arrange
        for (int i = 0; i < 15; i++) {
            scheduleRepository.save(Schedule.builder()
                    .interviewerId((long) i)
                    .candidateId((long) i + 100)
                    .startTime(baseTime.plusDays(i))
                    .endTime(baseTime.plusDays(i).plusHours(1))
                    .jobDefaultId(5L)
                    .status(ScheduleStatus.APPROVED)
                    .build());
        }

        // Act
        var result = scheduleService.getScheduleByStatus(ScheduleStatus.APPROVED, 1, 10);

        // Assert
        assertEquals(5, result.getContent().size());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("TC_SCH_015: Invalid status enum")
    void testGetScheduleByStatus_InvalidEnum() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> scheduleService.getScheduleByStatus(null, 0, 10));
    }

    @Test
    @DisplayName("TC_SCH_016: No schedules with status")
    void testGetScheduleByStatus_EmptyResult() {
        // Act
        var result = scheduleService.getScheduleByStatus(ScheduleStatus.CANCELLED, 0, 10);

        // Assert
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("TC_SCH_017: Case sensitivity in status")
    void testGetScheduleByStatus_CaseSensitivity() {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            // Attempting to pass lowercase string to enum
            ScheduleStatus.valueOf("pending");
        });
    }

    // ==================== getCalendar Tests ====================

    @Test
    @DisplayName("TC_SCH_018: Get calendar for recruiter")
    void testGetCalendar_RecruiterView() {
        // Arrange
        scheduleRepository.save(Schedule.builder()
                .interviewerId(1L)
                .candidateId(2L)
                .startTime(baseTime)
                .endTime(baseTime.plusHours(1))
                .jobDefaultId(5L)
                .build());

        // Act
        List<Schedule> result = scheduleService.getCalendar(1L, null, baseTime, baseTime.plusDays(1));

        // Assert
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getInterviewerId());
    }

    @Test
    @DisplayName("TC_SCH_019: Get calendar for candidate")
    void testGetCalendar_CandidateView() {
        // Arrange
        scheduleRepository.save(Schedule.builder()
                .interviewerId(1L)
                .candidateId(2L)
                .startTime(baseTime)
                .endTime(baseTime.plusHours(1))
                .jobDefaultId(5L)
                .build());

        // Act
        List<Schedule> result = scheduleService.getCalendar(null, 2L, baseTime, baseTime.plusDays(1));

        // Assert
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getCandidateId());
    }

    @Test
    @DisplayName("TC_SCH_020: Filter calendar by status")
    void testGetCalendar_StatusFilter() {
        // Arrange
        scheduleRepository.save(Schedule.builder()
                .interviewerId(1L)
                .candidateId(2L)
                .startTime(baseTime)
                .endTime(baseTime.plusHours(1))
                .jobDefaultId(5L)
                .status(ScheduleStatus.APPROVED)
                .build());

        scheduleRepository.save(Schedule.builder()
                .interviewerId(1L)
                .candidateId(3L)
                .startTime(baseTime.plusDays(1))
                .endTime(baseTime.plusDays(1).plusHours(1))
                .jobDefaultId(5L)
                .status(ScheduleStatus.PENDING)
                .build());

        // Act
        List<Schedule> result = scheduleService.getCalendar(1L, null, baseTime, baseTime.plusDays(2), ScheduleStatus.APPROVED);

        // Assert
        assertEquals(1, result.size());
        assertEquals(ScheduleStatus.APPROVED, result.get(0).getStatus());
    }

    @Test
    @DisplayName("TC_SCH_021: Calendar date range no results")
    void testGetCalendar_NoResultsInRange() {
        // Arrange
        scheduleRepository.save(Schedule.builder()
                .interviewerId(1L)
                .candidateId(2L)
                .startTime(baseTime)
                .endTime(baseTime.plusHours(1))
                .jobDefaultId(5L)
                .build());

        // Act
        LocalDateTime futureStart = baseTime.plusDays(30);
        LocalDateTime futureEnd = baseTime.plusDays(31);
        List<Schedule> result = scheduleService.getCalendar(1L, null, futureStart, futureEnd);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("TC_SCH_022: Multiple schedules in date range")
    void testGetCalendar_MultipleResults() {
        // Arrange
        for (int i = 0; i < 3; i++) {
            scheduleRepository.save(Schedule.builder()
                    .interviewerId((long) i)
                    .candidateId((long) i + 100)
                    .startTime(baseTime.plusDays(i))
                    .endTime(baseTime.plusDays(i).plusHours(1))
                    .jobDefaultId(5L)
                    .build());
        }

        // Act
        List<Schedule> result = scheduleService.getCalendar(null, null, baseTime, baseTime.plusDays(3));

        // Assert
        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("TC_SCH_023: Invalid date range validation")
    void testGetCalendar_InvalidDateRange() {
        // Act & Assert
        assertThrows(Exception.class, () -> 
            scheduleService.getCalendar(1L, null, baseTime.plusDays(10), baseTime));
    }

    // ==================== deleteByIds Tests ====================

    @Test
    @DisplayName("TC_SCH_025: Batch delete valid IDs")
    void testDeleteByIds_BatchDelete() {
        // Arrange
        Schedule s1 = scheduleRepository.save(Schedule.builder()
                .interviewerId(1L).candidateId(2L)
                .startTime(baseTime).endTime(baseTime.plusHours(1))
                .jobDefaultId(5L).build());
        Schedule s2 = scheduleRepository.save(Schedule.builder()
                .interviewerId(2L).candidateId(3L)
                .startTime(baseTime).endTime(baseTime.plusHours(1))
                .jobDefaultId(5L).build());

        // Act
        scheduleService.deleteByIds(List.of(s1.getId(), s2.getId()));

        // Assert
        assertFalse(scheduleRepository.existsById(s1.getId()));
        assertFalse(scheduleRepository.existsById(s2.getId()));
    }

    @Test
    @DisplayName("TC_SCH_026: Delete already deleted schedule")
    void testDeleteByIds_Idempotent() {
        // Arrange
        Schedule schedule = scheduleRepository.save(Schedule.builder()
                .interviewerId(1L).candidateId(2L)
                .startTime(baseTime).endTime(baseTime.plusHours(1))
                .jobDefaultId(5L).build());

        // Act
        scheduleService.deleteByIds(List.of(schedule.getId()));
        scheduleService.deleteByIds(List.of(schedule.getId())); // Delete again

        // Assert - No exception thrown
        assertFalse(scheduleRepository.existsById(schedule.getId()));
    }

    @Test
    @DisplayName("TC_SCH_027: Delete empty list")
    void testDeleteByIds_EmptyList() {
        // Arrange
        scheduleRepository.save(Schedule.builder()
                .interviewerId(1L).candidateId(2L)
                .startTime(baseTime).endTime(baseTime.plusHours(1))
                .jobDefaultId(5L).build());
        long initialCount = scheduleRepository.count();

        // Act
        scheduleService.deleteByIds(List.of());

        // Assert
        assertEquals(initialCount, scheduleRepository.count());
    }

    @Test
    @DisplayName("TC_SCH_028: Mixed existing and non-existing IDs")
    void testDeleteByIds_PartialDelete() {
        // Arrange
        Schedule schedule = scheduleRepository.save(Schedule.builder()
                .interviewerId(1L).candidateId(2L)
                .startTime(baseTime).endTime(baseTime.plusHours(1))
                .jobDefaultId(5L).build());

        // Act
        scheduleService.deleteByIds(List.of(schedule.getId(), 999L));

        // Assert
        assertFalse(scheduleRepository.existsById(schedule.getId()));
    }

    @Test
    @DisplayName("TC_SCH_029: Delete with null list")
    void testDeleteByIds_NullList() {
        // Act & Assert
        assertThrows(Exception.class, () -> scheduleService.deleteByIds(null));
    }
}
