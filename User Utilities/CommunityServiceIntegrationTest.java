package com.resourceservice.service.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.jober.utilsservice.dto.WalletDTO;
import com.jober.utilsservice.dto.WalletResDTO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

/**
 * Integration tests for CommunityService.
 * Validates cross-service communication with Search and Payment services using WireMock.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CommunityServiceIntegrationTest {

    @Autowired
    private CommunityService communityService;

    private WireMockServer wireMockServer;

    @Before
    public void setup() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8089));
        wireMockServer.start();
        configureFor("localhost", 8089);
    }

    @After
    public void tearDown() {
        wireMockServer.stop();
    }

    /**
     * TC_016: getOrganization - Happy path
     * Objective: Verify retrieval of organization details from the Search Service.
     * Input: Valid organization ID.
     * Expected: Map containing organization ID and name from the mocked service response.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    public void getOrganization_WhenSearchServiceResponds_ShouldReturnOrgData() {
        stubFor(get(urlMatching("/bs-search/organization/_find_by_id.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"name\": \"Test Organization\"}")));

        Map<String, Object> result = communityService.getOrganization(1L);

        assertNotNull(result);
        assertEquals(1, result.get("id"));
        assertEquals("Test Organization", result.get("name"));
    }

    /**
     * TC_017: getOrganization - Exception (JSON failure)
     * Objective: Verify error handling when the Search Service returns malformed JSON.
     * Input: API returns "invalid json" string.
     * Expected: Exception caught and handled by the service.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    public void getOrganization_WhenJsonIsMalformed_ShouldHandleException() {
        stubFor(get(urlMatching("/bs-search/organization/_find_by_id.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("invalid json")));

        try {
            communityService.getOrganization(1L);
        } catch (Exception e) {
            // Success: exception was reported
        }
    }

    /**
     * TC_018: saveWallet - Happy path
     * Objective: Verify wallet creation via the Payment Service.
     * Input: WalletDTO with user ID and initial points.
     * Expected: Status "SUCCESS" from the mocked payment service.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    public void saveWallet_WhenPaymentServiceResponds_ShouldReturnSuccess() {
        stubFor(post(urlEqualTo("/bs-payment/wallet/_internal_save"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"SUCCESS\", \"message\": \"Wallet saved\"}")));

        WalletDTO walletDTO = WalletDTO.builder()
                .userId(1L)
                .addingPoint(java.math.BigDecimal.valueOf(100.0))
                .build();

        Map<String, Object> result = communityService.saveWallet(walletDTO);

        assertNotNull(result);
        assertEquals("SUCCESS", result.get("status"));
    }

    /**
     * TC_019: getWalletByUser - Happy path
     * Objective: Verify retrieval of user wallet balance from the Payment Service.
     * Input: User ID.
     * Expected: WalletResDTO containing the correct user ID and point balance.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    public void getWalletByUser_WhenPaymentServiceResponds_ShouldReturnWalletData() {
        stubFor(get(urlMatching("/bs-payment/wallet/_get_by_user_id.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"SUCCESS\", \"data\": {\"userId\": 1, \"totalPoint\": 500.0}}")));

        WalletResDTO result = communityService.getWalletByUser(1L);

        assertNotNull(result);
        assertEquals(Long.valueOf(1), result.getUserId());
        assertEquals(java.math.BigDecimal.valueOf(500.0), result.getTotalPoint());
    }

    /**
     * TC_020: getWalletByUser - Exception (Swallowed Error)
     * Objective: Confirm a bug where the service returns null instead of reporting JSON errors.
     * Input: API returns invalid data structure for the "data" field.
     * Expected: WalletResDTO is non-null if the system reports error, or null if bug exists.
     * CheckDB: N
     * Rollback: N
     */
    @Test
    public void getWalletByUser_WhenJsonStructureIsInvalid_ShouldReportError() {
        stubFor(get(urlMatching("/bs-payment/wallet/_get_by_user_id.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"SUCCESS\", \"data\": \"not an object\"}")));

        WalletResDTO result = communityService.getWalletByUser(1L);
        // If result is null, the bug where 'finally' swallows the exception is confirmed
        assertNotNull("System should have reported JSON failure instead of returning null", result);
    }
}
