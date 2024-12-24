/*
 * Copyright © 2023-2024 chensoul.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chensoul.test;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public class RegistrationPasswordLiveTest {
    private final String BASE_URI = "http://localhost:8080/";

    @Test
    public void givenInvalidPassword_thenBadRequest() {
        // too short
        assertEquals(HttpStatus.BAD_REQUEST.value(), getResponseForPassword("123"));

        // no special character
        assertEquals(HttpStatus.BAD_REQUEST.value(), getResponseForPassword("1abZRplYU"));

        // no upper case letter
        assertEquals(HttpStatus.BAD_REQUEST.value(), getResponseForPassword("1_abidpsvl"));

        // no number
        assertEquals(HttpStatus.BAD_REQUEST.value(), getResponseForPassword("abZRYUpl"));

        // alphabet sequence
        assertEquals(HttpStatus.BAD_REQUEST.value(), getResponseForPassword("1_abcZRYU"));

        // qwerty sequence
        assertEquals(HttpStatus.BAD_REQUEST.value(), getResponseForPassword("1_abZRTYU"));

        // numeric sequence
        assertEquals(HttpStatus.BAD_REQUEST.value(), getResponseForPassword("123_zqrtU"));

        // valid password
        assertEquals(HttpStatus.OK.value(), getResponseForPassword("12_zwRHIPKA"));
    }

    private int getResponseForPassword(String pass) {
        final Map<String, String> param = new HashMap<>();
        final String randomName = UUID.randomUUID().toString();
        param.put("firstName", randomName);
        param.put("lastName", "Doe");
        param.put("email", randomName + "@x.com");
        param.put("password", pass);
        param.put("matchingPassword", pass);

        final Response response = RestAssured.given().formParams(param).accept(MediaType.APPLICATION_JSON_VALUE).post(BASE_URI + "user/registration");
        return response.getStatusCode();
    }
}
