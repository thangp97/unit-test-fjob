package com.resourceservice.service.impl;

import com.jober.utilsservice.model.PageableModel;
import com.jober.utilsservice.utils.modelCustom.Paging;
import com.jober.utilsservice.utils.modelCustom.Response;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import com.resourceservice.common.CommonUtils;
import com.resourceservice.dto.BonusDTO;
import com.resourceservice.dto.FreelancerDTO;
import com.resourceservice.dto.JobDTO;
import com.resourceservice.dto.PaymentDTO;
import com.resourceservice.dto.UserCommonDTO;
import com.resourceservice.dto.request.UserParamDTO;
import com.resourceservice.model.Freelancer;
import com.resourceservice.model.Job;
import com.resourceservice.model.Payment;
import com.resourceservice.model.RequestWithDrawing;
import com.resourceservice.model.Settings;
import com.resourceservice.model.UserCommon;
import com.resourceservice.repository.CandidateManagementRepo;
import com.resourceservice.repository.FreelancerRepo;
import com.resourceservice.repository.JobRepo;
import com.resourceservice.repository.PaymentRepo;
import com.resourceservice.repository.RecruiterManagementRepo;
import com.resourceservice.repository.RequestWithDrawingRepo;
import com.resourceservice.repository.SettingsRepo;
import com.resourceservice.repository.UserCommonRepo;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.resourceservice.utilsmodule.constant.Constant.STATISTICAL_BY_MONTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminServiceImplTest {

    @Mock private UserCommonRepo userCommonRepo;
    @Mock private CandidateManagementRepo candidateManagementRepo;
    @Mock private RecruiterManagementRepo recruiterManagementRepo;
    @Mock private FreelancerRepo freelancerRepo;
    @Mock private JobRepo jobRepo;
    @Mock private PaymentRepo paymentRepo;
    @Mock private FreelancerServiceImpl freelancerService;
    @Mock private JobServiceImpl jobService;
    @Mock private UserCommonServiceImpl userCommonService;
    @Mock private SettingsRepo settingsRepo;
    @Mock private RequestWithDrawingRepo requestWithDrawingRepo;
    @Mock private EntityManager entityManager;
    @Mock private CommonUtils utils;

    @InjectMocks private AdminServiceImpl service;

    private UserCommon buildUser(Long id) {
        UserCommon u = new UserCommon();
        u.setId(id);
        u.setPhone("09" + id);
        u.setName("User" + id);
        u.setEmail("u" + id + "@x.com");
        u.setCreationDate(LocalDateTime.now());
        return u;
    }

    private Job buildJob(Long id) {
        Job j = new Job();
        j.setId(id);
        j.setName("Job" + id);
        j.setJob("Java");
        j.setPhone("0912");
        j.setEmail("job@x.com");
        j.setAddress("HN");
        j.setProvince("HN");
        j.setWard("Cau Giay");
        j.setDes("desc");
        j.setNumber(1);
        j.setActive(1);
        j.setLevel(1);
        j.setWebsite("https://x.com");
        j.setSalary("1000");
        j.setLat(21.0);
        j.setLng(105.0);
        j.setExpDate(LocalDateTime.now().plusDays(10));
        j.setCreationDate(LocalDateTime.now());
        return j;
    }

    private PageableModel pageableModel(int page, int size) {
        PageableModel p = new PageableModel();
        p.setPage(page);
        p.setSize(size);
        return p;
    }

    private Paging paging(int page, int size) {
        return new Paging(page, size);
    }

    private MockMultipartFile buildXlsxFile(boolean forFreelancer) throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Sheet1");
        sheet.createRow(0);
        var row = sheet.createRow(1);

        if (forFreelancer) {
            row.createCell(0).setCellValue("0912345678");
            row.createCell(1).setCellValue("Ha Noi");
            row.createCell(2).setCellValue("Candidate A");
            row.createCell(3).setCellValue("Java Dev");
            row.createCell(4).setCellValue("1000");
            row.createCell(5).setCellValue("1999");
            row.createCell(6).setCellValue("male");
            row.createCell(7).setCellValue("desc");
        } else {
            row.createCell(0).setCellValue("0912345678");
            row.createCell(1).setCellValue("job@x.com");
            row.createCell(2).setCellValue("Recruiter A");
            row.createCell(3).setCellValue("Ha Noi");
            row.createCell(4).setCellValue("Backend");
            row.createCell(5).setCellValue("1200");
            row.createCell(6).setCellValue("desc");
            row.createCell(7).setCellValue("ignored");
            row.createCell(8).setCellValue(java.sql.Timestamp.valueOf(LocalDateTime.now().plusDays(10)));
            row.createCell(9).setCellValue(2);
            row.createCell(10).setCellValue("https://x.com");
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        workbook.write(output);
        workbook.close();
        return new MockMultipartFile("file", "in.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
    }

    @BeforeEach
    void setUp() {}

    @Tag("CheckDB")
    @Test
    void TC_ADM_001_deleteUser_success() {
        doNothing().when(recruiterManagementRepo).deleteRecruiterManagementByUserId(anyList());
        doNothing().when(candidateManagementRepo).deleteCandidateManagementByUsers(anyList());
        doNothing().when(freelancerRepo).deleteFreelancerByUsers(anyList());
        doNothing().when(jobRepo).deleteJobByUsers(anyList());
        doNothing().when(userCommonRepo).deleteUserCommonByIds(anyList());
        doNothing().when(paymentRepo).deletePaymentByUsers(anyList());

        ResponseEntity<Response> response = service.deleteUser(Arrays.asList(1L, 2L));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Tag("Mock")
    @Test
    void TC_ADM_002_deleteUser_nullIdListTriggersFailure() {
        doThrow(new NullPointerException("boom")).when(recruiterManagementRepo).deleteRecruiterManagementByUserId(isNull());

        ResponseEntity<Response> response = service.deleteUser(null);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_003_latestRecruiter_hasData() {
        PageableModel model = pageableModel(1, 10);
        List<Job> jobs = Arrays.asList(buildJob(1L), buildJob(2L));
        Page<Job> page = new PageImpl<>(jobs, PageRequest.of(0, 10), 2);
        when(jobRepo.getLatestJob(any(LocalDateTime.class), any())).thenReturn(page);
        when(jobService.convertToJobDTO(any(Job.class))).thenReturn(new JobDTO());

        ResponseEntity<ResponseObject> response = service.latestRecruiter(model);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2L, response.getBody().getTotalCount());
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_004_latestRecruiter_noData() {
        PageableModel model = pageableModel(1, 10);
        Page<Job> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(jobRepo.getLatestJob(any(LocalDateTime.class), any())).thenReturn(page);

        ResponseEntity<ResponseObject> response = service.latestRecruiter(model);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0L, response.getBody().getTotalCount());
    }

    @Tag("Unit")
    @Test
    void TC_ADM_005_latestRecruiter_boundaryPageZero() {
        PageableModel model = pageableModel(0, 10);

        assertThrows(IllegalArgumentException.class, () -> service.latestRecruiter(model));
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_006_latestFreelancer_hasData() {
        FreelancerDTO dto = new FreelancerDTO();
        dto.setPageableModel(pageableModel(1, 10));
        Freelancer freelancer = new Freelancer();
        Page<Freelancer> page = new PageImpl<>(Collections.singletonList(freelancer), PageRequest.of(0, 10), 1);
        when(freelancerRepo.latestFreelancer(any(), anyString(), any())).thenReturn(page);
        when(freelancerService.convertToFreelancerDTO(any(Freelancer.class))).thenReturn(new FreelancerDTO());

        ResponseEntity<ResponseObject> response = service.latestFreelancer(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getTotalCount());
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_007_latestFreelancer_noData() {
        FreelancerDTO dto = new FreelancerDTO();
        dto.setPageableModel(pageableModel(1, 10));
        when(freelancerRepo.latestFreelancer(any(), anyString(), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        ResponseEntity<ResponseObject> response = service.latestFreelancer(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0L, response.getBody().getTotalCount());
    }

    @Tag("Mock")
    @Test
    void TC_ADM_008_latestFreelancer_pageableNull() {
        FreelancerDTO dto = new FreelancerDTO();
        dto.setPageableModel(null);

        ResponseEntity<ResponseObject> response = service.latestFreelancer(dto);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_009_getListUsers_withKeySearch() {
        UserParamDTO param = new UserParamDTO();
        param.setPaging(paging(1, 10));
        param.setRoles(Arrays.asList(1, 2));
        param.setRatings(Collections.singletonList(3));
        param.setKeySearch("abc");
        Page<UserCommon> page = new PageImpl<>(Collections.singletonList(buildUser(1L)), PageRequest.of(0, 10), 1);
        when(userCommonRepo.findUsersByKeySearch(anyString(), anyList(), anyList(), any())).thenReturn(page);
        when(userCommonService.buildUserCommonDTO(any(UserCommon.class))).thenReturn(new UserCommonDTO());

        ResponseEntity<ResponseObject> response = service.getListUsers(param);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getTotalCount());
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_010_getListUsers_rolesRatingsNull() {
        UserParamDTO param = new UserParamDTO();
        param.setPaging(paging(1, 10));
        param.setRoles(null);
        param.setRatings(null);
        param.setKeySearch(null);
        when(userCommonRepo.findUsers(anyList(), anyList(), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        ResponseEntity<ResponseObject> response = service.getListUsers(param);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0L, response.getBody().getTotalCount());
    }

    @Tag("Unit")
    @Test
    void TC_ADM_011_getListUsers_boundaryPageZero() {
        UserParamDTO param = new UserParamDTO();
        param.setPaging(paging(0, 10));

        assertThrows(IllegalArgumentException.class, () -> service.getListUsers(param));
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_012_getBlockedUsers_hasData() {
        UserCommonDTO dto = new UserCommonDTO();
        dto.setPageableModel(pageableModel(1, 10));
        Page<UserCommon> page = new PageImpl<>(Collections.singletonList(buildUser(1L)), PageRequest.of(0, 10), 1);
        when(userCommonRepo.findBlockedUsers(eq("0"), any())).thenReturn(page);
        when(userCommonService.buildUserCommonDTO(any(UserCommon.class))).thenReturn(new UserCommonDTO());

        ResponseEntity<ResponseObject> response = service.getBlockedUsers(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getTotalCount());
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_013_getBlockedUsers_noData() {
        UserCommonDTO dto = new UserCommonDTO();
        dto.setPageableModel(pageableModel(1, 10));
        when(userCommonRepo.findBlockedUsers(eq("0"), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        ResponseEntity<ResponseObject> response = service.getBlockedUsers(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0L, response.getBody().getTotalCount());
    }

    @Tag("Mock")
    @Test
    void TC_ADM_014_getBlockedUsers_pageableNull() {
        UserCommonDTO dto = new UserCommonDTO();
        dto.setPageableModel(null);

        ResponseEntity<ResponseObject> response = service.getBlockedUsers(dto);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Tag("Mock")
    @Test
    void TC_ADM_015_updateJob_successWithEmbeddingRefresh() {
        Map<String, Double> latLng = new HashMap<>();
        latLng.put("lat", 21.0);
        latLng.put("lng", 105.0);
        when(utils.convertAddressToCoordinate(anyString())).thenReturn(latLng);

        Job input = buildJob(1L);
        Job existing = buildJob(1L);
        when(jobRepo.findById(1L)).thenReturn(Optional.of(existing), Optional.of(existing));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        Query query = Mockito.mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyInt(), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);

        try (MockedConstruction<RestTemplate> mocked = Mockito.mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("status", "ok");
                    body.put("embedding", "[0.1,0.2]");
                    when(mock.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                            .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));
                })) {
            ResponseEntity<ResponseObject> response = service.updateJob(input);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(1, mocked.constructed().size());
            verify(jobRepo, times(1)).save(any(Job.class));
        }
    }

    @Tag("Mock")
    @Test
    void TC_ADM_016_updateJob_jobIdNotFound() {
        Map<String, Double> latLng = new HashMap<>();
        latLng.put("lat", 21.0);
        latLng.put("lng", 105.0);
        when(utils.convertAddressToCoordinate(anyString())).thenReturn(latLng);

        Job input = buildJob(99L);
        when(jobRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<ResponseObject> response = service.updateJob(input);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("Update failed"));
    }

    @Tag("Mock")
    @Test
    void TC_ADM_017_updateJob_encodeJobApiReturnsInvalidResult() {
        Map<String, Double> latLng = new HashMap<>();
        latLng.put("lat", 21.0);
        latLng.put("lng", 105.0);
        when(utils.convertAddressToCoordinate(anyString())).thenReturn(latLng);

        Job input = buildJob(1L);
        Job existing = buildJob(1L);
        when(jobRepo.findById(1L)).thenReturn(Optional.of(existing), Optional.of(existing));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        try (MockedConstruction<RestTemplate> mocked = Mockito.mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("status", "error");
                    when(mock.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                            .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));
                })) {
            ResponseEntity<ResponseObject> response = service.updateJob(input);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertTrue(response.getBody().getMessage().contains("Failed to generate embedding"));
            assertEquals(1, mocked.constructed().size());
        }
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_018_deleteFreelancerByIds_success() {
        when(freelancerRepo.deleteFreelancerByIds(Arrays.asList(11L, 12L))).thenReturn(2);

        ResponseEntity<ResponseObject> response = service.deleteFreelancerByIds(Arrays.asList(11L, 12L));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_019_deleteFreelancerByIds_nothingDeleted() {
        when(freelancerRepo.deleteFreelancerByIds(Collections.singletonList(11L))).thenReturn(0);

        ResponseEntity<ResponseObject> response = service.deleteFreelancerByIds(Collections.singletonList(11L));

        assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
    }

    @Tag("Mock")
    @Test
    void TC_ADM_020_deleteFreelancerByIds_nullInputCausesError() {
        when(freelancerRepo.deleteFreelancerByIds(isNull())).thenThrow(new NullPointerException("npe"));

        ResponseEntity<ResponseObject> response = service.deleteFreelancerByIds(null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_021_updateFreelancerById_statusUpdateSucceeds() {
        FreelancerDTO dto = new FreelancerDTO();
        dto.setId(1L);
        dto.setStatus(1);
        when(freelancerRepo.updateFreelancerById(1, 1L)).thenReturn(1);

        ResponseEntity<ResponseObject> response = service.updateFreelancerById(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_022_updateFreelancerById_noRowUpdated() {
        FreelancerDTO dto = new FreelancerDTO();
        dto.setId(1L);
        dto.setStatus(0);
        when(freelancerRepo.updateFreelancerById(0, 1L)).thenReturn(0);

        ResponseEntity<ResponseObject> response = service.updateFreelancerById(dto);

        assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_023_statisticalUserByTime_hasData() {
        UserCommonDTO dto = new UserCommonDTO();
        dto.setRoles(Arrays.asList(1, 2));
        dto.setStatisticalType(STATISTICAL_BY_MONTH);
        dto.setStartYear(LocalDateTime.now().minusMonths(1));
        dto.setEndYear(LocalDateTime.now());

        UserCommon u = buildUser(1L);
        u.setCreationDate(LocalDateTime.now().minusDays(2));

        when(userCommonRepo.statisticalUserByTime(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(u));
        when(userCommonService.buildUserCommonDTO(any(UserCommon.class))).thenReturn(new UserCommonDTO());

        ResponseEntity<ResponseObject> response = service.statisticalUserByTime(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getTotalCount());
        assertNotNull(response.getBody().getData());
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_024_statisticalUserByTime_noData() {
        UserCommonDTO dto = new UserCommonDTO();
        dto.setRoles(Arrays.asList(1, 2));
        dto.setStatisticalType(STATISTICAL_BY_MONTH);
        dto.setStartYear(LocalDateTime.now().minusMonths(1));
        dto.setEndYear(LocalDateTime.now());

        when(userCommonRepo.statisticalUserByTime(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(null);

        ResponseEntity<ResponseObject> response = service.statisticalUserByTime(dto);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_025_statisticalRevenueByTime_hasPayments() {
        PaymentDTO dto = new PaymentDTO();
        dto.setStartYear(2024);
        dto.setEndYear(2024);
        dto.setStatisticalType(STATISTICAL_BY_MONTH);

        Payment payment = new Payment();
        payment.setCreationdate(LocalDateTime.of(2024, 5, 10, 0, 0));
        payment.setTotalMoney(100.0);
        when(paymentRepo.statisticalRevenueByTime(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(payment));

        ResponseEntity<ResponseObject> response = service.statisticalRevenueByTime(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getData());
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_026_statisticalRevenueByTime_noPayments() {
        PaymentDTO dto = new PaymentDTO();
        dto.setStartYear(2024);
        dto.setEndYear(2024);
        dto.setStatisticalType(STATISTICAL_BY_MONTH);

        when(paymentRepo.statisticalRevenueByTime(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        ResponseEntity<ResponseObject> response = service.statisticalRevenueByTime(dto);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_027_revenueInRealtime_exists() {
        when(paymentRepo.revenueInRealtime()).thenReturn(1250000.0);

        ResponseEntity<ResponseObject> response = service.revenueInRealtime();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1250000.0, response.getBody().getData());
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_028_revenueInRealtime_null() {
        when(paymentRepo.revenueInRealtime()).thenReturn(null);

        ResponseEntity<ResponseObject> response = service.revenueInRealtime();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_029_bonusForUser_hasAllSections() {
        BonusDTO dto = new BonusDTO();
        dto.setKeyword("bonus");
        dto.setPageableModel(pageableModel(1, 10));

        Settings settings = new Settings();
        settings.setId(1L);
        settings.setKeywords("bonus");
        settings.setData("5");

        RequestWithDrawing req = new RequestWithDrawing();
        Page<RequestWithDrawing> page = new PageImpl<>(Collections.singletonList(req), PageRequest.of(0, 10), 1);

        when(settingsRepo.findSetting("bonus")).thenReturn(Collections.singletonList(settings));
        when(paymentRepo.revenueInRealtime()).thenReturn(1000.0);
        when(requestWithDrawingRepo.findRequestWithDrawing(any())).thenReturn(page);

        ResponseEntity<ResponseObject> response = service.bonusForUser(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> data = (Map<?, ?>) response.getBody().getData();
        assertTrue(data.containsKey("settings"));
        assertTrue(data.containsKey("revenueInRealtime"));
        assertTrue(data.containsKey("requestWithDrawings"));
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_030_bonusForUser_allDataSourcesEmpty() {
        BonusDTO dto = new BonusDTO();
        dto.setKeyword("bonus");
        dto.setPageableModel(pageableModel(1, 10));

        when(settingsRepo.findSetting("bonus")).thenReturn(Collections.emptyList());
        when(paymentRepo.revenueInRealtime()).thenReturn(null);
        when(requestWithDrawingRepo.findRequestWithDrawing(any())).thenReturn(null);

        ResponseEntity<ResponseObject> response = service.bonusForUser(dto);

        // QA expectation: when all sources are empty, API should return NOT_FOUND
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_031_updateBonusForUser_success() {
        BonusDTO dto = new BonusDTO();
        dto.setKeyword("bonus");
        dto.setData("5");
        when(settingsRepo.updateSettings("5", "bonus")).thenReturn(1);

        ResponseEntity<ResponseObject> response = service.updateBonusForUser(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Tag("CheckDB")
    @Test
    void TC_ADM_032_updateBonusForUser_fails() {
        BonusDTO dto = new BonusDTO();
        dto.setKeyword("bonus");
        dto.setData("5");
        when(settingsRepo.updateSettings("5", "bonus")).thenReturn(null);

        ResponseEntity<ResponseObject> response = service.updateBonusForUser(dto);

        assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
    }

    @Tag("Mock")
    @Test
    void TC_ADM_033_scanUser_freelancerSuccess() throws Exception {
        Map<String, Double> latLng = new HashMap<>();
        latLng.put("lat", 21.0);
        latLng.put("lng", 105.0);
        when(utils.convertAddressToCoordinate(anyString())).thenReturn(latLng);

        MockMultipartFile file = buildXlsxFile(true);
        UserCommon user = buildUser(1L);
        ResponseObject createUserResp = new ResponseObject();
        createUserResp.setData(Collections.singletonList(user));

        when(userCommonService.createUser(anyString())).thenReturn(createUserResp);
        when(freelancerRepo.save(any(Freelancer.class))).thenReturn(new Freelancer());

        // QA expectation follows testcase input exactly: scanObject = FREELANCER
        ResponseEntity<Response> response = service.scanUser("FREELANCER", file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(freelancerRepo, times(1)).save(any(Freelancer.class));
        verify(jobRepo, never()).save(any(Job.class));
    }

    @Tag("Mock")
    @Test
    void TC_ADM_034_scanUser_jobSuccess() throws Exception {
        Map<String, Double> latLng = new HashMap<>();
        latLng.put("lat", 21.0);
        latLng.put("lng", 105.0);
        when(utils.convertAddressToCoordinate(anyString())).thenReturn(latLng);

        MockMultipartFile file = buildXlsxFile(false);
        UserCommon user = buildUser(1L);
        ResponseObject createUserResp = new ResponseObject();
        createUserResp.setData(Collections.singletonList(user));

        when(userCommonService.createUser(anyString())).thenReturn(createUserResp);
        when(jobRepo.save(any(Job.class))).thenReturn(buildJob(99L));

        // QA expectation follows testcase input exactly: scanObject = JOB
        ResponseEntity<Response> response = service.scanUser("JOB", file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(jobRepo, times(1)).save(any(Job.class));
        verify(freelancerRepo, never()).save(any(Freelancer.class));
    }

    @Tag("Unit")
    @Test
    void TC_ADM_035_scanUser_invalidFileOrParseFailure() {
        MultipartFile invalid = new MockMultipartFile("file", "bad.xlsx", "application/octet-stream", new byte[0]);

        ResponseEntity<Response> response = service.scanUser("freelancer", invalid);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }

    @Tag("Unit")
    @Test
    void TC_ADM_036_scanUser_unsupportedScanObject() throws Exception {
        MockMultipartFile file = buildXlsxFile(true);

        assertThrows(IllegalArgumentException.class, () -> service.scanUser("UNKNOWN", file));
    }
}


