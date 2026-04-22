package com.resourceservice.service.impl;

import com.jober.utilsservice.dto.WalletDTO;
import com.jober.utilsservice.utils.modelCustom.Response;
import com.resourceservice.dto.RecruiterManagementDTO;
import com.resourceservice.interceptor.BearerTokenWrapper;
import com.resourceservice.model.RecruiterManagement;
import com.resourceservice.model.UserCommon;
import com.resourceservice.repository.RecruiterManagementRepo;
import com.resourceservice.repository.UserCommonRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.resourceservice.utilsmodule.constant.Constant.*;
import static com.jober.utilsservice.constant.Constant.BONUS_POINT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecruiterManagementImplTest {

    @Mock
    private RecruiterManagementRepo recruiterManagementRepo;

    @Mock
    private UserCommonRepo userCommonRepo;

    @Mock
    private CacheManagerService cacheManagerService;

    @Mock
    private CommunityService communityService;

    @Mock
    private BearerTokenWrapper tokenWrapper;

    @InjectMocks
    private RecruiterManagementImpl recruiterManagementService;

    @BeforeEach
    void setUp() {
        // Common setup if needed
    }

    private RecruiterManagementDTO buildDto(Long id, String comment, Long freelancerId, Integer ratingStar, String note) {
        RecruiterManagementDTO dto = new RecruiterManagementDTO();
        dto.setId(id);
        dto.setComment(comment);
        dto.setFreelancerId(freelancerId);
        dto.setRatingStar(ratingStar);
        dto.setNote(note);
        return dto;
    }

    // ==================== TC_RM_001 ====================
    @Test
    @DisplayName("TC_RM_001: saveRecruiterManagement - Happy path - Create new RecruiterManagement")
    void testSaveRecruiterManagement_CreateNew_Success() {
        // Arrange
        RecruiterManagementDTO dto = buildDto(null, "Good", 10L, 5, "note1");

        when(tokenWrapper.getUid()).thenReturn(1L);

        RecruiterManagement savedEntity = RecruiterManagement.builder()
                .id(1L)
                .comment("Good")
                .freelancerid(10L)
                .ratingstar(5)
                .note("note1")
                .build();

        when(recruiterManagementRepo.save(any(RecruiterManagement.class))).thenReturn(savedEntity);
        when(communityService.saveWallet(any(WalletDTO.class))).thenReturn(null);

        // Act
        ResponseEntity<Response> response = recruiterManagementService.saveRecruiterManagement(dto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify saveWallet was called
        verify(communityService, times(1)).saveWallet(any(WalletDTO.class));

        // Verify creationdate was set (captured via argument captor)
        ArgumentCaptor<RecruiterManagement> captor = ArgumentCaptor.forClass(RecruiterManagement.class);
        verify(recruiterManagementRepo).save(captor.capture());
        assertNotNull(captor.getValue().getCreationdate(), "creationdate should be set for new records");
    }

    // ==================== TC_RM_002 ====================
    @Test
    @DisplayName("TC_RM_002: saveRecruiterManagement - Happy path - Update existing RecruiterManagement")
    void testSaveRecruiterManagement_Update_Success() {
        // Arrange
        RecruiterManagementDTO dto = buildDto(5L, "Updated", 10L, 3, "note2");

        when(tokenWrapper.getUid()).thenReturn(1L);

        RecruiterManagement savedEntity = RecruiterManagement.builder()
                .id(5L)
                .comment("Updated")
                .build();

        when(recruiterManagementRepo.save(any(RecruiterManagement.class))).thenReturn(savedEntity);
        when(communityService.saveWallet(any(WalletDTO.class))).thenReturn(null);

        // Act
        ResponseEntity<Response> response = recruiterManagementService.saveRecruiterManagement(dto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify creationdate is NOT set for updates
        ArgumentCaptor<RecruiterManagement> captor = ArgumentCaptor.forClass(RecruiterManagement.class);
        verify(recruiterManagementRepo).save(captor.capture());
        assertNull(captor.getValue().getCreationdate(), "creationdate should NOT be set for existing records");

        verify(communityService, times(1)).saveWallet(any(WalletDTO.class));
    }

    // ==================== TC_RM_003 ====================
    @Test
    @DisplayName("TC_RM_003: saveRecruiterManagement - Edge case - repo.save() returns null")
    void testSaveRecruiterManagement_SaveReturnsNull() {
        // Arrange
        RecruiterManagementDTO dto = buildDto(null, "Test", 10L, 5, "n");

        when(tokenWrapper.getUid()).thenReturn(1L);
        when(recruiterManagementRepo.save(any(RecruiterManagement.class))).thenReturn(null);

        // Act
        ResponseEntity<Response> response = recruiterManagementService.saveRecruiterManagement(dto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify saveWallet was NOT called when save fails
        verify(communityService, never()).saveWallet(any(WalletDTO.class));
    }

    // ==================== TC_RM_004 ====================
    @Test
    @DisplayName("TC_RM_004: saveRecruiterManagement - Edge case - null comment and note fields")
    void testSaveRecruiterManagement_NullOptionalFields() {
        // Arrange
        RecruiterManagementDTO dto = buildDto(null, null, 10L, 0, null);

        when(tokenWrapper.getUid()).thenReturn(1L);

        RecruiterManagement savedEntity = RecruiterManagement.builder().id(1L).build();
        when(recruiterManagementRepo.save(any(RecruiterManagement.class))).thenReturn(savedEntity);
        when(communityService.saveWallet(any(WalletDTO.class))).thenReturn(null);

        // Act
        ResponseEntity<Response> response = recruiterManagementService.saveRecruiterManagement(dto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<RecruiterManagement> captor = ArgumentCaptor.forClass(RecruiterManagement.class);
        verify(recruiterManagementRepo).save(captor.capture());
        assertNull(captor.getValue().getComment());
        assertNull(captor.getValue().getNote());
    }

    // ==================== TC_RM_005 ====================
    @Test
    @DisplayName("TC_RM_005: saveRecruiterManagement - Boundary - ratingStar = 0")
    void testSaveRecruiterManagement_RatingStarZero() {
        // Arrange
        RecruiterManagementDTO dto = buildDto(null, "c", 1L, 0, "n");

        when(tokenWrapper.getUid()).thenReturn(1L);
        RecruiterManagement savedEntity = RecruiterManagement.builder().id(1L).build();
        when(recruiterManagementRepo.save(any(RecruiterManagement.class))).thenReturn(savedEntity);
        when(communityService.saveWallet(any(WalletDTO.class))).thenReturn(null);

        // Act
        ResponseEntity<Response> response = recruiterManagementService.saveRecruiterManagement(dto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<RecruiterManagement> captor = ArgumentCaptor.forClass(RecruiterManagement.class);
        verify(recruiterManagementRepo).save(captor.capture());
        assertEquals(0, captor.getValue().getRatingstar());
    }

    // ==================== TC_RM_006 ====================
    @Test
    @DisplayName("TC_RM_006: saveRecruiterManagement - Verify saveWallet is called with correct userId")
    void testSaveRecruiterManagement_VerifySaveWalletUserId() {
        // Arrange
        RecruiterManagementDTO dto = buildDto(null, "c", 1L, 5, "n");

        when(tokenWrapper.getUid()).thenReturn(99L);
        RecruiterManagement savedEntity = RecruiterManagement.builder().id(1L).build();
        when(recruiterManagementRepo.save(any(RecruiterManagement.class))).thenReturn(savedEntity);
        when(communityService.saveWallet(any(WalletDTO.class))).thenReturn(null);

        // Act
        recruiterManagementService.saveRecruiterManagement(dto);

        // Assert
        ArgumentCaptor<WalletDTO> walletCaptor = ArgumentCaptor.forClass(WalletDTO.class);
        verify(communityService).saveWallet(walletCaptor.capture());
        assertEquals(99L, walletCaptor.getValue().getUserId());
        assertEquals(BONUS_POINT, walletCaptor.getValue().getAddingPoint());
    }

    // ==================== TC_RM_007 ====================
    @Test
    @DisplayName("TC_RM_007: saveRecruiterManagement - Verify entity fields mapping")
    void testSaveRecruiterManagement_FieldMapping() {
        // Arrange
        RecruiterManagementDTO dto = buildDto(3L, "c1", 20L, 4, "n1");

        when(tokenWrapper.getUid()).thenReturn(2L);
        RecruiterManagement savedEntity = RecruiterManagement.builder().id(3L).build();
        when(recruiterManagementRepo.save(any(RecruiterManagement.class))).thenReturn(savedEntity);
        when(communityService.saveWallet(any(WalletDTO.class))).thenReturn(null);

        // Act
        recruiterManagementService.saveRecruiterManagement(dto);

        // Assert
        ArgumentCaptor<RecruiterManagement> captor = ArgumentCaptor.forClass(RecruiterManagement.class);
        verify(recruiterManagementRepo).save(captor.capture());

        RecruiterManagement captured = captor.getValue();
        assertEquals(3L, captured.getId());
        assertEquals("c1", captured.getComment());
        assertEquals(20L, captured.getFreelancerid());
        assertEquals(4, captured.getRatingstar());
        assertEquals("n1", captured.getNote());
        assertEquals(2L, captured.getUserCommon().getId());
        assertNotNull(captured.getUpdatedate());
    }
}
