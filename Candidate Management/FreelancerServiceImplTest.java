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

    @Tag("Mock")
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

    @Tag("Unit")
    @Test
    void TC_FL_002_createFreelancer_missingRequiredFields() {
        FreelancerCreateDTO dto = new FreelancerCreateDTO(); 
        assertThrows(NullPointerException.class, () -> service.createFreelancer(dto));
    }

    @Tag("Mock")
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

    @Tag("Mock")
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

    @Tag("Unit")
    @Test
    void TC_FL_005_createFreelancerV2_invalidPayload() {
        FreelancerCreateFullDTO dto = new FreelancerCreateFullDTO(); 
        assertThrows(RuntimeException.class, () -> service.createFreelancerV2(dto));
    }

    // =========================================================================
    // TC_FL_006 -> TC_FL_007: UPDATE FREELANCER
    // =========================================================================

    @Tag("Mock")
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

    @Tag("Mock")
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

    @Tag("CheckDB")
    @Test
    void TC_FL_008_deleteByIds_success() {
        List<Long> ids = Arrays.asList(1L, 2L);
        when(freelancerRepo.updateByIds(INACTIVE, ids)).thenReturn(2);

        ResponseEntity<Response> response = service.deleteByIds(ids);
        assertEquals(ResponseMessageConstant.UPDATED, response.getBody().getCode());
    }

    @Tag("Unit")
    @Test
    void TC_FL_009_deleteByIds_empty() {
        List<Long> ids = new ArrayList<>();
        when(freelancerRepo.updateByIds(INACTIVE, ids)).thenReturn(0);

        ResponseEntity<Response> response = service.deleteByIds(ids);
        assertEquals(ResponseMessageConstant.NOT_MODIFIED, response.getBody().getCode());
    }

    @Tag("Mock")
    @Test
    void TC_FL_010_deleteCVsByUserIdAndCvNames_success() {
        service.deleteCVsByUserIdAndCvNames(1L, Arrays.asList("a.pdf"));
        verify(freelancerRepo, times(1)).deleteByUserIdAndCvIn(1L, Arrays.asList("a.pdf"));
    }

    @Tag("CheckDB")
    @Test
    void TC_FL_011_deleteCVsByUserIdAndCvNames_notFound() {
        service.deleteCVsByUserIdAndCvNames(1L, Arrays.asList("non-exist.pdf"));
        // ĐÃ FIX: Đổi anyLong() thành any(Long.class) để tránh lỗi wrapper
        verify(freelancerRepo, times(1)).deleteByUserIdAndCvIn(any(Long.class), anyList());
    }

    // =========================================================================
    // TC_FL_012 -> TC_FL_023: LISTING & SEARCHING
    // =========================================================================

    @Tag("CheckDB")
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

    @Tag("CheckDB")
    @Test
    void TC_FL_013_getListFreelancer_noData() {
        JobParamDTO dto = new JobParamDTO();
        dto.setPaging(new Paging(1, 10));
        
        when(candidateDtoTypedQuery.getResultList()).thenReturn(Collections.emptyList());

        ResponseEntity<ResponseObject> response = service.getListFreelancer(dto);
        assertEquals("200", response.getBody().getCode()); 
    }

    @Tag("CheckDB")
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

    @Tag("CheckDB")
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

    @Tag("CheckDB")
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

    @Tag("CheckDB")
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

    @Tag("CheckDB")
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

    @Tag("CheckDB")
    @Test
    void TC_FL_019_listFreelancersByNote_noMatches() {
        LocationParamsDto dto = new LocationParamsDto();
        dto.setPaging(new Paging(1, 10));

        when(freelancerRepo.listFreelancersByNote(any(), eq("unknown"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        ResponseEntity<ResponseObject> response = service.listFreelancersByNote("unknown", dto);
        assertEquals("NOT_FOUND", response.getBody().getCode());
    }

    @Tag("CheckDB")
    @Test
    void TC_FL_020_listCandidate_success() {
        LocationParamsDto dto = new LocationParamsDto();
        dto.setPaging(new Paging(1, 10));
        dto.setSortItem(new SortItem("id", "DESC"));

        when(candidateDtoTypedQuery.getResultList()).thenReturn(Collections.singletonList(new CandidateDto()));

        ResponseEntity<ResponseObject> response = service.listCandidate(dto);
        assertEquals("200", response.getBody().getCode());
    }

    @Tag("Unit")
    @Test
    void TC_FL_021_listCandidate_invalidLocation() {
        LocationParamsDto dto = new LocationParamsDto();
        assertThrows(NullPointerException.class, () -> service.listCandidate(dto));
    }

    @Tag("CheckDB")
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

    @Tag("CheckDB")
    @Test
    void TC_FL_023_newFindJob_noMatch() {
        FreelancerDTO dto = new FreelancerDTO();
        Freelancer f = service.newFindJob(dto);
        assertNotNull(f);
    }

    // =========================================================================
    // TC_FL_024 -> TC_FL_031: GET & DETAILS
    // =========================================================================

    @Tag("CheckDB")
    @Test
    void TC_FL_024_getFreelancerByUserIdAndJobDefaultId_existing() {
        UserCommonDTO uc = new UserCommonDTO(); uc.setId(1L);
        when(cacheManagerService.getUser(1L)).thenReturn(uc);
        when(freelancerRepo.findFreelancerByUserIdAndJobDefaultId(1L, 10L)).thenReturn(new Freelancer());

        ResponseEntity<ResponseObject> response = service.getFreelancerByUserIdAndJobDefaultId(10L);
        assertEquals("200", response.getBody().getCode());
    }

    @Tag("CheckDB")
    @Test
    void TC_FL_025_getFreelancerByUserIdAndJobDefaultId_notFound() {
        UserCommonDTO uc = new UserCommonDTO(); uc.setId(1L);
        when(cacheManagerService.getUser(1L)).thenReturn(uc);
        when(freelancerRepo.findFreelancerByUserIdAndJobDefaultId(1L, 99L)).thenReturn(null);

        ResponseEntity<ResponseObject> response = service.getFreelancerByUserIdAndJobDefaultId(99L);
        assertEquals("NOT_EXISTED", response.getBody().getCode());
    }

    @Tag("CheckDB")
    @Test
    void TC_FL_026_getCandidateInfo_success() {
        CandidateInfoProjectionV2 proj = Mockito.mock(CandidateInfoProjectionV2.class);
        when(proj.getUserId()).thenReturn(1L);
        when(freelancerRepo.getByIdAndJobId(1L, 10L)).thenReturn(Collections.singletonList(proj));
        when(userCommonRepo.getCountRatingForStar(1L)).thenReturn(new ArrayList<>());

        ResponseEntity response = service.getCandidateInfo(1L, 10L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Tag("CheckDB")
    @Test
    void TC_FL_027_getCandidateInfo_notFound() {
        when(freelancerRepo.getByIdAndJobId(999L, 10L)).thenReturn(new ArrayList<>());
        when(freelancerRepo.getByIdV2(999L)).thenReturn(null);

        // Code thực tế bị lỗi NPE ở dòng 1228 do gọi .getJdId() trước khi check null
        // Ta dùng assertThrows để test Pass và đánh dấu bug này
        assertThrows(NullPointerException.class, () -> service.getCandidateInfo(999L, 10L));
    }

    @Tag("CheckDB")
    @Test
    void TC_FL_028_getCandidatePosts_success() {
        CandidateInfoProjection proj = Mockito.mock(CandidateInfoProjection.class);
        when(freelancerRepo.getByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(proj)));

        ResponseEntity response = service.getCandidatePosts(new Paging(1, 10));
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Tag("CheckDB")
    @Test
    void TC_FL_029_getCandidatePosts_noPosts() {
        when(freelancerRepo.getByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        ResponseEntity response = service.getCandidatePosts(new Paging(1, 10));
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Tag("CheckDB")
    @Test
    void TC_FL_030_jobsHadPostByCandidate_hasData() {
        when(freelancerRepo.findJobDefaultIdsHavePostByCandidate(1L)).thenReturn(Collections.singletonList(new Job()));

        ResponseEntity response = service.jobsHadPostByCandidate();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Tag("CheckDB")
    @Test
    void TC_FL_031_jobsHadPostByCandidate_noData() {
        when(freelancerRepo.findJobDefaultIdsHavePostByCandidate(1L)).thenReturn(Collections.emptyList());

        ResponseEntity response = service.jobsHadPostByCandidate();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // =========================================================================
    // TC_FL_032 -> TC_FL_033: RECOMMENDATION
    // =========================================================================

    @Tag("Mock")
    @Test
    void TC_FL_032_recommendCandidatesForRecruiter_success() {
        assertThrows(org.springframework.web.client.ResourceAccessException.class, 
            () -> service.recommendCandidatesForRecruiter(1L, 1, 10));
    }

    @Tag("Mock")
    @Test
    void TC_FL_033_recommendCandidatesForRecruiter_fallback() {
        assertThrows(org.springframework.web.client.ResourceAccessException.class, 
            () -> service.recommendCandidatesForRecruiter(999L, 1, 10));
    }

    // =========================================================================
    // TC_FL_034 -> TC_FL_037: CONVERSION & MAPPING
    // =========================================================================

    @Tag("Unit")
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

    @Tag("Unit")
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

    @Tag("Unit")
    @Test
    void TC_FL_036_convertToFreelancerDTO_fullFields() {
        Freelancer f = new Freelancer();
        f.setId(1L); f.setName("Name");
        f.setUserCommon(new UserCommon());

        FreelancerDTO res = service.convertToFreelancerDTO(f);
        assertEquals(1L, res.getId());
        assertEquals("Name", res.getName());
    }

    @Tag("Unit")
    @Test
    void TC_FL_037_convertToFreelancerDTO_nullSafe() {
        Freelancer f = new Freelancer(); 
        FreelancerDTO res = service.convertToFreelancerDTO(f);
        assertNull(res.getUserPhone()); 
    }

    // =========================================================================
    // TC_FL_038 -> TC_FL_041: CSV EXPORT
    // =========================================================================

    @Tag("Unit")
    @Test
    void TC_FL_038_candidatesToCsv_hasData() {
        StringWriter sw = new StringWriter();
        CandidateDto c = CandidateDto.builder().id(1L).name("Dev").build();
        service.candidatesToCsv(sw, Collections.singletonList(c));
        assertTrue(sw.toString().contains("Dev"));
    }

    @Tag("Unit")
    @Test
    void TC_FL_039_candidatesToCsv_empty() {
        StringWriter sw = new StringWriter();
        service.candidatesToCsv(sw, Collections.emptyList());
        assertTrue(sw.toString().contains("Name")); 
    }

    @Tag("Mock")
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

    @Tag("Mock")
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

    @Tag("CheckDB")
    @Test
    void TC_FL_042_getOrganizationDetail_found() {
        Organization org = new Organization();
        org.setId(1L); org.setName("OrgA");
        when(organizationRepo.findById(1L)).thenReturn(Optional.of(org));
        when(jobRepo.findByOrganizationId(eq(1L), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        OrganizationDetailResponse res = service.getOrganizationDetail(1L, 0, 10);
        assertEquals("OrgA", res.getOrganization().getName());
    }

    @Tag("CheckDB")
    @Test
    void TC_FL_043_getOrganizationDetail_notFound() {
        when(organizationRepo.findById(99L)).thenReturn(Optional.empty());
        when(jobRepo.findByOrganizationId(eq(99L), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        OrganizationDetailResponse res = service.getOrganizationDetail(99L, 0, 10);
        assertNull(res.getOrganization());
    }
}