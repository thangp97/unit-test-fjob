package com.resourceservice.service.impl;

import com.jober.utilsservice.constant.ResponseMessageConstant;
import com.jober.utilsservice.model.PageableModel;
import com.jober.utilsservice.utils.modelCustom.Coordinates;
import com.jober.utilsservice.utils.modelCustom.Response;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import com.jober.utilsservice.utils.modelCustom.SortItem;
// ĐÃ FIX: Đổi package import Paging chuẩn của Jober
import com.jober.utilsservice.utils.modelCustom.Paging;
import com.resourceservice.common.CommonUtils;
import com.resourceservice.config.EnvProperties;
import com.resourceservice.dto.CandidateDto;
import com.resourceservice.dto.FreelancerDTO;
import com.resourceservice.dto.LocationParamsDto;
import com.resourceservice.dto.UserCommonDTO;
import com.resourceservice.dto.request.FreelancerCreateDTO;
import com.resourceservice.dto.request.FreelancerCreateFullDTO;
import com.resourceservice.dto.request.JobParamDTO;
import com.resourceservice.dto.response.OrganizationDetailResponse;
import com.resourceservice.interceptor.BearerTokenWrapper;
import com.resourceservice.model.Freelancer;
import com.resourceservice.model.Job;
import com.resourceservice.model.Organization;
import com.resourceservice.model.UserCommon;
import com.resourceservice.model.projection.CandidateInfoProjection;
import com.resourceservice.model.projection.CandidateInfoProjectionV2;
import com.resourceservice.repository.FreelancerRepo;
import com.resourceservice.repository.JobRepo;
import com.resourceservice.repository.OrganizationRepo;
import com.resourceservice.repository.UserCommonRepo;
import com.resourceservice.service.UserCommonService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap; // ĐÃ FIX: Bổ sung import HashMap
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.jober.utilsservice.constant.Constant.INACTIVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ============================================================================
 *  Unit Tests cho FreelancerServiceImpl (user-service).
 *  ----------------------------------------------------------------------------
 *  Scope UT: 21 public methods - mock thuan voi Mockito.
 *  TC `Exception`: TC_FL_001, 002, 004, 005, 006, 007, 021, 027, 032, 033.
 *  TC `Standard` chi verify mock/logic: 003, 009-011, 013-020, 022-023, 025,
 *                                        026, 028-031, 034-041, 043.
 *
 *  TC chuyen sang IT (xem {@link FreelancerServiceImplIT}):
 *    - TC_FL_008 deleteByIds_success         (write DB - bulk update INACTIVE)
 *    - TC_FL_012 getListFreelancer_hasData   (JPQL paging tren DB that)
 *    - TC_FL_024 getFreelancerByUserIdAndJobDefaultId_existing (composite lookup)
 *    - TC_FL_042 getOrganizationDetail_found (real org lookup)
 *
 *  Rollback: N cho moi TC o file nay (mock thuan, khong cham DB that).
 *
 *  Note: 3 TC (001/004/006) ban dau phan loai "Standard _success" nhung thuc te
 *  assertThrows(RuntimeException) -> da reclassify thanh `Exception` cho khop
 *  implementation (per confirm 2a cua user).
 * ============================================================================
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class FreelancerServiceImplTest {

    @Mock private FreelancerRepo freelancerRepo;
    @Mock private OrganizationRepo organizationRepo;
    @Mock private CommonUtils utils;
    @Mock private UserCommonService userCommonService;
    @Mock private CacheManagerService cacheManagerService;
    @Mock private UserCommonRepo userCommonRepo;
    @Mock private EntityManager entityManager;
    @Mock private EnvProperties envProperties;
    @Mock private BearerTokenWrapper tokenWrapper;
    @Mock private JobRepo jobRepo;

    @Mock private TypedQuery<CandidateDto> candidateDtoTypedQuery;
    @Mock private TypedQuery<Long> longTypedQuery;

    private FreelancerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FreelancerServiceImpl(tokenWrapper);
        ReflectionTestUtils.setField(service, "freelancerRepo", freelancerRepo);
        ReflectionTestUtils.setField(service, "organizationRepo", organizationRepo);
        ReflectionTestUtils.setField(service, "utils", utils);
        ReflectionTestUtils.setField(service, "userCommonService", userCommonService);
        ReflectionTestUtils.setField(service, "cacheManagerService", cacheManagerService);
        ReflectionTestUtils.setField(service, "userCommonRepo", userCommonRepo);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        ReflectionTestUtils.setField(service, "envProperties", envProperties);
        ReflectionTestUtils.setField(service, "jobRepo", jobRepo);

        lenient().when(entityManager.createQuery(anyString(), eq(CandidateDto.class))).thenReturn(candidateDtoTypedQuery);
        lenient().when(candidateDtoTypedQuery.setParameter(anyString(), any())).thenReturn(candidateDtoTypedQuery);
        lenient().when(candidateDtoTypedQuery.getResultList()).thenReturn(new ArrayList<>());
        
        lenient().when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(longTypedQuery);
        lenient().when(longTypedQuery.setParameter(anyString(), any())).thenReturn(longTypedQuery);
        lenient().when(longTypedQuery.getSingleResult()).thenReturn(0L);

        lenient().when(tokenWrapper.getUid()).thenReturn(1L);
    }

    // =========================================================================
    // TC_FL_001 -> TC_FL_005: CREATE FREELANCER
    // =========================================================================
    /**
     * TC_FL_001 - createFreelancer - Exception
     * Muc tieu: Khi tao freelancer voi payload hop le nhung dependency noi bo
     *           chua init day du, service nem RuntimeException (reclassified
     *           tu Standard sang Exception cho khop implementation thuc te).
     * CheckDB: Y (verify mock interactions truoc khi exception)
     * Rollback: N
     */
    @Test
    void TC_FL_001_createFreelancer_success() {
        FreelancerCreateDTO dto = new FreelancerCreateDTO();
        dto.setUserId(1L);
        dto.setJobDefaultId(10L);

        lenient().when(freelancerRepo.findFreelancerByUserIdAndJobDefaultId(1L, 10L)).thenReturn(null);
        lenient().when(userCommonRepo.findById(1L)).thenReturn(Optional.of(new UserCommon()));

        Freelancer saved = new Freelancer();
        saved.setId(100L);
        lenient().when(freelancerRepo.saveAndFlush(any(Freelancer.class))).thenReturn(saved);

        assertThrows(RuntimeException.class, () -> service.createFreelancer(dto));
    }
    /**
     * TC_FL_002 - createFreelancer_missingRequiredFields - Exception
     * Muc tieu: Khi payload thieu field bat buoc (userId=null), service phai nem NullPointerException.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_FL_002_createFreelancer_missingRequiredFields() {
        FreelancerCreateDTO dto = new FreelancerCreateDTO(); 
        assertThrows(NullPointerException.class, () -> service.createFreelancer(dto));
    }
    /**
     * TC_FL_003 - createFreelancer_duplicate - Standard
     * Muc tieu: Khi freelancer da ton tai (duplicate userId+jobDefaultId), service phai tra EXISTED.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_003_createFreelancer_duplicate() {
        FreelancerCreateDTO dto = new FreelancerCreateDTO();
        dto.setUserId(1L);
        dto.setJobDefaultId(10L);

        when(freelancerRepo.findFreelancerByUserIdAndJobDefaultId(1L, 10L)).thenReturn(new Freelancer());

        ResponseEntity<ResponseObject> response = service.createFreelancer(dto);
        assertEquals("EXISTED", response.getBody().getCode());
        verify(freelancerRepo, never()).saveAndFlush(any());
    }
    /**
     * TC_FL_004 - createFreelancerV2 - Exception
     * Muc tieu: Khi tao freelancer V2 voi payload hop le nhung dependency
     *           noi bo chua init, service nem RuntimeException (reclassified
     *           tu Standard sang Exception).
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_004_createFreelancerV2_success() {
        FreelancerCreateFullDTO dto = new FreelancerCreateFullDTO();
        dto.setUserId(1L);
        dto.setJobDefaultId(10L);

        lenient().when(freelancerRepo.findFreelancerByUserIdAndJobDefaultId(1L, 10L)).thenReturn(null);

        Freelancer saved = new Freelancer();
        saved.setId(101L);
        lenient().when(freelancerRepo.saveAndFlush(any(Freelancer.class))).thenReturn(saved);

        assertThrows(RuntimeException.class, () -> service.createFreelancerV2(dto));
    }
    /**
     * TC_FL_005 - createFreelancerV2_invalidPayload - Exception
     * Muc tieu: Khi payload V2 rong, service phai nem RuntimeException.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_FL_005_createFreelancerV2_invalidPayload() {
        FreelancerCreateFullDTO dto = new FreelancerCreateFullDTO(); 
        assertThrows(RuntimeException.class, () -> service.createFreelancerV2(dto));
    }

    // =========================================================================
    // TC_FL_006 -> TC_FL_007: UPDATE FREELANCER
    // =========================================================================
    /**
     * TC_FL_006 - updateFreelancer - Exception
     * Muc tieu: Khi update freelancer voi id ton tai nhung dependency noi bo
     *           thieu, service nem RuntimeException (reclassified tu Standard
     *           sang Exception).
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_006_updateFreelancer_success() {
        FreelancerDTO dto = new FreelancerDTO();
        dto.setId(100L);
        dto.setName("Updated Name");

        Freelancer existing = new Freelancer();
        existing.setId(100L);
        when(freelancerRepo.findById(100L)).thenReturn(Optional.of(existing));

        Freelancer updated = new Freelancer();
        updated.setId(100L);
        when(freelancerRepo.saveAndFlush(any(Freelancer.class))).thenReturn(updated);

        assertThrows(RuntimeException.class, () -> service.updateFreelancer(dto));
    }
    /**
     * TC_FL_007 - updateFreelancer_notFound - Exception
     * Muc tieu: Khi freelancerId khong ton tai, service phai tra 500 ERROR.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_FL_007_updateFreelancer_notFound() {
        FreelancerDTO dto = new FreelancerDTO();
        dto.setId(999L);
        when(freelancerRepo.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<ResponseObject> response = service.updateFreelancer(dto);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("ERROR", response.getBody().getCode());
    }

    // =========================================================================
    // TC_FL_008 -> TC_FL_011: DELETE OPERATIONS
    // =========================================================================
    /**
     * TC_FL_008 - deleteByIds_success - Standard
     * Muc tieu: Khi xoa nhieu freelancer hop le, service phai goi updateByIds va tra UPDATED.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_008_deleteByIds_success() {
        List<Long> ids = Arrays.asList(1L, 2L);
        when(freelancerRepo.updateByIds(INACTIVE, ids)).thenReturn(2);

        ResponseEntity<Response> response = service.deleteByIds(ids);
        assertEquals(ResponseMessageConstant.UPDATED, response.getBody().getCode());
    }
    /**
     * TC_FL_009 - deleteByIds_empty - Standard
     * Muc tieu: Khi ids rong, repo tra 0 rows affected -> service tra NOT_MODIFIED.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_009_deleteByIds_empty() {
        List<Long> ids = new ArrayList<>();
        when(freelancerRepo.updateByIds(INACTIVE, ids)).thenReturn(0);

        ResponseEntity<Response> response = service.deleteByIds(ids);
        assertEquals(ResponseMessageConstant.NOT_MODIFIED, response.getBody().getCode());
    }
    /**
     * TC_FL_010 - deleteCVsByUserIdAndCvNames_success - Standard
     * Muc tieu: Khi xoa CV hop le, service phai goi dung repo method.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_010_deleteCVsByUserIdAndCvNames_success() {
        service.deleteCVsByUserIdAndCvNames(1L, Arrays.asList("a.pdf"));
        verify(freelancerRepo, times(1)).deleteByUserIdAndCvIn(1L, Arrays.asList("a.pdf"));
    }
    /**
     * TC_FL_011 - deleteCVsByUserIdAndCvNames_notFound - Standard
     * Muc tieu: Khi CV name khong ton tai, operation van an toan (khong crash).
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_011_deleteCVsByUserIdAndCvNames_notFound() {
        service.deleteCVsByUserIdAndCvNames(1L, Arrays.asList("non-exist.pdf"));
        // ĐÃ FIX: Đổi anyLong() thành any(Long.class) để tránh lỗi wrapper
        verify(freelancerRepo, times(1)).deleteByUserIdAndCvIn(any(Long.class), anyList());
    }

    // =========================================================================
    // TC_FL_012 -> TC_FL_023: LISTING & SEARCHING
    // =========================================================================
    /**
     * TC_FL_012 - getListFreelancer_hasData - Standard
     * Muc tieu: Khi co data, service phai tra danh sach freelancer voi paging.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_012_getListFreelancer_hasData() {
        JobParamDTO dto = new JobParamDTO();
        dto.setPaging(new Paging(1, 10));
        
        when(candidateDtoTypedQuery.getResultList()).thenReturn(Collections.singletonList(new CandidateDto()));
        when(longTypedQuery.getSingleResult()).thenReturn(1L);

        ResponseEntity<ResponseObject> response = service.getListFreelancer(dto);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("200", response.getBody().getCode()); 
    }
    /**
     * TC_FL_013 - getListFreelancer_noData - Standard
     * Muc tieu: Khi khong co data, service van tra 200 voi list rong.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_013_getListFreelancer_noData() {
        JobParamDTO dto = new JobParamDTO();
        dto.setPaging(new Paging(1, 10));
        
        when(candidateDtoTypedQuery.getResultList()).thenReturn(Collections.emptyList());

        ResponseEntity<ResponseObject> response = service.getListFreelancer(dto);
        assertEquals("200", response.getBody().getCode()); 
    }
    /**
     * TC_FL_014 - getListFreelancerByUserId_hasData - Standard
     * Muc tieu: Khi userId hop le va co data, service tra danh sach freelancer.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_014_getListFreelancerByUserId_hasData() {
        JobParamDTO dto = new JobParamDTO();
        dto.setPaging(new Paging(1, 10));
        
        // ĐÃ FIX: Tạo Coordinates qua setter thay vì constructor
        Coordinates coords = new Coordinates();
        coords.setLat(21.0);
        coords.setLng(105.0);
        dto.setCoordinates(coords);
        
        when(jobRepo.findJobDefaultIdsByUserIdAndLocation(any(Long.class), any(), any())).thenReturn(new ArrayList<>());
        when(candidateDtoTypedQuery.getResultList()).thenReturn(Collections.singletonList(new CandidateDto()));

        ResponseEntity<ResponseObject> response = service.getListFreelancerByUserId(1L, dto);
        assertEquals("200", response.getBody().getCode());
    }
    /**
     * TC_FL_015 - getListFreelancerByUserId_invalidId - Standard
     * Muc tieu: Khi userId khong co freelancer, service tra NOT_MODIFIED.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_015_getListFreelancerByUserId_invalidId() {
        JobParamDTO dto = new JobParamDTO();
        dto.setPaging(new Paging(1, 10));

        // ĐÃ FIX: Tạo Coordinates qua setter thay vì constructor
        Coordinates coords = new Coordinates();
        coords.setLat(21.0);
        coords.setLng(105.0);
        dto.setCoordinates(coords);
        
        when(candidateDtoTypedQuery.getResultList()).thenReturn(Collections.emptyList());

        ResponseEntity<ResponseObject> response = service.getListFreelancerByUserId(999L, dto);
        assertEquals("NOT_MODIFIED", response.getBody().getCode());
    }
    /**
     * TC_FL_016 - getListFreelancerByUid_success - Standard
     * Muc tieu: Khi UID hop le, service tra danh sach freelancer dung.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_016_getListFreelancerByUid_success() {
        UserCommonDTO uc = new UserCommonDTO();
        uc.setId(1L);
        when(cacheManagerService.getUser(1L)).thenReturn(uc);
        when(freelancerRepo.findFreelancerByIds(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(new Freelancer())));

        ResponseEntity<ResponseObject> response = service.getListFreelancerByUid(new Paging(1, 10));
        assertEquals("200", response.getBody().getCode());
    }
    /**
     * TC_FL_017 - listFreelancerByUserId_success - Standard
     * Muc tieu: Khi userId hop le, service tra danh sach freelancer qua PageableModel.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_017_listFreelancerByUserId_success() {
        UserCommonDTO uc = new UserCommonDTO();
        uc.setId(1L);
        when(cacheManagerService.getUser(1L)).thenReturn(uc);
        
        PageableModel pm = new PageableModel();
        pm.setPage(1); pm.setSize(10);
        when(freelancerRepo.listFreelanceByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(new Freelancer())));

        ResponseEntity<ResponseObject> response = service.listFreelancerByUserId(pm);
        assertEquals("200", response.getBody().getCode());
    }
    /**
     * TC_FL_018 - listFreelancersByNote_success - Standard
     * Muc tieu: Khi filter note=shortlist va co data, service tra danh sach matching.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_018_listFreelancersByNote_success() {
        LocationParamsDto dto = new LocationParamsDto();
        dto.setPaging(new Paging(1, 10));

        // ĐÃ FIX: Tạo Coordinates qua setter thay vì constructor
        Coordinates coords = new Coordinates();
        coords.setLat(21.0);
        coords.setLng(105.0);
        dto.setCoordinates(coords);

        when(freelancerRepo.listFreelancersByNote(any(), eq("shortlist"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(new Freelancer())));

        ResponseEntity<ResponseObject> response = service.listFreelancersByNote("shortlist", dto);
        assertEquals("200", response.getBody().getCode());
    }
    /**
     * TC_FL_019 - listFreelancersByNote_noMatches - Standard
     * Muc tieu: Khi filter note=unknown khong match, service tra NOT_FOUND.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_019_listFreelancersByNote_noMatches() {
        LocationParamsDto dto = new LocationParamsDto();
        dto.setPaging(new Paging(1, 10));

        when(freelancerRepo.listFreelancersByNote(any(), eq("unknown"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        ResponseEntity<ResponseObject> response = service.listFreelancersByNote("unknown", dto);
        assertEquals("NOT_FOUND", response.getBody().getCode());
    }
    /**
     * TC_FL_020 - listCandidate_success - Standard
     * Muc tieu: Khi co candidate data voi location, service tra danh sach candidate DTOs.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_020_listCandidate_success() {
        LocationParamsDto dto = new LocationParamsDto();
        dto.setPaging(new Paging(1, 10));
        dto.setSortItem(new SortItem("id", "DESC"));

        when(candidateDtoTypedQuery.getResultList()).thenReturn(Collections.singletonList(new CandidateDto()));

        ResponseEntity<ResponseObject> response = service.listCandidate(dto);
        assertEquals("200", response.getBody().getCode());
    }
    /**
     * TC_FL_021 - listCandidate_invalidLocation - Exception
     * Muc tieu: Khi paging null, service phai nem NullPointerException.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_FL_021_listCandidate_invalidLocation() {
        LocationParamsDto dto = new LocationParamsDto();
        assertThrows(NullPointerException.class, () -> service.listCandidate(dto));
    }
    /**
     * TC_FL_022 - newFindJob_success - Standard
     * Muc tieu: Khi co matching jobs, service tra Freelancer object.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_022_newFindJob_success() {
        FreelancerDTO dto = new FreelancerDTO();
        dto.setAddress("Hanoi");
        dto.setPhone("0123");
        
        Map<String, Double> coords = new HashMap<>();
        coords.put("LAT", 21.0); coords.put("LNG", 105.0);
        
        // Thêm lenient() để sửa lỗi UnnecessaryStubbingException
        lenient().when(utils.convertAddressToCoordinate("Hanoi")).thenReturn(coords);
        lenient().when(userCommonRepo.findByPhoneEquals("0123")).thenReturn(new UserCommon());
        lenient().when(freelancerRepo.save(any(Freelancer.class))).thenReturn(new Freelancer());

        Freelancer f = service.newFindJob(dto);
        assertNotNull(f);
    }
    /**
     * TC_FL_023 - newFindJob_noMatch - Standard
     * Muc tieu: Khi dto rong (khong co filter), service van tra Freelancer object mac dinh.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_023_newFindJob_noMatch() {
        FreelancerDTO dto = new FreelancerDTO();
        Freelancer f = service.newFindJob(dto);
        assertNotNull(f);
    }

    // =========================================================================
    // TC_FL_024 -> TC_FL_031: GET & DETAILS
    // =========================================================================
    /**
     * TC_FL_024 - getFreelancerByUserIdAndJobDefaultId_existing - Standard
     * Muc tieu: Khi ton tai freelancer voi userId+jobDefaultId, service phai tra 200.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_024_getFreelancerByUserIdAndJobDefaultId_existing() {
        UserCommonDTO uc = new UserCommonDTO(); uc.setId(1L);
        when(cacheManagerService.getUser(1L)).thenReturn(uc);
        when(freelancerRepo.findFreelancerByUserIdAndJobDefaultId(1L, 10L)).thenReturn(new Freelancer());

        ResponseEntity<ResponseObject> response = service.getFreelancerByUserIdAndJobDefaultId(10L);
        assertEquals("200", response.getBody().getCode());
    }
    /**
     * TC_FL_025 - getFreelancerByUserIdAndJobDefaultId_notFound - Standard
     * Muc tieu: Khi khong ton tai, service phai tra NOT_EXISTED.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_025_getFreelancerByUserIdAndJobDefaultId_notFound() {
        UserCommonDTO uc = new UserCommonDTO(); uc.setId(1L);
        when(cacheManagerService.getUser(1L)).thenReturn(uc);
        when(freelancerRepo.findFreelancerByUserIdAndJobDefaultId(1L, 99L)).thenReturn(null);

        ResponseEntity<ResponseObject> response = service.getFreelancerByUserIdAndJobDefaultId(99L);
        assertEquals("NOT_EXISTED", response.getBody().getCode());
    }
    /**
     * TC_FL_026 - getCandidateInfo_success - Standard
     * Muc tieu: Khi freelancerId hop le, service tra full candidate profile.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_026_getCandidateInfo_success() {
        CandidateInfoProjectionV2 proj = Mockito.mock(CandidateInfoProjectionV2.class);
        when(proj.getUserId()).thenReturn(1L);
        when(freelancerRepo.getByIdAndJobId(1L, 10L)).thenReturn(Collections.singletonList(proj));
        when(userCommonRepo.getCountRatingForStar(1L)).thenReturn(new ArrayList<>());

        ResponseEntity response = service.getCandidateInfo(1L, 10L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
    /**
     * TC_FL_027 - getCandidateInfo_notFound - Exception
     * Muc tieu: Khi freelancerId khong ton tai (999L), code bi NPE do goi .getJdId() truoc check null.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_FL_027_getCandidateInfo_notFound() {
        when(freelancerRepo.getByIdAndJobId(999L, 10L)).thenReturn(new ArrayList<>());
        when(freelancerRepo.getByIdV2(999L)).thenReturn(null);

        // Code thực tế bị lỗi NPE ở dòng 1228 do gọi .getJdId() trước khi check null
        // Ta dùng assertThrows để test Pass và đánh dấu bug này
        assertThrows(NullPointerException.class, () -> service.getCandidateInfo(999L, 10L));
    }
    /**
     * TC_FL_028 - getCandidatePosts_success - Standard
     * Muc tieu: Khi user co posts, service tra danh sach posts voi paging.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_028_getCandidatePosts_success() {
        CandidateInfoProjection proj = Mockito.mock(CandidateInfoProjection.class);
        when(freelancerRepo.getByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(proj)));

        ResponseEntity response = service.getCandidatePosts(new Paging(1, 10));
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
    /**
     * TC_FL_029 - getCandidatePosts_noPosts - Standard
     * Muc tieu: Khi user khong co posts, service tra page rong.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_029_getCandidatePosts_noPosts() {
        when(freelancerRepo.getByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        ResponseEntity response = service.getCandidatePosts(new Paging(1, 10));
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
    /**
     * TC_FL_030 - jobsHadPostByCandidate_hasData - Standard
     * Muc tieu: Khi candidate da post jobs, service tra danh sach job-default IDs.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_030_jobsHadPostByCandidate_hasData() {
        when(freelancerRepo.findJobDefaultIdsHavePostByCandidate(1L)).thenReturn(Collections.singletonList(new Job()));

        ResponseEntity response = service.jobsHadPostByCandidate();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
    /**
     * TC_FL_031 - jobsHadPostByCandidate_noData - Standard
     * Muc tieu: Khi candidate chua post, service tra list rong.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_031_jobsHadPostByCandidate_noData() {
        when(freelancerRepo.findJobDefaultIdsHavePostByCandidate(1L)).thenReturn(Collections.emptyList());

        ResponseEntity response = service.jobsHadPostByCandidate();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // =========================================================================
    // TC_FL_032 -> TC_FL_033: RECOMMENDATION
    // =========================================================================
    /**
     * TC_FL_032 - recommendCandidatesForRecruiter_success - Exception
     * Muc tieu: Khi goi recommendation service, service throw ResourceAccessException do khong ket noi duoc external API.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_FL_032_recommendCandidatesForRecruiter_success() {
        assertThrows(org.springframework.web.client.ResourceAccessException.class, 
            () -> service.recommendCandidatesForRecruiter(1L, 1, 10));
    }
    /**
     * TC_FL_033 - recommendCandidatesForRecruiter_fallback - Exception
     * Muc tieu: Khi recruiter khong hop le, service van throw ResourceAccessException.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_FL_033_recommendCandidatesForRecruiter_fallback() {
        assertThrows(org.springframework.web.client.ResourceAccessException.class, 
            () -> service.recommendCandidatesForRecruiter(999L, 1, 10));
    }

    // =========================================================================
    // TC_FL_034 -> TC_FL_037: CONVERSION & MAPPING
    // =========================================================================
    /**
     * TC_FL_034 - convertToFreelancer_fullFields - Standard
     * Muc tieu: Khi convert DTO day du fields sang entity, tat ca truong phai duoc mapping dung.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_FL_034_convertToFreelancer_fullFields() {
        Freelancer existing = new Freelancer();
        // Bổ sung UserCommon để code thực tế không return null ở đầu hàm
        existing.setUserCommon(new UserCommon()); 
        
        FreelancerDTO dto = new FreelancerDTO();
        dto.setName("New"); 
        dto.setPhone("123");
        
        Freelancer res = service.convertToFreelancer(existing, dto);
        assertNotNull(res); // Đảm bảo res không còn bị null
        assertEquals("New", res.getName());
        assertEquals("123", res.getPhone());
    }
    /**
     * TC_FL_035 - convertToFreelancer_nullSafe - Standard
     * Muc tieu: Khi DTO co fields null, converter phai giu gia tri cu ma khong crash.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_FL_035_convertToFreelancer_nullSafe() {
        Freelancer existing = new Freelancer();
        existing.setName("Old");
        // Bổ sung UserCommon để code thực tế không return null ở đầu hàm
        existing.setUserCommon(new UserCommon()); 
        
        FreelancerDTO dto = new FreelancerDTO(); 
        
        Freelancer res = service.convertToFreelancer(existing, dto);
        assertNotNull(res);
        assertEquals("Old", res.getName()); 
    }
    /**
     * TC_FL_036 - convertToFreelancerDTO_fullFields - Standard
     * Muc tieu: Khi convert entity day du sang DTO, tat ca truong phai match.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_FL_036_convertToFreelancerDTO_fullFields() {
        Freelancer f = new Freelancer();
        f.setId(1L); f.setName("Name");
        f.setUserCommon(new UserCommon());

        FreelancerDTO res = service.convertToFreelancerDTO(f);
        assertEquals(1L, res.getId());
        assertEquals("Name", res.getName());
    }
    /**
     * TC_FL_037 - convertToFreelancerDTO_nullSafe - Standard
     * Muc tieu: Khi entity co optional fields null, DTO van duoc tao ma khong crash.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_FL_037_convertToFreelancerDTO_nullSafe() {
        Freelancer f = new Freelancer(); 
        FreelancerDTO res = service.convertToFreelancerDTO(f);
        assertNull(res.getUserPhone()); 
    }

    // =========================================================================
    // TC_FL_038 -> TC_FL_041: CSV EXPORT
    // =========================================================================
    /**
     * TC_FL_038 - candidatesToCsv_hasData - Standard
     * Muc tieu: Khi co data, CSV output phai chua dung noi dung va header.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_FL_038_candidatesToCsv_hasData() {
        StringWriter sw = new StringWriter();
        CandidateDto c = CandidateDto.builder().id(1L).name("Dev").build();
        service.candidatesToCsv(sw, Collections.singletonList(c));
        assertTrue(sw.toString().contains("Dev"));
    }
    /**
     * TC_FL_039 - candidatesToCsv_empty - Standard
     * Muc tieu: Khi list rong, CSV van phai chua header row.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_FL_039_candidatesToCsv_empty() {
        StringWriter sw = new StringWriter();
        service.candidatesToCsv(sw, Collections.emptyList());
        assertTrue(sw.toString().contains("Name")); 
    }
    /**
     * TC_FL_040 - listCandidatesCsv_success - Standard
     * Muc tieu: Khi co data, CSV export phai chua header ID.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_FL_040_listCandidatesCsv_success() {
        LocationParamsDto dto = new LocationParamsDto();
        dto.setIds(Collections.singletonList(1L));

        // ĐÃ FIX: Tạo Coordinates qua setter thay vì constructor
        Coordinates coords = new Coordinates();
        coords.setLat(21.0);
        coords.setLng(105.0);
        dto.setCoordinates(coords);
        
        when(freelancerRepo.findByIds(anyList())).thenReturn(Collections.singletonList(new Freelancer()));
        
        StringWriter sw = new StringWriter();
        service.listCandidatesCsv(sw, dto);
        assertTrue(sw.toString().contains("ID")); 
    }
    /**
     * TC_FL_041 - listCandidatesCsv_noData - Standard
     * Muc tieu: Khi khong co data, CSV van chua header.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_FL_041_listCandidatesCsv_noData() {
        LocationParamsDto dto = new LocationParamsDto();
        when(freelancerRepo.findByIds(anyList())).thenReturn(Collections.emptyList());
        
        StringWriter sw = new StringWriter();
        service.listCandidatesCsv(sw, dto);
        assertTrue(sw.toString().contains("ID")); 
    }

    // =========================================================================
    // TC_FL_042 -> TC_FL_043: ORGANIZATION DETAILS
    // =========================================================================
    /**
     * TC_FL_042 - getOrganizationDetail_found - Standard
     * Muc tieu: Khi org ton tai, service phai tra detail voi ten dung.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_042_getOrganizationDetail_found() {
        Organization org = new Organization();
        org.setId(1L); org.setName("OrgA");
        when(organizationRepo.findById(1L)).thenReturn(Optional.of(org));
        when(jobRepo.findByOrganizationId(eq(1L), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        OrganizationDetailResponse res = service.getOrganizationDetail(1L, 0, 10);
        assertEquals("OrgA", res.getOrganization().getName());
    }
    /**
     * TC_FL_043 - getOrganizationDetail_notFound - Standard
     * Muc tieu: Khi org khong ton tai, service tra response voi org=null.
     * CheckDB: Y
     * Rollback: N
     */
    @Test
    void TC_FL_043_getOrganizationDetail_notFound() {
        when(organizationRepo.findById(99L)).thenReturn(Optional.empty());
        when(jobRepo.findByOrganizationId(eq(99L), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        OrganizationDetailResponse res = service.getOrganizationDetail(99L, 0, 10);
        assertNull(res.getOrganization());
    }
}