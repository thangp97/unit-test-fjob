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

@RunWith(MockitoJUnitRunner.class)
public class SettingsImplTest {

    @Mock
    private SettingsRepo settingsRepo;

    @InjectMocks
    private SettingsImpl settingsService;

    @Test
    public void getSettings_returnsSettingDto() {
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

    @Test
    public void getSettings_whenNullList_returnsNotFound() {
        when(settingsRepo.findAll()).thenReturn(null);

        ResponseEntity<ResponseObject> response = settingsService.getSettings();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    public void getSettings_emptyList() {
        when(settingsRepo.findAll()).thenReturn(Collections.emptyList());
        ResponseEntity<ResponseObject> response = settingsService.getSettings();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getData());
    }

    @Test
    public void getSettings_unrecognizedKeywords() {
        Settings unknown = new Settings();
        unknown.setKeywords("UNKNOWN");
        unknown.setData("data");
        when(settingsRepo.findAll()).thenReturn(Collections.singletonList(unknown));

        ResponseEntity<ResponseObject> response = settingsService.getSettings();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // TC005 - BUG - null keywords in Settings causes NPE
    @Test
    public void getSettings_NullKeywords_BUG_Confirmed() {
        Settings activeFee = new Settings();
        activeFee.setKeywords(null); // This causes NPE when calling .equals(ACTIVE_FEE)
        activeFee.setData("100");

        when(settingsRepo.findAll()).thenReturn(Collections.singletonList(activeFee));

        ResponseEntity<ResponseObject> response = settingsService.getSettings();
        assertNotNull(response);
    }

    @Test
    public void updateSettings_saveReturnsNull() {
        Settings input = new Settings();
        input.setKeywords(ACTIVE_FEE);
        input.setData("200");
        when(settingsRepo.save("200", ACTIVE_FEE)).thenReturn(null);

        ResponseEntity<ResponseObject> response = settingsService.updateSettings(input);
        // Service returns 304 NOT_MODIFIED and null body when save result is null
        assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
        assertNull(response.getBody());
    }
}
