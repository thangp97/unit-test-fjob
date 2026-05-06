package com.resourceservice.config.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.resourceservice.utilsmodule.errors.OAuth2AuthenticationProcessingException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OAuth2UserInfoFactoryTest {

  @Test
  void getOAuth2UserInfo_googleRegistration_returnsGoogleUserInfo() {
    // Test Case ID: TC_OAUTH2_USER_INFO_FACTORY_001
    // Scenario ID: SC_OAUTH2_USER_INFO_FACTORY_HAPPY_001
    // Objective: Verify factory returns GoogleOAuth2UserInfo for google registration id.
    // Covered Branch/Path: registrationId=google branch.
    // DB Check: Not applicable.
    // Rollback: Not applicable.

    // Arrange
    Map<String, Object> attributes = Map.of("sub", "sub-id", "email", "candidate@example.com");

    // Act
    OAuth2UserInfo actual = OAuth2UserInfoFactory.getOAuth2UserInfo("google", attributes);

    // Assert
    assertThat(actual).isInstanceOf(GoogleOAuth2UserInfo.class);
  }

  @Test
  void getOAuth2UserInfo_facebookRegistration_returnsFacebookUserInfo() {
    // Test Case ID: TC_OAUTH2_USER_INFO_FACTORY_002
    // Scenario ID: SC_OAUTH2_USER_INFO_FACTORY_HAPPY_002
    // Objective: Verify factory returns FacebookOAuth2UserInfo for facebook registration id.
    // Covered Branch/Path: registrationId=facebook branch.
    // DB Check: Not applicable.
    // Rollback: Not applicable.

    // Arrange
    Map<String, Object> attributes = Map.of("id", "fb-id", "email", "candidate@example.com");

    // Act
    OAuth2UserInfo actual = OAuth2UserInfoFactory.getOAuth2UserInfo("facebook", attributes);

    // Assert
    assertThat(actual).isInstanceOf(FacebookOAuth2UserInfo.class);
  }

  @Test
  void getOAuth2UserInfo_unsupportedRegistration_throwsProcessingException() {
    // Test Case ID: TC_OAUTH2_USER_INFO_FACTORY_003
    // Scenario ID: SC_OAUTH2_USER_INFO_FACTORY_SAD_001
    // Objective: Verify factory throws exception for unsupported providers.
    // Covered Branch/Path: unsupported registration id -> exception branch.
    // DB Check: Not applicable.
    // Rollback: Not applicable.

    // Arrange
    Map<String, Object> attributes = Map.of("id", "x");

    // Act
    OAuth2AuthenticationProcessingException ex =
        assertThrows(
            OAuth2AuthenticationProcessingException.class,
            () -> OAuth2UserInfoFactory.getOAuth2UserInfo("linkedin", attributes));

    // Assert
    assertThat(ex.getMessage()).contains("Login with linkedin is not supported yet");
  }
}
