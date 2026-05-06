package com.resourceservice.service.impl;

import com.jober.utilsservice.dto.WalletDTO;
import com.jober.utilsservice.utils.modelCustom.Response;
import com.resourceservice.dto.RecruiterManagementDTO;
import com.resourceservice.interceptor.BearerTokenWrapper;
import com.resourceservice.model.RecruiterManagement;
import com.resourceservice.repository.RecruiterManagementRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static com.jober.utilsservice.constant.Constant.BONUS_POINT;
import static com.jober.utilsservice.constant.ResponseMessageConstant.UPDATED;
import static com.resourceservice.utilsmodule.constant.Constant.NOT_MODIFY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ============================================================================
 * Unit Tests cho RecruiterManagementImpl (user-service).
 * ----------------------------------------------------------------------------
 * Scope UT: saveRecruiterManagement (success, update, save-fail, null payload).
 * Rollback: N (mock thuan, khong cham DB that).
 * ============================================================================
 */
@ExtendWith(MockitoExtension.class)
class RecruiterManagementImplTest {

    @Mock
    private RecruiterManagementRepo<RecruiterManagement, Long> recruiterManagementRepo;
    @Mock
    private CacheManagerService cacheManagerService;
    @Mock
    private CommunityService communityService;
    @Mock
    private BearerTokenWrapper tokenWrapper;

    private RecruiterManagementImpl service;

    @BeforeEach
    void setUp() {
        service = new RecruiterManagementImpl(tokenWrapper);
        ReflectionTestUtils.setField(service, "recruiterManagementRepo", recruiterManagementRepo);
        ReflectionTestUtils.setField(service, "cacheManagerService", cacheManagerService);
        ReflectionTestUtils.setField(service, "communityService", communityService);
    }

    /**
     * TC_RCM_001 - saveRecruiterManagement - Standard
     * Muc tieu: Khi tao recruiter-management moi, service phai save va tra UPDATED.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_RCM_001_saveRecruiterManagement_create_success() {
        RecruiterManagementDTO dto = new RecruiterManagementDTO();
        dto.setId(null);
        dto.setFreelancerId(10L);
        dto.setRatingStar(4);
        dto.setNote("shortlist");

        when(tokenWrapper.getUid()).thenReturn(1L);

        RecruiterManagement saved = new RecruiterManagement();
        saved.setId(100L);
        when(recruiterManagementRepo.save(any(RecruiterManagement.class))).thenReturn(saved);

        ResponseEntity<Response> response = service.saveRecruiterManagement(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(UPDATED, response.getBody().getCode());
        verify(recruiterManagementRepo, times(1)).save(any(RecruiterManagement.class));

        ArgumentCaptor<WalletDTO> walletCaptor = ArgumentCaptor.forClass(WalletDTO.class);
        verify(communityService, times(1)).saveWallet(walletCaptor.capture());
        WalletDTO actualWallet = walletCaptor.getValue();
        assertEquals(1L, actualWallet.getUserId());
        assertEquals(BONUS_POINT, actualWallet.getAddingPoint());
    }

    /**
     * TC_RCM_002 - saveRecruiterManagement - Standard
     * Muc tieu: Khi update recruiter-management (id != null), service phai save va tra UPDATED.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_RCM_002_saveRecruiterManagement_update_success() {
        RecruiterManagementDTO dto = new RecruiterManagementDTO();
        dto.setId(5L);
        dto.setFreelancerId(10L);

        when(tokenWrapper.getUid()).thenReturn(1L);

        RecruiterManagement saved = new RecruiterManagement();
        saved.setId(5L);
        when(recruiterManagementRepo.save(any(RecruiterManagement.class))).thenReturn(saved);

        ResponseEntity<Response> response = service.saveRecruiterManagement(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(UPDATED, response.getBody().getCode());
        verify(recruiterManagementRepo, times(1)).save(any(RecruiterManagement.class));
        verify(communityService, times(1)).saveWallet(any(WalletDTO.class));
    }

    /**
     * TC_RCM_003 - saveRecruiterManagement - Standard
     * Muc tieu: Khi repo.save tra null, service phai tra NOT_MODIFY.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_RCM_003_saveRecruiterManagement_saveFail() {
        RecruiterManagementDTO dto = new RecruiterManagementDTO();
        dto.setId(null);
        dto.setFreelancerId(10L);

        when(tokenWrapper.getUid()).thenReturn(1L);
        when(recruiterManagementRepo.save(any(RecruiterManagement.class))).thenReturn(null);

        ResponseEntity<Response> response = service.saveRecruiterManagement(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(NOT_MODIFY, response.getBody().getCode());
        verify(communityService, never()).saveWallet(any(WalletDTO.class));
    }

    /**
     * TC_RCM_004 - saveRecruiterManagement - Exception
     * Muc tieu: Khi payload null, service nem NullPointerException.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_RCM_004_saveRecruiterManagement_nullPayload() {
        assertThrows(NullPointerException.class, () -> service.saveRecruiterManagement(null));
    }
}
