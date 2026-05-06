package com.jober.searchservice.service.impl;

import static com.jober.utilsservice.constant.ResponseMessageConstant.FOUND;
import static com.jober.utilsservice.constant.ResponseMessageConstant.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.jober.searchservice.model.District;
import com.jober.searchservice.model.Province;
import com.jober.searchservice.model.Ward;
import com.jober.searchservice.repository.DistrictRepo;
import com.jober.searchservice.repository.ProvinceRepo;
import com.jober.searchservice.repository.WardRepo;
import com.jober.utilsservice.errors.ResponseEntitySerializable;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

  private AddressServiceImpl addressService;

  @Mock
  private ProvinceRepo provinceRepo;
  @Mock
  private DistrictRepo districtRepo;
  @Mock
  private WardRepo wardRepo;

  @BeforeEach
  void setUp() {
    addressService = new AddressServiceImpl();
    ReflectionTestUtils.setField(addressService, "provinceRepo", provinceRepo);
    ReflectionTestUtils.setField(addressService, "districtRepo", districtRepo);
    ReflectionTestUtils.setField(addressService, "wardRepo", wardRepo);
  }

  @Test
  void getProvinces_existingProvinces_returnsFoundResponse() {
    // Test Case ID: TC_SEARCH_ADDRESS_001
    // Scenario ID: HAPPY_001
    // Objective: Verify default address filter data is returned when provinces exist.
    // Covered Branch/Path: provinceRepo.findAll -> non-empty list -> FOUND response.
    // DB Check: Verify one read from province repository and no district/ward query.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    Province inputProvince = new Province();
    inputProvince.setCode("HN");
    inputProvince.setName("Ha Noi");
    List<Province> expectedProvinces = List.of(inputProvince);

    when(provinceRepo.findAll()).thenReturn(expectedProvinces);

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse = addressService.getProvinces();

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getStatus()).isEqualTo(FOUND);
    assertThat(actualBody.getTotalCount()).isEqualTo(1L);
    assertThat(actualBody.getCurrentCount()).isEqualTo(1);
    assertThat(actualBody.getData()).isEqualTo(expectedProvinces);
    verify(provinceRepo, times(1)).findAll();
    verifyNoMoreInteractions(provinceRepo);
    verify(districtRepo, never()).findByProvinceCode(org.mockito.ArgumentMatchers.anyString());
    verify(wardRepo, never()).findByProvinceCode(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void getProvinces_emptyProvinces_returnsNotFoundResponse() {
    // Test Case ID: TC_SEARCH_ADDRESS_002
    // Scenario ID: EDGE_001
    // Objective: Verify the system returns an empty province list when no province data exists.
    // Covered Branch/Path: provinceRepo.findAll -> empty list -> NOT_FOUND response.
    // DB Check: Verify one read from province repository and no write operation.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    List<Province> expectedEmptyProvinces = Collections.emptyList();
    when(provinceRepo.findAll()).thenReturn(expectedEmptyProvinces);

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse = addressService.getProvinces();

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getStatus()).isEqualTo(NOT_FOUND);
    assertThat(actualBody.getTotalCount()).isEqualTo(0L);
    assertThat(actualBody.getCurrentCount()).isEqualTo(0);
    assertThat((List<?>) actualBody.getData()).isEmpty();
    verify(provinceRepo, times(1)).findAll();
    verifyNoMoreInteractions(provinceRepo);
  }

  @Test
  void getProvinces_nullRepositoryResult_returnsNotFoundResponse() {
    // Test Case ID: TC_SEARCH_ADDRESS_007
    // Scenario ID: EDGE_004
    // Objective: Verify null province repository result is handled as no data.
    // Covered Branch/Path: provinceRepo.findAll -> null list -> NOT_FOUND response.
    // DB Check: Verify one province read and no data write operation.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    when(provinceRepo.findAll()).thenReturn(null);

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse = addressService.getProvinces();

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getStatus()).isEqualTo(NOT_FOUND);
    assertThat((List<?>) actualBody.getData()).isEmpty();
    verify(provinceRepo, times(1)).findAll();
    verifyNoMoreInteractions(provinceRepo);
  }

  @Test
  void getDistrictsByProvinceCode_existingDistricts_returnsFoundResponse() {
    // Test Case ID: TC_SEARCH_ADDRESS_003
    // Scenario ID: HAPPY_002
    // Objective: Verify districts are loaded for the selected location filter province.
    // Covered Branch/Path: districtRepo.findByProvinceCode -> non-empty list -> FOUND response.
    // DB Check: Verify the province code is passed to district repository exactly once.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputProvinceCode = "HN";
    District expectedDistrict = new District();
    expectedDistrict.setCode("BD");
    expectedDistrict.setProvinceCode(inputProvinceCode);
    List<District> expectedDistricts = List.of(expectedDistrict);

    when(districtRepo.findByProvinceCode(inputProvinceCode)).thenReturn(expectedDistricts);

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse =
        addressService.getDistrictsByProvinceCode(inputProvinceCode);

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getStatus()).isEqualTo(FOUND);
    assertThat(actualBody.getTotalCount()).isEqualTo(1L);
    assertThat(actualBody.getData()).isEqualTo(expectedDistricts);
    verify(districtRepo, times(1)).findByProvinceCode(inputProvinceCode);
    verifyNoMoreInteractions(districtRepo);
  }

  @Test
  void getDistrictsByProvinceCode_noDistricts_returnsNotFoundResponse() {
    // Test Case ID: TC_SEARCH_ADDRESS_004
    // Scenario ID: EDGE_002
    // Objective: Verify an empty district list is returned when the selected province has no districts.
    // Covered Branch/Path: districtRepo.findByProvinceCode -> empty list -> NOT_FOUND response.
    // DB Check: Verify read query uses the requested province code and no write operation occurs.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputProvinceCode = "UNKNOWN";
    when(districtRepo.findByProvinceCode(inputProvinceCode)).thenReturn(Collections.emptyList());

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse =
        addressService.getDistrictsByProvinceCode(inputProvinceCode);

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getStatus()).isEqualTo(NOT_FOUND);
    assertThat((List<?>) actualBody.getData()).isEmpty();
    verify(districtRepo, times(1)).findByProvinceCode(inputProvinceCode);
    verifyNoMoreInteractions(districtRepo);
  }
 
  @Test
  void getDistrictsByProvinceCode_nullRepositoryResult_returnsNotFoundResponse() {
    // Test Case ID: TC_SEARCH_ADDRESS_008
    // Scenario ID: EDGE_005
    // Objective: Verify null district repository result is handled as no data.
    // Covered Branch/Path: districtRepo.findByProvinceCode -> null list -> NOT_FOUND response.
    // DB Check: Verify read query uses the requested province code and no write operation occurs.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputProvinceCode = "NULL_DISTRICT";
    when(districtRepo.findByProvinceCode(inputProvinceCode)).thenReturn(null);

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse =
        addressService.getDistrictsByProvinceCode(inputProvinceCode);

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getStatus()).isEqualTo(NOT_FOUND);
    assertThat((List<?>) actualBody.getData()).isEmpty();
    verify(districtRepo, times(1)).findByProvinceCode(inputProvinceCode);
    verifyNoMoreInteractions(districtRepo);
  }

  @Test
  void getDistrictsByProvinceCode_nullProvinceCode_rejectsRequestWithoutDatabaseRead() {
    // Test Case ID: TC_SEARCH_ADDRESS_010
    // Scenario ID: SAD_004
    // Objective: Verify invalid province code is rejected before querying district data.
    // Assumption: Invalid provinceCode should be rejected as BAD_REQUEST based on search/filter contract.
    // Covered Branch/Path: provinceCode null -> validation rejection -> no repository read.
    // DB Check: Verify district repository is never queried with an invalid province code.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputProvinceCode = null;
    HttpStatus expectedHttpStatus = HttpStatus.BAD_REQUEST;
    logEvidence("mvn -pl search-service -DskipTests=false -Dtest=AddressServiceImplTest test",
        "TC_SEARCH_ADDRESS_010 expects invalid province code to be rejected before DB read");

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse =
        addressService.getDistrictsByProvinceCode(inputProvinceCode);

    // Assert
    verify(districtRepo, never()).findByProvinceCode(any());
    verifyNoMoreInteractions(districtRepo);
    assertThat(actualResponse.getStatusCode()).isEqualTo(expectedHttpStatus);
  }

  @Test
  void getWardsByProvinceCode_existingWards_returnsFoundResponse() {
    // Test Case ID: TC_SEARCH_ADDRESS_005
    // Scenario ID: HAPPY_003
    // Objective: Verify wards are loaded for the selected location filter province.
    // Covered Branch/Path: wardRepo.findByProvinceCode -> non-empty list -> FOUND response.
    // DB Check: Verify the province code is passed to ward repository exactly once.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputProvinceCode = "HN";
    Ward expectedWard = new Ward();
    expectedWard.setCode("P01");
    expectedWard.setProvincecode(inputProvinceCode);
    List<Ward> expectedWards = List.of(expectedWard);

    when(wardRepo.findByProvinceCode(inputProvinceCode)).thenReturn(expectedWards);

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse =
        addressService.getWardsByProvinceCode(inputProvinceCode);

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getStatus()).isEqualTo(FOUND);
    assertThat(actualBody.getTotalCount()).isEqualTo(1L);
    assertThat(actualBody.getData()).isEqualTo(expectedWards);
    verify(wardRepo, times(1)).findByProvinceCode(inputProvinceCode);
    verifyNoMoreInteractions(wardRepo);
  }

  @Test
  void getWardsByProvinceCode_noWards_returnsNotFoundResponse() {
    // Test Case ID: TC_SEARCH_ADDRESS_006
    // Scenario ID: EDGE_003
    // Objective: Verify an empty ward list is returned when the selected province has no wards.
    // Covered Branch/Path: wardRepo.findByProvinceCode -> empty list -> NOT_FOUND response.
    // DB Check: Verify read query uses the requested province code and no write operation occurs.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputProvinceCode = "UNKNOWN";
    when(wardRepo.findByProvinceCode(inputProvinceCode)).thenReturn(Collections.emptyList());

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse =
        addressService.getWardsByProvinceCode(inputProvinceCode);

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getStatus()).isEqualTo(NOT_FOUND);
    assertThat((List<?>) actualBody.getData()).isEmpty();
    verify(wardRepo, times(1)).findByProvinceCode(inputProvinceCode);
    verifyNoMoreInteractions(wardRepo);
  }

  @Test
  void getWardsByProvinceCode_nullRepositoryResult_returnsNotFoundResponse() {
    // Test Case ID: TC_SEARCH_ADDRESS_009
    // Scenario ID: EDGE_006
    // Objective: Verify null ward repository result is handled as no data.
    // Covered Branch/Path: wardRepo.findByProvinceCode -> null list -> NOT_FOUND response.
    // DB Check: Verify read query uses the requested province code and no write operation occurs.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputProvinceCode = "NULL_WARD";
    when(wardRepo.findByProvinceCode(inputProvinceCode)).thenReturn(null);

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse =
        addressService.getWardsByProvinceCode(inputProvinceCode);

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getStatus()).isEqualTo(NOT_FOUND);
    assertThat((List<?>) actualBody.getData()).isEmpty();
    verify(wardRepo, times(1)).findByProvinceCode(inputProvinceCode);
    verifyNoMoreInteractions(wardRepo);
  }

  @Test
  void getWardsByProvinceCode_blankProvinceCode_rejectsRequestWithoutDatabaseRead() {
    // Test Case ID: TC_SEARCH_ADDRESS_011
    // Scenario ID: SAD_005
    // Objective: Verify blank province code is rejected before querying ward data.
    // Assumption: Blank provinceCode should be rejected as BAD_REQUEST based on search/filter contract.
    // Covered Branch/Path: provinceCode blank -> validation rejection -> no repository read.
    // DB Check: Verify ward repository is never queried with an invalid province code.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputProvinceCode = " ";
    HttpStatus expectedHttpStatus = HttpStatus.BAD_REQUEST;
    logEvidence("mvn -pl search-service -DskipTests=false -Dtest=AddressServiceImplTest test",
        "TC_SEARCH_ADDRESS_011 expects blank province code to be rejected before DB read");

    // Act
    ResponseEntitySerializable<ResponseObject> actualResponse =
        addressService.getWardsByProvinceCode(inputProvinceCode);

    // Assert
    verify(wardRepo, never()).findByProvinceCode(anyString());
    verifyNoMoreInteractions(wardRepo);
    assertThat(actualResponse.getStatusCode()).isEqualTo(expectedHttpStatus);
  }

  private void logEvidence(String command, String expectation) {
    System.out.println("TEST_EVIDENCE command=\"" + command + "\" expectation=\"" + expectation + "\"");
  }
}
