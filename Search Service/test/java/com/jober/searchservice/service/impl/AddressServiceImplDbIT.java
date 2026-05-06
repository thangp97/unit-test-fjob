package com.jober.searchservice.service.impl;

import static com.jober.utilsservice.constant.ResponseMessageConstant.FOUND;
import static com.jober.utilsservice.constant.ResponseMessageConstant.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

import com.jober.searchservice.integration.PostgresContainerBaseIT;
import com.jober.searchservice.model.District;
import com.jober.searchservice.model.Province;
import com.jober.searchservice.model.Ward;
import com.jober.searchservice.repository.DistrictRepo;
import com.jober.searchservice.repository.ProvinceRepo;
import com.jober.searchservice.repository.WardRepo;
import com.jober.utilsservice.errors.ResponseEntitySerializable;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class AddressServiceImplDbIT extends PostgresContainerBaseIT {

  @Autowired
  private ProvinceRepo provinceRepo;
  @Autowired
  private DistrictRepo districtRepo;
  @Autowired
  private WardRepo wardRepo;

  private AddressServiceImpl addressService;

  @BeforeEach
  void setUp() {
    wardRepo.deleteAll();
    districtRepo.deleteAll();
    provinceRepo.deleteAll();

    addressService = new AddressServiceImpl();
    ReflectionTestUtils.setField(addressService, "provinceRepo", provinceRepo);
    ReflectionTestUtils.setField(addressService, "districtRepo", districtRepo);
    ReflectionTestUtils.setField(addressService, "wardRepo", wardRepo);
  }

  @Test
  void getProvinces_existingData_readsFromRealDatabase() {
    Province province = new Province();
    province.setCode("HN");
    province.setName("Ha Noi");
    province.setIsActive(true);
    provinceRepo.save(province);

    ResponseEntitySerializable<ResponseObject> response = addressService.getProvinces();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject body = (ResponseObject) response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getStatus()).isEqualTo(FOUND);
    assertThat(body.getCurrentCount()).isEqualTo(1);
    assertThat((List<?>) body.getData()).hasSize(1);
  }

  @Test
  void getDistrictsByProvinceCode_filtersByPersistedProvinceCode() {
    District district1 = new District();
    district1.setCode("D1");
    district1.setName("Ba Dinh");
    district1.setProvinceCode("HN");
    districtRepo.save(district1);

    District district2 = new District();
    district2.setCode("D2");
    district2.setName("Thu Duc");
    district2.setProvinceCode("HCM");
    districtRepo.save(district2);

    ResponseEntitySerializable<ResponseObject> response =
        addressService.getDistrictsByProvinceCode("HN");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject body = (ResponseObject) response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getStatus()).isEqualTo(FOUND);
    assertThat((List<?>) body.getData()).hasSize(1);
  }

  @Test
  void getWardsByProvinceCode_missingData_returnsNotFoundFromRealDatabase() {
    Ward ward = new Ward();
    ward.setCode("W1");
    ward.setName("Ward 1");
    ward.setProvincecode("HCM");
    wardRepo.save(ward);

    ResponseEntitySerializable<ResponseObject> response =
        addressService.getWardsByProvinceCode("HN");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject body = (ResponseObject) response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getStatus()).isEqualTo(NOT_FOUND);
    assertThat(body.getCurrentCount()).isEqualTo(0);
  }
}
