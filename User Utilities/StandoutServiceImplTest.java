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

    @Test
    public void getStandoutUsers_recruiterGetsCandidates() {
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

    @Test
    public void getStandoutUsers_candidateGetsOrganizations() {
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

    @Test(expected = RuntimeException.class)
    public void getStandoutUsers_missingHeader_throws() {
        when(request.getHeader("Authorization")).thenReturn(null);
        standoutService.getStandoutUsers(request, null, 10, 0);
    }

    @Test
    public void getFeaturedBrands_mapsDefaults() {
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
        assertTrue(dto.getIsProCompany());
    }

    @Test
    public void getCategories_returnsList() {
        when(organizationRepo.findDistinctCategories()).thenReturn(
            Collections.singletonList("Technology"));

        List<String> categories = standoutService.getCategories();

        assertEquals(1, categories.size());
        assertEquals("Technology", categories.get(0));
    }

    @Test
    public void getFeaturedBrands_proCompanyLogic() {
        // Logic - isProCompany based on postCount > 10
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

    @Test
    public void getStandoutUsers_InvalidToken_Throws() {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token");
        try {
            standoutService.getStandoutUsers(request, null, 10, 0);
            fail("Expected exception for invalid token");
        } catch (Exception e) {
            // Success
        }
    }

    @Test
    public void getStandoutUsers_EmptyPage() {
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

    @Test
    public void getCategories_Empty() {
        when(organizationRepo.findDistinctCategories()).thenReturn(Collections.emptyList());
        List<String> result = standoutService.getCategories();
        assertTrue(result.isEmpty());
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


