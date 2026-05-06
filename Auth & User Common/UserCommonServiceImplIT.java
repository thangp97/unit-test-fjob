package com.resourceservice.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.jober.utilsservice.dto.WalletResDTO;
import com.jober.utilsservice.utils.Utility;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import com.resourceservice.common.CommonUtils;
import com.resourceservice.dto.UserInforDto;
import com.resourceservice.dto.request.UserForChangingPass;
import com.resourceservice.feign.PaymentFeignClient;
import com.resourceservice.interceptor.BearerTokenWrapper;
import com.resourceservice.model.UserCommon;
import com.resourceservice.repository.RecruiterConfigurationRepository;
import com.resourceservice.repository.UserCommonRepo;
import com.resourceservice.utilsmodule.CacheService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration Tests cho {@link UserCommonServiceImpl}.
 * Chứa các TC Standard có CheckDB=Y và Rollback=Y (thay đổi trạng thái DB thật).
 */
@SpringBootTest
@Testcontainers
@Transactional
@ActiveProfiles("test")
class UserCommonServiceImplIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("pgvector/pgvector:pg15")
                    .withDatabaseName("jober_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withInitScript("init-pgvector.sql");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private UserCommonServiceImpl service;
    @Autowired private UserCommonRepo userCommonRepo;
    @Autowired private RecruiterConfigurationRepository recruiterConfigRepo;

    // External dependencies mock
    @MockBean private PaymentFeignClient paymentFeignClient;
    @MockBean private S3ServiceImpl s3Service;
    @MockBean private CommunityService communityService;
    @MockBean private CacheService cacheService;
    @MockBean private CacheManager cacheManager;
    @MockBean private BearerTokenWrapper tokenWrapper;
    @MockBean private CommonUtils utils;

    private UserCommon seedUser(String phone, String email, int role, boolean isPremium) {
        UserCommon u = new UserCommon();
        u.setPhone(phone);
        u.setEmail(email);
        u.setName("User " + phone);
        u.setRole(role);
        u.setIsPremium(isPremium);
        u.setActive(1);
        u.setCreationDate(LocalDateTime.now());
        u.setPin("encoded_pin");
        return userCommonRepo.save(u);
    }

    /**
     * TC_UC_004 - createUser - Standard
     * Mục tiêu: Create new user successfully (role=CANDIDATE) -> DB lưu user mới.
     * CheckDB: Y
     * Rollback: Y
     */
    @Test
    void TC_UC_004_createUser_success_candidate() {
        String body = "{\"phone\":\"0912345670\",\"email\":\"c@x.com\",\"password\":\"" +
                Base64.getEncoder().encodeToString("pwd123".getBytes()) + "\",\"role\":1}";

        try (MockedStatic<Utility> utilityMock = Mockito.mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.getContent(anyString(), anyString(), anyString(), anyString())).thenReturn("sms");
            utilityMock.when(() -> Utility.isSendSMS(anyString(), anyString(), anyString())).thenReturn(true);
            utilityMock.when(() -> Utility.responseObject(anyString(), anyString(), anyString(), any())).thenCallRealMethod();

            ResponseObject resp = service.createUser(body);

            assertEquals("200 OK", resp.getStatus());
            UserCommon saved = userCommonRepo.findByPhoneEquals("0912345670");
            assertNotNull(saved);
            assertEquals("c@x.com", saved.getEmail());
            assertEquals(1, saved.getRole());
        }
    }

    /**
     * TC_UC_008 - createUser - Standard
     * Mục tiêu: Valid introPhone provided -> save user và save wallet cho người giới thiệu.
     * CheckDB: Y
     * Rollback: Y
     */
    @Test
    void TC_UC_008_createUser_withIntroPhone_addsBonusWallet() {
        seedUser("0999888777", "intro@x.com", 1, false);

        String body = "{\"phone\":\"0912345671\",\"email\":\"a1@x.com\",\"password\":\"" +
                Base64.getEncoder().encodeToString("pwd123".getBytes()) +
                "\",\"role\":1,\"introPhone\":\"0999888777\"}";

        try (MockedStatic<Utility> utilityMock = Mockito.mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.getContent(anyString(), anyString(), anyString(), anyString())).thenReturn("sms");
            utilityMock.when(() -> Utility.isSendSMS(anyString(), anyString(), anyString())).thenReturn(true);
            utilityMock.when(() -> Utility.responseObject(anyString(), anyString(), anyString(), any())).thenCallRealMethod();

            ResponseObject resp = service.createUser(body);

            assertEquals("200 OK", resp.getStatus());
            assertNotNull(userCommonRepo.findByPhoneEquals("0912345671"));
            verify(communityService, Mockito.atLeastOnce()).saveWallet(any());
        }
    }

    /**
     * TC_UC_009 - saveUser - Standard
     * Mục tiêu: Regular user update -> DB thay đổi email.
     * CheckDB: Y
     * Rollback: Y
     */
    @Test
    void TC_UC_009_saveUser_regularUpdate() {
        seedUser("0900111222", "old@x.com", 1, false);
        String body = "{\"phone\":\"0900111222\",\"email\":\"new@x.com\",\"name\":\"New Name\"}";

        ResponseObject resp = service.saveUser(body);

        assertEquals("200 OK", resp.getStatus());
        UserCommon updated = userCommonRepo.findByPhoneEquals("0900111222");
        assertEquals("new@x.com", updated.getEmail());
        assertEquals("New Name", updated.getName());
    }

    /**
     * TC_UC_010 - saveUser - Standard
     * Mục tiêu: Forgot password flow -> pin được thay đổi.
     * CheckDB: Y
     * Rollback: Y
     */
    @Test
    void TC_UC_010_saveUser_forgotPassword() {
        UserCommon u = seedUser("0900111333", "old2@x.com", 1, false);
        String oldPin = u.getPin();
        String body = "{\"phone\":\"0900111333\",\"type\":\"forgot_pass\"}";

        try (MockedStatic<Utility> utilityMock = Mockito.mockStatic(Utility.class)) {
            utilityMock.when(Utility::generatePin).thenReturn(123456);
            utilityMock.when(() -> Utility.getContent(anyString(), anyString(), anyString(), anyString())).thenReturn("sms");
            utilityMock.when(() -> Utility.isSendSMS(anyString(), anyString(), anyString())).thenReturn(true);
            utilityMock.when(() -> Utility.responseObject(anyString(), anyString(), anyString(), any())).thenCallRealMethod();

            ResponseObject resp = service.saveUser(body);

            assertEquals("200 OK", resp.getStatus());
            UserCommon updated = userCommonRepo.findByPhoneEquals("0900111333");
            assertNotEquals(oldPin, updated.getPin());
        }
    }

    /**
     * TC_UC_023 - forgetPassword - Standard
     * Mục tiêu: User exists -> update pin.
     * CheckDB: Y
     * Rollback: Y
     */
    @Test
    void TC_UC_023_forgetPassword_userExists() {
        UserCommon u = seedUser("0900111444", "old3@x.com", 1, false);
        String oldPin = u.getPin();
        UserForChangingPass param = UserForChangingPass.builder().phone("0900111444").build();

        try (MockedStatic<Utility> utilityMock = Mockito.mockStatic(Utility.class)) {
            utilityMock.when(Utility::generatePin).thenReturn(654321);
            utilityMock.when(() -> Utility.getContent(anyString(), anyString(), anyString(), anyString())).thenReturn("sms");
            utilityMock.when(() -> Utility.isSendSMS(anyString(), anyString(), anyString())).thenReturn(true);
            utilityMock.when(() -> Utility.responseObject(anyString(), anyString(), anyString(), any())).thenCallRealMethod();

            ResponseObject resp = service.forgetPassword(param);

            assertEquals("200 OK", resp.getStatus());
            UserCommon updated = userCommonRepo.findByPhoneEquals("0900111444");
            assertNotEquals(oldPin, updated.getPin());
        }
    }

    /**
     * TC_UC_025 - saveOrUpdateAvatar - Standard
     * Mục tiêu: Upload new avatar successfully -> DB update avatar field.
     * CheckDB: Y
     * Rollback: Y
     */
    @Test
    void TC_UC_025_saveOrUpdateAvatar_success() throws IOException {
        UserCommon u = seedUser("0900111555", "avatar@x.com", 1, false);
        u.setAvatar("old_avatar.png");
        userCommonRepo.save(u);
        when(tokenWrapper.getUid()).thenReturn(u.getId());
        
        MockMultipartFile file = new MockMultipartFile("file", "new_avatar.png", "image/png", new byte[10]);
        when(utils.convert(any())).thenReturn(new File("new_avatar.png"));
        Mockito.doNothing().when(s3Service).uploadFile(anyLong(), any(File.class));

        ResponseEntity<?> resp = service.saveOrUpdateAvatar(u.getId(), file);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        UserCommon updated = userCommonRepo.findById(u.getId()).get();
        assertEquals("http://s3/new_avatar.png", updated.getAvatar());
    }

    /**
     * TC_UC_028 - processUserOidc - Standard
     * Mục tiêu: User does not exist -> create new UserCommon.
     * CheckDB: Y
     * Rollback: Y
     */
    @Test
    void TC_UC_028_processUserOidc_createNew() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("email", "oidc@x.com");
        attrs.put("name", "OIDC User");
        attrs.put("sub", "g1");
        OidcUser mockUser = Mockito.mock(OidcUser.class);

        OidcUser res = service.processUserOidc("google", attrs, mockUser);

        assertNotNull(res);
        UserCommon u = userCommonRepo.findByEmail("oidc@x.com");
        assertNotNull(u);
        assertEquals("OIDC User", u.getName());
    }

    /**
     * TC_UC_031 - processUserOAuth2 - Standard
     * Mục tiêu: New OAuth2 user (FB) -> user created.
     * CheckDB: Y
     * Rollback: Y
     */
    @Test
    void TC_UC_031_processUserOAuth2_facebookNewUser() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("id", "fb1");
        attrs.put("email", "fb@x.com");
        attrs.put("name", "FB User");
        Map<String, Object> picture = new LinkedHashMap<>();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("url", "http://img.test/fb.png");
        picture.put("data", data);
        attrs.put("picture", picture);
        OAuth2User mockUser = Mockito.mock(OAuth2User.class);

        OAuth2User res = service.processUserOAuth2("facebook", attrs, mockUser);

        assertNotNull(res);
        UserCommon u = userCommonRepo.findByEmail("fb@x.com");
        assertNotNull(u);
        assertEquals("FB User", u.getName());
    }

    /**
     * TC_UC_033 - processUserOAuth2 - Standard
     * Mục tiêu: Payload new user -> user created.
     * CheckDB: Y
     * Rollback: Y
     */
    @Test
    void TC_UC_033_processUserOAuth2_payloadNewUser() {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("g@x.com");
        payload.setSubject("sub1");
        payload.set("name", "G User");

        OAuth2User res = service.processUserOAuth2(payload);

        assertNotNull(res);
        UserCommon u = userCommonRepo.findByEmail("g@x.com");
        assertNotNull(u);
        assertEquals("G User", u.getName());
    }

    /**
     * TC_UC_038 - updateInforUser - Standard
     * Mục tiêu: Update email -> DB thay đổi email.
     * CheckDB: Y
     * Rollback: Y
     */
    @Test
    void TC_UC_038_updateInforUser_success() {
        UserCommon u = seedUser("0900111666", "old5@x.com", 1, false);
        when(tokenWrapper.getUid()).thenReturn(u.getId());
        UserInforDto dto = new UserInforDto();
        dto.setEmail("new5@x.com");

        ResponseEntity<?> resp = service.updateInforUser(u.getId(), dto);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        UserCommon updated = userCommonRepo.findById(u.getId()).get();
        assertEquals("new5@x.com", updated.getEmail());
    }

    /**
     * TC_UC_058 - updatePremiumUser - Standard
     * Mục tiêu: Successful upgrade -> isPremium=true.
     * CheckDB: Y
     * Rollback: Y
     */
    @Test
    void TC_UC_058_updatePremiumUser_success() throws Throwable {
        UserCommon u = seedUser("0900111777", "prem@x.com", 1, false);
        WalletResDTO wallet = new WalletResDTO();
        wallet.setUserId(u.getId());
        wallet.setTotalMoney(100000L);

        ResponseObject mockResp = new ResponseObject();
        mockResp.setData(wallet);
        when(paymentFeignClient.getCurrentUserWallet()).thenReturn(mockResp);
        when(paymentFeignClient.updateWallet(any())).thenReturn(new ResponseObject());

        when(tokenWrapper.getUid()).thenReturn(u.getId());
        ResponseEntity<String> resp = service.updatePremiumUser(50000L, 3);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        UserCommon updated = userCommonRepo.findById(u.getId()).get();
        assertTrue(updated.getIsPremium());
        assertNotNull(updated.getPremiumExpDate());
    }

    /**
     * TC_UC_062 - checkAndExpirePremium - Standard
     * Mục tiêu: Users with expired premium -> isPremium cleared.
     * CheckDB: Y
     * Rollback: Y
     */
    @Test
    void TC_UC_062_checkAndExpirePremium_hasExpired() {
        UserCommon u = seedUser("0900111888", "exp@x.com", 1, true);
        u.setPremiumExpDate(LocalDateTime.now().minusDays(1));
        userCommonRepo.save(u);

        UserCommonServiceImpl.PremiumScheduler scheduler = service.new PremiumScheduler();
        scheduler.checkAndExpirePremium();

        UserCommon updated = userCommonRepo.findById(u.getId()).get();
        assertFalse(updated.getIsPremium());
        assertNull(updated.getPremiumExpDate());
    }
}
