package com.jober.searchservice.service.impl;

import static com.jober.utilsservice.constant.ResponseMessageConstant.FOUND;
import static com.jober.utilsservice.constant.ResponseMessageConstant.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jober.searchservice.model.SearchingSuggestion;
import com.jober.searchservice.repository.SearchingSuggestionRepo;
import com.jober.searchservice.utilsmodule.CacheService;
import com.jober.utilsservice.errors.ResponseEntitySerializable;
import com.jober.utilsservice.errors.RestExceptionHandler;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SearchingSuggestionImplTest {

  private SearchingSuggestionImpl searchingSuggestionService;

  @Mock
  private SearchingSuggestionRepo searchingSuggestionRepo;
  @Mock
  private CacheManager cacheManager;
  @Mock
  private CacheService cacheService;
  @Mock
  private RestExceptionHandler restExceptionHandler;

  @BeforeEach
  void setUp() {
    searchingSuggestionService = new SearchingSuggestionImpl();
    ReflectionTestUtils.setField(searchingSuggestionService, "searchingSuggestionRepo",
        searchingSuggestionRepo);
    ReflectionTestUtils.setField(searchingSuggestionService, "cacheManager", cacheManager);
    ReflectionTestUtils.setField(searchingSuggestionService, "cacheService", cacheService);
    ReflectionTestUtils.setField(searchingSuggestionService, "responseObject", new ResponseObject());
    ReflectionTestUtils.setField(SearchingSuggestionImpl.class, "restExceptionHandler",
        restExceptionHandler);
    ReflectionTestUtils.setField(SearchingSuggestionImpl.class, "responseEntity", null);
  }

  @Test
  void getDataSearch_getAllTrue_returnsAllSuggestions() {
    // Test Case ID: TC_SEARCH_SUGGESTION_001
    // Scenario ID: HAPPY_001
    // Objective: Verify default search suggestion data is returned when user opens search screen.
    // Covered Branch/Path: parsed input -> isGetAll true -> findSearchingSuggestion list branch.
    // DB Check: Verify repository read-all query is called once and no write occurs.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputBody = "{\"paging\":{\"page\":1,\"size\":10},\"isGetAll\":true}";
    List<SearchingSuggestion> expectedSuggestions = List.of(buildSuggestion("java", "job", 1));
    when(searchingSuggestionRepo.findSearchingSuggestion()).thenReturn(expectedSuggestions);

    // Act
    ResponseEntitySerializable actualResponse = searchingSuggestionService.getDataSearch(inputBody);

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getData()).isEqualTo(expectedSuggestions);
    assertThat(actualBody.getTotalCount()).isEqualTo(1L);
    assertThat(actualBody.getCurrentCount()).isEqualTo(1);
    verify(searchingSuggestionRepo, times(1)).findSearchingSuggestion();
    verify(searchingSuggestionRepo, never()).save(any());
  }

  @Test
  void getDataSearch_getAllTrueRepositoryReturnsNull_returnsNotFoundResponse() {
    // Test Case ID: TC_SEARCH_SUGGESTION_018
    // Scenario ID: EDGE_005
    // Objective: Verify null all-suggestion repository result is handled as no data.
    // Assumption: Null repository result should not be reported as success and should produce NOT_FOUND.
    // Covered Branch/Path: parsed input -> isGetAll true -> repository returns null -> NOT_FOUND response.
    // DB Check: Verify repository read-all query is called once and no write occurs.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputBody = "{\"paging\":{\"page\":1,\"size\":10},\"isGetAll\":true}";
    HttpStatus expectedHttpStatus = HttpStatus.NOT_FOUND;
    logEvidence("mvn -pl search-service -DskipTests=false -Dtest=SearchingSuggestionImplTest test",
        "TC_SEARCH_SUGGESTION_018 expects get-all null repository result to produce NOT_FOUND");
    when(searchingSuggestionRepo.findSearchingSuggestion()).thenReturn(null);

    // Act
    ResponseEntitySerializable actualResponse = searchingSuggestionService.getDataSearch(inputBody);

    // Assert
    verify(searchingSuggestionRepo, times(1)).findSearchingSuggestion();
    verify(searchingSuggestionRepo, never()).save(any());
    assertThat(actualResponse).isNotNull();
    assertThat(actualResponse.getStatusCode()).isEqualTo(expectedHttpStatus);
  }

  @Test
  void getDataSearch_getAllFalse_returnsPagedSuggestions() {
    // Test Case ID: TC_SEARCH_SUGGESTION_002
    // Scenario ID: HAPPY_002
    // Objective: Verify paged suggestion data is returned when the client requests a page.
    // Covered Branch/Path: parsed input -> isGetAll false -> repository page present branch.
    // DB Check: Verify repository receives expected pageable and no write occurs.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputBody = "{\"paging\":{\"page\":2,\"size\":5},\"isGetAll\":false}";
    SearchingSuggestion expectedSuggestion = buildSuggestion("tester", "job", 2);
    Page<SearchingSuggestion> expectedPage =
        new PageImpl<>(List.of(expectedSuggestion), PageRequest.of(1, 5), 6);
    when(searchingSuggestionRepo.findSearchingSuggestion(PageRequest.of(1, 5))).thenReturn(expectedPage);

    // Act
    ResponseEntitySerializable actualResponse = searchingSuggestionService.getDataSearch(inputBody);

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getData()).isEqualTo(List.of(expectedSuggestion));
    assertThat(actualBody.getTotalCount()).isEqualTo(6L);
    assertThat(actualBody.getCurrentCount()).isEqualTo(1);
    verify(searchingSuggestionRepo, times(1)).findSearchingSuggestion(PageRequest.of(1, 5));
    verify(searchingSuggestionRepo, never()).save(any());
  }

  @Test
  void getDataSearch_missingPaging_returnsExceptionResponse() {
    // Test Case ID: TC_SEARCH_SUGGESTION_003
    // Scenario ID: SAD_001
    // Objective: Verify malformed search request is converted to a system error response.
    // Covered Branch/Path: parsed input -> paging null -> NullPointerException catch branch.
    // DB Check: Verify no repository access happens when paging is missing.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputBodyWithoutPaging = "{\"isGetAll\":false}";
    ResponseEntitySerializable<Object> expectedErrorResponse =
        new ResponseEntitySerializable<>(new ResponseObject("ERROR", "500", "paging is required"),
            HttpStatus.INTERNAL_SERVER_ERROR);
    when(restExceptionHandler.handleNullPointerException(any(NullPointerException.class)))
        .thenReturn(expectedErrorResponse);

    // Act
    ResponseEntitySerializable actualResponse =
        searchingSuggestionService.getDataSearch(inputBodyWithoutPaging);

    // Assert
    assertThat(actualResponse).isEqualTo(expectedErrorResponse);
    verify(restExceptionHandler, times(1)).handleNullPointerException(any(NullPointerException.class));
    verify(searchingSuggestionRepo, never()).findSearchingSuggestion();
    verify(searchingSuggestionRepo, never()).save(any());
  }

  @Test
  void getDataSearch_zeroPage_rejectsRequestBeforeRepositoryRead() {
    // Test Case ID: TC_SEARCH_SUGGESTION_019
    // Scenario ID: SAD_006
    // Objective: Verify zero page number is rejected before repository access.
    // Assumption: Page numbers below one are invalid and should be rejected without repository access.
    // Covered Branch/Path: paging.page <= 0 -> validation rejection -> no repository read/write.
    // DB Check: Verify searchingSuggestionRepo is not called for invalid paging.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputBody = "{\"paging\":{\"page\":0,\"size\":10},\"isGetAll\":false}";
    HttpStatus expectedHttpStatus = HttpStatus.BAD_REQUEST;
    logEvidence("mvn -pl search-service -DskipTests=false -Dtest=SearchingSuggestionImplTest test",
        "TC_SEARCH_SUGGESTION_019 expects zero page to be rejected before DB read");

    // Act
    final ResponseEntitySerializable[] actualResponse = new ResponseEntitySerializable[1];
    Throwable actualThrowable = org.assertj.core.api.Assertions.catchThrowable(() ->
        actualResponse[0] = searchingSuggestionService.getDataSearch(inputBody));

    // Assert
    verify(searchingSuggestionRepo, never()).findSearchingSuggestion(any(PageRequest.class));
    verify(searchingSuggestionRepo, never()).save(any());
    assertThat(actualThrowable).isNull();
    assertThat(actualResponse[0]).isNotNull();
    assertThat(actualResponse[0].getStatusCode()).isEqualTo(expectedHttpStatus);
  }

  @Test
  void getDataSearch_pagedRepositoryReturnsNull_returnsNotFoundResponse() {
    // Test Case ID: TC_SEARCH_SUGGESTION_013
    // Scenario ID: EDGE_004
    // Objective: Verify a null paged repository result is reported as not found.
    // Covered Branch/Path: isGetAll false -> repository page null -> NOT_FOUND response branch.
    // DB Check: Verify repository receives expected pageable and no write occurs.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputBody = "{\"paging\":{\"page\":1,\"size\":10},\"isGetAll\":false}";
    when(searchingSuggestionRepo.findSearchingSuggestion(PageRequest.of(0, 10))).thenReturn(null);

    // Act
    ResponseEntitySerializable actualResponse = searchingSuggestionService.getDataSearch(inputBody);

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(actualResponse.getBody()).isNull();
    verify(searchingSuggestionRepo, times(1)).findSearchingSuggestion(PageRequest.of(0, 10));
    verify(searchingSuggestionRepo, never()).save(any());
  }

  @Test
  void addDataSearch_validSuggestion_savesAndReturnsSuggestion() {
    // Test Case ID: TC_SEARCH_SUGGESTION_004
    // Scenario ID: HAPPY_003
    // Objective: Verify a search suggestion can be persisted for future search/filter suggestions.
    // Covered Branch/Path: repository save success -> OK response.
    // DB Check: Capture saved suggestion and verify input data is passed to repository.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    SearchingSuggestion inputSuggestion = buildSuggestion("java", "job", 1);
    when(searchingSuggestionRepo.save(inputSuggestion)).thenReturn(inputSuggestion);

    // Act
    ResponseEntitySerializable actualResponse = searchingSuggestionService.addDataSearch(inputSuggestion);

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getData()).isEqualTo(inputSuggestion);
    ArgumentCaptor<SearchingSuggestion> suggestionCaptor =
        ArgumentCaptor.forClass(SearchingSuggestion.class);
    verify(searchingSuggestionRepo, times(1)).save(suggestionCaptor.capture());
    assertThat(suggestionCaptor.getValue()).isSameAs(inputSuggestion);
    assertThat(suggestionCaptor.getValue().getVal()).isEqualTo("java");
    assertThat(suggestionCaptor.getValue().getObject()).isEqualTo("job");
    assertThat(suggestionCaptor.getValue().getRank()).isEqualTo(1);
  }

  @Test
  void addDataSearch_repositoryThrowsNullPointer_returnsExceptionResponse() {
    // Test Case ID: TC_SEARCH_SUGGESTION_005
    // Scenario ID: SAD_002
    // Objective: Verify repository null error is converted to an error response.
    // Covered Branch/Path: repository save throws NullPointerException -> catch branch.
    // DB Check: Verify save is attempted once and failure is surfaced through exception handler.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    SearchingSuggestion inputSuggestion = buildSuggestion("java", "job", 1);
    NullPointerException expectedException = new NullPointerException("db failure");
    ResponseEntitySerializable<Object> expectedErrorResponse =
        new ResponseEntitySerializable<>(new ResponseObject("ERROR", "500", "db failure"),
            HttpStatus.INTERNAL_SERVER_ERROR);
    when(searchingSuggestionRepo.save(inputSuggestion)).thenThrow(expectedException);
    when(restExceptionHandler.handleNullPointerException(expectedException)).thenReturn(expectedErrorResponse);

    // Act
    ResponseEntitySerializable actualResponse = searchingSuggestionService.addDataSearch(inputSuggestion);

    // Assert
    assertThat(actualResponse).isEqualTo(expectedErrorResponse);
    verify(searchingSuggestionRepo, times(1)).save(inputSuggestion);
    verify(restExceptionHandler, times(1)).handleNullPointerException(expectedException);
  }

  @Test
  void getDataSearchByCondition_cacheMiss_returnsMatchingSuggestionsAndCachesThem() {
    // Test Case ID: TC_SEARCH_SUGGESTION_006
    // Scenario ID: HAPPY_004
    // Objective: Verify typed search keyword returns matching suggestions for the selected object.
    // Covered Branch/Path: condition request -> getObjectSearch cache miss -> repository query -> cache put.
    // DB Check: Verify query uses search text and object criteria and no write occurs.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputSearchText = "java";
    String inputObject = "job";
    SearchingSuggestion inputCondition = buildSuggestion(inputSearchText, inputObject, 1);
    List<SearchingSuggestion> expectedSuggestions = List.of(inputCondition);
    when(cacheService.isExistedInCache(cacheManager, inputObject, inputSearchText)).thenReturn(false);
    when(searchingSuggestionRepo.findSearchingSuggestionByCondition(inputSearchText, inputObject))
        .thenReturn(expectedSuggestions);

    // Act
    ResponseEntitySerializable actualResponse =
        searchingSuggestionService.getDataSearchByCondition(inputCondition);

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getData()).isEqualTo(expectedSuggestions);
    assertThat(actualBody.getTotalCount()).isEqualTo(1L);
    verify(searchingSuggestionRepo, times(1))
        .findSearchingSuggestionByCondition(inputSearchText, inputObject);
    verify(cacheService, times(1)).putCache(cacheManager, inputObject, inputSearchText,
        expectedSuggestions);
    verify(searchingSuggestionRepo, never()).save(any());
  }

  @Test
  void getDataSearchByCondition_nullInput_returnsExceptionResponse() {
    // Test Case ID: TC_SEARCH_SUGGESTION_007
    // Scenario ID: SAD_003
    // Objective: Verify null condition request is converted to an error response.
    // Covered Branch/Path: searchingSuggestion null -> NullPointerException catch branch.
    // DB Check: Verify no repository access happens when input is null.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    SearchingSuggestion inputCondition = null;
    ResponseEntitySerializable<Object> expectedErrorResponse =
        new ResponseEntitySerializable<>(new ResponseObject("ERROR", "500", "condition required"),
            HttpStatus.INTERNAL_SERVER_ERROR);
    when(restExceptionHandler.handleNullPointerException(any(NullPointerException.class)))
        .thenReturn(expectedErrorResponse);

    // Act
    ResponseEntitySerializable actualResponse =
        searchingSuggestionService.getDataSearchByCondition(inputCondition);

    // Assert
    assertThat(actualResponse).isEqualTo(expectedErrorResponse);
    verify(restExceptionHandler, times(1)).handleNullPointerException(any(NullPointerException.class));
    verify(searchingSuggestionRepo, never()).findSearchingSuggestionByCondition(any(), any());
    verify(searchingSuggestionRepo, never()).save(any());
  }

  @Test
  void getDataSearchByCondition_repositoryRuntimeException_returnsErrorResponse() {
    // Test Case ID: TC_SEARCH_SUGGESTION_016
    // Scenario ID: SAD_004
    // Objective: Verify runtime repository errors in condition search do not return a null response.
    // Assumption: Repository/cache runtime failures should surface an error response, not null.
    // Covered Branch/Path: getObjectSearch throws RuntimeException -> error response branch.
    // DB Check: Verify repository read is attempted once and no save/write occurs.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputSearchText = "java";
    String inputObject = "job";
    SearchingSuggestion inputCondition = buildSuggestion(inputSearchText, inputObject, 1);
    HttpStatus expectedHttpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    logEvidence("mvn -pl search-service -DskipTests=false -Dtest=SearchingSuggestionImplTest test",
        "TC_SEARCH_SUGGESTION_016 expects repository runtime errors to produce a non-null error response");
    when(cacheService.isExistedInCache(cacheManager, inputObject, inputSearchText)).thenReturn(false);
    when(searchingSuggestionRepo.findSearchingSuggestionByCondition(inputSearchText, inputObject))
        .thenThrow(new RuntimeException("query failure"));

    // Act
    ResponseEntitySerializable actualResponse =
        searchingSuggestionService.getDataSearchByCondition(inputCondition);

    // Assert
    verify(searchingSuggestionRepo, times(1))
        .findSearchingSuggestionByCondition(inputSearchText, inputObject);
    verify(searchingSuggestionRepo, never()).save(any());
    assertThat(actualResponse).isNotNull();
    assertThat(actualResponse.getStatusCode()).isEqualTo(expectedHttpStatus);
  }

  @Test
  void getDataSearchByMatchCondition_cacheHit_updatesRankAndReturnsSuggestion() {
    // Test Case ID: TC_SEARCH_SUGGESTION_008
    // Scenario ID: HAPPY_005
    // Objective: Verify exact-match search uses cached suggestion and increments its rank.
    // Covered Branch/Path: cache hit -> addOrUpdateObject existing object -> save updated rank.
    // DB Check: Verify repository lookup is skipped and save receives rank incremented by one.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputSearchText = "java";
    String inputObject = "job";
    SearchingSuggestion inputCondition = buildSuggestion(inputSearchText, inputObject, 1);
    SearchingSuggestion cachedSuggestion = buildSuggestion(inputSearchText, inputObject, 3);
    when(cacheService.isExistedInCache(cacheManager, inputObject, inputSearchText)).thenReturn(true);
    when(cacheService.<SearchingSuggestion>getCache(cacheManager, inputObject, inputSearchText))
        .thenReturn(cachedSuggestion);

    // Act
    ResponseEntitySerializable actualResponse =
        searchingSuggestionService.getDataSearchByMatchCondition(inputCondition);

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getStatus()).isEqualTo(SUCCESS);
    assertThat(actualBody.getMessage()).isEqualTo(FOUND);
    assertThat(actualBody.getData()).isSameAs(cachedSuggestion);
    ArgumentCaptor<SearchingSuggestion> suggestionCaptor =
        ArgumentCaptor.forClass(SearchingSuggestion.class);
    verify(searchingSuggestionRepo, never())
        .findSearchingSuggestionByMatchCondition(inputSearchText, inputObject);
    verify(searchingSuggestionRepo, times(1)).save(suggestionCaptor.capture());
    assertThat(suggestionCaptor.getValue().getRank()).isEqualTo(4);
    verify(cacheService, times(1)).putCache(cacheManager, inputObject, inputSearchText,
        cachedSuggestion);
  }

  @Test
  void getDataSearchByMatchCondition_cacheMissAndNoDbMatch_createsNewSuggestion() {
    // Test Case ID: TC_SEARCH_SUGGESTION_017
    // Scenario ID: EDGE_001
    // Objective: Verify a new suggestion is created and returned when exact keyword is not found.
    // Assumption: The response should return the newly created suggestion after the rank update.
    // Covered Branch/Path: cache miss -> repository returns null -> addOrUpdateObject create branch.
    // DB Check: Capture saved suggestion and verify response returns the same created data.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputSearchText = "golang";
    String inputObject = "job";
    SearchingSuggestion inputCondition = buildSuggestion(inputSearchText, inputObject, null);
    HttpStatus expectedHttpStatus = HttpStatus.OK;
    Integer expectedInitialRank = 1;
    logEvidence("mvn -pl search-service -DskipTests=false -Dtest=SearchingSuggestionImplTest test",
        "TC_SEARCH_SUGGESTION_017 expects newly created exact-match suggestion to be returned in response data");
    when(cacheService.isExistedInCache(cacheManager, inputObject, inputSearchText)).thenReturn(false);
    when(searchingSuggestionRepo.findSearchingSuggestionByMatchCondition(inputSearchText, inputObject))
        .thenReturn(null);

    // Act
    ResponseEntitySerializable actualResponse =
        searchingSuggestionService.getDataSearchByMatchCondition(inputCondition);

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(expectedHttpStatus);
    ArgumentCaptor<SearchingSuggestion> suggestionCaptor =
        ArgumentCaptor.forClass(SearchingSuggestion.class);
    verify(searchingSuggestionRepo, times(1))
        .findSearchingSuggestionByMatchCondition(inputSearchText, inputObject);
    verify(searchingSuggestionRepo, times(1)).save(suggestionCaptor.capture());
    SearchingSuggestion actualSavedSuggestion = suggestionCaptor.getValue();
    assertThat(actualSavedSuggestion.getVal()).isEqualTo(inputSearchText);
    assertThat(actualSavedSuggestion.getObject()).isEqualTo(inputObject);
    assertThat(actualSavedSuggestion.getRank()).isEqualTo(expectedInitialRank);
    assertThat(actualSavedSuggestion.getCreationDate()).isNotNull();
    assertThat(actualSavedSuggestion.getUpdateDate()).isNotNull();
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getData()).isEqualTo(actualSavedSuggestion);
    verify(cacheService, times(1)).putCache(cacheManager, inputObject, inputSearchText,
        actualSavedSuggestion);
  }

  @Test
  void getDataSearchByMatchCondition_cacheMissAndDbMatch_updatesRankAndReturnsSuggestion() {
    // Test Case ID: TC_SEARCH_SUGGESTION_020
    // Scenario ID: HAPPY_007
    // Objective: Verify exact-match DB hit increments rank and returns the updated suggestion.
    // Covered Branch/Path: cache miss -> repository match found -> addOrUpdateObject update branch.
    // DB Check: Capture saved suggestion and verify rank is incremented from existing DB data.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputSearchText = "java";
    String inputObject = "job";
    SearchingSuggestion inputCondition = buildSuggestion(inputSearchText, inputObject, null);
    SearchingSuggestion existingSuggestion = buildSuggestion(inputSearchText, inputObject, 9);
    Integer expectedRank = 10;
    logEvidence("mvn -pl search-service -DskipTests=false -Dtest=SearchingSuggestionImplTest test",
        "TC_SEARCH_SUGGESTION_020 expects cache-miss DB match to be rank-incremented and returned");
    when(cacheService.isExistedInCache(cacheManager, inputObject, inputSearchText)).thenReturn(false);
    when(searchingSuggestionRepo.findSearchingSuggestionByMatchCondition(inputSearchText, inputObject))
        .thenReturn(existingSuggestion);

    // Act
    ResponseEntitySerializable actualResponse =
        searchingSuggestionService.getDataSearchByMatchCondition(inputCondition);

    // Assert
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    ArgumentCaptor<SearchingSuggestion> suggestionCaptor =
        ArgumentCaptor.forClass(SearchingSuggestion.class);
    verify(searchingSuggestionRepo, times(1)).save(suggestionCaptor.capture());
    SearchingSuggestion actualSavedSuggestion = suggestionCaptor.getValue();
    assertThat(actualSavedSuggestion).isSameAs(existingSuggestion);
    assertThat(actualSavedSuggestion.getRank()).isEqualTo(expectedRank);
    ResponseObject actualBody = (ResponseObject) actualResponse.getBody();
    assertThat(actualBody).isNotNull();
    assertThat(actualBody.getData()).isSameAs(existingSuggestion);
    verify(cacheService, times(1)).putCache(cacheManager, inputObject, inputSearchText,
        existingSuggestion);
  }

  @Test
  void getDataSearchByMatchCondition_nullInput_returnsExceptionResponse() {
    // Test Case ID: TC_SEARCH_SUGGESTION_015
    // Scenario ID: SAD_005
    // Objective: Verify null exact-match request is converted to an error response.
    // Covered Branch/Path: body null -> NullPointerException catch branch.
    // DB Check: Verify no repository read/write happens when input is null.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    SearchingSuggestion inputCondition = null;
    ResponseEntitySerializable<Object> expectedErrorResponse =
        new ResponseEntitySerializable<>(new ResponseObject("ERROR", "500", "match condition required"),
            HttpStatus.INTERNAL_SERVER_ERROR);
    when(restExceptionHandler.handleNullPointerException(any(NullPointerException.class)))
        .thenReturn(expectedErrorResponse);

    // Act
    ResponseEntitySerializable actualResponse =
        searchingSuggestionService.getDataSearchByMatchCondition(inputCondition);

    // Assert
    assertThat(actualResponse).isEqualTo(expectedErrorResponse);
    verify(restExceptionHandler, times(1)).handleNullPointerException(any(NullPointerException.class));
    verify(searchingSuggestionRepo, never()).findSearchingSuggestionByMatchCondition(any(), any());
    verify(searchingSuggestionRepo, never()).save(any());
  }

  @Test
  void getObjectSearch_cacheHit_returnsCachedSuggestionsWithoutRepositoryRead() {
    // Test Case ID: TC_SEARCH_SUGGESTION_010
    // Scenario ID: HAPPY_006
    // Objective: Verify suggestion lookup returns cached data without querying DB.
    // Covered Branch/Path: cache exists -> getCache branch.
    // DB Check: Verify repository condition query is never called.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputSearchText = "java";
    String inputObject = "job";
    List<SearchingSuggestion> expectedSuggestions = List.of(buildSuggestion(inputSearchText, inputObject, 1));
    when(cacheService.isExistedInCache(cacheManager, inputObject, inputSearchText)).thenReturn(true);
    when(cacheService.<List<SearchingSuggestion>>getCache(cacheManager, inputObject, inputSearchText))
        .thenReturn(expectedSuggestions);

    // Act
    List<SearchingSuggestion> actualSuggestions =
        searchingSuggestionService.getObjectSearch(inputSearchText, inputObject);

    // Assert
    assertThat(actualSuggestions).isEqualTo(expectedSuggestions);
    verify(searchingSuggestionRepo, never()).findSearchingSuggestionByCondition(any(), any());
    verify(cacheService, never()).putCache(eq(cacheManager), eq(inputObject), eq(inputSearchText), any());
  }

  @Test
  void getObjectSearch_cacheMissAndNoData_returnsEmptyListAndCachesIt() {
    // Test Case ID: TC_SEARCH_SUGGESTION_011
    // Scenario ID: EDGE_002
    // Objective: Verify no matching suggestion returns an empty list for the no-result search scenario.
    // Covered Branch/Path: cache miss -> repository returns empty list -> cache put empty list.
    // DB Check: Verify repository query uses the keyword and object filter exactly once.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputSearchText = "unknown";
    String inputObject = "job";
    List<SearchingSuggestion> expectedNoSuggestions = Collections.emptyList();
    when(cacheService.isExistedInCache(cacheManager, inputObject, inputSearchText)).thenReturn(false);
    when(searchingSuggestionRepo.findSearchingSuggestionByCondition(inputSearchText, inputObject))
        .thenReturn(expectedNoSuggestions);

    // Act
    List<SearchingSuggestion> actualSuggestions =
        searchingSuggestionService.getObjectSearch(inputSearchText, inputObject);

    // Assert
    assertThat(actualSuggestions).isEmpty();
    verify(searchingSuggestionRepo, times(1))
        .findSearchingSuggestionByCondition(inputSearchText, inputObject);
    verify(cacheService, times(1)).putCache(cacheManager, inputObject, inputSearchText,
        expectedNoSuggestions);
  }

  @Test
  void addOrUpdateObject_existingSuggestion_incrementsRankUpdatesCacheAndSaves() {
    // Test Case ID: TC_SEARCH_SUGGESTION_021
    // Scenario ID: HAPPY_008
    // Objective: Verify direct update helper increments rank and persists/cache-updates existing suggestion.
    // Covered Branch/Path: addOrUpdateObject existing object branch.
    // DB Check: Capture repository save and verify the same updated object is persisted.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputSearchText = "java";
    String inputObject = "job";
    SearchingSuggestion inputExistingSuggestion = buildSuggestion(inputSearchText, inputObject, 2);
    Integer expectedRank = 3;
    logEvidence("mvn -pl search-service -DskipTests=false -Dtest=SearchingSuggestionImplTest test",
        "TC_SEARCH_SUGGESTION_021 expects addOrUpdateObject existing branch to increment rank");

    // Act
    SearchingSuggestion actualSuggestion = searchingSuggestionService.addOrUpdateObject(
        inputSearchText, inputObject, inputExistingSuggestion);

    // Assert
    assertThat(actualSuggestion).isSameAs(inputExistingSuggestion);
    assertThat(actualSuggestion.getRank()).isEqualTo(expectedRank);
    assertThat(actualSuggestion.getUpdateDate()).isNotNull();
    verify(cacheService, times(1)).putCache(cacheManager, inputObject, inputSearchText,
        inputExistingSuggestion);
    verify(searchingSuggestionRepo, times(1)).save(inputExistingSuggestion);
  }

  @Test
  void addOrUpdateObject_nullSuggestion_createsSuggestionUpdatesCacheAndSaves() {
    // Test Case ID: TC_SEARCH_SUGGESTION_022
    // Scenario ID: EDGE_006
    // Objective: Verify direct create helper builds a new suggestion when no existing suggestion is found.
    // Covered Branch/Path: addOrUpdateObject null object branch.
    // DB Check: Capture repository save and verify created value, object, rank, and dates.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputSearchText = "golang";
    String inputObject = "job";
    SearchingSuggestion inputExistingSuggestion = null;
    Integer expectedRank = 1;
    logEvidence("mvn -pl search-service -DskipTests=false -Dtest=SearchingSuggestionImplTest test",
        "TC_SEARCH_SUGGESTION_022 expects addOrUpdateObject null branch to create rank-1 suggestion");

    // Act
    SearchingSuggestion actualSuggestion = searchingSuggestionService.addOrUpdateObject(
        inputSearchText, inputObject, inputExistingSuggestion);

    // Assert
    assertThat(actualSuggestion.getVal()).isEqualTo(inputSearchText);
    assertThat(actualSuggestion.getObject()).isEqualTo(inputObject);
    assertThat(actualSuggestion.getRank()).isEqualTo(expectedRank);
    assertThat(actualSuggestion.getCreationDate()).isNotNull();
    assertThat(actualSuggestion.getUpdateDate()).isNotNull();
    verify(cacheService, times(1)).putCache(cacheManager, inputObject, inputSearchText,
        actualSuggestion);
    verify(searchingSuggestionRepo, times(1)).save(actualSuggestion);
  }

  @Test
  void addOrUpdateObject_repositorySaveThrowsRuntimeException_propagatesFailure() {
    // Test Case ID: TC_SEARCH_SUGGESTION_023
    // Scenario ID: SAD_007
    // Objective: Verify save failure in helper is not hidden.
    // Covered Branch/Path: addOrUpdateObject existing branch -> repository save throws RuntimeException.
    // DB Check: Verify cache update is attempted and repository save failure is propagated.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputSearchText = "java";
    String inputObject = "job";
    SearchingSuggestion inputExistingSuggestion = buildSuggestion(inputSearchText, inputObject, 2);
    RuntimeException expectedException = new RuntimeException("db failure");
    logEvidence("mvn -pl search-service -DskipTests=false -Dtest=SearchingSuggestionImplTest test",
        "TC_SEARCH_SUGGESTION_023 expects addOrUpdateObject save failure to be propagated");
    when(searchingSuggestionRepo.save(inputExistingSuggestion)).thenThrow(expectedException);

    // Act & Assert
    assertThat(org.assertj.core.api.Assertions.catchThrowable(() ->
        searchingSuggestionService.addOrUpdateObject(inputSearchText, inputObject, inputExistingSuggestion)))
        .isSameAs(expectedException);
    verify(cacheService, times(1)).putCache(cacheManager, inputObject, inputSearchText,
        inputExistingSuggestion);
    verify(searchingSuggestionRepo, times(1)).save(inputExistingSuggestion);
  }

  @Test
  void searchingSuggestionConverter_validJson_returnsConvertedSuggestion() {
    // Test Case ID: TC_SEARCH_SUGGESTION_024
    // Scenario ID: EDGE_003
    // Objective: Verify JSON request body is converted into a SearchingSuggestion request object.
    // Assumption: Converter should parse request body because controllers call it before service operations.
    // Covered Branch/Path: valid JSON body -> parsed SearchingSuggestion.
    // DB Check: No DB access should happen because converter has no persistence responsibility.
    // Rollback: Repository is mocked, so no real database state is changed.

    // Arrange
    String inputBody = "{\"val\":\"java\",\"object\":\"job\"}";
    String expectedValue = "java";
    String expectedObject = "job";
    logEvidence("mvn -pl search-service -DskipTests=false -Dtest=SearchingSuggestionImplTest test",
        "TC_SEARCH_SUGGESTION_024 expects converter to parse valid JSON into SearchingSuggestion");

    // Act
    SearchingSuggestion actualConvertedSuggestion =
        searchingSuggestionService.searchingSuggestionConverter(inputBody);

    // Assert
    verifyNoInteractions(searchingSuggestionRepo);
    verifyNoInteractions(cacheService);
    assertThat(actualConvertedSuggestion).isNotNull();
    assertThat(actualConvertedSuggestion.getVal()).isEqualTo(expectedValue);
    assertThat(actualConvertedSuggestion.getObject()).isEqualTo(expectedObject);
  }

  private SearchingSuggestion buildSuggestion(String value, String object, Integer rank) {
    SearchingSuggestion suggestion = new SearchingSuggestion();
    suggestion.setVal(value);
    suggestion.setObject(object);
    suggestion.setRank(rank);
    return suggestion;
  }

  private void logEvidence(String command, String expectation) {
    System.out.println("TEST_EVIDENCE command=\"" + command + "\" expectation=\"" + expectation + "\"");
  }
}
