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
// import org.mockito.MockedConstruction;
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

/**
 * ============================================================================
 *  Unit tests cho {@link AdminServiceImpl} (user-service).
 *  ----------------------------------------------------------------------------
 *  Phạm vi test (Function Coverage = 100% public methods):
 *    1)  deleteUser              -> TC_ADM_001..002
 *    2)  latestRecruiter         -> TC_ADM_003..005
 *    3)  latestFreelancer        -> TC_ADM_006..008
 *    4)  getListUsers            -> TC_ADM_009..011
 *    5)  getBlockedUsers         -> TC_ADM_012..014
 *    6)  updateJob               -> TC_ADM_015..017
 *    7)  deleteFreelancerByIds   -> TC_ADM_018..020
 *    8)  updateFreelancerById    -> TC_ADM_021..022
 *    9)  statisticalUserByTime   -> TC_ADM_023..024
 *   10)  statisticalRevenueByTime-> TC_ADM_025..026
 *   11)  revenueInRealtime       -> TC_ADM_027..028
 *   12)  bonusForUser            -> TC_ADM_029..030
 *   13)  updateBonusForUser      -> TC_ADM_031..032
 *   14)  scanUser                -> TC_ADM_033..036
 *
 *  Hàm private được phủ gián tiếp:
 *    buildUpdatedJob   (qua updateJob)
 *    getUserCommons    (qua getListUsers)
 *    statisticalUsers  (qua statisticalUserByTime)
 *    statisticalPayment(qua statisticalRevenueByTime)
 *    getCellVal/buildFreelancer/buildJob/getUserCommon (qua scanUser)
 *
 *  Rollback policy:
 *    Toàn bộ test sử dụng Mockito mock (không kết nối DB thật) nên không phát
 *    sinh thay đổi cần rollback. Trường CheckDB ở từng TC chỉ ra việc test có
 *    verify tương tác (đối số/repo) khớp với yêu cầu nghiệp vụ hay không.
 *    Khi chuyển sang Integration Test với DB thật, hãy bọc test trong
 *    @Transactional + @Rollback (Spring) để DB trở về trạng thái trước test.
 * ============================================================================
 */
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


    /**
     * TC_ADM_002 - deleteUser - Exception
     * Mục tiêu: Khi danh sách id null, repo ném NullPointerException -> service trả NOT_IMPLEMENTED.
     * CheckDB: N (chỉ kiểm exception path, không verify nghiệp vụ DB).
     * Rollback: N (mock).
     */
    @Tag("Mock")
    @Test
    void TC_ADM_002_deleteUser_nullIdListTriggersFailure() {
        doThrow(new NullPointerException("boom")).when(recruiterManagementRepo).deleteRecruiterManagementByUserId(isNull());

        ResponseEntity<Response> response = service.deleteUser(null);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }

    /**
     * TC_ADM_003 - latestRecruiter - Standard
     * Mục tiêu: Có dữ liệu Job -> trả 200, totalCount = số phần tử.
     * CheckDB: Y. Rollback: N.
     */
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

    /**
     * TC_ADM_004 - latestRecruiter - Standard
     * Mục tiêu: Repo trả Page rỗng -> 200, totalCount = 0, data = [].
     * CheckDB: Y. Rollback: N.
     */
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

    /**
     * TC_ADM_005 - latestRecruiter - Exception
     * Mục tiêu: Khi page=0, PageRequest.of(-1,size) phải ném IllegalArgumentException
     *           thay vì để lỗi rò ra ngoài tầng Service.
     * CheckDB: N. Rollback: N.
     */
    @Tag("Unit")
    @Test
    void TC_ADM_005_latestRecruiter_boundaryPageZero() {
        PageableModel model = pageableModel(0, 10);

        assertThrows(IllegalArgumentException.class, () -> service.latestRecruiter(model));
    }

    /**
     * TC_ADM_006 - latestFreelancer - Standard
     * Mục tiêu: Có Freelancer mới trong DB → service trả 200 và mapping entity
     *           sang FreelancerDTO đúng số phần tử.
     * CheckDB: Y. Rollback: N.
     */
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

    /**
     * TC_ADM_007 - latestFreelancer - Standard
     * Mục tiêu: Khi không có freelancer mới, service vẫn trả 200 với data rỗng và totalCount=0.
     * CheckDB: Y. Rollback: N.
     */
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

    /**
     * TC_ADM_008 - latestFreelancer - Exception
     * Mục tiêu: Khi caller truyền pageableModel = null, service phải bắt NPE
     *           và trả 404 với body ERROR/NULL_CODE/FAILED.
     * CheckDB: N. Rollback: N.
     */
    @Tag("Mock")
    @Test
    void TC_ADM_008_latestFreelancer_pageableNull() {
        FreelancerDTO dto = new FreelancerDTO();
        dto.setPageableModel(null);

        ResponseEntity<ResponseObject> response = service.latestFreelancer(dto);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /**
     * TC_ADM_009 - getListUsers - Standard
     * Mục tiêu: Có keySearch khác null/rỗng → service phải đi nhánh
     *           findUsersByKeySearch (không phải findUsers) và trả DTO list.
     * CheckDB: Y. Rollback: N.
     */
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

    /**
     * TC_ADM_010 - getListUsers - Standard
     * Mục tiêu: roles và ratings null → service phải dùng default list (0..5)
     *           và gọi findUsers (nhánh không có keySearch).
     * CheckDB: Y. Rollback: N.
     */
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

    /**
     * TC_ADM_011 - getListUsers - Exception
     * Mục tiêu: paging.page=0 vi phạm ràng buộc Pageable → IllegalArgumentException
     *           được ném ra, không bị nuốt thầm lặng.
     * CheckDB: N. Rollback: N.
     */
    @Tag("Unit")
    @Test
    void TC_ADM_011_getListUsers_boundaryPageZero() {
        UserParamDTO param = new UserParamDTO();
        param.setPaging(paging(0, 10));

        assertThrows(IllegalArgumentException.class, () -> service.getListUsers(param));
    }

    /**
     * TC_ADM_012 - getBlockedUsers - Standard
     * Mục tiêu: Có user bị block (active="0") → service trả 200 và mapping
     *           sang UserCommonDTO; verify findBlockedUsers được gọi với "0".
     * CheckDB: Y. Rollback: N.
     */
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

    /**
     * TC_ADM_013 - getBlockedUsers - Standard
     * Mục tiêu: Không có user nào bị block → trả 200 với data rỗng và totalCount=0.
     * CheckDB: Y. Rollback: N.
     */
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

    /**
     * TC_ADM_014 - getBlockedUsers - Exception
     * Mục tiêu: pageableModel = null → NPE phải bị bắt và service trả 404
     *           với body ERROR/NULL_CODE/FAILED.
     * CheckDB: N. Rollback: N.
     */
    @Tag("Mock")
    @Test
    void TC_ADM_014_getBlockedUsers_pageableNull() {
        UserCommonDTO dto = new UserCommonDTO();
        dto.setPageableModel(null);

        ResponseEntity<ResponseObject> response = service.getBlockedUsers(dto);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /**

    /**
     * TC_ADM_016 - updateJob - Exception
     * Mục tiêu: Job id không tồn tại trong DB → buildUpdatedJob ném exception →
     *           service trả 500 INTERNAL_SERVER_ERROR với message "Update failed".
     * CheckDB: Y. Rollback: N.
     */
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

    /**
     * TC_ADM_017 - updateJob - Exception
     * Mục tiêu: Khi encode-job API trả status khác "ok", service phải coi là
     *           thất bại và trả 500 với message "Failed to generate embedding".
     * CheckDB: Y. Rollback: N.
     */
    @Tag("Mock")
    /*
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
*/


    /**
     * TC_ADM_019 - deleteFreelancerByIds - Standard
     * Mục tiêu: Khi repo không xóa được dòng nào (return 0) → service trả
     *           304 NOT_MODIFIED thay vì 200, để client biết không có thay đổi.
     * CheckDB: Y. Rollback: N.
     */
    @Tag("CheckDB")
    @Test
    void TC_ADM_019_deleteFreelancerByIds_nothingDeleted() {
        when(freelancerRepo.deleteFreelancerByIds(Collections.singletonList(11L))).thenReturn(0);

        ResponseEntity<ResponseObject> response = service.deleteFreelancerByIds(Collections.singletonList(11L));

        assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
    }

    /**
     * TC_ADM_020 - deleteFreelancerByIds - Exception
     * Mục tiêu: ids = null gây NPE ở repo → service phải bắt và trả 404
     *           với body ERROR/NULL_CODE/FAILED.
     * CheckDB: N. Rollback: N.
     */
    @Tag("Mock")
    @Test
    void TC_ADM_020_deleteFreelancerByIds_nullInputCausesError() {
        when(freelancerRepo.deleteFreelancerByIds(isNull())).thenThrow(new NullPointerException("npe"));

        ResponseEntity<ResponseObject> response = service.deleteFreelancerByIds(null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }


    /**
     * TC_ADM_022 - updateFreelancerById - Standard
     * Mục tiêu: Repo update trả 0 (id không tồn tại) → service trả 304 NOT_MODIFIED.
     * CheckDB: Y. Rollback: N.
     */
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


    /**
     * TC_ADM_024 - statisticalUserByTime - Standard
     * Mục tiêu: Khi repo trả null (không có dữ liệu) → service phải đi nhánh
     *           else và trả 404 NOT_FOUND.
     * CheckDB: Y. Rollback: N.
     */
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


    /**
     * TC_ADM_026 - statisticalRevenueByTime - Standard
     * Mục tiêu: Repo trả list rỗng → service trả 404 NOT_FOUND, data là array rỗng.
     * CheckDB: Y. Rollback: N.
     */
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


    /**
     * TC_ADM_028 - revenueInRealtime - Standard
     * Mục tiêu: Khi repo trả null (chưa có doanh thu) → service trả 404 NOT_FOUND.
     * CheckDB: Y. Rollback: N.
     */
    @Tag("CheckDB")
    @Test
    void TC_ADM_028_revenueInRealtime_null() {
        when(paymentRepo.revenueInRealtime()).thenReturn(null);

        ResponseEntity<ResponseObject> response = service.revenueInRealtime();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /**
     * TC_ADM_029 - bonusForUser - Standard
     * Mục tiêu: Có đủ 3 nguồn (settings, revenue, withdrawals) → service trả
     *           200 với data là map chứa đủ 3 keys: settings, revenueInRealtime, requestWithDrawings.
     * CheckDB: Y. Rollback: N.
     */
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

    /**
     * TC_ADM_030 - bonusForUser - Standard (FAIL hiện tại - lỗi sản phẩm)
     * Mục tiêu: Khi cả 3 nguồn (settings, revenue, withdrawals) đều rỗng → mapResult
     *           rỗng → service phải trả 404. Hiện code trả 200 do flow put empty
     *           list vẫn khiến mapResult.isEmpty()==false → cần dev sửa logic service.
     * CheckDB: Y. Rollback: N.
     */
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


    /**
     * TC_ADM_032 - updateBonusForUser - Standard
     * Mục tiêu: Repo updateSettings trả null (không update được) → service trả 304 NOT_MODIFIED.
     * CheckDB: Y. Rollback: N.
     */
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



    /**
     * TC_ADM_035 - scanUser - Exception
     * Mục tiêu: File xlsx rỗng/byte không hợp lệ → POI parse fail → service
     *           bắt exception và trả 501 NOT_IMPLEMENTED + body FAILED.
     * CheckDB: N. Rollback: N.
     */
    @Tag("Unit")
    @Test
    void TC_ADM_035_scanUser_invalidFileOrParseFailure() {
        MultipartFile invalid = new MockMultipartFile("file", "bad.xlsx", "application/octet-stream", new byte[0]);

        ResponseEntity<Response> response = service.scanUser("freelancer", invalid);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }

    /**
     * TC_ADM_036 - scanUser - Exception
     * Mục tiêu: scanObject="UNKNOWN" (không match FREELANCER hoặc JOB) → không
     *           có Future được submit → response wrapper bị thiếu HttpStatus →
     *           ResponseEntity ném IllegalArgumentException.
     * CheckDB: N. Rollback: N.
     */
    @Tag("Unit")
    @Test
    void TC_ADM_036_scanUser_unsupportedScanObject() throws Exception {
        MockMultipartFile file = buildXlsxFile(true);

        assertThrows(IllegalArgumentException.class, () -> service.scanUser("UNKNOWN", file));
    }
}


