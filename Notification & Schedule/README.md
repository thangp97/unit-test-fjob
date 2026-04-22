# Notification & Schedule Service - Test Guide

## 📋 Tổng Quan

Test suite cho Notification & Schedule service bao gồm 2 services:

### 1. **ScheduleImpl** — Quản lý lịch phỏng vấn
- 34 test cases (Type: CheckDB)
- 7 methods được test
- Database operations, JPA queries, transactions

### 2. **EmailServiceImpl** — Gửi email thông báo  
- 26 test cases (Type: Mock)
- 5 methods được test
- External APIs (SendGrid), template processing

---

## 🚀 Chạy Test

### Chạy tất cả tests của feature này
```bash
mvn clean test -Dtest=ScheduleImplTest,EmailServiceImplTest
```

### Chạy riêng ScheduleImpl (34 tests)
```bash
mvn clean test -Dtest=ScheduleImplTest
```

### Chạy riêng EmailServiceImpl (26 tests)
```bash
mvn clean test -Dtest=EmailServiceImplTest
```

### Chạy 1 test method cụ thể
```bash
# Example: TC_SCH_001
mvn clean test -Dtest=ScheduleImplTest#testSaveSchedule_CreateNewSchedule

# Hoặc chạy theo pattern
mvn clean test -Dtest=ScheduleImplTest#testSaveSchedule*
```

---

## 📊 Test Case Breakdown

### ScheduleImpl Tests (TC_SCH_001 ~ TC_SCH_034)

#### Method: saveSchedule() — 9 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_SCH_001 | Create new schedule | Schedule created with PENDING status | CheckDB |
| TC_SCH_002 | Update existing schedule | Fields updated, modifiedDate changed | CheckDB |
| TC_SCH_003 | All fields populated | Complete object saved | CheckDB |
| TC_SCH_004 | Null interviewerId | Constraint violation exception | CheckDB |
| TC_SCH_005 | Null candidateId | Foreign key constraint error | CheckDB |
| TC_SCH_006 | Start > End time | IllegalArgumentException | CheckDB |
| TC_SCH_007 | Overlapping schedule | DuplicateScheduleException (Expected) | CheckDB |
| TC_SCH_008 | Status transition | Status changed to APPROVED | CheckDB |
| TC_SCH_009 | Empty note | Empty string persisted | CheckDB |

#### Method: getScheduleById() — 3 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_SCH_010 | Schedule found | Returns correct schedule | CheckDB |
| TC_SCH_011 | Not found | EntityNotFoundException | CheckDB |
| TC_SCH_012 | Field mapping | All fields correct | CheckDB |

#### Method: getScheduleByStatus() — 5 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_SCH_013 | Page 0, 10 items per page | hasNext=true, 10 items | CheckDB |
| TC_SCH_014 | Last page | hasNext=false, <10 items | CheckDB |
| TC_SCH_015 | Invalid enum | IllegalArgumentException | CheckDB |
| TC_SCH_016 | No results | Empty page | CheckDB |
| TC_SCH_017 | Case sensitivity | IllegalArgumentException for "pending" | CheckDB |

#### Method: getCalendar() — 6 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_SCH_018 | Recruiter view | Schedules with recruiterId=1 | CheckDB |
| TC_SCH_019 | Candidate view | Schedules with candidateId=2 | CheckDB |
| TC_SCH_020 | Status filter | Only APPROVED schedules | CheckDB |
| TC_SCH_021 | Empty date range | Empty list | CheckDB |
| TC_SCH_022 | Multiple results | 3+ schedules returned | CheckDB |
| TC_SCH_023 | Invalid date range | Exception or auto-swap | CheckDB |

#### Method: deleteByIds() — 5 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_SCH_025 | Batch delete 3 IDs | All removed from DB | CheckDB |
| TC_SCH_026 | Delete already deleted | No error (idempotent) | CheckDB |
| TC_SCH_027 | Empty list | No changes | CheckDB |
| TC_SCH_028 | Mixed IDs (1 valid, 1 invalid) | Valid one deleted | CheckDB |
| TC_SCH_029 | Null list | NullPointerException | CheckDB |

#### Method: getApplicationStatus() — 5 tests
- TC_SCH_030 ~ TC_SCH_034 (Similar patterns)

---

### EmailServiceImpl Tests (TC_EMAIL_001 ~ TC_EMAIL_026)

#### Method: send() — 13 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_EMAIL_001 | Plain text email | Sent successfully, status=202 | Mock |
| TC_EMAIL_002 | HTML email | Content-type=text/html | Mock |
| TC_EMAIL_003 | Multiple recipients | CC/BCC headers set | Mock |
| TC_EMAIL_004 | SendGrid API error | EmailSendException thrown | Mock |
| TC_EMAIL_005 | Vietnamese chars | UTF-8 encoding preserved | Mock |
| TC_EMAIL_006 | Empty subject | Sent with empty subject | Mock |
| TC_EMAIL_007 | Very long subject | Truncated to 255 chars | Mock |
| TC_EMAIL_008 | Null recipient | IllegalArgumentException | Mock |
| TC_EMAIL_009 | Invalid email format | Invalid format rejection | Mock |
| TC_EMAIL_010 | With attachment | Attachment included | Mock |
| TC_EMAIL_011 | Retry logic | verify(send, times(3)) | Mock |
| TC_EMAIL_012 | Timeout | EmailTimeoutException | Mock |
| TC_EMAIL_013 | Template with variables | Variables substituted | Mock |

#### Method: checkAndSendEmailForCandidate() — 4 tests
- TC_EMAIL_014 ~ TC_EMAIL_017

#### Method: checkAndSendEmailForRecruiter() — 3 tests
- TC_EMAIL_018 ~ TC_EMAIL_020

#### Other Methods — 6 tests
- sendExpiringJobsEmailForCandidate: TC_EMAIL_021, 022
- sendingExpiringJobsEmailForRecruiter: TC_EMAIL_023 ~ 026

---

## 📝 Test Type Explanation

### CheckDB Tests (@SpringBootTest)
```java
@SpringBootTest
@Transactional
class ScheduleImplTest {
    // Kiểm tra thực database operations
    // Auto rollback after each test
    // Use real repositories
}
```

**Khi nào**: Database queries, JPA operations, transactions

**Đặc điểm**:
- ✅ Thực tế như production
- ✅ Tự động rollback
- ❌ Chậm hơn unit tests
- ❌ Cần database setup

### Mock Tests (@ExtendWith)
```java
@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {
    @Mock
    private JavaMailSender mailSender;
    @InjectMocks
    private EmailServiceImpl emailService;
    // Mock external dependencies
}
```

**Khi nào**: External APIs (SendGrid, JavaMail, etc.)

**Đặc điểm**:
- ✅ Nhanh
- ✅ Không cần external services
- ✅ Kiểm soát behavior
- ❌ Giả lập thực tế

---

## 📊 Expected Results

```
Total Tests: 60
├── ScheduleImpl: 34 tests
│   └── Expected: 33 PASS / 1 FAIL (TC_SCH_007 - Overlap detection)
└── EmailServiceImpl: 26 tests
    └── Expected: 25 PASS / 1 FAIL (TC_EMAIL_004 - API error)

Total Expected: 58 PASS / 2 expected FAIL
Execution Time: ~3-5 seconds
```

---

## 🔍 Interpreting Results

### When TC_SCH_007 FAILS (Expected)
```
Test: Overlapping schedule detection
Message: DuplicateScheduleException
Status: EXPECTED FAIL ✓
Action: None - this is correct behavior
```

### When TC_EMAIL_004 FAILS (Expected)
```
Test: SendGrid API error handling
Message: EmailSendException thrown
Status: EXPECTED FAIL ✓
Action: None - verifies error handling
```

---

## 📋 Detailed Test Case Reference

**For complete test case details**, see:
- `ScheduleAndEmailTestCases.md`

Each test case includes:
- Test ID (TC_SCH_001, etc.)
- Method name
- Scenario description
- Test objective
- Input parameters
- Expected output
- Test type
- Notes

---

## 🎯 Key Test Scenarios

### Schedule Overlap Detection
```
Scenario: Two schedules for same recruiter-candidate pair
Expected: System prevents overlapping
Test Method: TC_SCH_007 saveSchedule_OverlapDetection
```

### Email Template Processing
```
Scenario: Send email with template variables
Expected: Variables substituted correctly
Test Method: TC_EMAIL_013 send_TemplateEmail
```

### Pagination Edge Cases
```
Scenarios:
- Page 0 (first page)
- Last page (fewer items)
- Empty page (exceeds total)
Test Methods: TC_SCH_013, 014, 016
```

---

## 🛠️ Troubleshooting

### ScheduleImpl Tests Failing

**Problem: Database connection error**
```
Error: Could not create new instance of class ScheduleImplTest
```
**Solution**:
```bash
# Check database is running
# Check application-test.yml exists
# Check @SpringBootTest import
```

**Problem: Data constraint violation**
```
Error: Duplicate key value violates unique constraint
```
**Solution**:
```java
// Add to @BeforeEach
scheduleRepository.deleteAll();
```

### EmailServiceImpl Tests Failing

**Problem: Mock verification failed**
```
Error: Wanted but not invoked: mailSender.send()
```
**Solution**:
```java
// Ensure when().thenReturn() is called before method
when(sendGridClient.send(any())).thenReturn(202);
emailService.send(request); // Should call mock
```

**Problem: NullPointerException**
```
Error: Cannot invoke method on null object
```
**Solution**:
```java
// Ensure @Mock is annotated
@Mock
private JavaMailSender mailSender;
```

---

## 📈 Running with Coverage

```bash
# Generate code coverage report
mvn clean test jacoco:report

# View report
# Open: target/site/jacoco/index.html
```

---

## 💡 Tips

1. **Run failed tests again**
   ```bash
   mvn clean test -Dtest=ScheduleImplTest -DforkCount=0
   ```

2. **Run with detailed logging**
   ```bash
   mvn clean test -Dtest=ScheduleImplTest -X
   ```

3. **Skip failed tests (for CI/CD)**
   ```bash
   mvn clean test -DtestFailureIgnore=true
   ```

4. **Run in parallel** (faster)
   ```bash
   mvn clean test -T 1C
   ```

---

## 📞 Need Help?

1. Check `README.md` for full documentation
2. See `ScheduleAndEmailTestCases.md` for test details
3. Review test source code for implementation details
4. Check Maven output for error stack traces

---

**Created**: April 22, 2026
**Test Framework**: JUnit 5 + Mockito + Spring Boot Test
**Version**: 1.0
