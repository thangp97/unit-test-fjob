package com.resourceservice.service.impl;

import com.resourceservice.common.PageResponse;
import com.resourceservice.dto.FeaturedBrandDto;
import com.resourceservice.dto.StandoutOrganizationDto;
import com.resourceservice.dto.StandoutUserDto;
import com.resourceservice.model.UserCommon;
import com.resourceservice.model.projection.FeaturedBrandProjection;
import com.resourceservice.model.projection.StandoutOrganizationProjection;
import com.resourceservice.model.projection.StandoutUserProjection;
import com.resourceservice.repository.OrganizationRepo;
import com.resourceservice.repository.UserCommonRepo;
import com.resourceservice.utilsmodule.constant.Constant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for StandoutServiceImpl.
 * Validates logic for standout candidates, standout organizations, and featured brands.
 */
@RunWith(MockitoJUnitRunner.class)
public class StandoutServiceImplTest {

    @Mock
    private UserCommonRepo userCommonRepo;

    @Mock
    private OrganizationRepo organizationRepo;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private StandoutServiceImpl standoutService;

    /**
     * TC_007: getStandoutUsers - Recruiter role
     * Objective: Verify that a recruiter can retrieve standout candidates.
     * Input: Recruiter token, role set to RECRUITER_NUM.
     * Expected: PageResponse containing candidate data (StandoutUserDto).
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void getStandoutUsers_WhenUserIsRecruiter_ShouldReturnCandidateList() {
        String token = buildToken("0912345678");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        UserCommon recruiter = new UserCommon();
        recruiter.setId(1L);
        recruiter.setPhone("0912345678");
        recruiter.setRole(Integer.valueOf(Constant.RECRUITER_NUM));
        when(userCommonRepo.findByPhoneEquals("0912345678")).thenReturn(recruiter);

        StandoutUserProjection projection = createUserProjection(2L, "John Doe", "0987654321",
            "john@example.com", 4, "Hanoi", "Ba Dinh", new BigDecimal("100"));
        Page<StandoutUserProjection> userPage = new PageImpl<>(
            Collections.singletonList(projection), PageRequest.of(0, 10), 1);
        when(userCommonRepo.findStandoutUsersByPointAndRole(eq(Integer.valueOf(Constant.CANDIDATE_NUM)), any()))
            .thenReturn(userPage);

        PageResponse<?> response = standoutService.getStandoutUsers(request, null, 10, 0);

        assertNotNull(response);
        assertEquals(0, response.getPageNumber());
        assertEquals(10, response.getPageSize());
        assertEquals(Long.valueOf(1L), response.getTotalElements());

        List<?> data = response.getData();
        assertEquals(1, data.size());
        StandoutUserDto dto = (StandoutUserDto) data.get(0);
        assertEquals("John Doe", dto.getName());
    }

    /**
     * TC_008: getStandoutUsers - Candidate role
     * Objective: Verify that a candidate can retrieve standout organizations.
     * Input: Candidate token, role set to CANDIDATE_NUM.
     * Expected: PageResponse containing organization data (StandoutOrganizationDto).
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void getStandoutUsers_WhenUserIsCandidate_ShouldReturnOrganizationList() {
        String token = buildToken("0987654321");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        UserCommon candidate = new UserCommon();
        candidate.setId(2L);
        candidate.setPhone("0987654321");
        candidate.setRole(Integer.valueOf(Constant.CANDIDATE_NUM));
        when(userCommonRepo.findByPhoneEquals("0987654321")).thenReturn(candidate);

        StandoutOrganizationProjection orgProjection = createOrganizationProjection(3L, "Org A",
            "avatar.png", 7L, "IT");
        Page<StandoutOrganizationProjection> orgPage = new PageImpl<>(
            Collections.singletonList(orgProjection), PageRequest.of(0, 10), 1);
        when(organizationRepo.findStandoutOrganizations(eq("IT"), any())).thenReturn(orgPage);

        PageResponse<?> response = standoutService.getStandoutUsers(request, "IT", 10, 0);

        assertNotNull(response);
        assertEquals(1, response.getData().size());
        StandoutOrganizationDto dto = (StandoutOrganizationDto) response.getData().get(0);
        assertEquals("Org A", dto.getName());
    }

    /**
     * TC_009: getStandoutUsers - Missing Authorization
     * Objective: Verify that missing Authorization header throws an exception.
     * Input: HTTP request without "Authorization" header.
     * Expected: RuntimeException.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test(expected = RuntimeException.class)
    public void getStandoutUsers_WhenHeaderIsMissing_ShouldThrowException() {
        when(request.getHeader("Authorization")).thenReturn(null);
        standoutService.getStandoutUsers(request, null, 10, 0);
    }

    /**
     * TC_010: getStandoutUsers - Industry Filter
     * Objective: Verify that candidates can filter standout organizations by industry.
     * Input: Candidate token and industry "IT".
     * Expected: Filtered PageResponse containing organizations in the specified industry.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void getStandoutUsers_WhenIndustryFilterApplied_ShouldReturnFilteredOrganizations() {
        String token = buildToken("0987654321");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        UserCommon candidate = new UserCommon();
        candidate.setPhone("0987654321");
        candidate.setRole(Integer.valueOf(Constant.CANDIDATE_NUM));
        when(userCommonRepo.findByPhoneEquals("0987654321")).thenReturn(candidate);

        StandoutOrganizationProjection orgProjection = createOrganizationProjection(3L, "Org IT",
            "avatar.png", 5L, "IT");
        Page<StandoutOrganizationProjection> orgPage = new PageImpl<>(
            Collections.singletonList(orgProjection), PageRequest.of(0, 10), 1);
        when(organizationRepo.findStandoutOrganizations(eq("IT"), any())).thenReturn(orgPage);

        PageResponse<?> response = standoutService.getStandoutUsers(request, "IT", 10, 0);

        assertNotNull(response);
        assertEquals(1, response.getData().size());
    }

    /**
     * TC_011: getStandoutUsers - Invalid Token
     * Objective: Verify that malformed tokens cause an error.
     * Input: Malformed Authorization header.
     * Expected: Exception during token decoding.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void getStandoutUsers_WhenTokenIsMalformed_ShouldThrowException() {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token.parts");
        try {
            standoutService.getStandoutUsers(request, null, 10, 0);
            fail("Expected exception for invalid token");
        } catch (Exception e) {
            // Expected
        }
    }

    /**
     * TC_012: getStandoutUsers - Empty Results
     * Objective: Verify behavior when no standout users/orgs are found.
     * Input: Valid request, but repository returns empty page.
     * Expected: PageResponse with empty data list.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void getStandoutUsers_WhenNoResultsFound_ShouldReturnEmptyPageResponse() {
        String token = buildToken("0912345678");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        UserCommon recruiter = new UserCommon();
        recruiter.setPhone("0912345678");
        recruiter.setRole(Integer.valueOf(Constant.RECRUITER_NUM));
        when(userCommonRepo.findByPhoneEquals("0912345678")).thenReturn(recruiter);

        when(userCommonRepo.findStandoutUsersByPointAndRole(any(), any()))
            .thenReturn(new PageImpl<>(Collections.emptyList()));

        PageResponse<?> response = standoutService.getStandoutUsers(request, null, 10, 0);

        assertNotNull(response);
        assertTrue(response.getData().isEmpty());
    }

    /**
     * TC_013: getFeaturedBrands - Default mapping
     * Objective: Verify that featured brands are correctly retrieved and mapped with defaults.
     * Input: No category filter.
     * Expected: PageResponse with FeaturedBrandDto, defaults applied (e.g., industry "Khác").
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void getFeaturedBrands_WhenNoFilter_ShouldReturnAllFeaturedBrands() {
        FeaturedBrandProjection projection = createFeaturedBrandProjection(1L, "Brand A", null,
            null, 11L, "Desc");
        Page<FeaturedBrandProjection> orgPage = new PageImpl<>(
            Collections.singletonList(projection), PageRequest.of(0, 5), 1);
        when(organizationRepo.findFeaturedBrands(eq("TECH"), any())).thenReturn(orgPage);

        PageResponse<?> response = standoutService.getFeaturedBrands("TECH", 5, 0);

        assertNotNull(response);
        FeaturedBrandDto dto = (FeaturedBrandDto) response.getData().get(0);
        assertEquals("Brand A", dto.getName());
        assertEquals("Khác", dto.getIndustry());
    }

    /**
     * TC_014: getFeaturedBrands - Pro Company logic
     * Objective: Verify that companies with > 10 posts are marked as Pro.
     * Input: List of organizations with various post counts.
     * Expected: isProCompany is true for postCount > 10, false otherwise.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void getFeaturedBrands_WhenPostCountThresholdExceeded_ShouldMarkAsProCompany() {
        FeaturedBrandProjection pro = createFeaturedBrandProjection(1L, "Pro", null, null, 11L, "");
        FeaturedBrandProjection notPro = createFeaturedBrandProjection(2L, "Not Pro", null, null, 10L, "");
        
        Page<FeaturedBrandProjection> orgPage = new PageImpl<>(
            java.util.Arrays.asList(pro, notPro), PageRequest.of(0, 10), 2);
        when(organizationRepo.findFeaturedBrands(any(), any())).thenReturn(orgPage);

        PageResponse<?> response = standoutService.getFeaturedBrands(null, 10, 0);

        FeaturedBrandDto dtoPro = (FeaturedBrandDto) response.getData().get(0);
        FeaturedBrandDto dtoNotPro = (FeaturedBrandDto) response.getData().get(1);
        
        assertTrue(dtoPro.getIsProCompany());
        assertFalse(dtoNotPro.getIsProCompany());
    }

    /**
     * TC_015: getCategories
     * Objective: Verify retrieval of unique organization categories.
     * Input: Data exists in DB for categories.
     * Expected: List of category strings.
     * CheckDB: N (Mocked)
     * Rollback: N
     */
    @Test
    public void getCategories_WhenDataExists_ShouldReturnDistinctCategories() {
        when(organizationRepo.findDistinctCategories()).thenReturn(
            Collections.singletonList("Technology"));

        List<String> categories = standoutService.getCategories();

        assertEquals(1, categories.size());
        assertEquals("Technology", categories.get(0));
    }

    private static String buildToken(String userName) {
        String header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(("{\"user_name\":\"" + userName + "\"}")
                .getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".sig";
    }

    private static StandoutUserProjection createUserProjection(Long id, String name, String phone,
                                                                String email, Integer rating,
                                                                String address, String ward,
                                                                BigDecimal point) {
        return new StandoutUserProjection() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getPhone() {
                return phone;
            }

            @Override
            public String getEmail() {
                return email;
            }

            @Override
            public Integer getRating() {
                return rating;
            }

            @Override
            public String getAddress() {
                return address;
            }

            @Override
            public String getProvince() {
                return "Hanoi";
            }

            @Override
            public String getWard() {
                return ward;
            }

            @Override
            public BigDecimal getPoint() {
                return point;
            }
        };
    }

    private static StandoutOrganizationProjection createOrganizationProjection(Long id, String name,
                                                                               String avatar,
                                                                               Long postCount,
                                                                               String industry) {
        return new StandoutOrganizationProjection() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getAvatar() {
                return avatar;
            }

            @Override
            public Long getPostCount() {
                return postCount;
            }

            @Override
            public String getIndustry() {
                return industry;
            }
        };
    }

    private static FeaturedBrandProjection createFeaturedBrandProjection(Long id, String name,
                                                                         String avatar,
                                                                         String industry,
                                                                         Long postCount,
                                                                         String description) {
        return new FeaturedBrandProjection() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getAvatar() {
                return avatar;
            }

            @Override
            public String getIndustry() {
                return industry;
            }

            @Override
            public Long getPostCount() {
                return postCount;
            }

            @Override
            public String getDescription() {
                return description;
            }
        };
    }
}
