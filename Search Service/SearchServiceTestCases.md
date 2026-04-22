# Test Cases: Search Service (AddressService, OrganizationImpl, SearchingSuggestionImpl)

## Service 1: AddressServiceImpl (@DataJpaTest - CheckDB Type)

### Method 1: getProvinces()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_ADDR_001 | getProvinces | Get all provinces | Verify all provinces retrieved | No input (getAll) | Returns List<ProvinceDTO> size≥63 (Vietnam provinces), sorted | CheckDB | Pending | Happy path |
| TC_ADDR_002 | getProvinces | Empty database | Verify empty list handling | DB has no provinces | Returns empty List | CheckDB | Pending | Edge case |
| TC_ADDR_003 | getProvinces | Large dataset | Verify performance with many provinces | DB has 1000+ provinces | Returns all items, response time <1s | CheckDB | Pending | Boundary - scale |
| TC_ADDR_004 | getProvinces | Null return handling | Verify null value handling | DB query returns null | Returns empty List or throws NPE | CheckDB | Pending | Edge case |
| TC_ADDR_005 | getProvinces | Data integrity | Verify all fields populated | DB provinces with code, name | All fields populated in DTO | CheckDB | Pending | Data mapping |

---

### Method 2: getDistrictsByProvinceCode()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_ADDR_006 | getDistrictsByProvinceCode | Get districts by valid code | Verify districts for province | `provinceCode="79" (Ho Chi Minh)` | Returns List<DistrictDTO> with districts of HCMC | CheckDB | Pending | Happy path |
| TC_ADDR_007 | getDistrictsByProvinceCode | Invalid province code | Verify error on invalid code | `provinceCode="999"` | Returns empty List or throws exception | CheckDB | Pending | Exception handling |
| TC_ADDR_008 | getDistrictsByProvinceCode | Null province code | Verify null handling | `provinceCode=null` | throws NullPointerException or IllegalArgumentException | CheckDB | Pending | Validation |
| TC_ADDR_009 | getDistrictsByProvinceCode | Case sensitivity | Verify case handling | `provinceCode="79", then "79" (uppercase)` | Returns same results regardless of case | CheckDB | Pending | Case sensitivity |
| TC_ADDR_010 | getDistrictsByProvinceCode | Empty districts | Verify empty list | `provinceCode valid but has no districts` | Returns empty List | CheckDB | Pending | Edge case |
| TC_ADDR_011 | getDistrictsByProvinceCode | Large dataset | Verify performance | `provinceCode with 50+ districts` | Returns all items in <500ms | CheckDB | Pending | Performance |
| TC_ADDR_012 | getDistrictsByProvinceCode | Sorting | Verify districts sorted | Results for `provinceCode="01"` | List sorted by district name/code | CheckDB | Pending | Sorting |
| TC_ADDR_013 | getDistrictsByProvinceCode | Whitespace handling | Verify trim on code | `provinceCode=" 79 "` | Finds province after trim | CheckDB | Pending | Trimming |
| TC_ADDR_014 | getDistrictsByProvinceCode | Special characters | Verify special char handling | `provinceCode="79'"` | Returns empty or throws exception | CheckDB | Pending | Input sanitization |

---

### Method 3: getWardsByProvinceCode()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_ADDR_015 | getWardsByProvinceCode | Get wards by valid code | Verify wards for district | `districtCode="701" (District 1, HCMC)` | Returns List<WardDTO> with wards of District 1 | CheckDB | Pending | Happy path |
| TC_ADDR_016 | getWardsByProvinceCode | Invalid district code | Verify error handling | `districtCode="9999"` | Returns empty List | CheckDB | Pending | Exception handling |
| TC_ADDR_017 | getWardsByProvinceCode | Null district code | Verify null validation | `districtCode=null` | throws NullPointerException | CheckDB | Pending | Validation |
| TC_ADDR_018 | getWardsByProvinceCode | Empty wards list | Verify empty result | `districtCode valid, no wards` | Returns empty List | CheckDB | Pending | Edge case |
| TC_ADDR_019 | getWardsByProvinceCode | Large dataset | Verify performance | `districtCode with 30+ wards` | Returns all in <300ms | CheckDB | Pending | Performance |
| TC_ADDR_020 | getWardsByProvinceCode | Data completeness | Verify all ward fields | Results include code, name, type | All DTO fields populated | CheckDB | Pending | Data mapping |

---

## Service 2: OrganizationImpl (@DataJpaTest - CheckDB Type)

### Method 1: save()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_ORG_001 | save | Create new organization | Verify org creation | `Organization(userId=1L, name="Tech Corp", website="tech.com", industry="IT")` | Saved with id generated, createdDate set | CheckDB | Pending | Happy path |
| TC_ORG_002 | save | Update existing org | Verify org update | `Organization(id=1L, name="Tech Corp Updated", website="tech2.com")` | name and website updated, modifiedDate changed | CheckDB | Pending | Update |
| TC_ORG_003 | save | Create with all fields | Verify complete object | `Organization(..., logo, address, phone, email, description, recruiterCount)` | All fields persisted | CheckDB | Pending | Complete object |
| TC_ORG_004 | save | Duplicate organization name | Verify duplicate handling | `Organization(userId=1L, name="Duplicate"), then same name` | throws DuplicateNameException or allows duplicate | CheckDB | Pending | Uniqueness constraint |
| TC_ORG_005 | save | Null organization name | Verify required field | `Organization(name=null)` | throws ConstraintViolationException | CheckDB | Pending | Validation |
| TC_ORG_006 | save | Null userId | Verify user association | `Organization(userId=null)` | throws ConstraintViolationException | CheckDB | Pending | Foreign key |
| TC_ORG_007 | save | Empty optional fields | Verify optional field handling | `Organization(..., logo="", description="", phone=null)` | Empty strings and nulls persisted | CheckDB | Pending | Optional fields |
| TC_ORG_008 | save | Very long name | Verify string length limit | `name="A"*500` | Truncated to max length or throws exception | CheckDB | Pending | Boundary |
| TC_ORG_009 | save | Organization with recruiter list | Verify relationship handling | `Organization with recruiters list populated` | Recruiters persisted with org relationship | CheckDB | Pending | Relationship mapping |
| TC_ORG_010 | save | Invalid URL format | Verify website validation | `website="not-a-url"` | Validates or throws exception | CheckDB | Pending | Format validation |

---

### Method 2: getOrgByUserId()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_ORG_011 | getOrgByUserId | Organization found | Verify org retrieval by userId | `userId=1L, org exists` | Returns Organization with all fields | CheckDB | Pending | Happy path |
| TC_ORG_012 | getOrgByUserId | Organization not found | Verify error on missing org | `userId=999L, no org` | throws EntityNotFoundException | CheckDB | Pending | Exception |
| TC_ORG_013 | getOrgByUserId | Null userId | Verify null validation | `userId=null` | throws NullPointerException | CheckDB | Pending | Validation |
| TC_ORG_014 | getOrgByUserId | Complete object mapping | Verify all fields present | Retrieved organization | All fields (id, name, website, recruiters) populated | CheckDB | Pending | Data integrity |

---

### Method 3: aminGetOrgByUserId()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_ORG_015 | aminGetOrgByUserId | Admin get org by userId | Verify admin retrieval | `userId=1L` | Same result as getOrgByUserId() | CheckDB | Pending | Consistency check |
| TC_ORG_016 | aminGetOrgByUserId | Verify permission check | Verify admin access | `userId=2L, admin checks org` | Returns org or throws AccessDeniedException | CheckDB | Pending | Authorization |

---

### Method 4: getOrg()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_ORG_017 | getOrg | Get by organization ID | Verify org retrieval by ID | `orgId=1L` | Returns Organization | CheckDB | Pending | Happy path |
| TC_ORG_018 | getOrg | Get by organization name | Verify org retrieval by name | `name="Tech Corp"` | Returns Organization matching name | CheckDB | Pending | Name search |
| TC_ORG_019 | getOrg | Pagination - first page | Verify paginated results | `pageable.getPageNumber()=0, size=10` | Page with ≤10 orgs, hasNext correct | CheckDB | Pending | Pagination |
| TC_ORG_020 | getOrg | Pagination - middle page | Verify offset calculation | `pageable.getPageNumber()=1, size=10` | Returns items 11-20 | CheckDB | Pending | Page offset |
| TC_ORG_021 | getOrg | Pagination - empty page | Verify behavior on empty page | `pageable exceeds total count` | Returns empty Page | CheckDB | Pending | Edge case |
| TC_ORG_022 | getOrg | Empty results | Verify no match handling | `name="NonExistent"` | Returns empty List or Page | CheckDB | Pending | No results |
| TC_ORG_023 | getOrg | Case sensitivity search | Verify case handling | `name="tech corp"` vs `name="TECH CORP"` | Returns same organization if case-insensitive | CheckDB | Pending | Case handling |
| TC_ORG_024 | getOrg | Partial name match | Verify LIKE search | `name="Tech%"` | Returns organizations starting with "Tech" | CheckDB | Pending | Pattern matching |
| TC_ORG_025 | getOrg | Null parameters | Verify null handling | `orgId=null, name=null` | Returns all orgs or throws exception | CheckDB | Pending | Validation |
| TC_ORG_026 | getOrg | Whitespace in search | Verify trim on name | `name=" Tech Corp "` | Finds org after trim | CheckDB | Pending | Trimming |
| TC_ORG_027 | getOrg | Special characters | Verify escape handling | `name="Tech & Associates"` | Correctly escapes special chars | CheckDB | Pending | SQL injection safety |
| TC_ORG_028 | getOrg | Large page size | Verify performance | `size=1000, large dataset` | Returns in <2s | CheckDB | Pending | Performance |

---

### Method 5: getAllRecruiterByOrganization()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_ORG_029 | getAllRecruiterByOrganization | Get recruiters - page 0 | Verify pagination first page | `orgId=1L, pageable.pageNumber=0, size=10` | Returns 0-9 recruiters | CheckDB | Pending | Pagination |
| TC_ORG_030 | getAllRecruiterByOrganization | Get recruiters - page 2 | Verify pagination offset | `orgId=1L, pageable.pageNumber=2, size=10` | Returns items 20-29 (manual subList calculation) | CheckDB | Pending | Offset calculation |
| TC_ORG_031 | getAllRecruiterByOrganization | Get recruiters - last page | Verify last page | `orgId=1L, pageNumber=4, size=10, totalElements=45` | Returns 41-45 (5 items) | CheckDB | Pending | Last page |
| TC_ORG_032 | getAllRecruiterByOrganization | No recruiters | Verify empty list | `orgId has no recruiters` | Returns empty List or Page | CheckDB | Pending | Edge case |
| TC_ORG_033 | getAllRecruiterByOrganization | Null orgId | Verify null handling | `orgId=null` | throws NullPointerException | CheckDB | Pending | Validation |
| TC_ORG_034 | getAllRecruiterByOrganization | Size = 0 | Verify boundary | `pageable.size=0` | Returns empty or throws exception | CheckDB | Pending | Boundary |
| TC_ORG_035 | getAllRecruiterByOrganization | Negative page number | Verify negative index | `pageNumber=-1` | throws IllegalArgumentException or defaults to 0 | CheckDB | Pending | Validation |
| TC_ORG_036 | getAllRecruiterByOrganization | Very large page size | Verify truncation | `size=10000, only 100 recruiters` | Returns all 100 without error | CheckDB | Pending | Large dataset |

---

### Method 6: updateRecruiterStatus()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_ORG_037 | updateRecruiterStatus | Update to ACTIVE | Verify status change | `recruiterId=1L, status=ACTIVE` | recruiter.status = ACTIVE, modifiedDate updated | CheckDB | Pending | Happy path |
| TC_ORG_038 | updateRecruiterStatus | Recruiter not found | Verify error handling | `recruiterId=999L` | throws EntityNotFoundException | CheckDB | Pending | Exception |
| TC_ORG_039 | updateRecruiterStatus | Invalid status enum | Verify enum validation | `status="INVALID_STATUS"` | throws IllegalArgumentException | CheckDB | Pending | Enum validation |

---

## Service 3: SearchingSuggestionImpl (Mixed CheckDB + Mock)

### Method 1: getDataSearch()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_SRCH_001 | getDataSearch | Get all search data | Verify all records returned | No input (getAll) | Returns List<SearchDataDTO> all records | CheckDB | Pending | Happy path |
| TC_SRCH_002 | getDataSearch | Pagination - page 0 | Verify first page | `pageable.pageNumber=0, size=20` | Returns 0-19 items | CheckDB | Pending | Pagination |
| TC_SRCH_003 | getDataSearch | Pagination - empty page | Verify no results | `pageNumber exceeds total` | Returns empty Page | CheckDB | Pending | Edge case |
| TC_SRCH_004 | getDataSearch | Large dataset performance | Verify response time | `DB has 10000 records, page=0, size=50` | Returns in <1s | CheckDB | Pending | Performance |
| TC_SRCH_005 | getDataSearch | Null pageable | Verify null handling | `pageable=null` | throws NullPointerException or uses default | CheckDB | Pending | Validation |
| TC_SRCH_006 | getDataSearch | Database unavailable | Verify error handling | `Repository throws exception` | throws DataAccessException | CheckDB | Pending | Error handling |
| TC_SRCH_007 | getDataSearch | Sorting verification | Verify sort order | `pageable.sort=byCreatedDate DESC` | Results sorted by date descending | CheckDB | Pending | Sorting |

---

### Method 2: addDataSearch()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_SRCH_008 | addDataSearch | Add new search data | Verify record creation | `SearchData(keyword="java", type="JOB")` | Record saved with id, timestamp | CheckDB | Pending | Happy path |
| TC_SRCH_009 | addDataSearch | Add with all fields | Verify complete object | `SearchData(..., userId, category, filters)` | All fields persisted | CheckDB | Pending | Complete object |
| TC_SRCH_010 | addDataSearch | Null keyword | Verify required field | `keyword=null` | throws ConstraintViolationException | CheckDB | Pending | Validation |
| TC_SRCH_011 | addDataSearch | Empty string | Verify empty handling | `keyword=""` | Persisted as empty or throws exception | CheckDB | Pending | Edge case |

---

### Method 3: getDataSearchByCondition()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_SRCH_012 | getDataSearchByCondition | Filter by keyword | Verify keyword filtering | `keyword="python"` | Returns records containing "python" | CheckDB | Pending | Happy path |
| TC_SRCH_013 | getDataSearchByCondition | Filter by type | Verify type filtering | `type="CANDIDATE"` | Returns only CANDIDATE type records | CheckDB | Pending | Filter |
| TC_SRCH_014 | getDataSearchByCondition | Multiple conditions | Verify AND logic | `keyword="python", type="JOB"` | Returns records matching BOTH conditions | CheckDB | Pending | Multi-filter |
| TC_SRCH_015 | getDataSearchByCondition | No matches | Verify empty result | `keyword="xyzabc123"` | Returns empty List | CheckDB | Pending | Edge case |
| TC_SRCH_016 | getDataSearchByCondition | Case sensitivity | Verify case handling | `keyword="JAVA"` vs `keyword="java"` | Returns same results if case-insensitive | CheckDB | Pending | Case handling |
| TC_SRCH_017 | getDataSearchByCondition | Null condition | Verify null handling | `keyword=null` | Returns all or throws exception | CheckDB | Pending | Validation |
| TC_SRCH_018 | getDataSearchByCondition | Large result set | Verify performance | `Condition matches 1000+ records` | Returns in <1.5s | CheckDB | Pending | Performance |

---

### Method 4: getDataSearchByMatchCondition()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_SRCH_019 | getDataSearchByMatchCondition | Cache hit | Verify cache retrieval | `when(cache.get(key)).thenReturn(data)` | Returns cached result immediately | Mock | Pending | Cache hit |
| TC_SRCH_020 | getDataSearchByMatchCondition | Cache miss | Verify DB fallback | `when(cache.get(key)).thenReturn(null), DB has data` | Queries DB, updates cache | Mock | Pending | Cache miss |
| TC_SRCH_021 | getDataSearchByMatchCondition | Exact match only | Verify precision | `condition="java", no "javascript"` | Returns exact "java" only | CheckDB | Pending | Exact match |
| TC_SRCH_022 | getDataSearchByMatchCondition | Null condition | Verify null handling | `condition=null` | throws NullPointerException | CheckDB | Pending | Validation |
| TC_SRCH_023 | getDataSearchByMatchCondition | Response structure | Verify DTO format | Retrieved data | Returns properly formatted SearchResponseDTO | CheckDB | Pending | Structure validation |
| TC_SRCH_024 | getDataSearchByMatchCondition | Timeout handling | Verify timeout on cache | `when(cache.get()).thenThrow(TimeoutException)` | Falls back to DB | Mock | Pending | Timeout fallback |
| TC_SRCH_025 | getDataSearchByMatchCondition | Cache invalidation | Verify stale data handling | Cache expires, DB updated | Returns fresh data | Mock | Pending | Cache invalidation |
| TC_SRCH_026 | getDataSearchByMatchCondition | Large cached dataset | Verify performance | `cache has 50000 items` | Returns in <100ms | Mock | Pending | Performance |

---

### Method 5: addOrUpdateObject()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_SRCH_027 | addOrUpdateObject | Add new object | Verify creation | `SearchObject(keyword="angular", rank=1)` | Object saved, cache updated | CheckDB | Pending | Happy path |
| TC_SRCH_028 | addOrUpdateObject | Update existing | Verify rank increment | `Object exists, rank=5, new rank=6` | rank incremented to 6, lastSearchDate updated | CheckDB | Pending | Update |
| TC_SRCH_029 | addOrUpdateObject | Cache synchronization | Verify cache updated | `Object saved to DB` | Cache entry created/updated | Mock | Pending | Cache sync |
| TC_SRCH_030 | addOrUpdateObject | Rank calculation | Verify rank logic | `First search rank=1, repeat search rank=2` | Rank incremented per search frequency | CheckDB | Pending | Business logic |
| TC_SRCH_031 | addOrUpdateObject | Timestamp update | Verify date tracking | `Object updated` | lastSearchDate = current timestamp | CheckDB | Pending | Timestamp |
| TC_SRCH_032 | addOrUpdateObject | Null keyword | Verify validation | `keyword=null` | throws IllegalArgumentException | CheckDB | Pending | Validation |

---

### Method 6: getObjectSearch()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_SRCH_033 | getObjectSearch | Retrieve from cache | Verify cache read | `Object in cache with key="java"` | Returns cached object immediately | Mock | Pending | Cache hit |
| TC_SRCH_034 | getObjectSearch | Retrieve from DB | Verify DB fallback | `Object not in cache, exists in DB` | Queries DB, updates cache | CheckDB | Pending | Cache miss |
| TC_SRCH_035 | getObjectSearch | Object not found | Verify error handling | `Object not in cache or DB` | throws EntityNotFoundException | CheckDB | Pending | Not found |
| TC_SRCH_036 | getObjectSearch | Cache refresh on stale | Verify freshness | `Cache expired, DB updated` | Returns fresh data | Mock | Pending | Refresh |
| TC_SRCH_037 | getObjectSearch | Concurrent access | Verify thread safety | `Multiple threads access same key` | Returns consistent result | Mock | Pending | Concurrency |

---

### Method 7: searchingSuggestionConverter()

| Test Case ID | Method | Test Scenario | Test Objective | Input | Expected Output | Test Type | Result | Notes |
|---|---|---|---|---|---|---|---|---|
| TC_SRCH_038 | searchingSuggestionConverter | Convert entity to DTO | Verify mapping | `SearchObject entity with all fields` | Returns SearchDTO with same fields | CheckDB | Pending | Conversion |

---

## Test Summary

| Service | Method Count | Test Cases | Test Type Distribution |
|---|---|---|---|
| AddressServiceImpl | 3 | 20 | CheckDB: 20 |
| OrganizationImpl | 6 | 23 | CheckDB: 23 |
| SearchingSuggestionImpl | 7 | 38 | CheckDB: 28 / Mock: 10 |
| **Total** | **16** | **81** | **CheckDB: 71 / Mock: 10** |

---

## Annotations Required

### For Address & Organization Tests (@DataJpaTest)
```java
@SpringBootTest
@Transactional
@DataJpaTest
class AddressServiceImplTest {
    // Real repository, JPA queries, DB state verification
}
```

### For SearchingSuggestionImpl Tests (Mixed)
```java
@SpringBootTest
@Transactional
class SearchingSuggestionImplTest {
    @Mock
    private CacheService cacheService;
    @DataJpaTest
    private SearchObjectRepository repository;
    
    // CheckDB tests: real DB operations
    // Mock tests: cache operations stubbed
}
```

---

## Special Considerations

- **Pagination:** Use Math.ceil for manual page calculations in getAllRecruiterByOrganization
- **Cache operations:** Mock CacheService.get/put/evict methods
- **Case sensitivity:** Test both upper/lower case for string searches
- **Performance:** Set <1s threshold for list operations returning 10K+ items
- **Date handling:** Verify LocalDateTime comparisons across timezones
- **Database state:** Clean up between tests with @Transactional rollback
