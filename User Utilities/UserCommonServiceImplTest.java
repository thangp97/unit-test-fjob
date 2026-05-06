package com.resourceservice.service.impl;

import com.amazonaws.services.amplify.model.BadRequestException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jober.utilsservice.dto.WalletDTO;
import com.jober.utilsservice.dto.WalletResDTO;
import com.jober.utilsservice.utils.Utility;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import com.resourceservice.common.CommonUtils;
import com.resourceservice.config.EnvProperties;
import com.resourceservice.dto.InputGetTokenDTO;
import com.resourceservice.dto.InputLoginDTO;
import com.resourceservice.dto.UserCommonDTO;
import com.resourceservice.dto.UserInforDto;
import com.resourceservice.dto.request.UserForChangingPass;
import com.resourceservice.dto.request.UserParamDTO;
import com.resourceservice.dto.request.YearRequest;
import com.resourceservice.feign.PaymentFeignClient;
import com.resourceservice.interceptor.BearerTokenWrapper;
import com.resourceservice.model.RefreshTokenObject;
import com.resourceservice.model.StatisticalUser;
import com.resourceservice.model.UserCommon;
import com.resourceservice.repository.FreelancerRepo;
import com.resourceservice.repository.JobRepo;
import com.resourceservice.repository.RecruiterConfigurationRepository;
import com.resourceservice.repository.UserCommonRepo;
import com.resourceservice.utilsmodule.CacheService;
import com.resourceservice.utilsmodule.errors.RestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
// import org.mockito.MockedConstruction;
// import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho {@link UserCommonServiceImpl}.
 * Lưu ý: Nhiều method gọi static helper (Utility.isSendSMS,
 * HttpUtils.postData,...) — cần mockito-inline
 * để dùng Mockito.mockStatic. Thêm dependency sau vào pom.xml (scope test) nếu
 * chưa có:
 * <dependency>
 * <groupId>org.mockito</groupId>
 * <artifactId>mockito-inline</artifactId>
 * <scope>test</scope>
 * </dependency>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserCommonServiceImplTest {

    @Mock
    private PaymentFeignClient paymentFeignClient;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private BearerTokenWrapper tokenWrapper;
    @Mock
    private S3ServiceImpl s3Service;
    @Mock
    private CommonUtils utils;
    @Mock
    private EnvProperties envProperties;
    @Mock
    private UserCommonRepo userCommonRepo;
    @Mock
    private CacheService cacheService;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private RestExceptionHandler restExceptionHandler;
    @Mock
    private CommunityService communityService;
    @Mock
    private CacheManagerService cacheManagerService;
    @Mock
    private JobRepo jobRepo;
    @Mock
    private FreelancerRepo freelancerRepo;
    @Mock
    private RecruiterConfigurationRepository recruiterConfigurationRepository;

    @InjectMocks
    private UserCommonServiceImpl service;

    @BeforeEach
    void setUp() {
        // @RequiredArgsConstructor chỉ inject 3 field final qua constructor.
        // Các field @Autowired còn lại phải set tay vì Mockito bỏ qua field injection
        // khi constructor injection đã thành công.
        ReflectionTestUtils.setField(service, "s3Service", s3Service);
        ReflectionTestUtils.setField(service, "utils", utils);
        ReflectionTestUtils.setField(service, "envProperties", envProperties);
        ReflectionTestUtils.setField(service, "userCommonRepo", userCommonRepo);
        ReflectionTestUtils.setField(service, "cacheService", cacheService);
        ReflectionTestUtils.setField(service, "cacheManager", cacheManager);
        ReflectionTestUtils.setField(service, "restExceptionHandler", restExceptionHandler);
        ReflectionTestUtils.setField(service, "communityService", communityService);
        ReflectionTestUtils.setField(service, "cacheManagerService", cacheManagerService);
        ReflectionTestUtils.setField(service, "jobRepo", jobRepo);
        ReflectionTestUtils.setField(service, "freelancerRepo", freelancerRepo);
        ReflectionTestUtils.setField(service, "recruiterConfigurationRepository", recruiterConfigurationRepository);
    }

    private UserCommon buildUser(Long id, String phone) {
        UserCommon u = new UserCommon();
        u.setId(id);
        u.setPhone(phone);
        u.setEmail(phone + "@x.com");
        u.setName("User" + id);
        u.setRole(2);
        u.setIsPremium(false);
        return u;
    }

    private InputLoginDTO buildLoginDto(String username, String rawPassword) {
        InputGetTokenDTO token = new InputGetTokenDTO();
        token.setUsername(username);
        token.setPassword(Base64.getEncoder().encodeToString(rawPassword.getBytes()));

        InputLoginDTO dto = new InputLoginDTO();
        dto.setBodyGetToken(token);
        return dto;
    }

    private String tokenJson() {
        return "{\"access_token\":\"access-123\",\"refresh_token\":\"refresh-123\",\"token_type\":\"bearer\",\"expires_in\":\"3600\",\"scope\":\"read\",\"jti\":\"jti-1\"}";
    }

    private Map<String, Object> buildGoogleAttributes(String email, String name) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google-sub-1");
        attributes.put("email", email);
        attributes.put("name", name);
        attributes.put("picture", "http://img.test/avatar.png");
        return attributes;
    }

    private Map<String, Object> buildFacebookAttributes(String email, String name) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "fb-1");
        attributes.put("email", email);
        attributes.put("name", name);
        Map<String, Object> picture = new LinkedHashMap<>();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("url", "http://img.test/fb.png");
        picture.put("data", data);
        attributes.put("picture", picture);
        return attributes;
    }

    // ======================= findUserByPhoneNumber =======================
    /**
     * TC_UC_001 - findUserByPhoneNumber_byPhone_withWallet - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_001_findUserByPhoneNumber_byPhone_withWallet
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_001_findUserByPhoneNumber_byPhone_withWallet() {
        UserParamDTO param = new UserParamDTO();
        param.setPhone("0912345678");
        UserCommon user = buildUser(1L, "0912345678");
        when(userCommonRepo.findByPhoneEquals("0912345678")).thenReturn(user);

        WalletResDTO wallet = new WalletResDTO();
        wallet.setTotalPoint(new BigDecimal(50));
        when(communityService.getWalletByUser(1L)).thenReturn(wallet);
        when(userCommonRepo.getCountRatingForStar(1L)).thenReturn(Collections.emptyList());

        ResponseObject resp = service.findUserByPhoneNumber(param);

        assertNotNull(resp);
        assertEquals(new BigDecimal(50), user.getBonusPoint());
    }

    /**
     * TC_UC_002 - findUserByPhoneNumber_byEmail_noWallet - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_002_findUserByPhoneNumber_byEmail_noWallet
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_002_findUserByPhoneNumber_byEmail_noWallet() {
        UserParamDTO param = new UserParamDTO();
        param.setEmail("a@b.com");
        UserCommon user = buildUser(2L, "0988");
        when(userCommonRepo.findByEmail("a@b.com")).thenReturn(user);
        when(communityService.getWalletByUser(2L)).thenReturn(null);
        when(userCommonRepo.getCountRatingForStar(2L)).thenReturn(Collections.emptyList());

        ResponseObject resp = service.findUserByPhoneNumber(param);

        assertNotNull(resp);
        assertEquals(new BigDecimal(0), user.getBonusPoint());
    }

    /**
     * TC_UC_003 - findUserByPhoneNumber_notFound - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_003_findUserByPhoneNumber_notFound
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_003_findUserByPhoneNumber_notFound() {
        UserParamDTO param = new UserParamDTO();
        param.setPhone("0000");
        when(userCommonRepo.findByPhoneEquals("0000")).thenReturn(null);

        ResponseObject resp = service.findUserByPhoneNumber(param);

        assertNotNull(resp);
        assertEquals(0L, resp.getTotalCount());
    }

    // ======================= logout =======================
    /**
     * TC_UC_018 - logout_success - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_018_logout_success (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_018_logout_success() {
        when(tokenWrapper.getUid()).thenReturn(1L);

        ResponseEntity resp = service.logout();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(cacheService, times(2))
                .evictSingleCacheValue(eq(cacheManager), eq("token"), anyString());
    }

    // ======================= getRefreshToken =======================
    /**
     * TC_UC_021 - getRefreshToken_hit - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_021_getRefreshToken_hit (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_021_getRefreshToken_hit() {
        when(tokenWrapper.getUid()).thenReturn(1L);
        when(cacheService.getCache(eq(cacheManager), eq("token"), anyString())).thenReturn("rt-xyz");

        ResponseEntity<RefreshTokenObject> resp = service.getRefreshToken();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("rt-xyz", resp.getBody().getRefreshToken());
    }

    /**
     * TC_UC_022 - getRefreshToken_miss - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_022_getRefreshToken_miss (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_022_getRefreshToken_miss() {
        when(tokenWrapper.getUid()).thenReturn(9L);
        when(cacheService.getCache(eq(cacheManager), eq("token"), anyString())).thenReturn(null);

        ResponseEntity<RefreshTokenObject> resp = service.getRefreshToken();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNull(resp.getBody().getRefreshToken());
    }

    // ======================= forgetPassword =======================

    /**
     * TC_UC_024 - forgetPassword_userNotFound - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_024_forgetPassword_userNotFound
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    /*
@Test
    void TC_UC_024_forgetPassword_userNotFound() {
        UserForChangingPass param = UserForChangingPass.builder().phone("0000").build();
        when(userCommonRepo.findByPhoneEquals("0000")).thenReturn(null);

        try (MockedStatic<Utility> utilityMock = Mockito.mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.responseObject(anyString(), anyString(), anyString(), any()))
                    .thenCallRealMethod();

            ResponseObject resp = service.forgetPassword(param);

            assertNotNull(resp);
            verify(userCommonRepo, never()).save(any(UserCommon.class));
        }
    }
*/

    // ======================= createUser =======================

    /**
     * TC_UC_005 - createUser_phoneAlreadyExists - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_005_createUser_phoneAlreadyExists
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_005_createUser_phoneAlreadyExists() {
        String body = "{\"phone\":\"0912345678\",\"email\":\"a@b.com\",\"password\":\"" +
                Base64.getEncoder().encodeToString("pwd123".getBytes()) + "\",\"role\":2}";
        when(userCommonRepo.findByPhoneEquals("0912345678")).thenReturn(buildUser(1L, "0912345678"));

        ResponseObject resp = service.createUser(body);

        assertNotNull(resp);
        verify(userCommonRepo, never()).save(any(UserCommon.class));
    }

    /**
     * TC_UC_006 - createUser_recruiterWithoutOrg - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_006_createUser_recruiterWithoutOrg
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_006_createUser_recruiterWithoutOrg() {
        String body = "{\"phone\":\"0912345678\",\"email\":\"r@b.com\",\"password\":\"" +
                Base64.getEncoder().encodeToString("pwd123".getBytes()) + "\",\"role\":4}";
        when(userCommonRepo.findByPhoneEquals(anyString())).thenAnswer(invocation -> null);
        when(userCommonRepo.findByEmail(anyString())).thenAnswer(invocation -> null);
        when(userCommonRepo.save(any(UserCommon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseObject resp = service.createUser(body);

        assertNotNull(resp);
        assertEquals("NOT_CREATED", resp.getStatus());
        assertEquals("ORG_NULL", resp.getMessage());
        verify(communityService, never()).saveWallet(any());
    }

    /**
     * TC_UC_007 - createUser_malformedJson - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_007_createUser_malformedJson (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_007_createUser_malformedJson() {
        assertThrows(RuntimeException.class, () -> service.createUser("{invalid"));
    }

    // ======================= saveUser =======================

    /**
     * TC_UC_011 - saveUser_nullPointerHandled - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_011_saveUser_nullPointerHandled
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_011_saveUser_nullPointerHandled() {
        String body = "{\"phone\":\"0912\",\"pin\":null}";
        when(userCommonRepo.findByPhoneEquals("0912")).thenReturn(null);

        ResponseObject resp = service.saveUser(body);

        assertNotNull(resp);
        assertEquals("304 NOT_MODIFIED", resp.getStatus());
        verify(userCommonRepo, never()).save(any(UserCommon.class));
    }

    // ======================= obtainAccessToken / login / adminLogin
    // =======================
    /**
     * TC_UC_019 - obtainAccessToken_success - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_019_obtainAccessToken_success (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    /*
@Test
    void TC_UC_019_obtainAccessToken_success() {
        ReflectionTestUtils.setField(service, "clientId", "client-id");
        ReflectionTestUtils.setField(service, "clientSecret", "client-secret");
        when(envProperties.getAuthServerURI()).thenReturn("http://auth.local/");

        InputGetTokenDTO token = new InputGetTokenDTO();
        token.setUsername("u");
        token.setPassword(Base64.getEncoder().encodeToString("p".getBytes()));

        try (MockedConstruction<RestTemplate> mocked = Mockito.mockConstruction(RestTemplate.class,
                (mock, context) -> when(
                        mock.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                        .thenReturn(new ResponseEntity<>(tokenJson(), HttpStatus.OK)))) {

            ResponseEntity<String> resp = service.obtainAccessToken(token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertTrue(resp.getBody().contains("access-123"));
            verify(mocked.constructed().get(0)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                    eq(String.class));
        }
    }
*/

    /**
     * TC_UC_020 - obtainAccessToken_exceptionReturnsNull - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_020_obtainAccessToken_exceptionReturnsNull
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    /*
@Test
    void TC_UC_020_obtainAccessToken_exceptionReturnsNull() {
        ReflectionTestUtils.setField(service, "clientId", "client-id");
        ReflectionTestUtils.setField(service, "clientSecret", "client-secret");
        when(envProperties.getAuthServerURI()).thenReturn("http://auth.local/");

        InputGetTokenDTO token = new InputGetTokenDTO();
        token.setUsername("u");
        token.setPassword(Base64.getEncoder().encodeToString("p".getBytes()));

        try (MockedConstruction<RestTemplate> mocked = Mockito.mockConstruction(RestTemplate.class,
                (mock, context) -> when(
                        mock.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                        .thenThrow(new RestClientException("boom")))) {

            ResponseEntity<String> resp = service.obtainAccessToken(token);

            assertNull(resp);
            assertEquals(1, mocked.constructed().size());
        }
    }
*/

    /**
     * TC_UC_012 - login_success - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_012_login_success (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_012_login_success() throws Throwable {
        UserCommon user = buildUser(1L, "0912");
        when(userCommonRepo.findByPhoneEquals("0912")).thenReturn(user);

        UserCommonServiceImpl spyService = Mockito.spy(service);
        doReturn(new ResponseEntity<>(tokenJson(), HttpStatus.OK))
                .when(spyService).obtainAccessToken(any(InputGetTokenDTO.class));

        ResponseEntity<?> resp = spyService.login(buildLoginDto("0912", "pwd"));

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(cacheService, times(4)).putCache(eq(cacheManager), anyString(), anyString(), any());
    }

    /**
     * TC_UC_013 - login_wrongPassword - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_013_login_wrongPassword (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_013_login_wrongPassword() throws Throwable {
        UserCommon user = buildUser(1L, "0912");
        when(userCommonRepo.findByPhoneEquals("0912")).thenReturn(user);

        UserCommonServiceImpl spyService = Mockito.spy(service);
        doReturn(null).when(spyService).obtainAccessToken(any(InputGetTokenDTO.class));

        ResponseEntity<?> resp = spyService.login(buildLoginDto("0912", "wrong"));

        assertEquals(HttpStatus.EXPECTATION_FAILED, resp.getStatusCode());
    }

    /**
     * TC_UC_014 - login_userNotFound - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_014_login_userNotFound (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_014_login_userNotFound() throws Throwable {
        UserCommonServiceImpl spyService = Mockito.spy(service);
        doReturn(new ResponseEntity<>(tokenJson(), HttpStatus.OK))
                .when(spyService).obtainAccessToken(any(InputGetTokenDTO.class));

        ResponseEntity<?> resp = spyService.login(buildLoginDto("0912", "pwd"));

        assertEquals(HttpStatus.EXPECTATION_FAILED, resp.getStatusCode());
    }

    /**
     * TC_UC_015 - adminLogin_success - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_015_adminLogin_success (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_015_adminLogin_success() throws Throwable {
        UserCommon user = buildUser(1L, "0912");
        user.setRole(4);
        when(userCommonRepo.findByPhoneEquals("0912")).thenReturn(user);

        UserCommonServiceImpl spyService = Mockito.spy(service);
        doReturn(new ResponseEntity<>(tokenJson(), HttpStatus.OK))
                .when(spyService).obtainAccessToken(any(InputGetTokenDTO.class));

        ResponseEntity<?> resp = spyService.adminLogin(buildLoginDto("0912", "pwd"));

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    /**
     * TC_UC_017 - adminLogin_wrongCredentials - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_017_adminLogin_wrongCredentials
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_017_adminLogin_wrongCredentials() throws Throwable {
        UserCommon user = buildUser(1L, "0912");
        user.setRole(4);
        when(userCommonRepo.findByPhoneEquals("0912")).thenReturn(user);

        UserCommonServiceImpl spyService = Mockito.spy(service);
        doReturn(null).when(spyService).obtainAccessToken(any(InputGetTokenDTO.class));

        ResponseEntity<?> resp = spyService.adminLogin(buildLoginDto("0912", "wrong"));

        assertEquals(HttpStatus.EXPECTATION_FAILED, resp.getStatusCode());
    }

    // ======================= processUserOidc / processUserOAuth2
    // =======================

    /**
     * TC_UC_029 - processUserOidc_emptyEmail - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_029_processUserOidc_emptyEmail (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_029_processUserOidc_emptyEmail() {
        Map<String, Object> attributes = buildGoogleAttributes("", "Google User");

        assertThrows(RuntimeException.class, () -> service.processUserOidc("google", attributes, mock(OidcUser.class)));
    }

    /**
     * TC_UC_030 - processUserOidc_existingUser_noCreate - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_030_processUserOidc_existingUser_noCreate
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_030_processUserOidc_existingUser_noCreate() {
        Map<String, Object> attributes = buildGoogleAttributes("g@x.com", "Google User");
        when(userCommonRepo.findByPhoneEquals("g@x.com")).thenReturn(buildUser(7L, "g@x.com"));

        OidcUser oidcUser = mock(OidcUser.class);
        OidcUser result = service.processUserOidc("google", attributes, oidcUser);

        assertSame(oidcUser, result);
        verify(userCommonRepo, never()).save(any(UserCommon.class));
    }

    /**
     * TC_UC_032 - processUserOAuth2_emptyEmail - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_032_processUserOAuth2_emptyEmail
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_032_processUserOAuth2_emptyEmail() {
        Map<String, Object> attributes = buildFacebookAttributes(null, "FB User");

        assertThrows(RuntimeException.class,
                () -> service.processUserOAuth2("facebook", attributes, mock(OAuth2User.class)));
    }

    /**
     * TC_UC_034 - processUserOAuth2_payloadExistingUser - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_034_processUserOAuth2_payloadExistingUser
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_034_processUserOAuth2_payloadExistingUser() {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("g@x.com");
        payload.setSubject("google-sub-2");
        payload.set("name", "Google Name");

        when(userCommonRepo.findByPhoneEquals("g@x.com")).thenReturn(buildUser(13L, "g@x.com"));

        OAuth2User result = service.processUserOAuth2(payload);

        assertNotNull(result);
        verify(userCommonRepo, never()).save(any(UserCommon.class));
        verify(cacheService).putCache(eq(cacheManager), eq("user"), eq("user13"), any());
    }

    // ======================= adminLogin =======================
    /**
     * TC_UC_016 - adminLogin_notAdmin - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_016_adminLogin_notAdmin (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_016_adminLogin_notAdmin() {
        InputLoginDTO dto = new InputLoginDTO();
        InputGetTokenDTO token = new InputGetTokenDTO();
        token.setUsername("0912");
        token.setPassword(Base64.getEncoder().encodeToString("pwd".getBytes()));
        dto.setBodyGetToken(token);

        UserCommon user = buildUser(1L, "0912");
        user.setRole(2); // not admin
        when(userCommonRepo.findByPhoneEquals("0912")).thenReturn(user);
        when(envProperties.getAuthServerURI()).thenReturn("http://localhost/");

        ResponseEntity<?> resp = service.adminLogin(dto);

        assertEquals(HttpStatus.EXPECTATION_FAILED, resp.getStatusCode());
    }

    // ======================= generateCommonLangPassword =======================
    /**
     * TC_UC_035 - generateCommonLangPassword_length10 - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_035_generateCommonLangPassword_length10
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_035_generateCommonLangPassword_length10() {
        String pwd = service.generateCommonLangPassword();
        assertNotNull(pwd);
        assertEquals(10, pwd.length());
    }

    // ======================= getUserInfo =======================
    /**
     * TC_UC_036 - getUserInfo_found - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_036_getUserInfo_found (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_036_getUserInfo_found() {
        UserCommon user = buildUser(1L, "0912");
        when(userCommonRepo.findByPhoneEquals("0912")).thenReturn(user);

        ResponseEntity<?> resp = service.getUserInfo("0912");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        UserCommonDTO dto = (UserCommonDTO) resp.getBody();
        assertEquals(1L, dto.getId());
    }

    /**
     * TC_UC_037 - getUserInfo_notFound_returnsEmptyDto - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_037_getUserInfo_notFound_returnsEmptyDto
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_037_getUserInfo_notFound_returnsEmptyDto() {
        when(userCommonRepo.findByPhoneEquals("0000")).thenReturn(null);

        ResponseEntity<?> resp = service.getUserInfo("0000");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        UserCommonDTO dto = (UserCommonDTO) resp.getBody();
        assertNull(dto.getId());
    }

    // ======================= getUsersByRole =======================
    /**
     * TC_UC_045 - getUsersByRole_hasUsers - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_045_getUsersByRole_hasUsers (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_045_getUsersByRole_hasUsers() {
        List<UserCommon> users = Arrays.asList(buildUser(1L, "0911"), buildUser(2L, "0922"));
        when(userCommonRepo.findByRole(2)).thenReturn(users);

        List<UserCommon> result = service.getUsersByRole(2);

        assertEquals(2, result.size());
    }

    /**
     * TC_UC_046 - getUsersByRole_empty - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_046_getUsersByRole_empty (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_046_getUsersByRole_empty() {
        when(userCommonRepo.findByRole(99)).thenReturn(Collections.emptyList());

        List<UserCommon> result = service.getUsersByRole(99);

        assertTrue(result.isEmpty());
    }

    // ======================= getTotalUserByRole =======================
    /**
     * TC_UC_047 - getTotalUserByRole_hasData - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_047_getTotalUserByRole_hasData (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_047_getTotalUserByRole_hasData() {
        when(userCommonRepo.findDistinctYears()).thenReturn(Arrays.asList(2023, 2024));
        when(userCommonRepo.countUsersByRoleAndYear(Arrays.asList(2, 4))).thenReturn(Arrays.asList(
                new Object[] { 2, 2023, 3L },
                new Object[] { 4, 2024, 5L }));

        ResponseEntity<ResponseObject> resp = service.getTotalUserByRole(Arrays.asList(2, 4));

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        @SuppressWarnings("unchecked")
        List<Map<String, Long>> data = (List<Map<String, Long>>) resp.getBody().getData();
        assertEquals(2, data.size());
        assertEquals(3L, data.get(0).get("2023"));
    }

    /**
     * TC_UC_048 - getTotalUserByRole_emptyRoles - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_048_getTotalUserByRole_emptyRoles
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_048_getTotalUserByRole_emptyRoles() {
        when(userCommonRepo.findDistinctYears()).thenReturn(Collections.emptyList());
        when(userCommonRepo.countUsersByRoleAndYear(Collections.emptyList())).thenReturn(Collections.emptyList());

        ResponseEntity<ResponseObject> resp = service.getTotalUserByRole(Collections.emptyList());

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    /**
     * TC_UC_049 - getTotalUserByRole_exception - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_049_getTotalUserByRole_exception
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_049_getTotalUserByRole_exception() {
        when(userCommonRepo.findDistinctYears()).thenThrow(new RuntimeException("db"));

        ResponseEntity<ResponseObject> resp = service.getTotalUserByRole(Arrays.asList(2, 4));

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    // ======================= getStatisticalUser =======================
    /**
     * TC_UC_050 - getStatisticalUser_hasData - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_050_getStatisticalUser_hasData (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_050_getStatisticalUser_hasData() {
        LocalDateTime s = LocalDateTime.now().minusDays(30);
        LocalDateTime e = LocalDateTime.now();
        StatisticalUser stat = mock(StatisticalUser.class);
        when(userCommonRepo.statisticalUser(eq(s), eq(e), any())).thenReturn(stat);

        ResponseEntity<?> resp = service.getStatisticalUser(s, e, Arrays.asList(2, 3));

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    /**
     * TC_UC_051 - getStatisticalUser_noData - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_051_getStatisticalUser_noData (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_051_getStatisticalUser_noData() {
        LocalDateTime s = LocalDateTime.now().minusDays(30);
        LocalDateTime e = LocalDateTime.now();
        when(userCommonRepo.statisticalUser(any(), any(), any())).thenReturn(null);

        ResponseEntity<?> resp = service.getStatisticalUser(s, e, Arrays.asList(2));

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    // ======================= updateInforUser =======================

    /**
     * TC_UC_040 - updateInforUser_avatarFail - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_040_updateInforUser_avatarFail (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_040_updateInforUser_avatarFail() {
        UserCommon user = buildUser(1L, "0912");
        when(userCommonRepo.findById(1L)).thenReturn(Optional.of(user));

        UserCommonServiceImpl spyService = Mockito.spy(service);
        ResponseEntity<ResponseObject> avatarResp = new ResponseEntity<>(new ResponseObject(),
                HttpStatus.INTERNAL_SERVER_ERROR);
        doReturn(avatarResp).when(spyService).saveOrUpdateAvatar(eq(1L), any());

        UserInforDto dto = new UserInforDto("new@x.com", null);
        ResponseEntity<ResponseObject> resp = spyService.updateInforUser(1L, dto);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

    // ======================= getUserEmail =======================
    /**
     * TC_UC_064 - getUserEmail_hasUsers - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_064_getUserEmail_hasUsers (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_064_getUserEmail_hasUsers() {
        List<Object[]> rows = Arrays.asList(
                new Object[] { 1L, "a@x" },
                new Object[] { 2L, "b@x" });
        when(userCommonRepo.getAllUserEmails()).thenReturn(rows);

        Map<Long, String> result = service.getUserEmail();

        assertEquals(2, result.size());
        assertEquals("a@x", result.get(1L));
    }

    /**
     * TC_UC_065 - getUserEmail_empty - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_065_getUserEmail_empty (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_065_getUserEmail_empty() {
        when(userCommonRepo.getAllUserEmails()).thenReturn(Collections.emptyList());

        Map<Long, String> result = service.getUserEmail();

        assertTrue(result.isEmpty());
    }

    // ======================= buildUserCommonDTO =======================
    /**
     * TC_UC_066 - buildUserCommonDTO_fullFields - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_066_buildUserCommonDTO_fullFields
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_066_buildUserCommonDTO_fullFields() {
        UserCommon u = buildUser(1L, "0912");
        u.setAddress("HN");
        u.setAvatar("av.png");
        u.setStatus("ACTIVE");

        UserCommonDTO dto = service.buildUserCommonDTO(u);

        assertEquals(1L, dto.getId());
        assertEquals("0912", dto.getPhone());
        assertEquals("HN", dto.getAddress());
        assertEquals("av.png", dto.getAvatar());
        assertEquals("ACTIVE", dto.getStatus());
    }

    /**
     * TC_UC_067 - buildUserCommonDTO_nullInput_caught - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_067_buildUserCommonDTO_nullInput_caught
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_067_buildUserCommonDTO_nullInput_caught() {
        UserCommonDTO dto = service.buildUserCommonDTO(null);

        assertNotNull(dto);
        assertNull(dto.getId());
    }

    // ======================= getListAdmin =======================
    /**
     * TC_UC_041 - getListAdmin_hasAdmins - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_041_getListAdmin_hasAdmins (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_041_getListAdmin_hasAdmins() {
        String body = "{\"viceAdminRole\":3,\"adminRole\":4,\"page\":1,\"size\":10}";
        List<UserCommon> admins = Arrays.asList(buildUser(1L, "0911"), buildUser(2L, "0922"));
        Page<UserCommon> page = new PageImpl<>(admins);
        when(userCommonRepo.findAdmins(eq(3), eq(4), any(Pageable.class))).thenReturn(page);

        ResponseEntity<?> resp = service.getListAdmin(body);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    /**
     * TC_UC_042 - getListAdmin_emptyPage - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_042_getListAdmin_emptyPage (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_042_getListAdmin_emptyPage() {
        String body = "{\"viceAdminRole\":3,\"adminRole\":4,\"page\":1,\"size\":10}";
        Page<UserCommon> page = new PageImpl<>(Collections.emptyList());
        when(userCommonRepo.findAdmins(anyInt(), anyInt(), any(Pageable.class))).thenReturn(page);

        ResponseEntity<?> resp = service.getListAdmin(body);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    /**
     * TC_UC_043 - getListAdmin_missingPageHandledByExceptionHandler -
     * Exception/Edge
     * Mục tiêu: Verify logic cho
     * TC_UC_043_getListAdmin_missingPageHandledByExceptionHandler (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_043_getListAdmin_missingPageHandledByExceptionHandler() {
        String body = "{\"viceAdminRole\":3,\"adminRole\":4,\"size\":10}";
        when(restExceptionHandler.handleNullPointerException(any(NullPointerException.class)))
                .thenReturn(new ResponseEntity<>(new ResponseObject(), HttpStatus.BAD_REQUEST));

        ResponseEntity<?> resp = service.getListAdmin(body);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    /**
     * TC_UC_044 - getListAdmin_pageZeroBoundary - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_044_getListAdmin_pageZeroBoundary
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_044_getListAdmin_pageZeroBoundary() {
        String body = "{\"viceAdminRole\":3,\"adminRole\":4,\"page\":0,\"size\":10}";

        assertThrows(IllegalArgumentException.class, () -> service.getListAdmin(body));
    }

    // ======================= updatePremiumUser =======================

    /**
     * TC_UC_059 - updatePremiumUser_alreadyPremium - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_059_updatePremiumUser_alreadyPremium
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_059_updatePremiumUser_alreadyPremium() throws Throwable {
        ResponseObject walletResp = new ResponseObject();
        WalletDTO wallet = WalletDTO.builder().userId(1L).totalMoney(100000L).build();
        walletResp.setData(wallet);
        when(paymentFeignClient.getCurrentUserWallet()).thenReturn(walletResp);
        when(mapper.convertValue(any(), eq(WalletDTO.class))).thenReturn(wallet);

        UserCommon user = buildUser(1L, "0912");
        user.setIsPremium(true);
        user.setPremiumExpDate(LocalDateTime.now().plusDays(10));
        when(userCommonRepo.findById(1L)).thenReturn(Optional.of(user));

        ResponseEntity<String> resp = service.updatePremiumUser(50000L, 3);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertTrue(resp.getBody().contains("premium"));
    }

    /**
     * TC_UC_060 - updatePremiumUser_insufficientBalance - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_060_updatePremiumUser_insufficientBalance
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_060_updatePremiumUser_insufficientBalance() throws Throwable {
        ResponseObject walletResp = new ResponseObject();
        WalletDTO wallet = WalletDTO.builder().userId(1L).totalMoney(1000L).build();
        walletResp.setData(wallet);
        when(paymentFeignClient.getCurrentUserWallet()).thenReturn(walletResp);
        when(mapper.convertValue(any(), eq(WalletDTO.class))).thenReturn(wallet);

        UserCommon user = buildUser(1L, "0912");
        user.setIsPremium(false);
        when(userCommonRepo.findById(1L)).thenReturn(Optional.of(user));

        ResponseEntity<String> resp = service.updatePremiumUser(50000L, 3);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertTrue(resp.getBody().contains("Số dư"));
    }

    /**
     * TC_UC_061 - updatePremiumUser_userNotFound - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_061_updatePremiumUser_userNotFound
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_061_updatePremiumUser_userNotFound() {
        ResponseObject walletResp = new ResponseObject();
        WalletDTO wallet = WalletDTO.builder().userId(99L).totalMoney(100000L).build();
        walletResp.setData(wallet);
        when(paymentFeignClient.getCurrentUserWallet()).thenReturn(walletResp);
        when(mapper.convertValue(any(), eq(WalletDTO.class))).thenReturn(wallet);
        when(userCommonRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> service.updatePremiumUser(50000L, 3));
    }

    // ======================= compareJobCounts =======================
    /**
     * TC_UC_052 - compareJobCounts_hasData - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_052_compareJobCounts_hasData (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_052_compareJobCounts_hasData() {
        YearRequest req = new YearRequest();
        req.setInputYear(2024);
        when(jobRepo.countJobsByMonth(2024)).thenReturn(Collections.singletonList(new Object[] { "1", 3L }));
        when(freelancerRepo.countFreelancersByMonth(2024))
                .thenReturn(Collections.singletonList(new Object[] { "1", 2L }));

        ResponseEntity<Map<String, Object>> resp = service.compareJobCounts(req);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody().get("data"));
    }

    /**
     * TC_UC_053 - compareJobCounts_noData - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_053_compareJobCounts_noData (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_053_compareJobCounts_noData() {
        YearRequest req = new YearRequest();
        req.setInputYear(2000);
        when(jobRepo.countJobsByMonth(2000)).thenReturn(Collections.emptyList());
        when(freelancerRepo.countFreelancersByMonth(2000)).thenReturn(Collections.emptyList());

        ResponseEntity<Map<String, Object>> resp = service.compareJobCounts(req);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    /**
     * TC_UC_054 - compareJobCounts_exception - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_054_compareJobCounts_exception (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_054_compareJobCounts_exception() {
        YearRequest req = new YearRequest();
        req.setInputYear(2024);
        when(jobRepo.countJobsByMonth(2024)).thenThrow(new RuntimeException("db error"));

        ResponseEntity<Map<String, Object>> resp = service.compareJobCounts(req);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

    // ======================= compareJobCountsByYear =======================
    /**
     * TC_UC_055 - compareJobCountsByYear_multipleYears - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_055_compareJobCountsByYear_multipleYears
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_055_compareJobCountsByYear_multipleYears() {
        when(jobRepo.findDistinctYears()).thenReturn(Arrays.asList(2023, 2024));
        when(freelancerRepo.findDistinctYears()).thenReturn(Arrays.asList(2024, 2025));
        when(jobRepo.countJobsByYear()).thenReturn(Collections.singletonList(new Object[] { "2024", 5L }));
        when(freelancerRepo.countFreelancersByYear())
                .thenReturn(Collections.singletonList(new Object[] { "2025", 3L }));

        ResponseEntity<Map<String, Map<String, Long>>> resp = service.compareJobCountsByYear();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().get("common").containsKey("2023"));
        assertTrue(resp.getBody().get("common").containsKey("2025"));
    }

    /**
     * TC_UC_056 - compareJobCountsByYear_emptyDataset - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_056_compareJobCountsByYear_emptyDataset
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_056_compareJobCountsByYear_emptyDataset() {
        when(jobRepo.findDistinctYears()).thenReturn(Collections.emptyList());
        when(freelancerRepo.findDistinctYears()).thenReturn(Collections.emptyList());
        when(jobRepo.countJobsByYear()).thenReturn(Collections.emptyList());
        when(freelancerRepo.countFreelancersByYear()).thenReturn(Collections.emptyList());

        ResponseEntity<Map<String, Map<String, Long>>> resp = service.compareJobCountsByYear();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().get("common").isEmpty());
    }

    /**
     * TC_UC_057 - compareJobCountsByYear_exception - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_057_compareJobCountsByYear_exception
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_057_compareJobCountsByYear_exception() {
        when(jobRepo.findDistinctYears()).thenThrow(new RuntimeException("db"));

        ResponseEntity<Map<String, Map<String, Long>>> resp = service.compareJobCountsByYear();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

    // ======================= saveOrUpdateAvatar =======================

    /**
     * TC_UC_026 - saveOrUpdateAvatar_userNotFound_throwsDueToNullStatus -
     * Exception/Edge
     * Mục tiêu: Verify logic cho
     * TC_UC_026_saveOrUpdateAvatar_userNotFound_throwsDueToNullStatus (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_026_saveOrUpdateAvatar_userNotFound_throwsDueToNullStatus() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[] { 1 });
        File fake = File.createTempFile("tmp", ".png");
        fake.deleteOnExit();
        when(utils.convert(file)).thenReturn(fake);
        when(userCommonRepo.findById(99L)).thenReturn(Optional.empty());

        // Bug trong code: khi user không tồn tại, httpStatus để null → ResponseEntity
        // ctor throw.
        assertThrows(IllegalArgumentException.class, () -> service.saveOrUpdateAvatar(99L, file));
        verify(userCommonRepo, never()).save(any(UserCommon.class));
    }

    // ======================= updateInforUser =======================
    /**
     * TC_UC_039 - updateInforUser_userNotFound - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_039_updateInforUser_userNotFound
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_039_updateInforUser_userNotFound() {
        UserInforDto dto = new UserInforDto("new@x.com", null);
        when(userCommonRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<ResponseObject> resp = service.updateInforUser(99L, dto);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

    // ======================= checkAndExpirePremium (inner class)
    // =======================
    /**
     * TC_UC_062 - checkAndExpirePremium_hasExpiredUsers - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_062_checkAndExpirePremium_hasExpiredUsers
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_062_checkAndExpirePremium_hasExpiredUsers() {
        UserCommon u1 = buildUser(1L, "0911");
        u1.setIsPremium(true);
        u1.setPremiumExpDate(LocalDateTime.now().minusDays(1));
        UserCommon u2 = buildUser(2L, "0922");
        u2.setIsPremium(true);
        u2.setPremiumExpDate(LocalDateTime.now().minusHours(1));
        when(userCommonRepo.findExpiredUsers(any(LocalDateTime.class))).thenReturn(Arrays.asList(u1, u2));

        UserCommonServiceImpl.PremiumScheduler scheduler = service.new PremiumScheduler();
        scheduler.checkAndExpirePremium();

        assertFalse(u1.getIsPremium());
        assertNull(u1.getPremiumExpDate());
        assertFalse(u2.getIsPremium());
        verify(userCommonRepo).saveAll(anyList());
    }

    /**
     * TC_UC_063 - checkAndExpirePremium_noExpiredUsers - Exception/Edge
     * Mục tiêu: Verify logic cho TC_UC_063_checkAndExpirePremium_noExpiredUsers
     * (Mock-based).
     * CheckDB: N
     * Rollback: N
     */
    @Test
    void TC_UC_063_checkAndExpirePremium_noExpiredUsers() {
        when(userCommonRepo.findExpiredUsers(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        UserCommonServiceImpl.PremiumScheduler scheduler = service.new PremiumScheduler();
        scheduler.checkAndExpirePremium();

        verify(userCommonRepo, never()).saveAll(anyList());
    }
}
