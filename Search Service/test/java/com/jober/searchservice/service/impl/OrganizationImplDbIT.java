package com.jober.searchservice.service.impl;

import static com.jober.searchservice.utilsmodule.RecruiterStatus.APPROVED;
import static com.jober.searchservice.utilsmodule.RecruiterStatus.PENDING;
import static com.jober.utilsservice.constant.ResponseMessageConstant.FOUND;
import static com.jober.utilsservice.constant.ResponseMessageConstant.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;

import com.jober.searchservice.dto.PageResponse;
import com.jober.searchservice.dto.RecruiterCompany;
import com.jober.searchservice.integration.PostgresContainerBaseIT;
import com.jober.searchservice.model.Organization;
import com.jober.searchservice.model.RecruiterConfiguration;
import com.jober.searchservice.model.UserCommon;
import com.jober.searchservice.repository.OrganizationRepo;
import com.jober.searchservice.repository.RecruiterConfigurationRepository;
import com.jober.utilsservice.dto.OrgInputDTO;
import com.jober.utilsservice.dto.OrgSearchingDTO;
import com.jober.utilsservice.errors.ResponseEntitySerializable;
import com.jober.utilsservice.model.PageableModel;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class OrganizationImplDbIT extends PostgresContainerBaseIT {

  @Autowired
  private OrganizationRepo organizationRepo;
  @Autowired
  private RecruiterConfigurationRepository recruiterConfigurationRepository;
  @Autowired
  private TestEntityManager testEntityManager;

  private OrganizationImpl organizationService;

  @BeforeEach
  void setUp() {
    testEntityManager.getEntityManager().createNativeQuery("DELETE FROM recruiter_configuration")
        .executeUpdate();
    testEntityManager.getEntityManager().createNativeQuery("DELETE FROM usercommon").executeUpdate();
    organizationRepo.deleteAll();
    recruiterConfigurationRepository.deleteAll();

    organizationService = new OrganizationImpl();
    ReflectionTestUtils.setField(organizationService, "organizationRepo", organizationRepo);
    ReflectionTestUtils.setField(
        organizationService, "recruiterConfigurationRepository", recruiterConfigurationRepository);
  }

  @Test
  void save_newOrganization_persistsIntoRealDatabase() {
    OrgInputDTO input = new OrgInputDTO();
    input.setName("ACME");
    input.setDes("Global tech");

    ResponseObject response = organizationService.save(input);

    assertThat(response.getStatus()).isEqualTo(SUCCESS);
    Organization saved = organizationRepo.findByName("ACME");
    assertThat(saved).isNotNull();
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getActive()).isEqualTo(1);
  }

  @Test
  void getOrgByUserId_existingOrganization_readsFromRealDatabase() {
    Organization organization = persistOrganization("Jober", "description");

    Organization actual = organizationService.getOrgByUserId(organization.getId());

    assertThat(actual.getId()).isEqualTo(organization.getId());
    assertThat(actual.getName()).isEqualTo("Jober");
  }

  @Test
  void getOrg_nameFilter_returnsPagedDataFromRealDatabase() {
    persistOrganization("Acme Corp", "desc-1");
    persistOrganization("Other Corp", "desc-2");

    OrgSearchingDTO input = new OrgSearchingDTO();
    PageableModel paging = new PageableModel();
    paging.setPage(1);
    paging.setSize(10);
    input.setPaging(paging);
    input.setName("acme");

    ResponseEntitySerializable<ResponseObject> response = organizationService.getOrg(input);

    ResponseObject body = (ResponseObject) response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getStatus()).isEqualTo(FOUND);
    assertThat(body.getCurrentCount()).isEqualTo(1);
    assertThat((List<?>) body.getData()).hasSize(1);
  }

  @Test
  void getAllRecruiterByOrganization_joinsRealTablesAndPaginates() {
    Organization organization = persistOrganization("Acme Recruit", "desc");

    UserCommon user = new UserCommon();
    user.setName("Recruiter A");
    user.setEmail("recruiter.a@acme.test");
    user.setPhone("0909000001");
    user.setOrganizationId(organization.getId());
    user.setActive(1);
    user.setCreationDate(LocalDateTime.now());
    user.setUpdateDate(LocalDateTime.now());
    testEntityManager.persistAndFlush(user);

    RecruiterConfiguration configuration = new RecruiterConfiguration();
    configuration.setCusId(user.getId());
    configuration.setOrganizationId(organization.getId());
    configuration.setStatus(PENDING);
    recruiterConfigurationRepository.saveAndFlush(configuration);

    PageResponse<RecruiterCompany> page =
        organizationService.getAllRecruiterByOrganization(10, 0, "Acme");

    assertThat(page.getTotalElement()).isEqualTo(1);
    assertThat(page.getData()).hasSize(1);
    assertThat(page.getData().get(0).getStatus()).isEqualTo(PENDING);
    assertThat(page.getData().get(0).getCompany()).contains("Acme");
  }

  @Test
  void updateRecruiterStatus_existingRecord_updatesRealDatabase() {
    Organization organization = persistOrganization("Status Org", "desc");

    UserCommon user = new UserCommon();
    user.setName("Recruiter B");
    user.setEmail("recruiter.b@status.test");
    user.setPhone("0909000002");
    user.setOrganizationId(organization.getId());
    user.setActive(1);
    user.setCreationDate(LocalDateTime.now());
    user.setUpdateDate(LocalDateTime.now());
    testEntityManager.persistAndFlush(user);

    RecruiterConfiguration configuration = new RecruiterConfiguration();
    configuration.setCusId(user.getId());
    configuration.setOrganizationId(organization.getId());
    configuration.setStatus(PENDING);
    recruiterConfigurationRepository.saveAndFlush(configuration);

    organizationService.updateRecruiterStatus(user.getId(), "APPROVED");

    RecruiterConfiguration updated =
        recruiterConfigurationRepository.findById(user.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(APPROVED);
  }

  private Organization persistOrganization(String name, String desc) {
    Organization organization = new Organization();
    organization.setName(name);
    organization.setDescription(desc);
    organization.setActive(1);
    organization.setCreationDate(LocalDateTime.now());
    organization.setUpdateDate(LocalDateTime.now());
    return organizationRepo.saveAndFlush(organization);
  }
}
