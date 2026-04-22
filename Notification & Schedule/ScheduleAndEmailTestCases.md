# Test Cases: Schedule & Email Service (Notification & Schedule)

## Service 1: ScheduleImpl (@DataJpaTest / @SpringBootTest - CheckDB Type)

### Method 1: saveSchedule()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_SCH_001 | saveSchedule | Create new schedule | Verify schedule is created with default values | `Schedule(interviewerId=1L, candidateId=2L, startTime=2024-05-01 10:00, endTime=2024-05-01 11:00, jobDefaultId=5L, status=null, note=null)` | Schedule saved with id, status=PENDING, createdDate set | CheckDB | Pending | Happy path |
| TC_SCH_002 | saveSchedule | Update existing schedule | Verify schedule fields are updated | `Schedule(id=1L, startTime=2024-05-02 14:00, endTime=2024-05-02 15:00, note="Updated note")` | schedule.note="Updated note", schedule.modifiedDate updated | CheckDB | Pending | Update operation |
| TC_SCH_003 | saveSchedule | Create schedule with all fields | Verify all optional fields are saved | `Schedule(interviewerId=1L, candidateId=2L, startTime=2024-05-01 10:00, endTime=2024-05-01 11:00, status=APPROVED, note="Interview note", jobDefaultId=5L)` | All fields persisted correctly | CheckDB | Pending | Complete object |
| TC_SCH_004 | saveSchedule | Null interviewerId | Verify null handling for recruiter | `Schedule(interviewerId=null, candidateId=2L, startTime=..., endTime=..., jobDefaultId=5L)` | EntityNotFoundException or constraint violation | CheckDB | Pending | Boundary - Null |
| TC_SCH_005 | saveSchedule | Null candidateId | Verify null handling for candidate | `Schedule(interviewerId=1L, candidateId=null, startTime=..., endTime=..., jobDefaultId=5L)` | EntityNotFoundException or constraint violation | CheckDB | Pending | Boundary - Null |
| TC_SCH_006 | saveSchedule | startTime > endTime | Verify time validation | `Schedule(startTime=2024-05-01 15:00, endTime=2024-05-01 10:00)` | throws InvalidScheduleException or ConstraintViolationException | CheckDB | Pending | Validation error |
| TC_SCH_007 | saveSchedule | Overlapping schedule | Verify overlap detection | DB has Schedule(interviewerId=1, candidateId=2, startTime=10:00-11:00); Try to save Schedule(interviewerId=1, candidateId=2, startTime=10:30-11:30) | throws DuplicateScheduleException | CheckDB | Pending | Business logic validation |
| TC_SCH_008 | saveSchedule | Schedule status transition | Verify status update PENDING→APPROVED | `Schedule(id=3L, status=APPROVED, interviewerId=1L, candidateId=2L)` | schedule.status=APPROVED, schedule.modifiedDate updated | CheckDB | Pending | Status change |
| TC_SCH_009 | saveSchedule | Save with empty note | Verify empty string handling | `Schedule(..., note="", ...)` | note="" persisted (not null) | CheckDB | Pending | Edge case |

---

### Method 2: getScheduleById()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_SCH_010 | getScheduleById | Schedule exists | Verify schedule is retrieved with correct fields | `Long id=1L` | Returns Schedule with id=1L, all fields populated | CheckDB | Pending | Happy path |
| TC_SCH_011 | getScheduleById | Schedule not found | Verify exception when not found | `Long id=999L` | throws EntityNotFoundException("Schedule not found") | CheckDB | Pending | Exception handling |
| TC_SCH_012 | getScheduleById | Verify field mapping | Verify all fields are correctly mapped from DB | `Long id=1L, DB record has note="Test note"` | result.note="Test note", result.status preserved | CheckDB | Pending | Data integrity |

---

### Method 3: getScheduleByStatus()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_SCH_013 | getScheduleByStatus | Get PENDING schedules, page 0 | Verify pagination - first page | `status=PENDING, page=0, size=10` | Page with ≤10 items, hasNext correct | CheckDB | Pending | Pagination - first |
| TC_SCH_014 | getScheduleByStatus | Get APPROVED schedules, last page | Verify pagination - last page | `status=APPROVED, page=2 (last)` | Returns remaining items, hasNext=false | CheckDB | Pending | Pagination - last |
| TC_SCH_015 | getScheduleByStatus | Invalid status enum | Verify enum validation | `status="INVALID_STATUS"` | throws IllegalArgumentException | CheckDB | Pending | Enum validation |
| TC_SCH_016 | getScheduleByStatus | No schedules with status | Verify empty result | `status=CANCELLED, DB has no CANCELLED records` | Returns empty Page(content=[], totalElements=0, hasNext=false) | CheckDB | Pending | Edge case - empty |
| TC_SCH_017 | getScheduleByStatus | Case sensitivity | Verify enum case sensitivity | `status="pending" (lowercase)` | throws IllegalArgumentException | CheckDB | Pending | Boundary - case |

---

### Method 4: getCalendar()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_SCH_018 | getCalendar | Recruiter view - filter by recruiter | Verify recruiter can see their schedules | `recruiterId=1L, startDate=2024-05-01, endDate=2024-05-31` | Returns List<Schedule> where recruiterId=1, dates in range | CheckDB | Pending | Recruiter view |
| TC_SCH_019 | getCalendar | Candidate view - filter by candidate | Verify candidate can see their schedules | `candidateId=2L, startDate=2024-05-01, endDate=2024-05-31` | Returns List<Schedule> where candidateId=2, dates in range | CheckDB | Pending | Candidate view |
| TC_SCH_020 | getCalendar | Filter by status | Verify status filtering | `recruiterId=1L, status=APPROVED, startDate=..., endDate=...` | Returns only APPROVED schedules | CheckDB | Pending | Status filter |
| TC_SCH_021 | getCalendar | Date range no results | Verify empty list on no match | `startDate=2025-01-01, endDate=2025-02-01, DB has no schedules in this period` | Returns empty List | CheckDB | Pending | Edge case - empty |
| TC_SCH_022 | getCalendar | Overlapping schedules in range | Verify multiple results | `DB has 3 schedules in range, all different recruiters` | Returns List size=3, sorted by startTime | CheckDB | Pending | Multiple results |
| TC_SCH_023 | getCalendar | startDate > endDate | Verify date validation | `startDate=2024-05-31, endDate=2024-05-01` | throws InvalidDateRangeException or swaps dates automatically | CheckDB | Pending | Validation |

---

### Method 5: getCalendarById()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_SCH_024 | getCalendarById | Placeholder method | Verify method returns expected structure | Any valid Long id | Returns Calendar or throws UnsupportedOperationException | CheckDB | Pending | Stub/TODO method |

---

### Method 6: deleteByIds()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_SCH_025 | deleteByIds | Batch delete valid IDs | Verify multiple schedules deleted | `ids=[1L, 2L, 3L], all exist in DB` | All 3 records removed, findById returns empty Optional | CheckDB | Pending | Happy path - batch |
| TC_SCH_026 | deleteByIds | Delete already deleted | Verify idempotent delete | `ids=[1L], schedule 1L already deleted` | No exception, count unchanged | CheckDB | Pending | Idempotency |
| TC_SCH_027 | deleteByIds | Empty list | Verify empty list handling | `ids=[]` | No records deleted, DB unchanged | CheckDB | Pending | Edge case - empty |
| TC_SCH_028 | deleteByIds | Mixed existing/non-existing IDs | Verify partial delete | `ids=[1L, 999L], only 1L exists` | Deletes 1L only, no exception | CheckDB | Pending | Partial delete |
| TC_SCH_029 | deleteByIds | Null list | Verify null handling | `ids=null` | throws NullPointerException or IllegalArgumentException | CheckDB | Pending | Null validation |

---

### Method 7: getApplicationStatus()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_SCH_030 | getApplicationStatus | Get application status by schedule | Verify status retrieval | `scheduleId=1L, status exists` | Returns ApplicationStatus enum (PENDING/APPROVED/REJECTED) | CheckDB | Pending | Happy path |
| TC_SCH_031 | getApplicationStatus | Schedule not found | Verify exception on missing schedule | `scheduleId=999L` | throws EntityNotFoundException | CheckDB | Pending | Exception |
| TC_SCH_032 | getApplicationStatus | Filter by multiple status | Verify multi-status filtering | `statusList=[PENDING, APPROVED]` | Returns schedules with either status | CheckDB | Pending | Multi-filter |
| TC_SCH_033 | getApplicationStatus | Empty result set | Verify empty list | `scheduleId search with no matches` | Returns empty List | CheckDB | Pending | Edge case |
| TC_SCH_034 | getApplicationStatus | Case-insensitive search | Verify case handling | `status="pending" (lowercase)` | Returns PENDING enum match | CheckDB | Pending | Case sensitivity |

---

## Service 2: EmailServiceImpl (@ExtendWith(MockitoExtension.class) - Mock Type)

### Method 1: send()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_EMAIL_001 | send | Send plain text email | Verify plain text email sent | `to="test@example.com", subject="Test", body="Hello"` | verify(sendGrid, times(1)).send(MimeMessage), status=202 | Mock | Pending | Happy path |
| TC_EMAIL_002 | send | Send HTML email | Verify HTML content preserved | `to="test@example.com", subject="Test", body="<h1>Title</h1>", isHtml=true` | MimeMessage content-type=text/html | Mock | Pending | HTML support |
| TC_EMAIL_003 | send | Multiple recipients | Verify CC/BCC handling | `to="test1@example.com", cc="test2@example.com", bcc="test3@example.com"` | All recipients in MimeMessage headers | Mock | Pending | Multi-recipient |
| TC_EMAIL_004 | send | SendGrid API error | Verify error handling on SendGrid failure | `when(sendGrid.send()).thenThrow(SendGridException)` | throws EmailSendException, logs error | Mock | Pending | Exception handling |
| TC_EMAIL_005 | send | UTF-8 encoding | Verify Vietnamese character encoding | `body="Xin chào từ Việt Nam"` | MimeMessage charset=UTF-8, characters preserved | Mock | Pending | Encoding - Vietnamese |
| TC_EMAIL_006 | send | Empty subject | Verify subject handling | `subject=""` | Email sent with empty subject or default subject | Mock | Pending | Edge case |
| TC_EMAIL_007 | send | Very long subject | Verify subject truncation | `subject="A"*500` | Email sent with subject truncated to 255 chars or throws exception | Mock | Pending | Boundary - length |
| TC_EMAIL_008 | send | Null recipient | Verify null validation | `to=null` | throws IllegalArgumentException("Recipient required") | Mock | Pending | Validation |
| TC_EMAIL_009 | send | Invalid email format | Verify email format validation | `to="notanemail"` | throws InvalidEmailException | Mock | Pending | Format validation |
| TC_EMAIL_010 | send | Email with attachment | Verify attachment handling | `body="Test", attachments=[file1.pdf]` | verify(MimeMessage.addPart(attachment)) | Mock | Pending | Attachment support |
| TC_EMAIL_011 | send | Retry logic on failure | Verify retry mechanism | `SendGrid fails 2 times, succeeds on 3rd` | verify(sendGrid.send(), times(3)) | Mock | Pending | Retry logic |
| TC_EMAIL_012 | send | Timeout handling | Verify timeout exception | `when(sendGrid.send()).thenThrow(TimeoutException)` | throws EmailTimeoutException | Mock | Pending | Timeout |
| TC_EMAIL_013 | send | Template email | Verify template variable substitution | `template="Hello {name}", variables={"name":"John"}` | Email body contains "Hello John" | Mock | Pending | Template processing |

---

### Method 2: checkAndSendEmailForCandidate()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_EMAIL_014 | checkAndSendEmailForCandidate | Send to scheduled candidates | Verify email sent to scheduled candidates | `DB has candidates with schedule=true, email="test@example.com"` | verify(send(), times(n)) for each candidate | Mock | Pending | Happy path |
| TC_EMAIL_015 | checkAndSendEmailForCandidate | No scheduled candidates | Verify no emails sent | `DB has no candidates with schedule=true` | verify(send(), times(0)) | Mock | Pending | Edge case |
| TC_EMAIL_016 | checkAndSendEmailForCandidate | Batch send with errors | Verify error handling in batch | `3 candidates, send fails for 1` | Sends for 2 successfully, logs failure for 1 | Mock | Pending | Batch error handling |
| TC_EMAIL_017 | checkAndSendEmailForCandidate | Template rendering | Verify email template applied | `template="Scheduled interview on {date}"` | Email body contains interpolated date | Mock | Pending | Template |

---

### Method 3: checkAndSendEmailForRecruiter()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_EMAIL_018 | checkAndSendEmailForRecruiter | Send to recruiters with active jobs | Verify recruiter notification | `DB has recruiters with active jobs, email="recruiter@example.com"` | verify(send(), times(n)) for each recruiter | Mock | Pending | Happy path |
| TC_EMAIL_019 | checkAndSendEmailForRecruiter | Aggregate candidate count | Verify candidate count aggregation | `Recruiter with 5 pending applications` | Email body shows "5 new candidates" | Mock | Pending | Aggregation |
| TC_EMAIL_020 | checkAndSendEmailForRecruiter | No active jobs | Verify no emails sent | `DB has no recruiters with active jobs` | verify(send(), times(0)) | Mock | Pending | Edge case |

---

### Method 4: sendExpiringJobsEmailForCandidate()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_EMAIL_021 | sendExpiringJobsEmailForCandidate | Send expiring jobs notification | Verify expiring jobs template | `Candidates with jobs expiring in 7 days` | Email template applied with job titles | Mock | Pending | Template application |
| TC_EMAIL_022 | sendExpiringJobsEmailForCandidate | Email subject line | Verify subject format | `expiredJobsCount=3` | Subject contains "3 jobs expiring soon" | Mock | Pending | Subject formatting |

---

### Method 5: sendingExpiringJobsEmailForRecruiter()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_EMAIL_023 | sendingExpiringJobsEmailForRecruiter | Send to recruiters with expiring jobs | Verify recruiter expiring job notification | `Recruiters with jobs expiring in 7 days` | Email sent with job count aggregation | Mock | Pending | Happy path |
| TC_EMAIL_024 | sendingExpiringJobsEmailForRecruiter | Aggregate by recruiter | Verify candidate aggregation in email | `3 jobs expiring, 2 different recruiters` | Email lists all 3 jobs grouped by recruiter | Mock | Pending | Aggregation |
| TC_EMAIL_025 | sendingExpiringJobsEmailForRecruiter | Subject line formatting | Verify subject with expiring count | `expiringCount=5` | Subject="5 of your jobs are expiring soon" | Mock | Pending | Subject format |
| TC_EMAIL_026 | sendingExpiringJobsEmailForRecruiter | No expiring jobs | Verify no emails sent | `No jobs expiring in next 7 days` | verify(send(), times(0)) | Mock | Pending | Edge case |

---

## Test Summary

| Service | Method Count | Test Cases | Test Type Distribution |
|---|---|---|---|
| ScheduleImpl | 7 | 34 | CheckDB: 34 |
| EmailServiceImpl | 5 | 26 | Mock: 26 |
| **Total** | **12** | **60** | **CheckDB: 34 / Mock: 26** |

---

## Annotations Required

### For ScheduleImpl Tests (@DataJpaTest with @Transactional)
```java
@SpringBootTest
@Transactional
@DataJpaTest
class ScheduleImplTest {
    // Use real repository, test DB operations
}
```

### For EmailServiceImpl Tests (@ExtendWith with Mockito)
```java
@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private SendGridClient sendGridClient;
    @InjectMocks
    private EmailServiceImpl emailService;
}
```

---

## Special Considerations

- **Schedule overlaps:** Detect time conflicts for same recruiter-candidate pair
- **Status transitions:** Track state changes (PENDING→APPROVED→COMPLETED)
- **Pagination:** Include edge cases for page boundaries and empty results
- **External APIs:** Mock SendGrid and JavaMailSender completely
- **Template processing:** Verify Thymeleaf template variable substitution
- **Character encoding:** Test Vietnamese characters and special symbols
- **Timezone handling:** Verify LocalDateTime comparisons across timezones
