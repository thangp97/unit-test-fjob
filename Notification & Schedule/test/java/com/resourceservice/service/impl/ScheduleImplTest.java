package com.resourceservice.service.impl;

import static com.jober.utilsservice.constant.ResponseMessageConstant.CREATED;
import static com.jober.utilsservice.constant.ResponseMessageConstant.NOT_FOUND;
import static com.jober.utilsservice.constant.ResponseMessageConstant.NOT_MODIFIED;
import static com.jober.utilsservice.constant.ResponseMessageConstant.UPDATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jober.utilsservice.utils.modelCustom.Paging;
import com.jober.utilsservice.utils.modelCustom.Response;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import com.resourceservice.dto.request.ScheduleParamDTO;
import com.resourceservice.dto.request.ScheduleRqDTO;
import com.resourceservice.dto.request.StatusRequest;
import com.resourceservice.interceptor.BearerTokenWrapper;
import com.resourceservice.model.Freelancer;
import com.resourceservice.model.Job;
import com.resourceservice.model.Schedule;
import com.resourceservice.model.UserCommon;
import com.resourceservice.repository.FreelancerRepo;
import com.resourceservice.repository.JobRepo;
import com.resourceservice.repository.ScheduleRepo;
import com.resourceservice.repository.UserCommonRepo;
import com.resourceservice.repository.ChatRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = ScheduleImplTest.JpaOnlyTestApplication.class)
@Transactional
@Rollback
@TestPropertySource(properties = {
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.datasource.url=${SCHEDULE_IT_DB_URL:jdbc:postgresql://localhost:5433/jober_test}",
    "spring.datasource.username=${SCHEDULE_IT_DB_USER:admin}",
    "spring.datasource.password=${SCHEDULE_IT_DB_PASSWORD:juile2022}",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQL81Dialect",
    "spring.jpa.properties.hibernate.format_sql=true"
})
class ScheduleImplTest {

  private ScheduleImpl scheduleImpl;
  private long scheduleCountBefore;
  private long jobCountBefore;
  private long freelancerCountBefore;
  private long userCountBefore;

  @Autowired
  private ScheduleRepo scheduleRepo;
  @Autowired
  private FreelancerRepo<Freelancer, Long> freelancerRepo;
  @Autowired
  private JobRepo<Job, Long> jobRepo;
  @Autowired
  private UserCommonRepo userCommonRepo;
  @Autowired
  private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    scheduleCountBefore = scheduleRepo.count();
    jobCountBefore = jobRepo.count();
    freelancerCountBefore = freelancerRepo.count();
    userCountBefore = userCommonRepo.count();

    scheduleImpl = new ScheduleImpl(new BearerTokenWrapper());
    ReflectionTestUtils.setField(scheduleImpl, "scheduleRepo", scheduleRepo);
    ReflectionTestUtils.setField(scheduleImpl, "ScheduleRepo", scheduleRepo);
    ReflectionTestUtils.setField(scheduleImpl, "freelancerRepo", freelancerRepo);
    ReflectionTestUtils.setField(scheduleImpl, "jobRepo", jobRepo);
    ReflectionTestUtils.setField(scheduleImpl, "entityManager", entityManager);
  }

  @AfterTransaction
  void rollbackRestoresDatabaseState() {
    assertThat(scheduleRepo.count()).isEqualTo(scheduleCountBefore);
    assertThat(jobRepo.count()).isEqualTo(jobCountBefore);
    assertThat(freelancerRepo.count()).isEqualTo(freelancerCountBefore);
    assertThat(userCommonRepo.count()).isEqualTo(userCountBefore);
  }

  @Test
  void saveSchedule_createNewSchedule_writesRealDatabaseAndRollsBackAfterTest() {
    // Test Case ID: TC_SCHEDULE_DB_IT_001
    // Scenario ID: SC_SCHEDULE_DB_IT_HAPPY_001
    // Objective: Verify ScheduleImpl.saveSchedule writes to a real PostgreSQL test database.
    // DB Check: Uses real Spring Data repositories and verifies persisted Schedule fields from DB.
    // Rollback: @Transactional + @Rollback rolls back inserted UserCommon/Freelancer/Job/Schedule rows after the test.

    // Arrange
    TestData testData = persistCandidateAndJob();
    LocalDateTime expectedStartDate = LocalDateTime.of(2026, 7, 10, 9, 0);
    LocalDateTime expectedEndDate = LocalDateTime.of(2026, 7, 10, 10, 0);
    ScheduleRqDTO input = new ScheduleRqDTO();
    input.setFreelancerId(testData.freelancer.getId());
    input.setJobId(testData.job.getId());
    input.setStatus("INTERVIEW_SCHEDULED");
    input.setTopic("DB rollback interview");
    input.setDes("Integration test schedule");
    input.setAddress("Test office");
    input.setStartDate(expectedStartDate);
    input.setEndDate(expectedEndDate);
    input.setType(1);
    input.setInterviewMethod(false);
    input.setFeedback("Bring portfolio");

    // Act
    ResponseEntity<?> actual = scheduleImpl.saveSchedule(input);
    entityManager.flush();
    entityManager.clear();

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(actual.getBody()).isInstanceOf(ResponseObject.class);
    assertThat(((ResponseObject) actual.getBody()).getStatus()).isEqualTo(CREATED);
    List<Schedule> schedules =
        scheduleRepo.findByFreelancerAndJob(testData.freelancer.getId(), testData.job.getId());
    assertThat(schedules).hasSize(1);
    Schedule persisted = schedules.get(0);
    assertThat(persisted.getStatus()).isEqualTo("INTERVIEW_SCHEDULED");
    assertThat(persisted.getTopic()).isEqualTo("DB rollback interview");
    assertThat(persisted.getAddress()).isEqualTo("Test office");
    assertThat(persisted.getStartDate()).isEqualTo(expectedStartDate);
    assertThat(persisted.getEndDate()).isEqualTo(expectedEndDate);
    assertThat(scheduleRepo.count()).isEqualTo(scheduleCountBefore + 1);
  }

  @Test
  void saveSchedule_updateByScheduleId_updatesRealDatabaseAndRollsBackAfterTest() {
    // Test Case ID: TC_SCHEDULE_DB_IT_003
    // Scenario ID: SC_SCHEDULE_DB_IT_HAPPY_003
    // Objective: Verify update-by-scheduleId uses a real persisted Schedule and stores changed fields.
    // DB Check: Reads and writes via real PostgreSQL-backed repositories.
    // Rollback: @Transactional + @Rollback restores all inserted/updated rows after the test.

    // Arrange
    TestData testData = persistCandidateAndJob();
    Schedule schedule = persistSchedule(testData, "PENDING", "Initial topic");
    Long scheduleId = schedule.getId();
    ScheduleRqDTO input = new ScheduleRqDTO();
    input.setScheduleId(scheduleId);
    input.setFreelancerId(testData.freelancer.getId());
    input.setJobId(testData.job.getId());
    input.setStatus("INTERVIEW_SCHEDULED");
    input.setTopic("Updated DB interview");
    input.setAddress("Updated office");
    input.setStartDate(LocalDateTime.of(2026, 9, 10, 9, 0));
    input.setEndDate(LocalDateTime.of(2026, 9, 10, 10, 0));

    // Act
    ResponseEntity<?> actual = scheduleImpl.saveSchedule(input);
    entityManager.flush();
    entityManager.clear();

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
    Schedule updated = scheduleRepo.findByScheduleId(scheduleId);
    assertThat(updated).isNotNull();
    assertThat(updated.getStatus()).isEqualTo("INTERVIEW_SCHEDULED");
    assertThat(updated.getTopic()).isEqualTo("Updated DB interview");
    assertThat(updated.getAddress()).isEqualTo("Updated office");
  }

  @Test
  void saveSchedule_existingFreelancerAndJob_updatesExistingRealDatabaseScheduleAndRollsBackAfterTest() {
    // Test Case ID: TC_SCHEDULE_DB_IT_004
    // Scenario ID: SC_SCHEDULE_DB_IT_HAPPY_004
    // Objective: Verify saveSchedule updates an existing schedule for the same freelancer/job pair.
    // DB Check: Uses real findByFreelancerAndJob and verifies no second schedule is created.
    // Rollback: @Transactional + @Rollback restores all inserted/updated rows after the test.

    // Arrange
    TestData testData = persistCandidateAndJob();
    Schedule existing = persistSchedule(testData, "PENDING", "Existing topic");
    ScheduleRqDTO input = new ScheduleRqDTO();
    input.setFreelancerId(testData.freelancer.getId());
    input.setJobId(testData.job.getId());
    input.setStatus("INTERVIEW_SCHEDULED");
    input.setTopic("Existing pair updated");
    input.setAddress("Existing pair room");
    input.setStartDate(LocalDateTime.of(2026, 10, 10, 9, 0));
    input.setEndDate(LocalDateTime.of(2026, 10, 10, 10, 0));

    // Act
    ResponseEntity<?> actual = scheduleImpl.saveSchedule(input);
    entityManager.flush();
    entityManager.clear();

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<Schedule> schedules =
        scheduleRepo.findByFreelancerAndJob(testData.freelancer.getId(), testData.job.getId());
    assertThat(schedules).hasSize(1);
    assertThat(schedules.get(0).getId()).isEqualTo(existing.getId());
    assertThat(schedules.get(0).getTopic()).isEqualTo("Existing pair updated");
  }

  @Test
  void saveSchedule_freelancerNotFound_throwsBeforeRealDatabaseWrite() {
    // Test Case ID: TC_SCHEDULE_DB_IT_005
    // Scenario ID: SC_SCHEDULE_DB_IT_SAD_001
    // Objective: Verify a missing freelancer rejects before inserting a Schedule.
    // DB Check: Uses real repository lookup and verifies schedule count is unchanged.
    // Rollback: @Transactional + @Rollback protects database state even if the test fails.

    // Arrange
    ScheduleRqDTO input = new ScheduleRqDTO();
    input.setFreelancerId(-999999L);
    input.setJobId(-1L);

    // Act
    NoSuchElementException actual =
        assertThrows(NoSuchElementException.class, () -> scheduleImpl.saveSchedule(input));

    // Assert
    assertThat(actual).isNotNull();
    assertThat(scheduleRepo.count()).isEqualTo(scheduleCountBefore);
  }

  @Test
  void saveSchedule_scheduleIdNotFound_throwsBeforeRealDatabaseWrite() {
    // Test Case ID: TC_SCHEDULE_DB_IT_006
    // Scenario ID: SC_SCHEDULE_DB_IT_SAD_002
    // Objective: Verify a missing scheduleId rejects before save.
    // DB Check: Uses real freelancer and repository lookup, then verifies schedule count unchanged.
    // Rollback: @Transactional + @Rollback restores inserted setup rows.

    // Arrange
    TestData testData = persistCandidateAndJob();
    ScheduleRqDTO input = new ScheduleRqDTO();
    input.setScheduleId(-999999L);
    input.setFreelancerId(testData.freelancer.getId());
    input.setJobId(testData.job.getId());

    // Act
    NoSuchElementException actual =
        assertThrows(NoSuchElementException.class, () -> scheduleImpl.saveSchedule(input));

    // Assert
    assertThat(actual).isNotNull();
    assertThat(scheduleRepo.count()).isEqualTo(scheduleCountBefore);
  }

  @Test
  void saveSchedule_missingJobForeignKey_throwsRealDatabaseExceptionAndRollsBackAfterTest() {
    // Test Case ID: TC_SCHEDULE_DB_IT_007
    // Scenario ID: SC_SCHEDULE_DB_IT_SAD_003
    // Objective: Verify DB write failure is raised by real PostgreSQL FK constraint.
    // DB Check: Uses a real freelancer but a missing job id to trigger database constraint failure.
    // Rollback: @Transactional + @Rollback restores inserted setup rows.

    // Arrange
    TestData testData = persistCandidateAndJob();
    ScheduleRqDTO input = new ScheduleRqDTO();
    input.setFreelancerId(testData.freelancer.getId());
    input.setJobId(-999999L);
    input.setStatus("INTERVIEW_SCHEDULED");
    input.setAddress("FK failure room");
    input.setStartDate(LocalDateTime.of(2026, 11, 10, 9, 0));

    // Act + Assert
    assertThrows(RuntimeException.class, () -> {
      scheduleImpl.saveSchedule(input);
      entityManager.flush();
    });
  }

  @Test
  void saveSchedule_missingStartDate_expectedBusinessValidationButCurrentlyWritesRealDatabase() {
    // Test Case ID: TC_SCHEDULE_DB_IT_REQ_GAP_001
    // Scenario ID: SC_SCHEDULE_DB_IT_VALIDATION_GAP_001
    // Objective: Verify required interview date validation from the use case specification.
    // DB Check: This test intentionally expects no DB write; current production writes data instead.
    // Rollback: @Transactional + @Rollback restores inserted rows after this failing requirement test.
    // Assumption: expected business message is derived from use case specification, not copied from current implementation.

    // Arrange
    TestData testData = persistCandidateAndJob();
    String expectedMessage = "Vui lòng nhập đầy đủ Ngày, Giờ và Địa điểm phỏng vấn.";
    ScheduleRqDTO input = new ScheduleRqDTO();
    input.setFreelancerId(testData.freelancer.getId());
    input.setJobId(testData.job.getId());
    input.setAddress("Office with missing date");

    // Act
    ResponseEntity<?> actual = scheduleImpl.saveSchedule(input);
    entityManager.flush();

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(actual.getBody()).isInstanceOf(Response.class);
    assertThat(((Response) actual.getBody()).getMessage()).isEqualTo(expectedMessage);
    assertThat(scheduleRepo.count()).isEqualTo(scheduleCountBefore);
  }

  @Test
  void saveSchedule_missingAddressAndMeetUrl_expectedBusinessValidationButCurrentlyWritesRealDatabase() {
    // Test Case ID: TC_SCHEDULE_DB_IT_REQ_GAP_002
    // Scenario ID: SC_SCHEDULE_DB_IT_VALIDATION_GAP_002
    // Objective: Verify required interview location validation from the use case specification.
    // DB Check: This test intentionally expects no DB write; current production writes data instead.
    // Rollback: @Transactional + @Rollback restores inserted rows after this failing requirement test.
    // Assumption: expected business message is derived from use case specification, not copied from current implementation.

    // Arrange
    TestData testData = persistCandidateAndJob();
    String expectedMessage = "Vui lòng nhập đầy đủ Ngày, Giờ và Địa điểm phỏng vấn.";
    ScheduleRqDTO input = new ScheduleRqDTO();
    input.setFreelancerId(testData.freelancer.getId());
    input.setJobId(testData.job.getId());
    input.setStartDate(LocalDateTime.of(2026, 12, 10, 9, 0));

    // Act
    ResponseEntity<?> actual = scheduleImpl.saveSchedule(input);
    entityManager.flush();

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(actual.getBody()).isInstanceOf(Response.class);
    assertThat(((Response) actual.getBody()).getMessage()).isEqualTo(expectedMessage);
    assertThat(scheduleRepo.count()).isEqualTo(scheduleCountBefore);
  }

  @Test
  void saveSchedule_pastDatetime_expectedBusinessValidationButCurrentlyWritesRealDatabase() {
    // Test Case ID: TC_SCHEDULE_DB_IT_REQ_GAP_003
    // Scenario ID: SC_SCHEDULE_DB_IT_VALIDATION_GAP_003
    // Objective: Verify invalid past interview time validation from the use case specification.
    // DB Check: This test intentionally expects no DB write; current production writes data instead.
    // Rollback: @Transactional + @Rollback restores inserted rows after this failing requirement test.
    // Assumption: expected business message is derived from use case specification, not copied from current implementation.

    // Arrange
    TestData testData = persistCandidateAndJob();
    String expectedMessage = "Thời gian phỏng vấn không hợp lệ. Vui lòng chọn lại.";
    ScheduleRqDTO input = new ScheduleRqDTO();
    input.setFreelancerId(testData.freelancer.getId());
    input.setJobId(testData.job.getId());
    input.setStartDate(LocalDateTime.of(2020, 1, 1, 9, 0));
    input.setAddress("Past interview room");

    // Act
    ResponseEntity<?> actual = scheduleImpl.saveSchedule(input);
    entityManager.flush();

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(actual.getBody()).isInstanceOf(Response.class);
    assertThat(((Response) actual.getBody()).getMessage()).isEqualTo(expectedMessage);
    assertThat(scheduleRepo.count()).isEqualTo(scheduleCountBefore);
  }

  @Test
  void getScheduleById_notFound_queriesRealDatabaseAndReturnsNotFound() {
    // Test Case ID: TC_SCHEDULE_DB_IT_008
    // Scenario ID: SC_SCHEDULE_DB_IT_READ_001
    // Objective: Verify getScheduleById not-found path against the real DB.
    // DB Check: Uses real ScheduleRepo.findByScheduleId for a missing id.
    // Rollback: @Transactional + @Rollback protects database state.

    // Arrange
    Long missingId = -999999L;

    // Act
    ResponseEntity<ResponseObject> actual = scheduleImpl.getScheduleById(missingId);

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(actual.getBody()).isNotNull();
    assertThat(actual.getBody().getStatus()).isEqualTo(NOT_FOUND);
    assertThat(actual.getBody().getData()).isNull();
  }

  @Test
  void getScheduleByStatus_validPaging_queriesRealDatabaseAndReturnsPage() {
    // Test Case ID: TC_SCHEDULE_DB_IT_009
    // Scenario ID: SC_SCHEDULE_DB_IT_READ_002
    // Objective: Verify getScheduleByStatus reads matching rows from the real DB with paging.
    // DB Check: Persists a schedule and reads it through real ScheduleRepo.getScheduleByStatus.
    // Rollback: @Transactional + @Rollback restores inserted rows.

    // Arrange
    TestData testData = persistCandidateAndJob();
    persistSchedule(testData, "INTERVIEW_SCHEDULED", "Status paging schedule");
    ScheduleParamDTO input = new ScheduleParamDTO();
    input.setStatus("INTERVIEW_SCHEDULED");
    input.setPaging(new Paging(1, 10));

    // Act
    ResponseEntity<ResponseObject> actual = scheduleImpl.getScheduleByStatus(input);

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(actual.getBody()).isNotNull();
    assertThat(actual.getBody().getData()).isNotNull();
  }

  @Test
  void deleteByIds_noRowsUpdated_queriesRealDatabaseAndReturnsNotModified() {
    // Test Case ID: TC_SCHEDULE_DB_IT_010
    // Scenario ID: SC_SCHEDULE_DB_IT_SAD_004
    // Objective: Verify deleteByIds returns NOT_MODIFIED when no real rows match.
    // DB Check: Calls real ScheduleRepo.updateByIds with a missing id.
    // Rollback: @Transactional + @Rollback protects database state.

    // Arrange
    List<Long> ids = List.of(-999999L);

    // Act
    ResponseEntity<Response> actual = scheduleImpl.deleteByIds(ids);

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(actual.getBody()).isNotNull();
    assertThat(actual.getBody().getStatus()).isEqualTo(NOT_MODIFIED);
    assertThat(scheduleRepo.count()).isEqualTo(scheduleCountBefore);
  }

  @Test
  void getApplicationStatus_hasMatchingSchedules_queriesRealDatabaseAndReturnsFound() {
    // Test Case ID: TC_SCHEDULE_DB_IT_011
    // Scenario ID: SC_SCHEDULE_DB_IT_READ_003
    // Objective: Verify getApplicationStatus aggregates rows from the real DB.
    // DB Check: Persists schedules and queries real ScheduleRepo.aminGetScheduleByStatus.
    // Rollback: @Transactional + @Rollback restores inserted rows.

    // Arrange
    TestData testData = persistCandidateAndJob();
    persistSchedule(testData, "INTERVIEW_SCHEDULED", "Application status schedule");
    StatusRequest input = new StatusRequest(List.of("INTERVIEW_SCHEDULED", "UNMATCHED_STATUS"));

    // Act
    ResponseEntity<ResponseObject> actual = scheduleImpl.getApplicationStatus(input);

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(actual.getBody()).isNotNull();
    assertThat(actual.getBody().getStatus()).isEqualTo("FOUND");
    assertThat((List<?>) actual.getBody().getData()).isNotEmpty();
  }

  @Test
  void getApplicationStatus_noMatchingSchedules_queriesRealDatabaseAndReturnsNotFound() {
    // Test Case ID: TC_SCHEDULE_DB_IT_012
    // Scenario ID: SC_SCHEDULE_DB_IT_READ_004
    // Objective: Verify getApplicationStatus returns NOT_FOUND when no real rows match.
    // DB Check: Queries real ScheduleRepo.aminGetScheduleByStatus.
    // Rollback: @Transactional + @Rollback protects database state.

    // Arrange
    StatusRequest input = new StatusRequest(List.of("STATUS_THAT_SHOULD_NOT_EXIST_999999"));

    // Act
    ResponseEntity<ResponseObject> actual = scheduleImpl.getApplicationStatus(input);

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(actual.getBody()).isNotNull();
    assertThat(actual.getBody().getStatus()).isEqualTo(NOT_FOUND);
    assertThat((List<?>) actual.getBody().getData()).isEmpty();
  }

  @Test
  void getCalendarById_anyId_returnsNullWithoutDatabaseWrite() {
    // Test Case ID: TC_SCHEDULE_DB_IT_013
    // Scenario ID: SC_SCHEDULE_DB_IT_EDGE_001
    // Objective: Verify current placeholder behavior remains null.
    // DB Check: No write occurs; schedule count remains unchanged.
    // Rollback: @Transactional + @Rollback protects database state.

    // Arrange
    Long id = -1L;

    // Act
    ResponseEntity<?> actual = scheduleImpl.getCalendarById(id);

    // Assert
    assertThat(actual).isNull();
    assertThat(scheduleRepo.count()).isEqualTo(scheduleCountBefore);
  }

  @Test
  void deleteByIds_updatesRealDatabaseAndRollsBackAfterTest() {
    // Test Case ID: TC_SCHEDULE_DB_IT_002
    // Scenario ID: SC_SCHEDULE_DB_IT_HAPPY_002
    // Objective: Verify ScheduleImpl.deleteByIds updates active flag in a real PostgreSQL test database.
    // DB Check: Uses real ScheduleRepo.updateByIds and verifies @Where-filtered lookup no longer returns the row.
    // Rollback: @Transactional + @Rollback restores active flag and all inserted rows after the test.

    // Arrange
    TestData testData = persistCandidateAndJob();
    Schedule schedule = new Schedule();
    schedule.setFreelancer(testData.freelancer);
    schedule.setJob(testData.job);
    schedule.setStatus("INTERVIEW_SCHEDULED");
    schedule.setTopic("Delete rollback interview");
    schedule.setStartDate(LocalDateTime.of(2026, 8, 10, 9, 0));
    schedule.setEndDate(LocalDateTime.of(2026, 8, 10, 10, 0));
    schedule.setActive(1);
    schedule.setCreationDate(LocalDateTime.now());
    schedule.setUpdateDate(LocalDateTime.now());
    schedule = scheduleRepo.saveAndFlush(schedule);
    Long scheduleId = schedule.getId();

    // Act
    ResponseEntity<Response> actual = scheduleImpl.deleteByIds(List.of(scheduleId));
    entityManager.flush();
    entityManager.clear();

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(actual.getBody()).isNotNull();
    assertThat(actual.getBody().getStatus()).isEqualTo(UPDATED);
    assertThat(scheduleRepo.findByScheduleId(scheduleId)).isNull();
  }

  private Schedule persistSchedule(TestData testData, String status, String topic) {
    Schedule schedule = new Schedule();
    schedule.setFreelancer(testData.freelancer);
    schedule.setJob(testData.job);
    schedule.setStatus(status);
    schedule.setTopic(topic);
    schedule.setAddress("Persisted schedule room");
    schedule.setStartDate(LocalDateTime.of(2026, 8, 10, 9, 0));
    schedule.setEndDate(LocalDateTime.of(2026, 8, 10, 10, 0));
    schedule.setActive(1);
    schedule.setCreationDate(LocalDateTime.now());
    schedule.setUpdateDate(LocalDateTime.now());
    return scheduleRepo.saveAndFlush(schedule);
  }

  private TestData persistCandidateAndJob() {
    UserCommon recruiter = new UserCommon();
    recruiter.setName("IT Recruiter");
    recruiter.setEmail("it-recruiter@example.com");
    recruiter.setPhone("0900000001");
    recruiter.setRole(2);
    recruiter.setActive(1);
    recruiter.setCreationDate(LocalDateTime.now());
    recruiter = userCommonRepo.saveAndFlush(recruiter);

    UserCommon candidateUser = new UserCommon();
    candidateUser.setName("IT Candidate");
    candidateUser.setEmail("it-candidate@example.com");
    candidateUser.setPhone("0900000002");
    candidateUser.setRole(1);
    candidateUser.setActive(1);
    candidateUser.setCreationDate(LocalDateTime.now());
    candidateUser = userCommonRepo.saveAndFlush(candidateUser);

    Freelancer freelancer = new Freelancer();
    freelancer.setUserCommon(candidateUser);
    freelancer.setName("IT Candidate");
    freelancer.setEmail("it-candidate@example.com");
    freelancer.setPhone("0900000002");
    freelancer.setJob("Java Engineer");
    freelancer.setActive(1);
    freelancer.setStatus(1);
    freelancer.setCreationDate(LocalDateTime.now());
    freelancer = freelancerRepo.saveAndFlush(freelancer);

    Job job = new Job();
    job.setUserCommon(recruiter);
    job.setName("Java Engineer");
    job.setJob("Java Engineer");
    job.setEmail("it-recruiter@example.com");
    job.setPhone("0900000001");
    job.setLat(10.0);
    job.setLng(106.0);
    job.setActive(1);
    job.setExpDate(LocalDateTime.of(2026, 12, 31, 23, 59));
    job.setCreationDate(LocalDateTime.now());
    job = jobRepo.saveAndFlush(job);

    return new TestData(freelancer, job);
  }

  private static class TestData {

    private final Freelancer freelancer;
    private final Job job;

    private TestData(Freelancer freelancer, Job job) {
      this.freelancer = freelancer;
      this.job = job;
    }
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @EntityScan(basePackages = "com.resourceservice.model")
  @EnableJpaRepositories(
      basePackages = "com.resourceservice.repository",
      excludeFilters = @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          classes = ChatRepository.class))
  static class JpaOnlyTestApplication {
  }
}
