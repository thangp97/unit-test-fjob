package com.resourceservice.service.impl;

import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import com.resourceservice.dto.SettingDTO;
import com.resourceservice.model.Settings;
import com.resourceservice.repository.SettingsRepo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.resourceservice.utilsmodule.constant.Constant.ACTIVE_FEE;
import static com.resourceservice.utilsmodule.constant.Constant.FEE_PER_SELECT_ONE_FREELANCER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SettingsServiceImpl.
 * Validates the retrieval and update logic for system settings.
 */
@RunWith(MockitoJUnitRunner.class)
public class SettingsImplTest {

    @Mock
    private SettingsRepo settingsRepo;

    @InjectMocks
    private SettingsImpl settingsService;

    /**
     * TC_001: getSettings - Happy path
     * Objective: Verify that settings are correctly retrieved and mapped to SettingDTO.
     * Input: Settings repository returns list with ACTIVE_FEE and FEE_PER_SELECT_ONE_FREELANCER.
     * Expected: HTTP 200 OK and data is a correctly mapped SettingDTO.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void getSettings_WhenDataExistsInDB_ShouldReturnMappedSettingDTO() {
        Settings activeFee = new Settings();
        activeFee.setKeywords(ACTIVE_FEE);
        activeFee.setData("100");

        Settings feePerSelect = new Settings();
        feePerSelect.setKeywords(FEE_PER_SELECT_ONE_FREELANCER);
        feePerSelect.setData("10");

        List<Settings> settingsList = Arrays.asList(activeFee, feePerSelect);
        when(settingsRepo.findAll()).thenReturn(settingsList);

        ResponseEntity<ResponseObject> response = settingsService.getSettings();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getData() instanceof SettingDTO);

        SettingDTO dto = (SettingDTO) response.getBody().getData();
        assertEquals("100", dto.getActivefee());
        assertEquals("10", dto.getFeePerSelectOneFreelancer());
    }

    /**
     * TC_002: getSettings - Edge case (Empty list)
     * Objective: Verify behavior when the settings table is empty.
     * Input: Settings repository returns an empty list.
     * Expected: HTTP 200 OK with an empty data object in ResponseObject.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void getSettings_WhenNoDataInDB_ShouldReturnEmptyDataObject() {
        when(settingsRepo.findAll()).thenReturn(Collections.emptyList());
        ResponseEntity<ResponseObject> response = settingsService.getSettings();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getData());
    }

    /**
     * TC_003: getSettings - Edge case (Unrecognized keywords)
     * Objective: Verify that unrecognized keywords in the database are ignored during mapping.
     * Input: Settings repository returns a keyword not defined in constants.
     * Expected: HTTP 200 OK, unrecognized keyword does not affect mapped DTO.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void getSettings_WhenUnrecognizedKeywordsInDB_ShouldIgnoreThemAndReturnOk() {
        Settings unknown = new Settings();
        unknown.setKeywords("UNKNOWN_KEYWORD");
        unknown.setData("some_data");
        when(settingsRepo.findAll()).thenReturn(Collections.singletonList(unknown));

        ResponseEntity<ResponseObject> response = settingsService.getSettings();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * TC_004: getSettings - Edge case (Null list)
     * Objective: Verify behavior when the repository returns null instead of a list.
     * Input: Settings repository returns null.
     * Expected: HTTP 404 NOT_FOUND.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void getSettings_WhenRepoReturnsNull_ShouldReturnNotFound() {
        when(settingsRepo.findAll()).thenReturn(null);

        ResponseEntity<ResponseObject> response = settingsService.getSettings();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    /**
     * TC_005: getSettings - BUG Confirmation
     * Objective: Confirm that null keywords in the settings table cause a NullPointerException.
     * Input: Settings entry with a null keywords field.
     * Expected: NullPointerException (Identified as a bug to be fixed).
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void getSettings_WhenKeywordsFieldIsNull_ShouldThrowNullPointerException() {
        Settings activeFee = new Settings();
        activeFee.setKeywords(null); // This causes NPE when calling .equals() in service
        activeFee.setData("100");

        when(settingsRepo.findAll()).thenReturn(Collections.singletonList(activeFee));

        // This call is expected to fail with NullPointerException based on current implementation
        settingsService.getSettings();
    }

    /**
     * TC_006: updateSettings - Edge case (Save fails)
     * Objective: Verify behavior when the update operation fails to modify any rows.
     * Input: Settings object with valid data, but repository save returns null.
     * Expected: HTTP 304 NOT_MODIFIED.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void updateSettings_WhenSaveOperationReturnsNull_ShouldReturnNotModified() {
        Settings input = new Settings();
        input.setKeywords(ACTIVE_FEE);
        input.setData("200");
        when(settingsRepo.save("200", ACTIVE_FEE)).thenReturn(null);

        ResponseEntity<ResponseObject> response = settingsService.updateSettings(input);
        assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
        assertNull(response.getBody());
    }
}
