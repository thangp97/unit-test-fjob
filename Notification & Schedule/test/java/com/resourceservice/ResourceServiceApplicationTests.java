package com.resourceservice;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;
@SpringBootTest
class ResourceServiceApplicationTests {
	private static final String TOKEN_URL = "http://localhost:2004/oauth/token";

	@Test
	void contextLoads() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("grant_type", "password");
		params.put("username", "0985913194");
		params.put("password", "666666");
		Response response = RestAssured.given()
				.auth()
				.basic("clientId", "nhung")
				.formParams(params)
				.post(TOKEN_URL);
		String token = response.jsonPath()
				.getString("access_token");
	}

}
