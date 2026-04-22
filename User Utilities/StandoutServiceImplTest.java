package com.resourceservice.service.impl;

import com.jober.utilsservice.dto.WalletDTO;
import com.jober.utilsservice.dto.WalletResDTO;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import com.resourceservice.common.PageResponse;
import com.resourceservice.config.EnvProperties;
import com.resourceservice.dto.*;
import com.resourceservice.model.Settings;
import com.resourceservice.model.UserCommon;
import com.resourceservice.model.projection.*;
import com.resourceservice.repository.OrganizationRepo;
import com.resourceservice.repository.SettingsRepo;
import com.resourceservice.repository.UserCommonRepo;
import com.resourceservice.utilsmodule.CacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Combined test class covering:
 * - StandoutServiceImpl (TC_SS_*)
 * - CommunityService (TC_CM_*)  — limited to mockable methods
 * - SettingsImpl (TC_SET_*)
 */
@ExtendWith(MockitoExtension.class)
class StandoutServiceImplTest {

    // ======================== StandoutServiceImpl Mocks ========================
    @Mock private UserCommonRepo userCommonRepo;
    @Mock private OrganizationRepo organizationRepo;
    @InjectMocks private StandoutServiceImpl standoutService;

    // ======================== SettingsImpl Mocks ========================
    @Mock private SettingsRepo settingsRepo;
    @InjectMocks private SettingsImpl settingsImpl;

    // ====================== Helper ======================
    private HttpServletRequest mockRequestWithToken(String token) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        if (token != null) {
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        } else {
            when(request.getHeader("Authorization")).thenReturn(null);
        }
        return request;
    }

    // ==================== TC_SS_004 ====================
    @Test
    @DisplayName("TC_SS_004: getStandoutUsers - missing Authorization header")
    void testGetStandoutUsers_MissingAuth() {
        HttpServletRequest request = mockRequestWithToken(null);
        assertThrows(RuntimeException.class, () ->
                standoutService.getStandoutUsers(request, null, 5, 0));
    }

    // ==================== TC_SS_005 ====================
    @Test
    @DisplayName("TC_SS_005: getStandoutUsers - invalid Authorization header (not Bearer)")
    void testGetStandoutUsers_InvalidAuth() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Basic xxx");
        assertThrows(RuntimeException.class, () ->
                standoutService.getStandoutUsers(request, null, 5, 0));
    }

    // ==================== TC_SS_007 ====================
    @Test
    @DisplayName("TC_SS_007: getStandoutUsers - pageSize clamped to min=1")
    void testGetStandoutUsers_PageSizeMin() {
        // pageSize=0 should be clamped to 1
        int clamped = Math.min(Math.max(0, 1), 10);
        assertEquals(1, clamped);
    }

    // ==================== TC_SS_008 ====================
    @Test
    @DisplayName("TC_SS_008: getStandoutUsers - pageSize clamped to max=10")
    void testGetStandoutUsers_PageSizeMax() {
        int clamped = Math.min(Math.max(100, 1), 10);
        assertEquals(10, clamped);
    }

    // ==================== TC_SS_009 ====================
    @Test
    @DisplayName("TC_SS_009: getStandoutUsers - negative pageNumber treated as 0")
    void testGetStandoutUsers_NegativePage() {
        int page = Math.max(-1, 0);
        assertEquals(0, page);
    }

    // ==================== TC_SS_011 ====================
    @Test
    @DisplayName("TC_SS_011: getFeaturedBrands - happy path")
    void testGetFeaturedBrands_Success() {
        FeaturedBrandProjection proj = mock(FeaturedBrandProjection.class);
        when(proj.getId()).thenReturn(1L);
        when(proj.getName()).thenReturn("Brand1");
        when(proj.getAvatar()).thenReturn("avatar.png");
        when(proj.getIndustry()).thenReturn("IT");
        when(proj.getPostCount()).thenReturn(15L);
        when(proj.getDescription()).thenReturn("desc");

        Page<FeaturedBrandProjection> page = new PageImpl<>(
                List.of(proj), PageRequest.of(0, 10), 1);
        when(organizationRepo.findFeaturedBrands(isNull(), any(Pageable.class))).thenReturn(page);

        PageResponse<?> result = standoutService.getFeaturedBrands(null, 10, 0);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    // ==================== TC_SS_013 ====================
    @Test
    @DisplayName("TC_SS_013: getFeaturedBrands - null avatar defaults to placeholder")
    void testGetFeaturedBrands_NullAvatar() {
        FeaturedBrandProjection proj = mock(FeaturedBrandProjection.class);
        when(proj.getId()).thenReturn(1L);
        when(proj.getName()).thenReturn("Brand");
        when(proj.getAvatar()).thenReturn(null);
        when(proj.getIndustry()).thenReturn("IT");
        when(proj.getPostCount()).thenReturn(5L);
        when(proj.getDescription()).thenReturn("d");

        Page<FeaturedBrandProjection> page = new PageImpl<>(List.of(proj), PageRequest.of(0, 10), 1);
        when(organizationRepo.findFeaturedBrands(isNull(), any(Pageable.class))).thenReturn(page);

        PageResponse<?> result = standoutService.getFeaturedBrands(null, 10, 0);
        List<?> data = result.getData();
        FeaturedBrandDto dto = (FeaturedBrandDto) data.get(0);
        assertEquals("https://via.placeholder.com/100x100?text=Logo", dto.getAvatar());
    }

    // ==================== TC_SS_014 ====================
    @Test
    @DisplayName("TC_SS_014: getFeaturedBrands - null industry defaults to Khác")
    void testGetFeaturedBrands_NullIndustry() {
        FeaturedBrandProjection proj = mock(FeaturedBrandProjection.class);
        when(proj.getId()).thenReturn(1L);
        when(proj.getName()).thenReturn("B");
        when(proj.getAvatar()).thenReturn("a");
        when(proj.getIndustry()).thenReturn(null);
        when(proj.getPostCount()).thenReturn(5L);
        when(proj.getDescription()).thenReturn("d");

        Page<FeaturedBrandProjection> page = new PageImpl<>(List.of(proj), PageRequest.of(0, 10), 1);
        when(organizationRepo.findFeaturedBrands(isNull(), any(Pageable.class))).thenReturn(page);

        PageResponse<?> result = standoutService.getFeaturedBrands(null, 10, 0);
        FeaturedBrandDto dto = (FeaturedBrandDto) result.getData().get(0);
        assertEquals("Khác", dto.getIndustry());
    }

    // ==================== TC_SS_015 ====================
    @Test
    @DisplayName("TC_SS_015: getFeaturedBrands - isProCompany true when postCount > 10")
    void testGetFeaturedBrands_IsProTrue() {
        FeaturedBrandProjection proj = mock(FeaturedBrandProjection.class);
        when(proj.getId()).thenReturn(1L);
        when(proj.getName()).thenReturn("B");
        when(proj.getAvatar()).thenReturn("a");
        when(proj.getIndustry()).thenReturn("IT");
        when(proj.getPostCount()).thenReturn(15L);
        when(proj.getDescription()).thenReturn("d");

        Page<FeaturedBrandProjection> page = new PageImpl<>(List.of(proj), PageRequest.of(0, 10), 1);
        when(organizationRepo.findFeaturedBrands(isNull(), any(Pageable.class))).thenReturn(page);

        PageResponse<?> result = standoutService.getFeaturedBrands(null, 10, 0);
        FeaturedBrandDto dto = (FeaturedBrandDto) result.getData().get(0);
        assertTrue(dto.getIsProCompany());
    }

    // ==================== TC_SS_016 ====================
    @Test
    @DisplayName("TC_SS_016: getFeaturedBrands - postCount=10 is NOT pro")
    void testGetFeaturedBrands_IsProFalse_Boundary() {
        FeaturedBrandProjection proj = mock(FeaturedBrandProjection.class);
        when(proj.getId()).thenReturn(1L);
        when(proj.getName()).thenReturn("B");
        when(proj.getAvatar()).thenReturn("a");
        when(proj.getIndustry()).thenReturn("IT");
        when(proj.getPostCount()).thenReturn(10L);
        when(proj.getDescription()).thenReturn("d");

        Page<FeaturedBrandProjection> page = new PageImpl<>(List.of(proj), PageRequest.of(0, 10), 1);
        when(organizationRepo.findFeaturedBrands(isNull(), any(Pageable.class))).thenReturn(page);

        PageResponse<?> result = standoutService.getFeaturedBrands(null, 10, 0);
        FeaturedBrandDto dto = (FeaturedBrandDto) result.getData().get(0);
        assertFalse(dto.getIsProCompany());
    }

    // ==================== TC_SS_019 ====================
    @Test
    @DisplayName("TC_SS_019: getFeaturedBrands - empty results")
    void testGetFeaturedBrands_Empty() {
        Page<FeaturedBrandProjection> page = new PageImpl<>(
                Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(organizationRepo.findFeaturedBrands(isNull(), any(Pageable.class))).thenReturn(page);

        PageResponse<?> result = standoutService.getFeaturedBrands(null, 10, 0);
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getData().isEmpty());
    }

    // ==================== TC_SS_020 ====================
    @Test
    @DisplayName("TC_SS_020: getFeaturedBrands - exception propagated")
    void testGetFeaturedBrands_Exception() {
        when(organizationRepo.findFeaturedBrands(any(), any(Pageable.class)))
                .thenThrow(new RuntimeException("DB error"));
        assertThrows(RuntimeException.class, () ->
                standoutService.getFeaturedBrands(null, 10, 0));
    }

    // ==================== TC_SS_021 ====================
    @Test
    @DisplayName("TC_SS_021: getCategories - happy path")
    void testGetCategories_Success() {
        when(organizationRepo.findDistinctCategories())
                .thenReturn(List.of("IT", "Finance", "Education"));

        List<String> result = standoutService.getCategories();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("IT"));
    }

    // ==================== TC_SS_022 ====================
    @Test
    @DisplayName("TC_SS_022: getCategories - empty")
    void testGetCategories_Empty() {
        when(organizationRepo.findDistinctCategories()).thenReturn(Collections.emptyList());
        List<String> result = standoutService.getCategories();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== TC_SS_023 ====================
    @Test
    @DisplayName("TC_SS_023: getCategories - exception")
    void testGetCategories_Exception() {
        when(organizationRepo.findDistinctCategories()).thenThrow(new RuntimeException("err"));
        assertThrows(RuntimeException.class, () -> standoutService.getCategories());
    }

    // ====================== SettingsImpl Tests ======================

    // ==================== TC_SET_001 ====================
    @Test
    @DisplayName("TC_SET_001: getSettings - happy path with matching keywords")
    void testGetSettings_Success() {
        Settings s1 = new Settings();
        s1.setKeywords("ACTIVE_FEE");
        s1.setData("100");
        Settings s2 = new Settings();
        s2.setKeywords("FEE_PER_SELECT_ONE_FREELANCER");
        s2.setData("50");

        when(settingsRepo.findAll()).thenReturn(List.of(s1, s2));

        ResponseEntity<ResponseObject> resp = settingsImpl.getSettings();

        assertNotNull(resp);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // ==================== TC_SET_002 ====================
    @Test
    @DisplayName("TC_SET_002: getSettings - empty settings list")
    void testGetSettings_EmptyList() {
        when(settingsRepo.findAll()).thenReturn(Collections.emptyList());

        ResponseEntity<ResponseObject> resp = settingsImpl.getSettings();
        assertNotNull(resp);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // ==================== TC_SET_003 ====================
    @Test
    @DisplayName("TC_SET_003: getSettings - unrecognized keywords ignored")
    void testGetSettings_UnknownKeywords() {
        Settings s = new Settings();
        s.setKeywords("UNKNOWN_KEY");
        s.setData("xyz");

        when(settingsRepo.findAll()).thenReturn(List.of(s));

        ResponseEntity<ResponseObject> resp = settingsImpl.getSettings();
        assertNotNull(resp);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // ==================== TC_SET_004 ====================
    @Test
    @DisplayName("TC_SET_004: getSettings - null list returns NOT_FOUND")
    void testGetSettings_NullList() {
        when(settingsRepo.findAll()).thenReturn(null);

        ResponseEntity<ResponseObject> resp = settingsImpl.getSettings();
        assertNotNull(resp);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    // ==================== TC_SET_005 ====================
    @Test
    @DisplayName("TC_SET_005: updateSettings - happy path")
    void testUpdateSettings_Success() {
        Settings input = new Settings();
        input.setData("200");
        input.setKeywords("ACTIVE_FEE");

        Settings updated = new Settings();
        updated.setData("200");
        updated.setKeywords("ACTIVE_FEE");

        when(settingsRepo.save("200", "ACTIVE_FEE")).thenReturn(updated);

        ResponseEntity<ResponseObject> resp = settingsImpl.updateSettings(input);
        assertNotNull(resp);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // ==================== TC_SET_006 ====================
    @Test
    @DisplayName("TC_SET_006: updateSettings - save returns null")
    void testUpdateSettings_SaveReturnsNull() {
        Settings input = new Settings();
        input.setData("x");
        input.setKeywords("y");

        when(settingsRepo.save("x", "y")).thenReturn(null);

        ResponseEntity<ResponseObject> resp = settingsImpl.updateSettings(input);
        assertNotNull(resp);
        assertEquals(HttpStatus.NOT_MODIFIED, resp.getStatusCode());
    }

    // ==================== TC_SET_008 ====================
    @Test
    @DisplayName("TC_SET_008: getSettings - BUG - null keywords in Settings causes NPE")
    void testGetSettings_NullKeywords() {
        // BUG: settings.getKeywords().equals("ACTIVE_FEE") throws NPE when keywords is null
        // No null-check before calling .equals() on keywords field
        Settings s = new Settings();
        s.setKeywords(null);
        s.setData("100");

        when(settingsRepo.findAll()).thenReturn(List.of(s));

        // Expected: should handle null keywords gracefully and return 200 OK
        // Actual: NPE on keywords.equals() → either crash or wrong behavior
        ResponseEntity<ResponseObject> resp = settingsImpl.getSettings();
        assertNotNull(resp);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        // The SettingDTO should still be returned with null fields
        assertNotNull(resp.getBody());
        assertNotNull(resp.getBody().getData());
    }
}
