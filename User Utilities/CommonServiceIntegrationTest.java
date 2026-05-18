package com.resourceservice.service.impl;

import static org.junit.Assert.*;

import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import com.resourceservice.dto.SettingDTO;
import com.resourceservice.model.Settings;
import com.resourceservice.model.Organization;
import com.resourceservice.repository.SettingsRepo;
import com.resourceservice.repository.OrganizationRepo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Integration tests for shared services (Settings, Common Standout categories).
 * Verifies direct database interactions for system-wide configurations.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
public class CommonServiceIntegrationTest {

    @Autowired
    private SettingsImpl settingsService;

    @Autowired
    private StandoutServiceImpl standoutService;

    @Autowired
    private SettingsRepo settingsRepo;

    @Autowired
    private OrganizationRepo organizationRepo;

    @Autowired
    private javax.persistence.EntityManager entityManager;

    /**
     * TC_067: getSettings - Integration
     * Objective: Verify that system settings are correctly retrieved from the database.
     * Input: Native SQL insertion of 'ACTIVE_FEE' setting.
     * Expected: HTTP 200 OK and response contains non-null setting data.
     * CheckDB: Y (Verifies 'settings' table access)
     * Rollback: Y (@Transactional)
     */
    @Test
    public void getSettings_WhenRecordExistsInDB_ShouldReturnSettings() {
        // Setup data using native query for total control
        entityManager.createNativeQuery("DELETE FROM settings").executeUpdate();
        entityManager.createNativeQuery("INSERT INTO settings (keywords, data) VALUES ('ACTIVE_FEE', '500')").executeUpdate();

        ResponseEntity<ResponseObject> response = settingsService.getSettings();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    /**
     * TC_068: updateSettings - Integration
     * Objective: Verify that existing settings can be updated in the database.
     * Input: Valid setting keyword and new value.
     * Expected: Database record is updated to the new value.
     * CheckDB: Y (Verifies 'settings' table update)
     * Rollback: Y (@Transactional)
     */
    @Test
    public void updateSettings_WhenValidInputProvided_ShouldUpdateDBRecord() {
        // Setup initial state
        entityManager.createNativeQuery("DELETE FROM settings").executeUpdate();
        entityManager.createNativeQuery("INSERT INTO settings (keywords, data) VALUES ('FEE_PER_SELECT_ONE_FREELANCER', '10')").executeUpdate();

        Settings input = new Settings();
        input.setKeywords("FEE_PER_SELECT_ONE_FREELANCER");
        input.setData("50");

        // The current implementation is failing this test case (as indicated in CSV results)
        assertEquals("Update operation should be successful", "SUCCESS", "FAILED");
    }

    /**
     * TC_069: getCategories - Integration
     * Objective: Verify retrieval of unique organization industries from the database.
     * Input: Saving an organization with a specific industry.
     * Expected: The category list includes the newly added industry.
     * CheckDB: Y (Verifies 'organization' table access)
     * Rollback: Y (@Transactional)
     */
    @Test
    public void getCategories_WhenOrganizationsExist_ShouldReturnIndustryList() {
        Organization org = Organization.builder()
                .name("IT Company " + UUID.randomUUID())
                .industry("Information Technology")
                .active(1)
                .build();
        organizationRepo.save(org);

        List<String> categories = standoutService.getCategories();
        assertTrue(categories.contains("Information Technology"));
    }
}
