package com.jober.searchservice.service.impl;

import static com.jober.searchservice.utilsmodule.RecruiterStatus.APPROVED;
import static com.jober.utilsservice.constant.ResponseMessageConstant.EXISTED;
import static com.jober.utilsservice.constant.ResponseMessageConstant.FOUND;
import static com.jober.utilsservice.constant.ResponseMessageConstant.NOT_FOUND;
import static com.jober.utilsservice.constant.ResponseMessageConstant.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jober.searchservice.dto.PageResponse;
import com.jober.searchservice.dto.RecruiterCompany;
import com.jober.searchservice.model.District;
import com.jober.searchservice.model.Organization;
import com.jober.searchservice.model.Province;
import com.jober.searchservice.model.RecruiterConfiguration;
import com.jober.searchservice.model.SearchingSuggestion;
import com.jober.searchservice.model.UserCommon;
import com.jober.searchservice.model.Ward;
import com.jober.searchservice.repository.DistrictRepo;
import com.jober.searchservice.repository.OrganizationRepo;
import com.jober.searchservice.repository.ProvinceRepo;
import com.jober.searchservice.repository.RecruiterConfigurationRepository;
import com.jober.searchservice.repository.SearchingSuggestionRepo;
import com.jober.searchservice.repository.WardRepo;
import com.jober.searchservice.utilsmodule.CacheService;
import com.jober.utilsservice.dto.OrgInputDTO;
import com.jober.utilsservice.dto.OrgSearchingDTO;
import com.jober.utilsservice.errors.ResponseEntitySerializable;
import com.jober.utilsservice.errors.RestExceptionHandler;
import com.jober.utilsservice.model.PageableModel;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Transactional
@Sql(scripts = "/search-service-it-schema.sql")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:searchserviceit;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false"
})
class SearchServiceDatabaseIntegrationTest {

  private AddressServiceImpl addressService;
  private OrganizationImpl organizationService;
  private SearchingSuggestionImpl searchingSuggestionService;

  @Autowired
  private EntityManager entityManager;
  @Autowired
  private ProvinceRepo provinceRepo;
  @Autowired
  private DistrictRepo districtRepo;
  @Autowired
  private WardRepo wardRepo;
  @Autowired
  private OrganizationRepo organizationRepo;
  @Autowired
  private RecruiterConfigurationRepository recruiterConfigurationRepository;
  @Autowired
  private SearchingSuggestionRepo searchingSuggestionRepo;

  @BeforeEach
  void setUp() {
    addressService = new AddressServiceImpl();
    ReflectionTestUtils.setField(addressService, "provinceRepo", provinceRepo);
    ReflectionTestUtils.setField(addressService, "districtRepo", districtRepo);
    ReflectionTestUtils.setField(addressService, "wardRepo", wardRepo);

    organizationService = new OrganizationImpl();
    ReflectionTestUtils.setField(organizationService, "organizationRepo", organizationRepo);
    ReflectionTestUtils.setField(organizationService, "recruiterConfigurationRepository",
        recruiterConfigurationRepository);

    searchingSuggestionService = new SearchingSuggestionImpl();
    ReflectionTestUtils.setField(searchingSuggestionService, "searchingSuggestionRepo",
        searchingSuggestionRepo);
    ReflectionTestUtils.setField(searchingSuggestionService, "cacheManager",
        new ConcurrentMapCacheManager("job", "company"));
    ReflectionTestUtils.setField(searchingSuggestionService, "cacheService", new CacheService());
    ReflectionTestUtils.setField(searchingSuggestionService, "responseObject", new ResponseObject());
    ReflectionTestUtils.setField(SearchingSuggestionImpl.class, "restExceptionHandler",
        new RestExceptionHandler());
    ReflectionTestUtils.setField(SearchingSuggestionImpl.class, "responseEntity", null);
  }

  @Test
  void getProvinces_databaseHasProvinces_returnsFoundResponseAndReadsRealDatabase() {
    // Test Case ID: TC_SEARCH_DB_ADDRESS_001
    // Scenario ID: DB_HAPPY_001
    // Objective: Verify provinces are loaded from the test database.
    // Covered Branch/Path: provinceRepo.findAll -> non-empty list -> FOUND response.
    // DB Check: Persist one province, call service, then verify repository count and response data.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    Province inputProvince = new Province();
    inputProvince.setCode("HN");
    inputProvince.setName("Ha Noi");
    Province expectedProvince = (Province) provinceRepo.saveAndFlush(inputProvince);

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse = addressService.getProvinces();

    // Assert
    assertThat(provinceRepo.count()).isEqualTo(1L);
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getStatus()).isEqualTo(FOUND);
    assertThat(actualBody.getTotalCount()).isEqualTo(1L);
    assertThat((List<?>) actualBody.getData()).extracting("code").containsExactly(expectedProvince.getCode());
  }

  @Test
  void getProvinces_databaseEmpty_returnsNotFoundResponseWithoutDatabaseWrite() {
    // Test Case ID: TC_SEARCH_DB_ADDRESS_004
    // Scenario ID: DB_EDGE_007
    // Objective: Verify empty province table returns NOT_FOUND for location filter data.
    // Covered Branch/Path: provinceRepo.findAll -> empty list -> NOT_FOUND response.
    // DB Check: Verify province table remains empty before and after service call.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    long expectedProvinceCount = 0L;

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse = addressService.getProvinces();

    // Assert
    assertThat(provinceRepo.count()).isEqualTo(expectedProvinceCount);
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getStatus()).isEqualTo(NOT_FOUND);
    assertThat((List<?>) actualBody.getData()).isEmpty();
  }

  @Test
  void getDistrictsByProvinceCode_databaseHasMatchingDistrict_returnsFoundResponse() {
    // Test Case ID: TC_SEARCH_DB_ADDRESS_002
    // Scenario ID: DB_HAPPY_002
    // Objective: Verify districts are loaded by province code from the test database.
    // Covered Branch/Path: districtRepo.findByProvinceCode -> non-empty list -> FOUND response.
    // DB Check: Persist matching and non-matching districts, verify only requested province is returned.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    District matchingDistrict = new District();
    matchingDistrict.setCode("BD");
    matchingDistrict.setName("Ba Dinh");
    matchingDistrict.setProvinceCode("HN");
    districtRepo.save(matchingDistrict);
    District otherDistrict = new District();
    otherDistrict.setCode("Q1");
    otherDistrict.setName("Quan 1");
    otherDistrict.setProvinceCode("HCM");
    districtRepo.saveAndFlush(otherDistrict);

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse =
        addressService.getDistrictsByProvinceCode("HN");

    // Assert
    assertThat(districtRepo.findByProvinceCode("HN")).hasSize(1);
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getStatus()).isEqualTo(FOUND);
    assertThat((List<?>) actualBody.getData()).extracting("code").containsExactly("BD");
  }

  @Test
  void getDistrictsByProvinceCode_databaseHasNoMatchingDistrict_returnsNotFoundResponse() {
    // Test Case ID: TC_SEARCH_DB_ADDRESS_005
    // Scenario ID: DB_EDGE_008
    // Objective: Verify district filter returns NOT_FOUND when the requested province has no districts.
    // Covered Branch/Path: districtRepo.findByProvinceCode -> empty list -> NOT_FOUND response.
    // DB Check: Persist a district for another province and verify requested province query is empty.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    District otherDistrict = new District();
    otherDistrict.setCode("Q1");
    otherDistrict.setName("Quan 1");
    otherDistrict.setProvinceCode("HCM");
    districtRepo.saveAndFlush(otherDistrict);

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse =
        addressService.getDistrictsByProvinceCode("HN");

    // Assert
    assertThat(districtRepo.findByProvinceCode("HN")).isEmpty();
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getStatus()).isEqualTo(NOT_FOUND);
    assertThat((List<?>) actualBody.getData()).isEmpty();
  }

  @Test
  void getWardsByProvinceCode_databaseHasMatchingWard_returnsFoundResponse() {
    // Test Case ID: TC_SEARCH_DB_ADDRESS_006
    // Scenario ID: DB_HAPPY_017
    // Objective: Verify ward filter returns matching wards for the requested province from the test database.
    // Covered Branch/Path: wardRepo.findByProvinceCode -> non-empty list -> FOUND response.
    // DB Check: Persist matching and non-matching wards, verify only requested province is returned.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    Ward matchingWard = new Ward();
    matchingWard.setCode("P02");
    matchingWard.setName("Phuong 2");
    matchingWard.setProvincecode("HN");
    wardRepo.save(matchingWard);
    Ward otherWard = new Ward();
    otherWard.setCode("P03");
    otherWard.setName("Phuong 3");
    otherWard.setProvincecode("HCM");
    wardRepo.saveAndFlush(otherWard);

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse =
        addressService.getWardsByProvinceCode("HN");

    // Assert
    assertThat(wardRepo.findByProvinceCode("HN")).hasSize(1);
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getStatus()).isEqualTo(FOUND);
    assertThat((List<?>) actualBody.getData()).extracting("code").containsExactly("P02");
  }

  @Test
  void getWardsByProvinceCode_databaseHasNoMatchingWard_returnsNotFoundResponse() {
    // Test Case ID: TC_SEARCH_DB_ADDRESS_003
    // Scenario ID: DB_EDGE_001
    // Objective: Verify no matching wards from the test database return NOT_FOUND.
    // Covered Branch/Path: wardRepo.findByProvinceCode -> empty list -> NOT_FOUND response.
    // DB Check: Persist a ward for another province and verify requested province query is empty.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    Ward ward = new Ward();
    ward.setCode("P01");
    ward.setName("Phuong 1");
    ward.setProvincecode("HCM");
    wardRepo.saveAndFlush(ward);

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse =
        addressService.getWardsByProvinceCode("HN");

    // Assert
    assertThat(wardRepo.findByProvinceCode("HN")).isEmpty();
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getStatus()).isEqualTo(NOT_FOUND);
    assertThat((List<?>) actualBody.getData()).isEmpty();
  }

  @Test
  void getOrgByUserId_databaseHasOrganization_returnsOrganization() {
    // Test Case ID: TC_SEARCH_DB_ORG_001
    // Scenario ID: DB_HAPPY_003
    // Objective: Verify organization detail is loaded by id from the test database.
    // Covered Branch/Path: findById -> Optional present -> return organization.
    // DB Check: Persist active organization and verify repository can read the same id.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    Organization expectedOrganization = organizationRepo.saveAndFlush(
        Organization.builder().name("FJob").active(1).build());

    // Act
    Organization actualOrganization = organizationService.getOrgByUserId(expectedOrganization.getId());

    // Assert
    assertThat(organizationRepo.findById(expectedOrganization.getId())).isPresent();
    assertThat(actualOrganization.getId()).isEqualTo(expectedOrganization.getId());
    assertThat(actualOrganization.getName()).isEqualTo("FJob");
  }

  @Test
  void aminGetOrgByUserId_databaseHasOrganization_returnsOrganizationForAdminPath() {
    // Test Case ID: TC_SEARCH_DB_ORG_008
    // Scenario ID: DB_HAPPY_011
    // Objective: Verify admin organization detail lookup reads the organization from the test database.
    // Covered Branch/Path: aminGetOrgByUserId -> findById present -> return organization.
    // DB Check: Persist active organization and verify the same row is returned by repository and service.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    Organization expectedOrganization = organizationRepo.saveAndFlush(
        Organization.builder().name("Admin Org").active(1).build());

    // Act
    Organization actualOrganization = organizationService.aminGetOrgByUserId(expectedOrganization.getId());

    // Assert
    assertThat(organizationRepo.findById(expectedOrganization.getId())).isPresent();
    assertThat(actualOrganization.getId()).isEqualTo(expectedOrganization.getId());
    assertThat(actualOrganization.getName()).isEqualTo("Admin Org");
  }

  @Test
  void aminGetOrgByUserId_missingOrganization_throwsNotFoundWithoutDatabaseWrite() {
    // Test Case ID: TC_SEARCH_DB_ORG_009
    // Scenario ID: DB_SAD_003
    // Objective: Verify admin missing organization lookup throws NOT_FOUND and does not change database data.
    // Covered Branch/Path: aminGetOrgByUserId -> findById empty -> IllegalArgumentException.
    // DB Check: Verify organization table remains empty after failed admin lookup.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    Long inputOrganizationId = 405L;

    // Act
    IllegalArgumentException actualException = assertThrows(IllegalArgumentException.class,
        () -> organizationService.aminGetOrgByUserId(inputOrganizationId));

    // Assert
    assertThat(actualException.getMessage()).isEqualTo(NOT_FOUND);
    assertThat(organizationRepo.count()).isZero();
  }

  @Test
  void getOrg_databaseHasNameMatches_returnsPagedOrganizationResponse() {
    // Test Case ID: TC_SEARCH_DB_ORG_002
    // Scenario ID: DB_HAPPY_004
    // Objective: Verify organization search by company keyword reads matching active organizations.
    // Covered Branch/Path: id null -> non-empty name -> findByName branch.
    // DB Check: Persist matching active organization and verify repository query finds it by lower-case keyword.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    organizationRepo.saveAndFlush(Organization.builder().name("FJob Company").active(1).build());
    OrgSearchingDTO inputSearch = buildOrganizationSearch(1, 10);
    inputSearch.setName("FJOB");

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse = organizationService.getOrg(inputSearch);

    // Assert
    assertThat(organizationRepo.findByName("fjob", PageRequest.of(0, 10)).getContent()).hasSize(1);
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getTotalCount()).isEqualTo(1L);
    assertThat((List<?>) actualBody.getData()).extracting("name").containsExactly("FJob Company");
  }

  @Test
  void getOrg_databaseHasRequestedId_returnsSingleOrganizationResponse() {
    // Test Case ID: TC_SEARCH_DB_ORG_010
    // Scenario ID: DB_HAPPY_012
    // Objective: Verify organization search by id returns exactly the requested organization row.
    // Covered Branch/Path: id present -> findById branch -> FOUND response with one organization.
    // DB Check: Persist two organizations and verify only the requested id is returned.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    Organization expectedOrganization = organizationRepo.saveAndFlush(
        Organization.builder().name("Target Org").active(1).build());
    organizationRepo.saveAndFlush(Organization.builder().name("Other Org").active(1).build());
    OrgSearchingDTO inputSearch = buildOrganizationSearch(1, 10);
    inputSearch.setId(expectedOrganization.getId());

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse = organizationService.getOrg(inputSearch);

    // Assert
    assertThat(organizationRepo.count()).isEqualTo(2L);
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getTotalCount()).isEqualTo(1L);
    assertThat((List<?>) actualBody.getData()).extracting("id")
        .containsExactly(expectedOrganization.getId());
  }

  @Test
  void getOrg_noNameOrId_returnsDefaultPagedOrganizations() {
    // Test Case ID: TC_SEARCH_DB_ORG_011
    // Scenario ID: DB_HAPPY_013
    // Objective: Verify default organization listing reads active organizations from the test database.
    // Covered Branch/Path: id null -> name blank/null -> findAll pageable branch.
    // DB Check: Persist two active organizations and verify both are returned on the first page.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    organizationRepo.saveAndFlush(Organization.builder().name("Default Org 1").active(1).build());
    organizationRepo.saveAndFlush(Organization.builder().name("Default Org 2").active(1).build());
    OrgSearchingDTO inputSearch = buildOrganizationSearch(1, 10);

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse = organizationService.getOrg(inputSearch);

    // Assert
    assertThat(organizationRepo.findAll(PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2L);
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getTotalCount()).isEqualTo(2L);
    assertThat((List<?>) actualBody.getData()).extracting("name")
        .containsExactly("Default Org 1", "Default Org 2");
  }

  @Test
  void save_newOrganization_persistsActiveOrganizationInDatabase() {
    // Test Case ID: TC_SEARCH_DB_ORG_003
    // Scenario ID: DB_HAPPY_005
    // Objective: Verify saving a new organization writes expected data to the test database.
    // Covered Branch/Path: findByName null -> id null -> set active/creation date -> save.
    // DB Check: Verify repository count increases and persisted organization has expected fields.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    OrgInputDTO inputOrganization = new OrgInputDTO();
    inputOrganization.setName("New Org");
    inputOrganization.setDes("Searchable company");

    // Act
    ResponseObject actualResponse = organizationService.save(inputOrganization);

    // Assert
    assertThat(actualResponse.getStatus()).isEqualTo(SUCCESS);
    Organization actualSavedOrganization = organizationRepo.findByName("New Org");
    assertThat(actualSavedOrganization).isNotNull();
    assertThat(actualSavedOrganization.getDescription()).isEqualTo("Searchable company");
    assertThat(actualSavedOrganization.getActive()).isEqualTo(1);
    assertThat(actualSavedOrganization.getCreationDate()).isNotNull();
  }

  @Test
  void save_existingOrganization_returnsExistedAndDoesNotInsertDuplicate() {
    // Test Case ID: TC_SEARCH_DB_ORG_004
    // Scenario ID: DB_SAD_001
    // Objective: Verify duplicate organization name is rejected without a second insert.
    // Covered Branch/Path: findByName returns existing organization -> EXISTED response.
    // DB Check: Persist existing organization and verify count remains one after save attempt.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    organizationRepo.saveAndFlush(Organization.builder().name("FJob").active(1).build());
    OrgInputDTO inputOrganization = new OrgInputDTO();
    inputOrganization.setName("FJob");

    // Act
    ResponseObject actualResponse = organizationService.save(inputOrganization);

    // Assert
    assertThat(actualResponse.getStatus()).isEqualTo(FOUND);
    assertThat(actualResponse.getCode()).isEqualTo(EXISTED);
    assertThat(organizationRepo.findByName("fjob", PageRequest.of(0, 10)).getTotalElements())
        .isEqualTo(1L);
  }

  @Test
  void updateRecruiterStatus_existingRecruiter_updatesDatabaseStatus() {
    // Test Case ID: TC_SEARCH_DB_ORG_005
    // Scenario ID: DB_HAPPY_006
    // Objective: Verify recruiter status update is persisted to the test database.
    // Covered Branch/Path: valid enum -> findById present -> set status -> save.
    // DB Check: Persist recruiter configuration, update it, and verify status from repository.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    RecruiterConfiguration configuration = new RecruiterConfiguration(null, 100L,
        com.jober.searchservice.utilsmodule.RecruiterStatus.PENDING);
    configuration = recruiterConfigurationRepository.saveAndFlush(configuration);

    // Act
    organizationService.updateRecruiterStatus(configuration.getCusId(), "APPROVED");

    // Assert
    RecruiterConfiguration actualConfiguration =
        recruiterConfigurationRepository.findById(configuration.getCusId()).orElseThrow();
    assertThat(actualConfiguration.getStatus()).isEqualTo(APPROVED);
  }

  @Test
  void updateRecruiterStatus_missingRecruiter_doesNotWriteDatabase() {
    // Test Case ID: TC_SEARCH_DB_ORG_012
    // Scenario ID: DB_EDGE_003
    // Objective: Verify status update for a missing recruiter leaves database state unchanged.
    // Covered Branch/Path: valid enum -> findById empty -> skip save branch.
    // DB Check: Verify recruiter configuration count remains zero after update attempt.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    Long inputRecruiterId = 999L;
    String inputStatus = "APPROVED";

    // Act
    organizationService.updateRecruiterStatus(inputRecruiterId, inputStatus);

    // Assert
    assertThat(recruiterConfigurationRepository.count()).isZero();
  }

  @Test
  void getAllRecruiterByOrganization_databaseHasRecruiters_returnsPagedRecruiters() {
    // Test Case ID: TC_SEARCH_DB_ORG_006
    // Scenario ID: DB_HAPPY_007
    // Objective: Verify recruiter list by organization uses real joins across organization, user, and config tables.
    // Covered Branch/Path: valid size/offset -> fromIndex within totalElements -> subList.
    // DB Check: Persist organization, active user, recruiter configuration and verify joined DTO fields.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    Organization organization = organizationRepo.saveAndFlush(
        Organization.builder().name("FJob").active(1).build());
    UserCommon user = UserCommon.builder()
        .name("Recruiter A")
        .email("a@example.com")
        .phone("123")
        .organizationId(organization.getId())
        .active(1)
        .build();
    entityManager.persist(user);
    entityManager.flush();
    recruiterConfigurationRepository.saveAndFlush(
        new RecruiterConfiguration(user.getId(), organization.getId(), APPROVED));

    // Act
    PageResponse<RecruiterCompany> actualResponse =
        organizationService.getAllRecruiterByOrganization(10, 0, "FJob");

    // Assert
    assertThat(organizationRepo.getAllRecruiter("FJob")).hasSize(1);
    assertThat(actualResponse.getTotalElement()).isEqualTo(1);
    assertThat(actualResponse.getData()).hasSize(1);
    assertThat(actualResponse.getData().get(0).getEmail()).isEqualTo("a@example.com");
    assertThat(actualResponse.getData().get(0).getStatus()).isEqualTo(APPROVED);
  }

  @Test
  void getAllRecruiterByOrganization_offsetPastData_returnsEmptyPageWithoutChangingDatabase() {
    // Test Case ID: TC_SEARCH_DB_ORG_013
    // Scenario ID: DB_EDGE_004
    // Objective: Verify recruiter pagination returns an empty data page when the offset is outside result range.
    // Covered Branch/Path: default page size -> pageIndex offset -> fromIndex >= totalElements -> empty list.
    // DB Check: Persist one joined recruiter row and verify it is not modified by paged read.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    Organization organization = organizationRepo.saveAndFlush(
        Organization.builder().name("Paging Org").active(1).build());
    UserCommon user = UserCommon.builder()
        .name("Recruiter Paging")
        .email("paging@example.com")
        .phone("456")
        .organizationId(organization.getId())
        .active(1)
        .build();
    entityManager.persist(user);
    entityManager.flush();
    recruiterConfigurationRepository.saveAndFlush(
        new RecruiterConfiguration(user.getId(), organization.getId(), APPROVED));

    // Act
    PageResponse<RecruiterCompany> actualResponse =
        organizationService.getAllRecruiterByOrganization(null, 2, "Paging");

    // Assert
    assertThat(organizationRepo.getAllRecruiter("Paging")).hasSize(1);
    assertThat(actualResponse.getTotalElement()).isEqualTo(1);
    assertThat(actualResponse.getPageSize()).isEqualTo(10);
    assertThat(actualResponse.getPageIndex()).isEqualTo(2);
    assertThat(actualResponse.getData()).isEmpty();
  }

  @Test
  void getDataSearch_getAllTrue_databaseHasSuggestions_returnsAllSuggestions() {
    // Test Case ID: TC_SEARCH_DB_SUGGESTION_001
    // Scenario ID: DB_HAPPY_008
    // Objective: Verify all search suggestions are read from the test database.
    // Covered Branch/Path: parsed input -> isGetAll true -> findSearchingSuggestion list branch.
    // DB Check: Persist one suggestion and verify service reads it from repository.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    searchingSuggestionRepo.saveAndFlush(buildSuggestion("java", "job", 1));
    String inputBody = "{\"paging\":{\"page\":1,\"size\":10},\"isGetAll\":true}";

    // Act
    ResponseEntitySerializable actualResponse = searchingSuggestionService.getDataSearch(inputBody);

    // Assert
    assertThat(searchingSuggestionRepo.findSearchingSuggestion()).hasSize(1);
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getTotalCount()).isEqualTo(1L);
    assertThat((List<?>) actualBody.getData()).extracting("val").containsExactly("java");
  }

  @Test
  void getDataSearch_getAllFalse_databaseHasSuggestions_returnsPagedSuggestions() {
    // Test Case ID: TC_SEARCH_DB_SUGGESTION_005
    // Scenario ID: DB_HAPPY_014
    // Objective: Verify paged search suggestions are read from the test database with total count preserved.
    // Covered Branch/Path: parsed input -> isGetAll false -> repository page branch.
    // DB Check: Persist three suggestions and verify page size two with total count three.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    searchingSuggestionRepo.saveAndFlush(buildSuggestion("java", "job", 1));
    searchingSuggestionRepo.saveAndFlush(buildSuggestion("python", "job", 1));
    searchingSuggestionRepo.saveAndFlush(buildSuggestion("golang", "job", 1));
    String inputBody = "{\"paging\":{\"page\":1,\"size\":2},\"isGetAll\":false}";

    // Act
    ResponseEntitySerializable actualResponse = searchingSuggestionService.getDataSearch(inputBody);

    // Assert
    assertThat(searchingSuggestionRepo.count()).isEqualTo(3L);
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getCurrentCount()).isEqualTo(2);
    assertThat(actualBody.getTotalCount()).isEqualTo(3L);
  }

  @Test
  void addDataSearch_validSuggestion_persistsSuggestionInDatabase() {
    // Test Case ID: TC_SEARCH_DB_SUGGESTION_002
    // Scenario ID: DB_HAPPY_009
    // Objective: Verify adding a search suggestion writes to the test database.
    // Covered Branch/Path: repository save success -> OK response.
    // DB Check: Call service save and verify repository can query persisted value.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    SearchingSuggestion inputSuggestion = buildSuggestion("tester", "job", 2);

    // Act
    ResponseEntitySerializable actualResponse = searchingSuggestionService.addDataSearch(inputSuggestion);

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(searchingSuggestionRepo.findSearchingSuggestionByMatchCondition("tester", "job"))
        .isNotNull();
  }

  @Test
  void getObjectSearch_cacheMiss_databaseHasMatches_returnsAndCachesSuggestions() {
    // Test Case ID: TC_SEARCH_DB_SUGGESTION_003
    // Scenario ID: DB_HAPPY_010
    // Objective: Verify cache miss reads matching suggestions from the test database.
    // Covered Branch/Path: cache miss -> repository returns list -> cache put.
    // DB Check: Persist matching suggestion and verify repository condition query returns it.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    searchingSuggestionRepo.saveAndFlush(buildSuggestion("java developer", "job", 5));

    // Act
    List<SearchingSuggestion> actualSuggestions =
        searchingSuggestionService.getObjectSearch("java", "job");

    // Assert
    assertThat(searchingSuggestionRepo.findSearchingSuggestionByCondition("java", "job")).hasSize(1);
    assertThat(actualSuggestions).hasSize(1);
    assertThat(actualSuggestions.get(0).getVal()).isEqualTo("java developer");
  }

  @Test
  void getDataSearchByCondition_databaseHasMatches_returnsMatchingSuggestionsAndCachesResult() {
    // Test Case ID: TC_SEARCH_DB_SUGGESTION_006
    // Scenario ID: DB_HAPPY_015
    // Objective: Verify typed search condition returns matching suggestions from the database and stores cache.
    // Covered Branch/Path: getDataSearchByCondition -> getObjectSearch cache miss -> repository condition query.
    // DB Check: Persist two matching rows and one different object row; verify only matching object rows return.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    searchingSuggestionRepo.saveAndFlush(buildSuggestion("java backend", "job", 5));
    searchingSuggestionRepo.saveAndFlush(buildSuggestion("java fresher", "job", 4));
    searchingSuggestionRepo.saveAndFlush(buildSuggestion("java street", "company", 1));
    SearchingSuggestion inputCondition = buildSuggestion("java", "job", null);

    // Act
    ResponseEntitySerializable actualResponse =
        searchingSuggestionService.getDataSearchByCondition(inputCondition);

    // Assert
    assertThat(searchingSuggestionRepo.findSearchingSuggestionByCondition("java", "job")).hasSize(2);
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getCurrentCount()).isEqualTo(2);
    assertThat((List<?>) actualBody.getData()).extracting("object").containsOnly("job");
  }

  @Test
  void getObjectSearch_cacheHit_returnsCachedSuggestionsWithoutDatabaseRows() {
    // Test Case ID: TC_SEARCH_DB_SUGGESTION_007
    // Scenario ID: DB_EDGE_005
    // Objective: Verify suggestion lookup uses cache hit even after backing DB rows are removed inside the test.
    // Covered Branch/Path: getObjectSearch cache miss populates cache -> cache hit branch returns cached list.
    // DB Check: Delete backing rows after cache warm-up and verify DB is empty while service still returns cache data.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    searchingSuggestionRepo.saveAndFlush(buildSuggestion("cache java", "job", 3));
    List<SearchingSuggestion> expectedCachedSuggestions =
        searchingSuggestionService.getObjectSearch("cache", "job");
    searchingSuggestionRepo.deleteAll();
    searchingSuggestionRepo.flush();

    // Act
    List<SearchingSuggestion> actualSuggestions =
        searchingSuggestionService.getObjectSearch("cache", "job");

    // Assert
    assertThat(searchingSuggestionRepo.count()).isZero();
    assertThat(expectedCachedSuggestions).hasSize(1);
    assertThat(actualSuggestions).hasSize(1);
    assertThat(actualSuggestions.get(0).getVal()).isEqualTo("cache java");
  }

  @Test
  void getDataSearchByMatchCondition_noDatabaseMatch_createsSuggestionAndIncrementsRank() {
    // Test Case ID: TC_SEARCH_DB_SUGGESTION_004
    // Scenario ID: DB_EDGE_002
    // Objective: Verify exact-match search creates a new suggestion when no DB row exists.
    // Covered Branch/Path: cache miss -> repository returns null -> addOrUpdateObject create branch.
    // DB Check: Verify repository has the created suggestion with rank one after service call.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    SearchingSuggestion inputCondition = buildSuggestion("golang", "job", null);

    // Act
    ResponseEntitySerializable actualResponse =
        searchingSuggestionService.getDataSearchByMatchCondition(inputCondition);

    // Assert
    SearchingSuggestion actualSavedSuggestion =
        searchingSuggestionRepo.findSearchingSuggestionByMatchCondition("golang", "job");
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(actualSavedSuggestion).isNotNull();
    assertThat(actualSavedSuggestion.getRank()).isEqualTo(1);
    assertThat(actualSavedSuggestion.getCreationDate()).isNotNull();
  }

  @Test
  void addOrUpdateObject_existingSuggestion_incrementsRankAndUpdatesDatabase() {
    // Test Case ID: TC_SEARCH_DB_SUGGESTION_008
    // Scenario ID: DB_HAPPY_016
    // Objective: Verify existing suggestion ranking is incremented and persisted in the database.
    // Covered Branch/Path: addOrUpdateObject existing suggestion branch -> rank increment -> repository save.
    // DB Check: Persist rank two, call service update, then verify rank three through repository query.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    SearchingSuggestion inputExistingSuggestion = (SearchingSuggestion) searchingSuggestionRepo.saveAndFlush(
        buildSuggestion("spring", "job", 2));

    // Act
    SearchingSuggestion actualSuggestion =
        searchingSuggestionService.addOrUpdateObject("spring", "job", inputExistingSuggestion);

    // Assert
    SearchingSuggestion actualSavedSuggestion =
        searchingSuggestionRepo.findSearchingSuggestionByMatchCondition("spring", "job");
    assertThat(actualSuggestion.getRank()).isEqualTo(3);
    assertThat(actualSavedSuggestion).isNotNull();
    assertThat(actualSavedSuggestion.getRank()).isEqualTo(3);
    assertThat(actualSavedSuggestion.getUpdateDate()).isNotNull();
  }

  @Test
  void searchingSuggestionConverter_anyBody_returnsNullWithoutDatabaseAccess() {
    // Test Case ID: TC_SEARCH_DB_SUGGESTION_009
    // Scenario ID: DB_EDGE_006
    // Objective: Verify converter stub currently returns null and does not read or write database data.
    // Covered Branch/Path: searchingSuggestionConverter direct return path.
    // DB Check: Verify suggestion table count remains zero after converter call.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    String inputBody = "{\"val\":\"java\",\"object\":\"job\"}";

    // Act
    SearchingSuggestion actualSuggestion = searchingSuggestionService.searchingSuggestionConverter(inputBody);

    // Assert
    assertThat(actualSuggestion).isNull();
    assertThat(searchingSuggestionRepo.count()).isZero();
  }

  @Test
  void getOrgByUserId_missingOrganization_throwsNotFoundWithoutDatabaseWrite() {
    // Test Case ID: TC_SEARCH_DB_ORG_007
    // Scenario ID: DB_SAD_002
    // Objective: Verify missing organization lookup throws NOT_FOUND and does not write data.
    // Covered Branch/Path: findById -> Optional empty -> IllegalArgumentException.
    // DB Check: Verify organization count remains zero after failed lookup.
    // Rollback: Repository uses test database inside @Transactional test, so changes roll back after test.

    // Arrange
    Long inputOrganizationId = 404L;

    // Act
    IllegalArgumentException actualException = assertThrows(IllegalArgumentException.class,
        () -> organizationService.getOrgByUserId(inputOrganizationId));

    // Assert
    assertThat(actualException.getMessage()).isEqualTo(NOT_FOUND);
    assertThat(organizationRepo.count()).isZero();
  }

  private OrgSearchingDTO buildOrganizationSearch(int page, int size) {
    PageableModel paging = new PageableModel();
    paging.setPage(page);
    paging.setSize(size);
    OrgSearchingDTO search = new OrgSearchingDTO();
    search.setPaging(paging);
    return search;
  }

  private SearchingSuggestion buildSuggestion(String value, String object, Integer rank) {
    SearchingSuggestion suggestion = new SearchingSuggestion();
    suggestion.setVal(value);
    suggestion.setObject(object);
    suggestion.setRank(rank);
    suggestion.setCreationDate(LocalDateTime.now());
    suggestion.setUpdateDate(LocalDateTime.now());
    return suggestion;
  }
}
