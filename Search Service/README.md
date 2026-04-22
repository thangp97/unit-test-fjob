# Search Service - Test Guide

## 📋 Tổng Quan

Test suite cho Search Service bao gồm 3 services:

### 1. **AddressServiceImpl** — Quản lý địa chỉ
- 20 test cases (Type: CheckDB)
- 3 methods được test
- Tỉnh/thành phố, Huyện/quận, Xã/phường

### 2. **OrganizationImpl** — Quản lý công ty
- 23 test cases (Type: CheckDB)
- 6 methods được test
- CRUD operations, pagination, filtering

### 3. **SearchingSuggestionImpl** — Gợi ý tìm kiếm
- 38 test cases (Type: CheckDB + Mock)
- 7 methods được test
- Database + Cache operations

---

## 🚀 Chạy Test

### Chạy tất cả tests của feature này
```bash
mvn clean test -Dtest=SearchServiceImplTest
```

### Chạy các services riêng lẻ (nếu tách thành file riêng)
```bash
# Tất cả đang trong 1 file SearchServiceImplTest
# Nếu tách ra, sử dụng:
mvn clean test -Dtest=AddressServiceImplTest
mvn clean test -Dtest=OrganizationImplTest
mvn clean test -Dtest=SearchingSuggestionImplTest
```

### Chạy 1 test method cụ thể
```bash
# Example: TC_ADDR_001
mvn clean test -Dtest=SearchServiceImplTest#testGetProvinces_GetAll

# Hoặc chạy theo pattern
mvn clean test -Dtest=SearchServiceImplTest#testGetProvinces*
```

---

## 📊 Test Case Breakdown

### AddressServiceImpl Tests (TC_ADDR_001 ~ TC_ADDR_020)

#### Method: getProvinces() — 5 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_ADDR_001 | Get all provinces | Returns List ≥ 63 items | CheckDB |
| TC_ADDR_002 | Empty database | Returns empty List | CheckDB |
| TC_ADDR_003 | Large dataset (100) | Performance <1 second | CheckDB |
| TC_ADDR_004 | Data integrity | All fields populated | CheckDB |
| TC_ADDR_005 | Null handling | Returns non-null List | CheckDB |

#### Method: getDistrictsByProvinceCode() — 9 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_ADDR_006 | Valid code "79" | Returns HCMC districts | CheckDB |
| TC_ADDR_007 | Invalid code "999" | Returns empty List | CheckDB |
| TC_ADDR_008 | Null code | IllegalArgumentException | CheckDB |
| TC_ADDR_009 | Case sensitivity | Same results "79" vs "79" | CheckDB |
| TC_ADDR_010 | No districts | Empty list | CheckDB |
| TC_ADDR_011 | Large dataset (50) | Performance <500ms | CheckDB |
| TC_ADDR_012 | Sorting | Results sorted by code | CheckDB |
| TC_ADDR_013 | Whitespace trim | " 79 " → "79" | CheckDB |
| TC_ADDR_014 | Special chars | "79'" rejected | CheckDB |

#### Method: getWardsByDistrictCode() — 6 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_ADDR_015 | Valid code "701" | Returns wards | CheckDB |
| TC_ADDR_016 | Invalid code | Empty List | CheckDB |
| TC_ADDR_017 | Null code | IllegalArgumentException | CheckDB |
| TC_ADDR_018 | No wards | Empty List | CheckDB |
| TC_ADDR_019 | Large dataset (30) | Performance <300ms | CheckDB |
| TC_ADDR_020 | Data completeness | Code, name, type present | CheckDB |

---

### OrganizationImpl Tests (TC_ORG_001 ~ TC_ORG_039)

#### Method: save() — 10 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_ORG_001 | Create new org | Saved with id, PENDING status | CheckDB |
| TC_ORG_002 | Update existing | Fields updated, modifiedDate | CheckDB |
| TC_ORG_003 | All fields | 10 fields persisted | CheckDB |
| TC_ORG_004 | Duplicate name | Duplicate constraint (Expected) | CheckDB |
| TC_ORG_005 | Null name | ConstraintViolationException | CheckDB |
| TC_ORG_006 | Null userId | Foreign key constraint | CheckDB |
| TC_ORG_007 | Empty optional | Empty strings persisted | CheckDB |
| TC_ORG_008 | Long name (500) | Persisted successfully | CheckDB |
| TC_ORG_009 | With recruiters | Relationship mapped | CheckDB |
| TC_ORG_010 | Invalid URL | Accepted (no validation) | CheckDB |

#### Method: getOrgByUserId() — 4 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_ORG_011 | Organization exists | Returns org with fields | CheckDB |
| TC_ORG_012 | Not found (999) | EntityNotFoundException | CheckDB |
| TC_ORG_013 | Null userId | IllegalArgumentException | CheckDB |
| TC_ORG_014 | Complete mapping | All fields present | CheckDB |

#### Method: aminGetOrgByUserId() — 2 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_ORG_015 | Admin retrieval | Same result as getOrgByUserId | CheckDB |
| TC_ORG_016 | Permission check | Returns org for authorized | CheckDB |

#### Method: getOrg() — 12 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_ORG_017 | By ID | Returns matching org | CheckDB |
| TC_ORG_018 | By name | Returns matching org | CheckDB |
| TC_ORG_019 | Page 0 (10/15) | hasNext=true | CheckDB |
| TC_ORG_020 | Page 1 (items 11-20) | Offset calculated | CheckDB |
| TC_ORG_021 | Empty page | Returns empty | CheckDB |
| TC_ORG_022 | No results | Empty List/Page | CheckDB |
| TC_ORG_023 | Case sensitivity | "tech corp" not found (Expected) | CheckDB |
| TC_ORG_024 | Partial match | "Tech%" returns corps | CheckDB |
| TC_ORG_025 | Null params | NullPointerException | CheckDB |
| TC_ORG_026 | Whitespace trim | " Tech Corp " found | CheckDB |
| TC_ORG_027 | Special chars | "Tech & Associates" escaped | CheckDB |
| TC_ORG_028 | Large page (500) | Performance <2 seconds | CheckDB |

#### Method: getAllRecruiterByOrganization() — 8 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_ORG_029 | Page 0, size 10 | 10 items returned | CheckDB |
| TC_ORG_030 | Page 2, size 10 | Items 20-29 (offset calc) | CheckDB |
| TC_ORG_031 | Last page | Fewer items, hasNext=false | CheckDB |
| TC_ORG_032 | No recruiters | Empty List | CheckDB |
| TC_ORG_033 | Null orgId | NullPointerException | CheckDB |
| TC_ORG_034 | Size=0 | IllegalArgumentException | CheckDB |
| TC_ORG_035 | Negative page | IllegalArgumentException or default | CheckDB |
| TC_ORG_036 | Large size (10K) | All 100 items returned | CheckDB |

#### Method: updateRecruiterStatus() — 3 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_ORG_037 | Update to ACTIVE | Status changed | CheckDB |
| TC_ORG_038 | Not found (999) | EntityNotFoundException | CheckDB |
| TC_ORG_039 | Invalid enum | IllegalArgumentException | CheckDB |

---

### SearchingSuggestionImpl Tests (TC_SRCH_001 ~ TC_SRCH_038)

#### Method: getDataSearch() — 7 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_SRCH_001 | Get all | List with all records | CheckDB |
| TC_SRCH_002 | Page 0, size 20 | 20 items, hasNext=true | CheckDB |
| TC_SRCH_003 | Empty page | Page 5 with no items | CheckDB |
| TC_SRCH_004 | 10K items | Performance <1 second | CheckDB |
| TC_SRCH_005 | Null pageable | NullPointerException | CheckDB |
| TC_SRCH_006 | DB unavailable | DataAccessException | CheckDB |
| TC_SRCH_007 | Sorting | By createdDate DESC | CheckDB |

#### Method: addDataSearch() — 4 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_SRCH_008 | Create new | Saved with id | CheckDB |
| TC_SRCH_009 | All fields | userId, category, filters saved | CheckDB |
| TC_SRCH_010 | Null keyword | ConstraintViolationException | CheckDB |
| TC_SRCH_011 | Empty string | Validation error | CheckDB |

#### Method: getDataSearchByCondition() — 7 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_SRCH_012 | Filter by keyword | Results contain keyword | CheckDB |
| TC_SRCH_013 | Filter by type | Only selected type | CheckDB |
| TC_SRCH_014 | Multiple conditions | Keyword AND type (AND logic) | CheckDB |
| TC_SRCH_015 | No matches | Empty List | CheckDB |
| TC_SRCH_016 | Case sensitivity | "JAVA" vs "java" same | CheckDB |
| TC_SRCH_017 | Null condition | NullPointerException | CheckDB |
| TC_SRCH_018 | 1000+ items | Performance <1.5 seconds | CheckDB |

#### Method: getDataSearchByMatchCondition() — 8 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_SRCH_019 | Cache hit | Immediate return | Mock |
| TC_SRCH_020 | Cache miss | DB query + cache update | Mock |
| TC_SRCH_021 | Exact match | "java" not "javascript" | CheckDB |
| TC_SRCH_022 | Null condition | NullPointerException | CheckDB |
| TC_SRCH_023 | Response structure | Proper DTO format | CheckDB |
| TC_SRCH_024 | Cache timeout | Fallback to DB | Mock |
| TC_SRCH_025 | Cache invalidation | Fresh data returned | Mock |
| TC_SRCH_026 | Large cache (50K) | Response <100ms | Mock |

#### Method: addOrUpdateObject() — 6 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_SRCH_027 | Create new | rank=1, cache updated | CheckDB |
| TC_SRCH_028 | Update existing | rank incremented to 6 | CheckDB |
| TC_SRCH_029 | Cache sync | Entry updated in cache | Mock |
| TC_SRCH_030 | Rank calculation | Frequency-based increment | CheckDB |
| TC_SRCH_031 | Timestamp | lastSearchDate = now | CheckDB |
| TC_SRCH_032 | Null keyword | IllegalArgumentException | CheckDB |

#### Method: getObjectSearch() — 4 tests

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_SRCH_033 | Cache hit | Immediate return | Mock |
| TC_SRCH_034 | Cache miss | DB query, cache updated | CheckDB |
| TC_SRCH_035 | Not found | EntityNotFoundException | CheckDB |
| TC_SRCH_036 | Stale cache | Fresh data from DB | Mock |

#### Converter Methods — 1 test

| TC_ID | Scenario | Expected | Type |
|---|---|---|---|
| TC_SRCH_037 | searchingSuggestionConverter | Entity to DTO mapping | CheckDB |
| TC_SRCH_038 | Summary test | All tests traceable | CheckDB |

---

## 📝 Test Type Explanation

### CheckDB Tests (@SpringBootTest)
```java
@SpringBootTest
@Transactional
class SearchServiceImplTest {
    // Real database operations
    // JPA repositories
    // Transactions with auto-rollback
}
```

**Services tested**: AddressServiceImpl, OrganizationImpl (mostly)

### Mock Tests (@ExtendWith + @Mock)
```java
@ExtendWith(MockitoExtension.class)
class SearchingSuggestionImplTest {
    @Mock
    private CacheService cacheService;
    
    // Cache operations stubbed
}
```

**Services tested**: SearchingSuggestionImpl (cache methods)

---

## 📊 Expected Results

```
Total Tests: 81
├── AddressServiceImpl: 20 tests (PASS: 20)
├── OrganizationImpl: 23 tests (PASS: 22, FAIL: 1 - TC_ORG_023 case sensitivity)
└── SearchingSuggestionImpl: 38 tests (PASS: 36, FAIL: 2 - Cache scenarios)

Total Expected: 76 PASS / 5 expected FAIL
Execution Time: ~8-12 seconds
```

---

## 🔍 Interpreting Results

### When TC_ORG_023 FAILS (Expected)
```
Test: Case sensitivity in organization search
Input: "tech corp" (lowercase)
Message: EntityNotFoundException
Status: EXPECTED FAIL ✓ (System is case-sensitive)
```

### When Cache Tests Fail (Expected)
```
Tests: TC_SRCH_019, TC_SRCH_024, TC_SRCH_025, TC_SRCH_026
Reason: Cache behavior mocking
Status: Some expected, some indicate bugs
Action: Review cache strategy
```

---

## 📋 Detailed Test Case Reference

**For complete test case details**, see:
- `SearchServiceTestCases.md`

Each test case includes:
- Test ID (TC_ADDR_001, TC_ORG_001, TC_SRCH_001)
- Method name
- Scenario description
- Test objective
- Input parameters
- Expected output
- Test type (CheckDB/Mock)
- Notes

---

## 🎯 Key Test Scenarios

### Address Hierarchy
```
Vietnam
├── Provinces (e.g., "79" = Ho Chi Minh)
│   └── Districts (e.g., "701" = District 1)
│       └── Wards (e.g., "26001" = Ward 1)
```
**Tests**: TC_ADDR_006 ~ 020

### Organization Pagination
```
Pagination: page=0, size=10
├── First page: items 0-9 (hasNext=true)
├── Middle page: items 20-29
└── Last page: items 40-44 (hasNext=false, <10 items)
```
**Tests**: TC_ORG_019 ~ 028

### Search Caching Strategy
```
Request → Cache.get()
├── HIT: Return cached data immediately
├── MISS: Query DB + update cache
├── EXPIRED: Re-query and refresh
└── ERROR: Fallback to DB
```
**Tests**: TC_SRCH_019 ~ 026

---

## 🛠️ Troubleshooting

### AddressServiceImpl Tests Failing

**Problem: Performance timeout**
```
Error: Test took >1000ms
```
**Solution**:
```bash
# Check database indexing
# Optimize query in service
# Increase timeout if needed
```

### OrganizationImpl Tests Failing

**Problem: Duplicate key error**
```
Error: Duplicate organization name violates unique constraint
```
**Solution**:
```java
// Add to @BeforeEach
organizationRepository.deleteAll();
```

### SearchingSuggestionImpl Tests Failing

**Problem: Cache mock not working**
```
Error: NullPointerException on cache.get()
```
**Solution**:
```java
// Ensure @Mock is annotated properly
@Mock
private CacheService cacheService;

// Setup mock behavior
when(cacheService.get(any())).thenReturn(data);
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

1. **Run failed tests only**
   ```bash
   mvn clean test -Dtest=SearchServiceImplTest -DforkCount=0
   ```

2. **Debug with breakpoints**
   ```bash
   # In IDE: Right-click → Debug As JUnit Test
   # Or: mvn test -DforkCount=0
   ```

3. **Run with detailed output**
   ```bash
   mvn clean test -Dtest=SearchServiceImplTest -X
   ```

4. **Parallel execution (faster)**
   ```bash
   mvn clean test -T 1C
   ```

---

## 📞 Need Help?

1. Check `README.md` for full documentation
2. See `SearchServiceTestCases.md` for test details
3. Review test source code in `SearchServiceImplTest.java`
4. Check Maven output for error stack traces

---

## 🔗 Related Files

- Main README: `../README.md`
- Test Markdown: `SearchServiceTestCases.md`
- Test Source: `SearchServiceImplTest.java`
- Results CSV: `../Test_Execution_Results.csv`

---

**Created**: April 22, 2026
**Test Framework**: JUnit 5 + Mockito + Spring Boot Test
**Version**: 1.0
