package com.resourceservice.service.impl;

import com.resourceservice.dto.UserCommonDTO;
import com.resourceservice.service.UserCommonService;
import com.resourceservice.utilsmodule.CacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import static com.resourceservice.utilsmodule.constant.Constant.USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests cho {@link CacheManagerService}.
 * Cả {@code getUser} và {@code adminGetUser} hiện có logic y hệt nhau — test coverage cho cả hai.
 * Phạm vi: Phủ 100% method
 * CheckDB: N
 * Rollback: N
 */
@ExtendWith(MockitoExtension.class)
class CacheManagerServiceTest {

    @Mock private CacheService cacheService;
    @Mock private UserCommonService userCommonService;
    @Mock private CacheManager cacheManager;

    @InjectMocks private CacheManagerService service;

    // ======================= getUser =======================
    /**
     * TC_CACHE_001 - getUser - Standard
     * Mục tiêu: Cache hit -> trả UserCommonDTO.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_CACHE_001_getUser_cacheHit() {
        UserCommonDTO dto = new UserCommonDTO();
        dto.setId(1L);
        when(cacheService.getCache(eq(cacheManager), eq(USER), eq(USER + 1L))).thenReturn(dto);

        UserCommonDTO result = service.getUser(1L);

        assertEquals(1L, result.getId());
        verify(cacheService).getCache(cacheManager, USER, USER + 1L);
    }

    /**
     * TC_CACHE_002 - getUser - Exception
     * Mục tiêu: Cache miss -> trả null.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_CACHE_002_getUser_cacheMiss() {
        when(cacheService.getCache(eq(cacheManager), eq(USER), eq(USER + 99L))).thenReturn(null);

        UserCommonDTO result = service.getUser(99L);

        assertNull(result);
        verify(cacheService).getCache(cacheManager, USER, USER + 99L);
    }

    /**
     * TC_CACHE_003 - getUser - Exception
     * Mục tiêu: uid=null -> key="USERnull", trả null.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_CACHE_003_getUser_nullUid() {
        when(cacheService.getCache(eq(cacheManager), eq(USER), eq(USER + "null"))).thenReturn(null);

        UserCommonDTO result = service.getUser(null);

        assertNull(result);
        verify(cacheService).getCache(cacheManager, USER, USER + "null");
    }

    // ======================= adminGetUser =======================
    /**
     * TC_CACHE_004 - adminGetUser - Standard
     * Mục tiêu: Cache hit -> trả UserCommonDTO.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_CACHE_004_adminGetUser_cacheHit() {
        UserCommonDTO dto = new UserCommonDTO();
        dto.setId(4L);
        dto.setRole(4);
        when(cacheService.getCache(eq(cacheManager), eq(USER), eq(USER + 4L))).thenReturn(dto);

        UserCommonDTO result = service.adminGetUser(4L);

        assertEquals(4L, result.getId());
        assertEquals(4, result.getRole());
        verify(cacheService).getCache(cacheManager, USER, USER + 4L);
    }

    /**
     * TC_CACHE_005 - adminGetUser - Exception
     * Mục tiêu: Cache miss -> trả null.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_CACHE_005_adminGetUser_cacheMiss() {
        when(cacheService.getCache(eq(cacheManager), eq(USER), eq(USER + 7L))).thenReturn(null);

        UserCommonDTO result = service.adminGetUser(7L);

        assertNull(result);
        verify(cacheService).getCache(cacheManager, USER, USER + 7L);
    }

    /**
     * TC_CACHE_006 - adminGetUser - Exception
     * Mục tiêu: uid=0 -> key="USER0", trả null.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_CACHE_006_adminGetUser_zeroUid() {
        when(cacheService.getCache(eq(cacheManager), eq(USER), eq(USER + 0L))).thenReturn(null);

        UserCommonDTO result = service.adminGetUser(0L);

        assertNull(result);
        verify(cacheService).getCache(cacheManager, USER, USER + 0L);
    }
}
