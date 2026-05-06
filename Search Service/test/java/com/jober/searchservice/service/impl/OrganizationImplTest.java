package com.jober.searchservice.service.impl;

import static com.jober.searchservice.utilsmodule.RecruiterStatus.APPROVED;
import static com.jober.searchservice.utilsmodule.RecruiterStatus.PENDING;
import static com.jober.utilsservice.constant.Constant.ACTIVE;
import static com.jober.utilsservice.constant.ResponseMessageConstant.EXISTED;
import static com.jober.utilsservice.constant.ResponseMessageConstant.FOUND;
import static com.jober.utilsservice.constant.ResponseMessageConstant.NOT_FOUND;
import static com.jober.utilsservice.constant.ResponseMessageConstant.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.jober.searchservice.dto.PageResponse;
import com.jober.searchservice.dto.RecruiterCompany;
import com.jober.searchservice.model.Organization;
import com.jober.searchservice.model.RecruiterConfiguration;
import com.jober.searchservice.repository.OrganizationRepo;
import com.jober.searchservice.repository.RecruiterConfigurationRepository;
import com.jober.utilsservice.dto.OrgInputDTO;
import com.jober.utilsservice.dto.OrgSearchingDTO;
import com.jober.utilsservice.errors.ResponseEntitySerializable;
import com.jober.utilsservice.model.PageableModel;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrganizationImplTest {

  private OrganizationImpl organizationService;

  @Mock
  private OrganizationRepo organizationRepo;
  @Mock
  private RecruiterConfigurationRepository recruiterConfigurationRepository;

  @BeforeEach
  void setUp() {
    organizationService = new OrganizationImpl();
    ReflectionTestUtils.setField(organizationService, "organizationRepo", organizationRepo);
    ReflectionTestUtils.setField(organizationService, "recruiterConfigurationRepository",
        recruiterConfigurationRepository);
  }

  @Test
  void getOrgByUserId_existingOrganization_returnsOrganization() {
    // Test Case ID: TC_SEARCH_ORG_001
    // Scenario ID: HAPPY_001
    // Objective: Verify organization detail is returned when the organization id exists.
    // Covered Branch/Path: findById -> Optional present -> return organization.
    // DB Check: Verify organizationRepo.findById is called with requested id.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    Long inputOrganizationId = 10L;
    Organization expectedOrganization = Organization.builder().id(inputOrganizationId).name("FJob").build();
    when(organizationRepo.findById(inputOrganizationId)).thenReturn(Optional.of(expectedOrganization));

    // Act
    Organization actualOrganization = organizationService.getOrgByUserId(inputOrganizationId);

    // Assert
    assertThat(actualOrganization).isEqualTo(expectedOrganization);
    verify(organizationRepo, times(1)).findById(inputOrganizationId);
    verifyNoMoreInteractions(organizationRepo);
  }

  @Test
  void getOrgByUserId_missingOrganization_throwsNotFoundException() {
    // Test Case ID: TC_SEARCH_ORG_002
    // Scenario ID: SAD_001
    // Objective: Verify missing organization id is rejected with NOT_FOUND.
    // Covered Branch/Path: findById -> Optional empty -> IllegalArgumentException.
    // DB Check: Verify one read by id and no write operation.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    Long inputOrganizationId = 404L;
    when(organizationRepo.findById(inputOrganizationId)).thenReturn(Optional.empty());

    // Act
    IllegalArgumentException actualException = assertThrows(IllegalArgumentException.class,
        () -> organizationService.getOrgByUserId(inputOrganizationId));

    // Assert
    assertThat(actualException.getMessage()).isEqualTo(NOT_FOUND);
    verify(organizationRepo, times(1)).findById(inputOrganizationId);
    verify(organizationRepo, never()).save(any());
  }

  @Test
  void aminGetOrgByUserId_existingOrganization_returnsOrganization() {
    // Test Case ID: TC_SEARCH_ORG_003
    // Scenario ID: HAPPY_002
    // Objective: Verify admin lookup returns organization when id exists.
    // Covered Branch/Path: findById -> Optional present -> return organization.
    // DB Check: Verify organizationRepo.findById is called with requested id.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    Long inputOrganizationId = 11L;
    Organization expectedOrganization = Organization.builder().id(inputOrganizationId).name("Admin Org").build();
    when(organizationRepo.findById(inputOrganizationId)).thenReturn(Optional.of(expectedOrganization));

    // Act
    Organization actualOrganization = organizationService.aminGetOrgByUserId(inputOrganizationId);

    // Assert
    assertThat(actualOrganization).isEqualTo(expectedOrganization);
    verify(organizationRepo, times(1)).findById(inputOrganizationId);
    verifyNoMoreInteractions(organizationRepo);
  }

  @Test
  void aminGetOrgByUserId_missingOrganization_throwsNotFoundException() {
    // Test Case ID: TC_SEARCH_ORG_015
    // Scenario ID: SAD_004
    // Objective: Verify admin lookup rejects a missing organization id with NOT_FOUND.
    // Covered Branch/Path: admin findById -> Optional empty -> IllegalArgumentException.
    // DB Check: Verify one read by id and no write operation.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    Long inputOrganizationId = 405L;
    when(organizationRepo.findById(inputOrganizationId)).thenReturn(Optional.empty());

    // Act
    IllegalArgumentException actualException = assertThrows(IllegalArgumentException.class,
        () -> organizationService.aminGetOrgByUserId(inputOrganizationId));

    // Assert
    assertThat(actualException.getMessage()).isEqualTo(NOT_FOUND);
    verify(organizationRepo, times(1)).findById(inputOrganizationId);
    verify(organizationRepo, never()).save(any());
  }

  @Test
  void getOrg_idProvided_returnsSingleOrganizationResponse() {
    // Test Case ID: TC_SEARCH_ORG_004
    // Scenario ID: HAPPY_003
    // Objective: Verify organization search by id returns one organization.
    // Covered Branch/Path: orgSearchingDTO.id != null -> findById branch.
    // DB Check: Verify read by id and no name/default search query.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    OrgSearchingDTO inputSearch = buildOrganizationSearch(1, 10);
    inputSearch.setId(20L);
    Organization expectedOrganization = Organization.builder().id(20L).name("FJob").build();
    when(organizationRepo.findById(inputSearch.getId())).thenReturn(Optional.of(expectedOrganization));

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse = organizationService.getOrg(inputSearch);

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getStatus()).isEqualTo(FOUND);
    assertThat(actualBody.getTotalCount()).isEqualTo(1L);
    assertThat((List<?>) actualBody.getData()).hasSize(1);
    assertThat(((List<?>) actualBody.getData()).get(0)).isEqualTo(expectedOrganization);
    verify(organizationRepo, times(1)).findById(inputSearch.getId());
    verify(organizationRepo, never()).findByName(anyString(), any());
  }

  @Test
  void getOrg_missingId_rejectsWithNotFoundBusinessErrorWithoutWrite() {
    // Test Case ID: TC_SEARCH_ORG_019
    // Scenario ID: SAD_005
    // Objective: Verify organization search by missing id returns a clear NOT_FOUND business error.
    // Assumption: Missing id should surface the same NOT_FOUND business error as getOrgByUserId.
    // Covered Branch/Path: orgSearchingDTO.id != null -> findById empty -> NOT_FOUND rejection.
    // DB Check: Verify one read by id and no save/write operation occurs.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    OrgSearchingDTO inputSearch = buildOrganizationSearch(1, 10);
    inputSearch.setId(999L);
    String expectedErrorMessage = NOT_FOUND;
    logEvidence("mvn -pl search-service -DskipTests=false -Dtest=OrganizationImplTest test",
        "TC_SEARCH_ORG_019 expects missing organization id to fail with NOT_FOUND, not Optional.get crash");
    when(organizationRepo.findById(inputSearch.getId())).thenReturn(Optional.empty());

    // Act
    Throwable actualThrowable = catchThrowable(() -> organizationService.getOrg(inputSearch));

    // Assert
    verify(organizationRepo, times(1)).findById(inputSearch.getId());
    verify(organizationRepo, never()).save(any());
    verify(organizationRepo, never()).findByName(anyString(), any());
    assertThat(actualThrowable)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedErrorMessage);
  }

  @Test
  void getOrg_nullPaging_rejectsRequestBeforeDatabaseRead() {
    // Test Case ID: TC_SEARCH_ORG_020
    // Scenario ID: SAD_006
    // Objective: Verify organization search rejects missing paging before accessing data.
    // Assumption: Missing paging should be rejected as invalid input, not as an unhandled NullPointerException.
    // Covered Branch/Path: orgSearchingDTO.paging null -> validation rejection -> no repository read.
    // DB Check: Verify organization repository is not called when paging is invalid.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    OrgSearchingDTO inputSearch = new OrgSearchingDTO();
    String expectedErrorMessageFragment = "paging";
    logEvidence("mvn -pl search-service -DskipTests=false -Dtest=OrganizationImplTest test",
        "TC_SEARCH_ORG_020 expects missing paging to be rejected before DB read");

    // Act
    Throwable actualThrowable = catchThrowable(() -> organizationService.getOrg(inputSearch));

    // Assert
    verifyNoInteractions(organizationRepo);
    verifyNoInteractions(recruiterConfigurationRepository);
    assertThat(actualThrowable)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(expectedErrorMessageFragment);
  }

  @Test
  void getOrg_nameProvided_returnsPagedOrganizationResponse() {
    // Test Case ID: TC_SEARCH_ORG_005
    // Scenario ID: HAPPY_004
    // Objective: Verify organization search by company keyword returns matching organizations.
    // Covered Branch/Path: id null -> non-empty name -> findByName branch.
    // DB Check: Verify lower-cased name and expected pageable are passed to repository.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    OrgSearchingDTO inputSearch = buildOrganizationSearch(2, 5);
    inputSearch.setName("FJOB");
    Organization expectedOrganization = Organization.builder().id(21L).name("FJob").build();
    Page<Organization> expectedPage = new PageImpl<>(List.of(expectedOrganization),
        PageRequest.of(1, 5), 6);
    when(organizationRepo.findByName("fjob", PageRequest.of(1, 5))).thenReturn(expectedPage);

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse = organizationService.getOrg(inputSearch);

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getTotalCount()).isEqualTo(6L);
    assertThat(actualBody.getCurrentCount()).isEqualTo(1);
    assertThat((List<?>) actualBody.getData()).hasSize(1);
    assertThat(((List<?>) actualBody.getData()).get(0)).isEqualTo(expectedOrganization);
    verify(organizationRepo, times(1)).findByName("fjob", PageRequest.of(1, 5));
    verify(organizationRepo, never()).findAll(PageRequest.of(1, 5));
  }

  @Test
  void getOrg_noIdAndNoName_returnsDefaultPagedOrganizationResponse() {
    // Test Case ID: TC_SEARCH_ORG_006
    // Scenario ID: HAPPY_005
    // Objective: Verify default organization list is returned when no id/name filter is provided.
    // Covered Branch/Path: id null -> name null/empty -> findAll branch.
    // DB Check: Verify findAll receives the requested pageable.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    OrgSearchingDTO inputSearch = buildOrganizationSearch(1, 10);
    Organization expectedOrganization = Organization.builder().id(30L).name("Default Org").build();
    Page<Organization> expectedPage = new PageImpl<>(List.of(expectedOrganization),
        PageRequest.of(0, 10), 1);
    when(organizationRepo.findAll(PageRequest.of(0, 10))).thenReturn(expectedPage);

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse = organizationService.getOrg(inputSearch);

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getTotalCount()).isEqualTo(1L);
    assertThat((List<?>) actualBody.getData()).hasSize(1);
    assertThat(((List<?>) actualBody.getData()).get(0)).isEqualTo(expectedOrganization);
    verify(organizationRepo, times(1)).findAll(PageRequest.of(0, 10));
    verify(organizationRepo, never()).findByName(anyString(), any());
  }

  @Test
  void getOrg_emptyName_returnsDefaultPagedOrganizationResponse() {
    // Test Case ID: TC_SEARCH_ORG_016
    // Scenario ID: EDGE_004
    // Objective: Verify empty organization name is treated as no name filter.
    // Covered Branch/Path: id null -> name non-null but empty -> findAll branch.
    // DB Check: Verify findAll is used and name query is not called.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    OrgSearchingDTO inputSearch = buildOrganizationSearch(1, 10);
    inputSearch.setName("");
    Organization expectedOrganization = Organization.builder().id(31L).name("Default Org").build();
    Page<Organization> expectedPage = new PageImpl<>(List.of(expectedOrganization),
        PageRequest.of(0, 10), 1);
    when(organizationRepo.findAll(PageRequest.of(0, 10))).thenReturn(expectedPage);

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse = organizationService.getOrg(inputSearch);

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat((List<?>) actualBody.getData()).hasSize(1);
    verify(organizationRepo, times(1)).findAll(PageRequest.of(0, 10));
    verify(organizationRepo, never()).findByName(anyString(), any());
  }

  @Test
  void save_existingOrganization_returnsExistedWithoutSaving() {
    // Test Case ID: TC_SEARCH_ORG_007
    // Scenario ID: SAD_002
    // Objective: Verify duplicate organization name is not saved.
    // Covered Branch/Path: findByName returns existing organization -> EXISTED response.
    // DB Check: Verify duplicate check occurs and save is never called.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    OrgInputDTO inputOrganization = new OrgInputDTO();
    inputOrganization.setName("FJob");
    Organization existingOrganization = Organization.builder().id(100L).name("FJob").build();
    when(organizationRepo.findByName(inputOrganization.getName())).thenReturn(existingOrganization);

    // Act
    ResponseObject actualResponse = organizationService.save(inputOrganization);

    // Assert
    assertThat(actualResponse.getStatus()).isEqualTo(FOUND);
    assertThat(actualResponse.getCode()).isEqualTo(EXISTED);
    assertThat(actualResponse.getData()).isEqualTo(existingOrganization);
    verify(organizationRepo, times(1)).findByName(inputOrganization.getName());
    verify(organizationRepo, never()).save(any());
  }

  @Test
  void save_newOrganization_returnsSuccessAndPersistsActiveOrganization() {
    // Test Case ID: TC_SEARCH_ORG_008
    // Scenario ID: HAPPY_006
    // Objective: Verify a new organization is created when its name does not exist.
    // Covered Branch/Path: findByName null -> id null -> set active/creation date -> save.
    // DB Check: Capture saved organization and verify name, description, active flag and dates.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    OrgInputDTO inputOrganization = new OrgInputDTO();
    inputOrganization.setName("New Org");
    inputOrganization.setDes("Searchable company");
    when(organizationRepo.findByName(inputOrganization.getName())).thenReturn(null);
    when(organizationRepo.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    ResponseObject actualResponse = organizationService.save(inputOrganization);

    // Assert
    assertThat(actualResponse.getStatus()).isEqualTo(SUCCESS);
    ArgumentCaptor<Organization> organizationCaptor = ArgumentCaptor.forClass(Organization.class);
    verify(organizationRepo, times(1)).findByName(inputOrganization.getName());
    verify(organizationRepo, times(1)).save(organizationCaptor.capture());
    Organization actualSavedOrganization = organizationCaptor.getValue();
    assertThat(actualSavedOrganization.getName()).isEqualTo(inputOrganization.getName());
    assertThat(actualSavedOrganization.getDescription()).isEqualTo(inputOrganization.getDes());
    assertThat(actualSavedOrganization.getActive()).isEqualTo(ACTIVE);
    assertThat(actualSavedOrganization.getCreationDate()).isNotNull();
    assertThat(actualSavedOrganization.getUpdateDate()).isNotNull();
    assertThat(actualResponse.getData()).isEqualTo(actualSavedOrganization);
  }

  @Test
  void save_existingIdOrganization_persistsWithoutCreationDefaults() {
    // Test Case ID: TC_SEARCH_ORG_017
    // Scenario ID: EDGE_005
    // Objective: Verify saving an input with id follows the update path.
    // Covered Branch/Path: findByName null -> id non-null -> skip active/creation date defaults -> save.
    // DB Check: Capture saved organization and verify active/creationDate are not initialized in update path.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    OrgInputDTO inputOrganization = new OrgInputDTO();
    inputOrganization.setId(88L);
    inputOrganization.setName("Existing Org");
    inputOrganization.setDes("Updated description");
    when(organizationRepo.findByName(inputOrganization.getName())).thenReturn(null);
    when(organizationRepo.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    ResponseObject actualResponse = organizationService.save(inputOrganization);

    // Assert
    assertThat(actualResponse.getStatus()).isEqualTo(SUCCESS);
    ArgumentCaptor<Organization> organizationCaptor = ArgumentCaptor.forClass(Organization.class);
    verify(organizationRepo, times(1)).findByName(inputOrganization.getName());
    verify(organizationRepo, times(1)).save(organizationCaptor.capture());
    Organization actualSavedOrganization = organizationCaptor.getValue();
    assertThat(actualSavedOrganization.getName()).isEqualTo(inputOrganization.getName());
    assertThat(actualSavedOrganization.getDescription()).isEqualTo(inputOrganization.getDes());
    assertThat(actualSavedOrganization.getActive()).isNull();
    assertThat(actualSavedOrganization.getCreationDate()).isNull();
    assertThat(actualSavedOrganization.getUpdateDate()).isNotNull();
  }

  @Test
  void save_nullInput_rejectsRequestWithoutDatabaseRead() {
    // Test Case ID: TC_SEARCH_ORG_021
    // Scenario ID: SAD_007
    // Objective: Verify null organization input is rejected before checking duplicates or saving.
    // Assumption: Null organization input should be rejected before repository access.
    // Covered Branch/Path: orgInputDTO null -> validation rejection -> no repository read/write.
    // DB Check: Verify organization repository is never called for invalid input.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    OrgInputDTO inputOrganization = null;
    String expectedErrorMessageFragment = "organization";
    logEvidence("mvn -pl search-service -DskipTests=false -Dtest=OrganizationImplTest test",
        "TC_SEARCH_ORG_021 expects null organization save input to be rejected before DB access");

    // Act
    Throwable actualThrowable = catchThrowable(() -> organizationService.save(inputOrganization));

    // Assert
    verifyNoInteractions(organizationRepo);
    verifyNoInteractions(recruiterConfigurationRepository);
    assertThat(actualThrowable)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(expectedErrorMessageFragment);
  }

  @Test
  void save_blankName_rejectsRequestWithoutSaving() {
    // Test Case ID: TC_SEARCH_ORG_022
    // Scenario ID: SAD_008
    // Objective: Verify blank organization name is rejected and no organization is persisted.
    // Assumption: Blank organization name is invalid for search/filter company data.
    // Covered Branch/Path: orgInputDTO.name blank -> validation rejection -> no save.
    // DB Check: Verify no duplicate lookup or save occurs for invalid organization name.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    OrgInputDTO inputOrganization = new OrgInputDTO();
    inputOrganization.setName(" ");
    String expectedErrorMessageFragment = "name";
    logEvidence("mvn -pl search-service -DskipTests=false -Dtest=OrganizationImplTest test",
        "TC_SEARCH_ORG_022 expects blank organization name to be rejected before DB access");

    // Act
    Throwable actualThrowable = catchThrowable(() -> organizationService.save(inputOrganization));

    // Assert
    verifyNoInteractions(organizationRepo);
    verifyNoInteractions(recruiterConfigurationRepository);
    assertThat(actualThrowable)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(expectedErrorMessageFragment);
  }

  @Test
  void save_existingIdOrganization_preservesInputIdWhenPersistingUpdate() {
    // Test Case ID: TC_SEARCH_ORG_023
    // Scenario ID: EDGE_007
    // Objective: Verify update path preserves the organization id instead of creating an id-less entity.
    // Assumption: When save receives an id, it represents an update and should preserve that id.
    // Covered Branch/Path: findByName null -> id non-null -> save update data with same id.
    // DB Check: Capture saved organization and verify saved id equals input id.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    OrgInputDTO inputOrganization = new OrgInputDTO();
    inputOrganization.setId(88L);
    inputOrganization.setName("Existing Org");
    inputOrganization.setDes("Updated description");
    Long expectedOrganizationId = inputOrganization.getId();
    logEvidence("mvn -pl search-service -DskipTests=false -Dtest=OrganizationImplTest test",
        "TC_SEARCH_ORG_023 expects update save path to preserve organization id");
    when(organizationRepo.findByName(inputOrganization.getName())).thenReturn(null);
    when(organizationRepo.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    ResponseObject actualResponse = organizationService.save(inputOrganization);

    // Assert
    assertThat(actualResponse.getStatus()).isEqualTo(SUCCESS);
    ArgumentCaptor<Organization> organizationCaptor = ArgumentCaptor.forClass(Organization.class);
    verify(organizationRepo, times(1)).findByName(inputOrganization.getName());
    verify(organizationRepo, times(1)).save(organizationCaptor.capture());
    Organization actualSavedOrganization = organizationCaptor.getValue();
    assertThat(actualSavedOrganization.getId()).isEqualTo(expectedOrganizationId);
  }

  @Test
  void save_repositorySaveThrowsRuntimeException_propagatesFailure() {
    // Test Case ID: TC_SEARCH_ORG_024
    // Scenario ID: SAD_009
    // Objective: Verify repository save failure is not hidden behind a success response.
    // Covered Branch/Path: findByName null -> repository save throws RuntimeException.
    // DB Check: Verify duplicate check and save are attempted once with expected organization data.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    OrgInputDTO inputOrganization = new OrgInputDTO();
    inputOrganization.setName("New Org");
    RuntimeException expectedException = new RuntimeException("db failure");
    logEvidence("mvn -pl search-service -DskipTests=false -Dtest=OrganizationImplTest test",
        "TC_SEARCH_ORG_024 expects repository save failure to be propagated");
    when(organizationRepo.findByName(inputOrganization.getName())).thenReturn(null);
    when(organizationRepo.save(any(Organization.class))).thenThrow(expectedException);

    // Act & Assert
    Throwable actualThrowable = catchThrowable(() -> organizationService.save(inputOrganization));
    verify(organizationRepo, times(1)).findByName(inputOrganization.getName());
    verify(organizationRepo, times(1)).save(any(Organization.class));
    assertThat(actualThrowable).isSameAs(expectedException);
  }

  @Test
  void getAllRecruiterByOrganization_validPaging_returnsRequestedPage() {
    // Test Case ID: TC_SEARCH_ORG_009
    // Scenario ID: HAPPY_007
    // Objective: Verify recruiters are paginated for an organization filter.
    // Covered Branch/Path: valid size/offset -> fromIndex within totalElements -> subList.
    // DB Check: Verify organizationName is passed to recruiter query.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    Integer inputSize = 2;
    Integer inputOffset = 1;
    String inputOrganizationName = "FJob";
    List<RecruiterCompany> expectedRecruiters = List.of(
        new RecruiterCompany("A", "a@example.com", "1", "FJob", PENDING),
        new RecruiterCompany("B", "b@example.com", "2", "FJob", APPROVED),
        new RecruiterCompany("C", "c@example.com", "3", "FJob", PENDING));
    when(organizationRepo.getAllRecruiter(inputOrganizationName)).thenReturn(expectedRecruiters);

    // Act
    PageResponse<RecruiterCompany> actualResponse = organizationService.getAllRecruiterByOrganization(
        inputSize, inputOffset, inputOrganizationName);

    // Assert
    assertThat(actualResponse.getTotalElement()).isEqualTo(3);
    assertThat(actualResponse.getTotalPage()).isEqualTo(2);
    assertThat(actualResponse.getPageIndex()).isEqualTo(inputOffset);
    assertThat(actualResponse.getPageSize()).isEqualTo(inputSize);
    assertThat(actualResponse.getData()).containsExactly(expectedRecruiters.get(2));
    verify(organizationRepo, times(1)).getAllRecruiter(inputOrganizationName);
  }

  @Test
  void getAllRecruiterByOrganization_invalidPaging_usesDefaultPaging() {
    // Test Case ID: TC_SEARCH_ORG_010
    // Scenario ID: EDGE_001
    // Objective: Verify invalid paging falls back to default page size and page index.
    // Covered Branch/Path: size <= 0 -> 10; offset < 0 -> 0; fromIndex within totalElements.
    // DB Check: Verify recruiter query is still executed once.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    List<RecruiterCompany> expectedRecruiters = List.of(
        new RecruiterCompany("A", "a@example.com", "1", "FJob", PENDING));
    when(organizationRepo.getAllRecruiter(null)).thenReturn(expectedRecruiters);

    // Act
    PageResponse<RecruiterCompany> actualResponse =
        organizationService.getAllRecruiterByOrganization(0, -1, null);

    // Assert
    assertThat(actualResponse.getTotalElement()).isEqualTo(1);
    assertThat(actualResponse.getTotalPage()).isEqualTo(1);
    assertThat(actualResponse.getPageIndex()).isEqualTo(0);
    assertThat(actualResponse.getPageSize()).isEqualTo(10);
    assertThat(actualResponse.getData()).containsExactlyElementsOf(expectedRecruiters);
    verify(organizationRepo, times(1)).getAllRecruiter(null);
  }

  @Test
  void getAllRecruiterByOrganization_nullPaging_usesDefaultPaging() {
    // Test Case ID: TC_SEARCH_ORG_018
    // Scenario ID: EDGE_006
    // Objective: Verify null paging values fall back to default size and offset.
    // Covered Branch/Path: size null -> 10; offset null -> 0.
    // DB Check: Verify recruiter query is executed once and no write operation occurs.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    when(organizationRepo.getAllRecruiter("FJob")).thenReturn(Collections.emptyList());

    // Act
    PageResponse<RecruiterCompany> actualResponse =
        organizationService.getAllRecruiterByOrganization(null, null, "FJob");

    // Assert
    assertThat(actualResponse.getTotalElement()).isEqualTo(0);
    assertThat(actualResponse.getTotalPage()).isEqualTo(0);
    assertThat(actualResponse.getPageIndex()).isEqualTo(0);
    assertThat(actualResponse.getPageSize()).isEqualTo(10);
    assertThat(actualResponse.getData()).isEmpty();
    verify(organizationRepo, times(1)).getAllRecruiter("FJob");
  }

  @Test
  void getAllRecruiterByOrganization_offsetBeyondData_returnsEmptyPage() {
    // Test Case ID: TC_SEARCH_ORG_011
    // Scenario ID: EDGE_002
    // Objective: Verify an empty recruiter page is returned when requested offset is beyond data.
    // Covered Branch/Path: fromIndex >= totalElements -> empty paginated list.
    // DB Check: Verify read query happens once and no data write operation occurs.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    List<RecruiterCompany> expectedRecruiters = List.of(
        new RecruiterCompany("A", "a@example.com", "1", "FJob", PENDING));
    when(organizationRepo.getAllRecruiter("FJob")).thenReturn(expectedRecruiters);

    // Act
    PageResponse<RecruiterCompany> actualResponse =
        organizationService.getAllRecruiterByOrganization(2, 2, "FJob");

    // Assert
    assertThat(actualResponse.getTotalElement()).isEqualTo(1);
    assertThat(actualResponse.getTotalPage()).isEqualTo(1);
    assertThat(actualResponse.getData()).isEmpty();
    verify(organizationRepo, times(1)).getAllRecruiter("FJob");
  }

  @Test
  void getAllRecruiterByOrganization_repositoryReturnsNull_returnsEmptyPage() {
    // Test Case ID: TC_SEARCH_ORG_025
    // Scenario ID: EDGE_008
    // Objective: Verify null recruiter list from repository is handled as no data.
    // Assumption: Null recruiter list should be treated as empty page for robust no-result handling.
    // Covered Branch/Path: repository returns null -> empty PageResponse.
    // DB Check: Verify recruiter query is executed once and no write operation occurs.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    Integer inputSize = 10;
    Integer inputOffset = 0;
    String inputOrganizationName = "FJob";
    Integer expectedTotalElement = 0;
    Integer expectedTotalPage = 0;
    logEvidence("mvn -pl search-service -DskipTests=false -Dtest=OrganizationImplTest test",
        "TC_SEARCH_ORG_025 expects null recruiter repository result to be treated as empty page");
    when(organizationRepo.getAllRecruiter(inputOrganizationName)).thenReturn(null);

    // Act
    final PageResponse<RecruiterCompany>[] actualResponse = new PageResponse[1];
    Throwable actualThrowable = org.assertj.core.api.Assertions.catchThrowable(() ->
        actualResponse[0] = organizationService.getAllRecruiterByOrganization(
            inputSize, inputOffset, inputOrganizationName));

    // Assert
    verify(organizationRepo, times(1)).getAllRecruiter(inputOrganizationName);
    verify(organizationRepo, never()).save(any());
    assertThat(actualThrowable).isNull();
    assertThat(actualResponse[0].getTotalElement()).isEqualTo(expectedTotalElement);
    assertThat(actualResponse[0].getTotalPage()).isEqualTo(expectedTotalPage);
    assertThat(actualResponse[0].getData()).isEmpty();
  }

  @Test
  void updateRecruiterStatus_existingRecruiter_savesUpdatedStatus() {
    // Test Case ID: TC_SEARCH_ORG_012
    // Scenario ID: HAPPY_008
    // Objective: Verify recruiter status is updated when recruiter configuration exists.
    // Covered Branch/Path: valid enum -> findById present -> set status -> save.
    // DB Check: Capture saved recruiter configuration and verify status is APPROVED.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    Long inputRecruiterId = 200L;
    String inputStatus = "APPROVED";
    RecruiterConfiguration existingConfiguration =
        new RecruiterConfiguration(inputRecruiterId, 10L, PENDING);
    when(recruiterConfigurationRepository.findById(inputRecruiterId))
        .thenReturn(Optional.of(existingConfiguration));

    // Act
    organizationService.updateRecruiterStatus(inputRecruiterId, inputStatus);

    // Assert
    ArgumentCaptor<RecruiterConfiguration> configurationCaptor =
        ArgumentCaptor.forClass(RecruiterConfiguration.class);
    verify(recruiterConfigurationRepository, times(1)).findById(inputRecruiterId);
    verify(recruiterConfigurationRepository, times(1)).save(configurationCaptor.capture());
    assertThat(configurationCaptor.getValue().getStatus()).isEqualTo(APPROVED);
  }

  @Test
  void updateRecruiterStatus_missingRecruiter_doesNotSave() {
    // Test Case ID: TC_SEARCH_ORG_013
    // Scenario ID: EDGE_003
    // Objective: Verify no DB write occurs when recruiter configuration is missing.
    // Covered Branch/Path: valid enum -> findById empty -> no save.
    // DB Check: Verify findById is called and save is never called.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    Long inputRecruiterId = 201L;
    String inputStatus = "REJECTED";
    when(recruiterConfigurationRepository.findById(inputRecruiterId)).thenReturn(Optional.empty());

    // Act
    organizationService.updateRecruiterStatus(inputRecruiterId, inputStatus);

    // Assert
    verify(recruiterConfigurationRepository, times(1)).findById(inputRecruiterId);
    verify(recruiterConfigurationRepository, never()).save(any());
  }

  @Test
  void updateRecruiterStatus_invalidStatus_throwsIllegalArgumentExceptionWithoutDbRead() {
    // Test Case ID: TC_SEARCH_ORG_014
    // Scenario ID: SAD_003
    // Objective: Verify invalid status is rejected before accessing recruiter configuration data.
    // Covered Branch/Path: RecruiterStatus.valueOf invalid value -> IllegalArgumentException.
    // DB Check: Verify no repository read or write occurs when status is invalid.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    Long inputRecruiterId = 202L;
    String inputInvalidStatus = "BLOCKED";

    // Act
    IllegalArgumentException actualException = assertThrows(IllegalArgumentException.class,
        () -> organizationService.updateRecruiterStatus(inputRecruiterId, inputInvalidStatus));

    // Assert
    assertThat(actualException).isNotNull();
    verify(recruiterConfigurationRepository, never()).findById(inputRecruiterId);
    verify(recruiterConfigurationRepository, never()).save(any());
  }

  private OrgSearchingDTO buildOrganizationSearch(int page, int size) {
    PageableModel paging = new PageableModel();
    paging.setPage(page);
    paging.setSize(size);

    OrgSearchingDTO search = new OrgSearchingDTO();
    search.setPaging(paging);
    return search;
  }

  private void logEvidence(String command, String expectation) {
    System.out.println("TEST_EVIDENCE command=\"" + command + "\" expectation=\"" + expectation + "\"");
  }
}
