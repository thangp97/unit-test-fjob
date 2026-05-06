package com.resourceservice.config.User;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class FacebookOAuth2UserInfoTest {

  @Test
  void getImageUrl_pictureDataUrlExists_returnsUrl() {
    // Test Case ID: TC_FACEBOOK_USER_INFO_001
    // Scenario ID: SC_FACEBOOK_USER_INFO_HAPPY_001
    // Objective: Verify getImageUrl returns nested picture.data.url when present.
    // Covered Branch/Path: picture exists -> data exists -> url exists.
    // DB Check: Not applicable.
    // Rollback: Not applicable.

    // Arrange
    Map<String, Object> attributes =
        Map.of(
            "id", "fb-id",
            "name", "Candidate",
            "email", "candidate@example.com",
            "picture", Map.of("data", Map.of("url", "https://cdn/img.jpg")));
    FacebookOAuth2UserInfo userInfo = new FacebookOAuth2UserInfo(attributes);

    // Act
    String actual = userInfo.getImageUrl();

    // Assert
    assertThat(actual).isEqualTo("https://cdn/img.jpg");
  }

  @Test
  void getImageUrl_pictureMissing_returnsNull() {
    // Test Case ID: TC_FACEBOOK_USER_INFO_002
    // Scenario ID: SC_FACEBOOK_USER_INFO_SAD_001
    // Objective: Verify getImageUrl returns null when picture node does not exist.
    // Covered Branch/Path: picture missing -> return null.
    // DB Check: Not applicable.
    // Rollback: Not applicable.

    // Arrange
    Map<String, Object> attributes = Map.of("id", "fb-id", "name", "Candidate");
    FacebookOAuth2UserInfo userInfo = new FacebookOAuth2UserInfo(attributes);

    // Act
    String actual = userInfo.getImageUrl();

    // Assert
    assertThat(actual).isNull();
  }

  @Test
  void getImageUrl_pictureExistsButDataMissing_returnsNull() {
    // Test Case ID: TC_FACEBOOK_USER_INFO_003
    // Scenario ID: SC_FACEBOOK_USER_INFO_EDGE_001
    // Objective: Verify getImageUrl returns null when picture exists but data node is missing.
    // Covered Branch/Path: picture exists -> data missing -> return null.
    // DB Check: Not applicable.
    // Rollback: Not applicable.

    // Arrange
    Map<String, Object> attributes = Map.of("picture", Map.of("other", "value"));
    FacebookOAuth2UserInfo userInfo = new FacebookOAuth2UserInfo(attributes);

    // Act
    String actual = userInfo.getImageUrl();

    // Assert
    assertThat(actual).isNull();
  }

  @Test
  void getImageUrl_dataExistsButUrlMissing_returnsNull() {
    // Test Case ID: TC_FACEBOOK_USER_INFO_004
    // Scenario ID: SC_FACEBOOK_USER_INFO_EDGE_002
    // Objective: Verify getImageUrl returns null when data exists but url field is absent.
    // Covered Branch/Path: picture exists -> data exists -> url missing -> return null.
    // DB Check: Not applicable.
    // Rollback: Not applicable.

    // Arrange
    Map<String, Object> attributes = Map.of("picture", Map.of("data", Map.of("height", 100)));
    FacebookOAuth2UserInfo userInfo = new FacebookOAuth2UserInfo(attributes);

    // Act
    String actual = userInfo.getImageUrl();

    // Assert
    assertThat(actual).isNull();
  }
}
