package com.resourceservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resourceservice.common.CommonUtils;
import com.resourceservice.config.EnvProperties;
import com.resourceservice.dto.InputGetTokenDTO;
import com.resourceservice.dto.InputLoginDTO;
import com.resourceservice.dto.UserCommonDTO;
import com.resourceservice.feign.PaymentFeignClient;
import com.resourceservice.interceptor.BearerTokenWrapper;
import com.resourceservice.model.UserCommon;
import com.resourceservice.repository.FreelancerRepo;
import com.resourceservice.repository.JobRepo;
import com.resourceservice.repository.RecruiterConfigurationRepository;
import com.resourceservice.repository.UserCommonRepo;
import com.resourceservice.utilsmodule.CacheService;
import com.resourceservice.utilsmodule.errors.RestExceptionHandler;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserCommonServiceImplTest {

  private UserCommonServiceImpl userCommonService;

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

  @BeforeEach
  void setUp() {
    userCommonService = Mockito.spy(new UserCommonServiceImpl(paymentFeignClient, mapper, tokenWrapper));
    ReflectionTestUtils.setField(userCommonService, "envProperties", envProperties);
    ReflectionTestUtils.setField(userCommonService, "userCommonRepo", userCommonRepo);
    ReflectionTestUtils.setField(userCommonService, "cacheService", cacheService);
    ReflectionTestUtils.setField(userCommonService, "cacheManager", cacheManager);
    ReflectionTestUtils.setField(userCommonService, "restExceptionHandler", restExceptionHandler);
    ReflectionTestUtils.setField(userCommonService, "communityService", communityService);
    ReflectionTestUtils.setField(userCommonService, "cacheManagerService", cacheManagerService);
    ReflectionTestUtils.setField(userCommonService, "jobRepo", jobRepo);
    ReflectionTestUtils.setField(userCommonService, "freelancerRepo", freelancerRepo);
    ReflectionTestUtils.setField(
        userCommonService, "recruiterConfigurationRepository", recruiterConfigurationRepository);
  }

  @Test
  void login_validRequest_returnsOkAndCachesUserAndToken() {
    // Test Case ID: TC_USER_COMMON_SERVICE_LOGIN_001
    // Scenario ID: SC_USER_COMMON_SERVICE_HAPPY_001
    // Objective: Verify candidate login success path caches user and tokens.
    // Covered Branch/Path: token retrieved -> user found -> DTO built -> cache put -> HTTP 200.
    // DB Check: Read-only DB path. Verify userCommonRepo.findByPhoneEquals() called once; verify save() never called.
    // Rollback: DB and cache are mocked; no real database/cache/external state is changed.

    // Arrange
    InputGetTokenDTO inputGetTokenDTO =
        new InputGetTokenDTO("0909000111", Base64.getEncoder().encodeToString("secret".getBytes()));
    InputLoginDTO input = new InputLoginDTO();
    input.setBodyGetToken(inputGetTokenDTO);

    ResponseEntity<String> tokenResponse =
        ResponseEntity.ok(
            "{\"access_token\":\"access-1\",\"refresh_token\":\"refresh-1\",\"expires_in\":\"3600\"}");
    doReturn(tokenResponse).when(userCommonService).obtainAccessToken(inputGetTokenDTO);

    UserCommon user = new UserCommon();
    user.setId(10L);
    user.setPhone("0909000111");
    user.setName("Candidate A");
    user.setRole(1);
    user.setEmail("candidate@example.com");
    when(userCommonRepo.findByPhoneEquals("0909000111")).thenReturn(user);

    // Act
    ResponseEntity<?> actual = userCommonService.login(input);

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(actual.getBody()).isInstanceOf(Map.class);

    Map<String, Object> responseBody = (Map<String, Object>) actual.getBody();
    assertThat(responseBody).containsKeys("user", "tokenObj");
    assertThat(responseBody.get("user")).isInstanceOf(UserCommonDTO.class);
    assertThat(responseBody.get("tokenObj").toString()).contains("\"access_token\":\"access-1\"");

    verify(userCommonRepo, times(1)).findByPhoneEquals("0909000111");
    verify(userCommonRepo, never()).save(any(UserCommon.class));
    ArgumentCaptor<String> cacheKeyCaptor = ArgumentCaptor.forClass(String.class);
    verify(cacheService, times(4))
        .putCache(eq(cacheManager), anyString(), cacheKeyCaptor.capture(), any());
    assertThat(cacheKeyCaptor.getAllValues())
        .containsExactlyInAnyOrder("user10", "access_token10", "refresh_token10", "expires_in10");
  }

  @Test
  void login_userNotFound_returnsExpectationFailedAndNoCacheWrite() {
    // Test Case ID: TC_USER_COMMON_SERVICE_LOGIN_002
    // Scenario ID: SC_USER_COMMON_SERVICE_SAD_001
    // Objective: Verify login returns failure when user cannot be loaded from repository.
    // Covered Branch/Path: token retrieved -> user null -> NullPointerException path -> HTTP 417.
    // DB Check: Read-only DB path. Verify findByPhoneEquals() called once; verify save() never called; cache write never called.
    // Rollback: DB and cache are mocked; no real database/cache/external state is changed.

    // Arrange
    InputGetTokenDTO inputGetTokenDTO =
        new InputGetTokenDTO("0909000222", Base64.getEncoder().encodeToString("secret".getBytes()));
    InputLoginDTO input = new InputLoginDTO();
    input.setBodyGetToken(inputGetTokenDTO);
    ResponseEntity<String> tokenResponse =
        ResponseEntity.ok(
            "{\"access_token\":\"access-2\",\"refresh_token\":\"refresh-2\",\"expires_in\":\"3600\"}");
    doReturn(tokenResponse).when(userCommonService).obtainAccessToken(inputGetTokenDTO);
    when(userCommonRepo.findByPhoneEquals("0909000222")).thenReturn(null);

    // Act
    ResponseEntity<?> actual = userCommonService.login(input);

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.EXPECTATION_FAILED);
    assertThat(actual.getBody()).isEqualTo("FAILED");
    verify(userCommonRepo, times(1)).findByPhoneEquals("0909000222");
    verify(userCommonRepo, never()).save(any(UserCommon.class));
    verify(cacheService, never()).putCache(any(), anyString(), anyString(), any());
  }

  @Test
  void login_invalidTokenPayload_throwsRuntimeException() {
    // Test Case ID: TC_USER_COMMON_SERVICE_LOGIN_003
    // Scenario ID: SC_USER_COMMON_SERVICE_EDGE_001
    // Objective: Verify login throws RuntimeException when token response is invalid JSON.
    // Covered Branch/Path: token parsing fails -> JsonProcessingException -> RuntimeException.
    // DB Check: Read-only DB path. Verify repository lookup called once; verify save() never called; cache write never called.
    // Rollback: DB and cache are mocked; no real database/cache/external state is changed.

    // Arrange
    InputGetTokenDTO inputGetTokenDTO =
        new InputGetTokenDTO("0909000333", Base64.getEncoder().encodeToString("secret".getBytes()));
    InputLoginDTO input = new InputLoginDTO();
    input.setBodyGetToken(inputGetTokenDTO);
    doReturn(ResponseEntity.ok("invalid-json")).when(userCommonService).obtainAccessToken(inputGetTokenDTO);

    UserCommon user = new UserCommon();
    user.setId(11L);
    user.setPhone("0909000333");
    when(userCommonRepo.findByPhoneEquals("0909000333")).thenReturn(user);

    // Act
    RuntimeException ex = assertThrows(RuntimeException.class, () -> userCommonService.login(input));

    // Assert
    assertThat(ex).isNotNull();
    verify(userCommonRepo, times(1)).findByPhoneEquals("0909000333");
    verify(userCommonRepo, never()).save(any(UserCommon.class));
    verify(cacheService, never()).putCache(any(), anyString(), anyString(), any());
  }

  @Test
  void login_nullTokenResponse_returnsExpectationFailed() {
    // Test Case ID: TC_USER_COMMON_SERVICE_LOGIN_004
    // Scenario ID: SC_USER_COMMON_SERVICE_SAD_002
    // Objective: Verify login returns failure when access-token call returns null response.
    // Covered Branch/Path: obtainAccessToken returns null -> NullPointerException path -> HTTP 417.
    // DB Check: Read-only DB path still executes findByPhoneEquals(); verify save() never called; cache write never called.
    // Rollback: DB and cache are mocked; no real database/cache/external state is changed.

    // Arrange
    InputGetTokenDTO inputGetTokenDTO =
        new InputGetTokenDTO("0909000444", Base64.getEncoder().encodeToString("secret".getBytes()));
    InputLoginDTO input = new InputLoginDTO();
    input.setBodyGetToken(inputGetTokenDTO);
    doReturn(null).when(userCommonService).obtainAccessToken(inputGetTokenDTO);
    UserCommon user = new UserCommon();
    user.setId(12L);
    user.setPhone("0909000444");
    when(userCommonRepo.findByPhoneEquals("0909000444")).thenReturn(user);

    // Act
    ResponseEntity<?> actual = userCommonService.login(input);

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.EXPECTATION_FAILED);
    assertThat(actual.getBody()).isEqualTo("FAILED");
    verify(userCommonRepo, times(1)).findByPhoneEquals("0909000444");
    verify(userCommonRepo, never()).save(any(UserCommon.class));
    verify(cacheService, never()).putCache(any(), anyString(), anyString(), any());
  }

  @Test
  void login_nullBodyGetToken_returnsExpectationFailed() {
    // Test Case ID: TC_USER_COMMON_SERVICE_LOGIN_005
    // Scenario ID: SC_USER_COMMON_SERVICE_EDGE_002
    // Objective: Verify login handles null token input body.
    // Covered Branch/Path: bodyGetToken null -> exception path -> HTTP 417.
    // DB Check: Verify repository read/write are not called and cache write is not called.
    // Rollback: DB and cache are mocked; no real database/cache/external state is changed.

    // Arrange
    InputLoginDTO input = new InputLoginDTO();
    input.setBodyGetToken(null);
    doReturn(null).when(userCommonService).obtainAccessToken(null);

    // Act
    ResponseEntity<?> actual = userCommonService.login(input);

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.EXPECTATION_FAILED);
    assertThat(actual.getBody()).isEqualTo("FAILED");
    verify(userCommonRepo, never()).findByPhoneEquals(anyString());
    verify(userCommonRepo, never()).save(any(UserCommon.class));
    verify(cacheService, never()).putCache(any(), anyString(), anyString(), any());
  }
}
