package com.resourceservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.resourceservice.config.EnvProperties;
import com.resourceservice.dto.InputLoginDTO;
import com.resourceservice.service.FacebookService;
import com.resourceservice.service.FreelancerService;
import com.resourceservice.service.UserCommonService;
import com.resourceservice.service.impl.S3ServiceImpl;
import com.resourceservice.utilsmodule.utils.CustomOAuth2UserService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
class UserCommonCtrlTest {

  @InjectMocks
  private UserCommonCtrl userCommonCtrl;

  @Mock
  private UserCommonService userCommonService;
  @Mock
  private S3ServiceImpl s3Service;
  @Mock
  private EnvProperties envProperties;
  @Mock
  private CustomOAuth2UserService customOAuth2UserService;
  @Mock
  private FacebookService facebookService;
  @Mock
  private FreelancerService freelancerService;

  @Test
  void login_validInput_returnsDelegatedResponse() {
    // Test Case ID: TC_USER_COMMON_CTRL_LOGIN_001
    // Scenario ID: SC_USER_COMMON_CTRL_HAPPY_001
    // Objective: Verify /bs-user/login delegates to service and returns service response.
    // Covered Branch/Path: Valid request -> userCommonService.login() -> return ResponseEntity.
    // DB Check: This method does not access repository/DAO directly (controller-layer delegation only).
    // Rollback: Not applicable.

    // Arrange
    InputLoginDTO input = new InputLoginDTO();
    ResponseEntity<?> expected = ResponseEntity.ok(Map.of("result", "ok"));
    when(userCommonService.login(input)).thenReturn(expected);

    // Act
    ResponseEntity<?> actual = userCommonCtrl.login(input);

    // Assert
    assertThat(actual).isSameAs(expected);
    verify(userCommonService).login(input);
    verifyNoMoreInteractions(userCommonService);
  }

  @Test
  void login_googleMobileValidToken_returnsUserAttributes() {
    // Test Case ID: TC_USER_COMMON_CTRL_GOOGLE_001
    // Scenario ID: SC_USER_COMMON_CTRL_HAPPY_002
    // Objective: Verify mobile Google login returns OAuth user attributes when token is valid.
    // Covered Branch/Path: idToken exists -> customOAuth2UserService success -> HTTP 200.
    // DB Check: This method does not access repository/DAO directly (controller-layer delegation only).
    // Rollback: Not applicable.

    // Arrange
    Map<String, String> input = new HashMap<>();
    input.put("idToken", "valid-token");
    OAuth2User oAuth2User = org.mockito.Mockito.mock(OAuth2User.class);
    Map<String, Object> expectedAttributes = Map.of("email", "candidate@example.com");

    when(customOAuth2UserService.loadUserFromIdToken("valid-token")).thenReturn(oAuth2User);
    when(oAuth2User.getAttributes()).thenReturn(expectedAttributes);

    // Act
    ResponseEntity<?> actual = userCommonCtrl.login(input);

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(actual.getBody()).isEqualTo(expectedAttributes);
    verify(customOAuth2UserService).loadUserFromIdToken("valid-token");
    verify(oAuth2User).getAttributes();
  }

  @Test
  void login_googleMobileInvalidToken_returnsUnauthorized() {
    // Test Case ID: TC_USER_COMMON_CTRL_GOOGLE_002
    // Scenario ID: SC_USER_COMMON_CTRL_SAD_001
    // Objective: Verify mobile Google login returns 401 when token verification fails.
    // Covered Branch/Path: OAuth2AuthenticationException -> catch -> HTTP 401.
    // DB Check: This method does not access repository/DAO directly (controller-layer delegation only).
    // Rollback: Not applicable.

    // Arrange
    Map<String, String> input = new HashMap<>();
    input.put("idToken", "invalid-token");
    when(customOAuth2UserService.loadUserFromIdToken("invalid-token"))
        .thenThrow(new OAuth2AuthenticationException(new OAuth2Error("invalid_token")));

    // Act
    ResponseEntity<?> actual = userCommonCtrl.login(input);

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(actual.getBody()).isEqualTo("Invalid ID token");
    verify(customOAuth2UserService).loadUserFromIdToken("invalid-token");
  }

  @Test
  void facebookLogin_validAccessToken_returnsUserAttributes() {
    // Test Case ID: TC_USER_COMMON_CTRL_FACEBOOK_001
    // Scenario ID: SC_USER_COMMON_CTRL_HAPPY_003
    // Objective: Verify mobile Facebook login returns user attributes when token is valid.
    // Covered Branch/Path: accessToken exists -> facebookService success -> HTTP 200.
    // DB Check: This method does not access repository/DAO directly (controller-layer delegation only).
    // Rollback: Not applicable.

    // Arrange
    Map<String, String> input = new HashMap<>();
    input.put("accessToken", "fb-token");
    Map<String, Object> expectedAttributes = Map.of("id", "123", "email", "candidate@example.com");
    when(facebookService.verifyTokenAndGetUser("fb-token")).thenReturn(expectedAttributes);

    // Act
    ResponseEntity<?> actual = userCommonCtrl.facebookLogin(input);

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(actual.getBody()).isEqualTo(expectedAttributes);
    verify(facebookService).verifyTokenAndGetUser("fb-token");
  }

  @Test
  void facebookLogin_serviceThrows_returnsUnauthorizedWithMessage() {
    // Test Case ID: TC_USER_COMMON_CTRL_FACEBOOK_002
    // Scenario ID: SC_USER_COMMON_CTRL_SAD_002
    // Objective: Verify mobile Facebook login returns 401 when service throws.
    // Covered Branch/Path: Exception -> handleException -> HTTP 401 with message.
    // DB Check: This method does not access repository/DAO directly (controller-layer delegation only).
    // Rollback: Not applicable.

    // Arrange
    Map<String, String> input = new HashMap<>();
    input.put("accessToken", "fb-token");
    when(facebookService.verifyTokenAndGetUser("fb-token"))
        .thenThrow(new IllegalArgumentException("Invalid access token"));

    // Act
    ResponseEntity<?> actual = userCommonCtrl.facebookLogin(input);

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(actual.getBody()).isEqualTo("Invalid access token");
    verify(facebookService).verifyTokenAndGetUser("fb-token");
  }
}
