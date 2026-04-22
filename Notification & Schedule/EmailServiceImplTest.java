package fjob.notification.schedule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fjob.common.dto.EmailRequest;
import fjob.common.exception.EmailSendException;
import fjob.notification.service.EmailServiceImpl;

import javax.mail.internet.MimeMessage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test Cases for EmailServiceImpl
 * Type: Mock (@ExtendWith(MockitoExtension.class))
 * Test Case IDs: TC_EMAIL_001 → TC_EMAIL_026
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailServiceImpl Unit Tests with Mocking")
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SendGridClient sendGridClient;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    // ==================== send() Tests ====================

    @Test
    @DisplayName("TC_EMAIL_001: Send plain text email - Happy path")
    void testSend_PlainTextEmail() {
        // Arrange
        EmailRequest request = EmailRequest.builder()
                .to("test@example.com")
                .subject("Test Subject")
                .body("Hello World")
                .isHtml(false)
                .build();

        when(sendGridClient.send(any())).thenReturn(202); // Success status

        // Act
        boolean result = emailService.send(request);

        // Assert
        assertTrue(result);
        verify(sendGridClient, times(1)).send(any());
    }

    @Test
    @DisplayName("TC_EMAIL_002: Send HTML email")
    void testSend_HtmlEmail() {
        // Arrange
        EmailRequest request = EmailRequest.builder()
                .to("test@example.com")
                .subject("Test Subject")
                .body("<h1>Title</h1>")
                .isHtml(true)
                .build();

        when(sendGridClient.send(any())).thenReturn(202);

        // Act
        boolean result = emailService.send(request);

        // Assert
        assertTrue(result);
        verify(sendGridClient, times(1)).send(argThat(msg -> 
            msg.getContentType().contains("text/html")));
    }

    @Test
    @DisplayName("TC_EMAIL_003: Send email with multiple recipients")
    void testSend_MultipleRecipients() {
        // Arrange
        EmailRequest request = EmailRequest.builder()
                .to("test1@example.com")
                .cc("test2@example.com")
                .bcc("test3@example.com")
                .subject("Test")
                .body("Content")
                .build();

        when(sendGridClient.send(any())).thenReturn(202);

        // Act
        boolean result = emailService.send(request);

        // Assert
        assertTrue(result);
        verify(sendGridClient, times(1)).send(any());
    }

    @Test
    @DisplayName("TC_EMAIL_004: SendGrid API error handling")
    void testSend_SendGridApiError() {
        // Arrange
        EmailRequest request = EmailRequest.builder()
                .to("test@example.com")
                .subject("Test")
                .body("Content")
                .build();

        when(sendGridClient.send(any())).thenThrow(new SendGridException("API Error"));

        // Act & Assert
        assertThrows(EmailSendException.class, () -> emailService.send(request));
    }

    @Test
    @DisplayName("TC_EMAIL_005: UTF-8 encoding for Vietnamese characters")
    void testSend_UtfEncoding() {
        // Arrange
        EmailRequest request = EmailRequest.builder()
                .to("test@example.com")
                .subject("Xin chào")
                .body("Xin chào từ Việt Nam")
                .build();

        when(sendGridClient.send(any())).thenReturn(202);

        // Act
        boolean result = emailService.send(request);

        // Assert
        assertTrue(result);
        verify(sendGridClient, times(1)).send(argThat(msg -> 
            msg.getContentType().contains("charset=UTF-8")));
    }

    @Test
    @DisplayName("TC_EMAIL_006: Send email with empty subject")
    void testSend_EmptySubject() {
        // Arrange
        EmailRequest request = EmailRequest.builder()
                .to("test@example.com")
                .subject("")
                .body("Content")
                .build();

        when(sendGridClient.send(any())).thenReturn(202);

        // Act
        boolean result = emailService.send(request);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("TC_EMAIL_007: Subject truncation on very long length")
    void testSend_VeryLongSubject() {
        // Arrange
        String longSubject = "A".repeat(500);
        EmailRequest request = EmailRequest.builder()
                .to("test@example.com")
                .subject(longSubject)
                .body("Content")
                .build();

        when(sendGridClient.send(any())).thenReturn(202);

        // Act
        boolean result = emailService.send(request);

        // Assert
        assertTrue(result);
        verify(sendGridClient, times(1)).send(argThat(msg -> 
            msg.getSubject().length() <= 255));
    }

    @Test
    @DisplayName("TC_EMAIL_008: Null recipient validation")
    void testSend_NullRecipient() {
        // Arrange
        EmailRequest request = EmailRequest.builder()
                .to(null)
                .subject("Test")
                .body("Content")
                .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> emailService.send(request));
    }

    @Test
    @DisplayName("TC_EMAIL_009: Invalid email format validation")
    void testSend_InvalidEmailFormat() {
        // Arrange
        EmailRequest request = EmailRequest.builder()
                .to("notanemail")
                .subject("Test")
                .body("Content")
                .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> emailService.send(request));
    }

    @Test
    @DisplayName("TC_EMAIL_010: Send email with attachment")
    void testSend_WithAttachment() {
        // Arrange
        EmailRequest request = EmailRequest.builder()
                .to("test@example.com")
                .subject("With Attachment")
                .body("See attachment")
                .attachmentPath("file.pdf")
                .build();

        when(sendGridClient.send(any())).thenReturn(202);

        // Act
        boolean result = emailService.send(request);

        // Assert
        assertTrue(result);
        verify(sendGridClient, times(1)).send(any());
    }

    @Test
    @DisplayName("TC_EMAIL_011: Retry logic on failure")
    void testSend_RetryLogic() {
        // Arrange
        EmailRequest request = EmailRequest.builder()
                .to("test@example.com")
                .subject("Test")
                .body("Content")
                .build();

        when(sendGridClient.send(any()))
                .thenThrow(new SendGridException("Temporary failure"))
                .thenThrow(new SendGridException("Temporary failure"))
                .thenReturn(202);

        // Act
        boolean result = emailService.send(request);

        // Assert
        assertTrue(result);
        verify(sendGridClient, times(3)).send(any());
    }

    @Test
    @DisplayName("TC_EMAIL_012: Timeout handling")
    void testSend_Timeout() {
        // Arrange
        EmailRequest request = EmailRequest.builder()
                .to("test@example.com")
                .subject("Test")
                .body("Content")
                .build();

        when(sendGridClient.send(any())).thenThrow(new TimeoutException("Request timeout"));

        // Act & Assert
        assertThrows(EmailSendException.class, () -> emailService.send(request));
    }

    @Test
    @DisplayName("TC_EMAIL_013: Template email with variable substitution")
    void testSend_TemplateEmail() {
        // Arrange
        EmailRequest request = EmailRequest.builder()
                .to("test@example.com")
                .subject("Hello {name}")
                .body("Dear {name},\nYour job interview is scheduled.")
                .templateName("interview_scheduled")
                .templateVariables(Map.of("name", "John"))
                .build();

        when(templateEngine.process(anyString(), any())).thenReturn("Dear John,\nYour job interview is scheduled.");
        when(sendGridClient.send(any())).thenReturn(202);

        // Act
        boolean result = emailService.send(request);

        // Assert
        assertTrue(result);
        verify(templateEngine, times(1)).process(anyString(), any());
    }

    // ==================== checkAndSendEmailForCandidate Tests ====================

    @Test
    @DisplayName("TC_EMAIL_014: Send to scheduled candidates")
    void testCheckAndSendEmailForCandidate_ScheduledCandidates() {
        // Arrange
        List<CandidateDTO> candidates = List.of(
                CandidateDTO.builder().id(1L).email("cand1@example.com").scheduleEmail(true).build(),
                CandidateDTO.builder().id(2L).email("cand2@example.com").scheduleEmail(true).build()
        );

        when(candidateService.getCandidatesWithScheduleEmail()).thenReturn(candidates);
        when(sendGridClient.send(any())).thenReturn(202);

        // Act
        emailService.checkAndSendEmailForCandidate();

        // Assert
        verify(sendGridClient, times(2)).send(any());
    }

    @Test
    @DisplayName("TC_EMAIL_015: No scheduled candidates")
    void testCheckAndSendEmailForCandidate_NoScheduledCandidates() {
        // Arrange
        when(candidateService.getCandidatesWithScheduleEmail()).thenReturn(List.of());

        // Act
        emailService.checkAndSendEmailForCandidate();

        // Assert
        verify(sendGridClient, times(0)).send(any());
    }

    @Test
    @DisplayName("TC_EMAIL_016: Batch send with partial errors")
    void testCheckAndSendEmailForCandidate_BatchError() {
        // Arrange
        List<CandidateDTO> candidates = List.of(
                CandidateDTO.builder().id(1L).email("cand1@example.com").build(),
                CandidateDTO.builder().id(2L).email("cand2@example.com").build()
        );

        when(candidateService.getCandidatesWithScheduleEmail()).thenReturn(candidates);
        when(sendGridClient.send(any()))
                .thenReturn(202)
                .thenThrow(new SendGridException("Failed"))
                .thenReturn(202);

        // Act
        emailService.checkAndSendEmailForCandidate();

        // Assert
        verify(sendGridClient, times(3)).send(any());
    }

    @Test
    @DisplayName("TC_EMAIL_017: Template rendering")
    void testCheckAndSendEmailForCandidate_TemplateRendering() {
        // Arrange
        List<CandidateDTO> candidates = List.of(
                CandidateDTO.builder().id(1L).email("cand@example.com")
                        .upcomingJobTitle("Java Developer").build()
        );

        when(candidateService.getCandidatesWithScheduleEmail()).thenReturn(candidates);
        when(templateEngine.process(anyString(), any()))
                .thenReturn("You have an interview for Java Developer");
        when(sendGridClient.send(any())).thenReturn(202);

        // Act
        emailService.checkAndSendEmailForCandidate();

        // Assert
        verify(templateEngine, times(1)).process(anyString(), any());
    }

    // ==================== checkAndSendEmailForRecruiter Tests ====================

    @Test
    @DisplayName("TC_EMAIL_018: Send to recruiters with active jobs")
    void testCheckAndSendEmailForRecruiter_ActiveJobs() {
        // Arrange
        List<RecruiterDTO> recruiters = List.of(
                RecruiterDTO.builder().id(1L).email("recruiter1@example.com").activeJobCount(3).build(),
                RecruiterDTO.builder().id(2L).email("recruiter2@example.com").activeJobCount(2).build()
        );

        when(recruiterService.getRecruitersWithActiveJobs()).thenReturn(recruiters);
        when(sendGridClient.send(any())).thenReturn(202);

        // Act
        emailService.checkAndSendEmailForRecruiter();

        // Assert
        verify(sendGridClient, times(2)).send(any());
    }

    @Test
    @DisplayName("TC_EMAIL_019: Aggregate candidate count in email")
    void testCheckAndSendEmailForRecruiter_CandidateAggregation() {
        // Arrange
        RecruiterDTO recruiter = RecruiterDTO.builder()
                .id(1L)
                .email("recruiter@example.com")
                .pendingApplications(5)
                .build();

        when(recruiterService.getRecruitersWithActiveJobs()).thenReturn(List.of(recruiter));
        when(sendGridClient.send(any())).thenReturn(202);

        // Act
        emailService.checkAndSendEmailForRecruiter();

        // Assert
        verify(sendGridClient, times(1)).send(argThat(msg -> 
            msg.getBody().contains("5 new candidates")));
    }

    @Test
    @DisplayName("TC_EMAIL_020: No active jobs")
    void testCheckAndSendEmailForRecruiter_NoActiveJobs() {
        // Arrange
        when(recruiterService.getRecruitersWithActiveJobs()).thenReturn(List.of());

        // Act
        emailService.checkAndSendEmailForRecruiter();

        // Assert
        verify(sendGridClient, times(0)).send(any());
    }

    // ==================== sendExpiringJobsEmail Tests ====================

    @Test
    @DisplayName("TC_EMAIL_021: Send expiring jobs notification")
    void testSendExpiringJobsEmailForCandidate_TemplateApplication() {
        // Arrange
        List<JobDTO> expiringJobs = List.of(
                JobDTO.builder().id(1L).title("Java Developer").expiringDate(LocalDate.now().plusDays(7)).build()
        );

        when(jobService.getExpiringJobs(7)).thenReturn(expiringJobs);
        when(templateEngine.process(anyString(), any())).thenReturn("Job Alert: Java Developer expires soon");
        when(sendGridClient.send(any())).thenReturn(202);

        // Act
        emailService.sendExpiringJobsEmailForCandidate();

        // Assert
        verify(templateEngine, times(1)).process(anyString(), any());
    }

    @Test
    @DisplayName("TC_EMAIL_022: Subject line formatting")
    void testSendExpiringJobsEmailForCandidate_SubjectFormat() {
        // Arrange
        List<JobDTO> jobs = List.of(
                JobDTO.builder().id(1L).title("Job 1").build(),
                JobDTO.builder().id(2L).title("Job 2").build(),
                JobDTO.builder().id(3L).title("Job 3").build()
        );

        when(jobService.getExpiringJobs(7)).thenReturn(jobs);
        when(sendGridClient.send(any())).thenReturn(202);

        // Act
        emailService.sendExpiringJobsEmailForCandidate();

        // Assert
        verify(sendGridClient, times(1)).send(argThat(msg -> 
            msg.getSubject().contains("3 jobs expiring soon")));
    }

    // ==================== sendingExpiringJobsEmailForRecruiter Tests ====================

    @Test
    @DisplayName("TC_EMAIL_023: Send to recruiters with expiring jobs")
    void testSendingExpiringJobsEmailForRecruiter_HappyPath() {
        // Arrange
        List<JobDTO> expiringJobs = List.of(
                JobDTO.builder().id(1L).recruiterId(1L).title("Job 1").build(),
                JobDTO.builder().id(2L).recruiterId(1L).title("Job 2").build()
        );

        when(jobService.getExpiringJobs(7)).thenReturn(expiringJobs);
        when(recruiterService.getRecruiterEmail(1L)).thenReturn("recruiter@example.com");
        when(sendGridClient.send(any())).thenReturn(202);

        // Act
        emailService.sendingExpiringJobsEmailForRecruiter();

        // Assert
        verify(sendGridClient, times(1)).send(any());
    }

    @Test
    @DisplayName("TC_EMAIL_024: Aggregate jobs by recruiter")
    void testSendingExpiringJobsEmailForRecruiter_JobAggregation() {
        // Arrange
        List<JobDTO> expiringJobs = List.of(
                JobDTO.builder().id(1L).recruiterId(1L).title("Job 1").build(),
                JobDTO.builder().id(2L).recruiterId(1L).title("Job 2").build(),
                JobDTO.builder().id(3L).recruiterId(2L).title("Job 3").build()
        );

        when(jobService.getExpiringJobs(7)).thenReturn(expiringJobs);
        when(recruiterService.getRecruiterEmail(anyLong())).thenReturn("recruiter@example.com");
        when(sendGridClient.send(any())).thenReturn(202);

        // Act
        emailService.sendingExpiringJobsEmailForRecruiter();

        // Assert
        verify(sendGridClient, times(2)).send(any()); // 2 recruiters
    }

    @Test
    @DisplayName("TC_EMAIL_025: Subject formatting with expiring count")
    void testSendingExpiringJobsEmailForRecruiter_SubjectFormat() {
        // Arrange
        List<JobDTO> expiringJobs = List.of(
                JobDTO.builder().id(1L).recruiterId(1L).title("Job 1").build(),
                JobDTO.builder().id(2L).recruiterId(1L).title("Job 2").build(),
                JobDTO.builder().id(3L).recruiterId(1L).title("Job 3").build(),
                JobDTO.builder().id(4L).recruiterId(1L).title("Job 4").build(),
                JobDTO.builder().id(5L).recruiterId(1L).title("Job 5").build()
        );

        when(jobService.getExpiringJobs(7)).thenReturn(expiringJobs);
        when(recruiterService.getRecruiterEmail(1L)).thenReturn("recruiter@example.com");
        when(sendGridClient.send(any())).thenReturn(202);

        // Act
        emailService.sendingExpiringJobsEmailForRecruiter();

        // Assert
        verify(sendGridClient, times(1)).send(argThat(msg -> 
            msg.getSubject().contains("5 of your jobs are expiring soon")));
    }

    @Test
    @DisplayName("TC_EMAIL_026: No expiring jobs")
    void testSendingExpiringJobsEmailForRecruiter_NoExpiringJobs() {
        // Arrange
        when(jobService.getExpiringJobs(7)).thenReturn(List.of());

        // Act
        emailService.sendingExpiringJobsEmailForRecruiter();

        // Assert
        verify(sendGridClient, times(0)).send(any());
    }
}
