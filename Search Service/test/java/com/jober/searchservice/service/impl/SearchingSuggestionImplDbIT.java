package com.jober.searchservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.jober.searchservice.integration.PostgresContainerBaseIT;
import com.jober.searchservice.model.SearchingSuggestion;
import com.jober.searchservice.repository.SearchingSuggestionRepo;
import com.jober.searchservice.utilsmodule.CacheService;
import com.jober.utilsservice.errors.ResponseEntitySerializable;
import com.jober.utilsservice.errors.RestExceptionHandler;
import com.jober.utilsservice.utils.modelCustom.ResponseObject;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class SearchingSuggestionImplDbIT extends PostgresContainerBaseIT {

  @Autowired
  private SearchingSuggestionRepo<SearchingSuggestion, Long> searchingSuggestionRepo;

  private SearchingSuggestionImpl searchingSuggestionService;
  private CacheService cacheService;
  private CacheManager cacheManager;

  @BeforeEach
  void setUp() {
    searchingSuggestionRepo.deleteAll();

    cacheManager = new ConcurrentMapCacheManager("job", "address", "name", "year", "salary");
    cacheService = new CacheService();
    searchingSuggestionService = new SearchingSuggestionImpl();

    ReflectionTestUtils.setField(
        searchingSuggestionService, "searchingSuggestionRepo", searchingSuggestionRepo);
    ReflectionTestUtils.setField(searchingSuggestionService, "cacheManager", cacheManager);
    ReflectionTestUtils.setField(searchingSuggestionService, "cacheService", cacheService);
    ReflectionTestUtils.setField(searchingSuggestionService, "responseObject", new ResponseObject());

    SearchingSuggestionImpl.restExceptionHandler = new RestExceptionHandler();
    SearchingSuggestionImpl.responseEntity = null;
  }

  @Test
  void getDataSearch_getAllTrue_readsPersistedRowsFromRealDatabase() {
    searchingSuggestionRepo.saveAndFlush(newSuggestion("java", "job", 3));
    searchingSuggestionRepo.saveAndFlush(newSuggestion("python", "job", 2));

    ResponseEntitySerializable response =
        searchingSuggestionService.getDataSearch("{\"paging\":{\"page\":1,\"size\":10},\"isGetAll\":true}");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject body = (ResponseObject) response.getBody();
    assertThat(body).isNotNull();
    assertThat((List<?>) body.getData()).hasSize(2);
    assertThat(body.getTotalCount()).isEqualTo(2L);
  }

  @Test
  void addDataSearch_persistsIntoRealDatabase() {
    SearchingSuggestion input = newSuggestion("golang", "job", 1);

    ResponseEntitySerializable response = searchingSuggestionService.addDataSearch(input);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(searchingSuggestionRepo.count()).isEqualTo(1);
    SearchingSuggestion fromDb =
        searchingSuggestionRepo.findSearchingSuggestionByMatchCondition("golang", "job");
    assertThat(fromDb).isNotNull();
    assertThat(fromDb.getRank()).isEqualTo(1);
  }

  @Test
  void getDataSearchByCondition_readsFromRealDatabaseAndCachesResult() {
    searchingSuggestionRepo.saveAndFlush(newSuggestion("java backend", "job", 4));
    searchingSuggestionRepo.saveAndFlush(newSuggestion("java spring", "job", 5));
    searchingSuggestionRepo.saveAndFlush(newSuggestion("ha noi", "address", 2));

    SearchingSuggestion input = new SearchingSuggestion();
    input.setVal("java");
    input.setObject("job");

    ResponseEntitySerializable response = searchingSuggestionService.getDataSearchByCondition(input);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject body = (ResponseObject) response.getBody();
    assertThat(body).isNotNull();
    assertThat((List<?>) body.getData()).hasSize(2);
    assertThat(cacheService.isExistedInCache(cacheManager, "job", "java")).isTrue();
  }

  @Test
  void getDataSearchByMatchCondition_whenMissing_createsRecordInRealDatabase() {
    SearchingSuggestion input = new SearchingSuggestion();
    input.setVal("rust");
    input.setObject("job");

    ResponseEntitySerializable response = searchingSuggestionService.getDataSearchByMatchCondition(input);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    SearchingSuggestion created =
        searchingSuggestionRepo.findSearchingSuggestionByMatchCondition("rust", "job");
    assertThat(created).isNotNull();
    assertThat(created.getRank()).isEqualTo(1);
  }

  @Test
  void addOrUpdateObject_existingRecord_incrementsRankInRealDatabase() {
    SearchingSuggestion existing = searchingSuggestionRepo.saveAndFlush(newSuggestion("react", "job", 2));

    SearchingSuggestion updated =
        searchingSuggestionService.addOrUpdateObject("react", "job", existing);

    SearchingSuggestion fromDb =
        searchingSuggestionRepo.findSearchingSuggestionByMatchCondition("react", "job");
    assertThat(updated.getRank()).isEqualTo(3);
    assertThat(fromDb).isNotNull();
    assertThat(fromDb.getRank()).isEqualTo(3);
  }

  @Test
  void getDataSearch_pagedQuery_usesRealDatabasePaging() {
    searchingSuggestionRepo.saveAndFlush(newSuggestion("kotlin", "job", 1));
    searchingSuggestionRepo.saveAndFlush(newSuggestion("scala", "job", 1));
    searchingSuggestionRepo.saveAndFlush(newSuggestion("swift", "job", 1));

    ResponseEntitySerializable response =
        searchingSuggestionService.getDataSearch("{\"paging\":{\"page\":1,\"size\":2},\"isGetAll\":false}");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseObject body = (ResponseObject) response.getBody();
    assertThat(body).isNotNull();
    assertThat((List<?>) body.getData()).hasSize(2);
    assertThat(body.getTotalCount()).isEqualTo(3L);
  }

  private SearchingSuggestion newSuggestion(String val, String object, int rank) {
    SearchingSuggestion suggestion = new SearchingSuggestion();
    suggestion.setVal(val);
    suggestion.setObject(object);
    suggestion.setRank(rank);
    suggestion.setCreationDate(LocalDateTime.now());
    suggestion.setUpdateDate(LocalDateTime.now());
    return suggestion;
  }
}
