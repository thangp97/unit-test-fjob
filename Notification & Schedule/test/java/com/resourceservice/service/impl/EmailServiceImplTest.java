package com.resourceservice.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.sendgrid.SendGrid;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EmailServiceImplTest {

  @Test
  void send_withRealSendGridConfiguration_doesNotThrow() {
    String apiKey = System.getenv("SENDGRID_API_KEY");
    String from = firstNonBlank(System.getenv("SENDGRID_FROM_EMAIL"), System.getenv("FROM_EMAIL"));
    String to = System.getenv("SENDGRID_IT_TO");

    assumeTrue(isNotBlank(apiKey),
        "Skipped real SendGrid test because SENDGRID_API_KEY is not configured.");
    assumeTrue(isNotBlank(from),
        "Skipped real SendGrid test because SENDGRID_FROM_EMAIL or FROM_EMAIL is not configured.");
    assumeTrue(isNotBlank(to),
        "Skipped real SendGrid test because SENDGRID_IT_TO is not configured.");

    EmailServiceImpl emailService = new EmailServiceImpl(null, null, new SendGrid(apiKey));
    ReflectionTestUtils.setField(emailService, "from", from);

    assertDoesNotThrow(() -> emailService.send(
        to,
        "Schedule integration test",
        "<p>This message was sent by the EmailServiceImpl integration smoke test.</p>"));
  }

  private static String firstNonBlank(String first, String second) {
    return isNotBlank(first) ? first : second;
  }

  private static boolean isNotBlank(String value) {
    return value != null && !value.trim().isEmpty();
  }
}
