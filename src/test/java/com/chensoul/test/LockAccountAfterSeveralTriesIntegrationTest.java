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

import com.chensoul.Application;
import com.chensoul.config.TestDbConfig;
import com.chensoul.config.TestIntegrationConfig;
import com.chensoul.persistence.dao.UserRepository;
import com.chensoul.persistence.model.User;
import static com.chensoul.security.LoginAttemptService.MAX_ATTEMPT;
import io.restassured.RestAssured;
import io.restassured.authentication.FormAuthConfig;
import io.restassured.specification.RequestSpecification;
import lombok.RequiredArgsConstructor;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test class for the case to see that the user is blocked after several tries
 */
@RequiredArgsConstructor
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { Application.class, TestDbConfig.class, TestIntegrationConfig.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LockAccountAfterSeveralTriesIntegrationTest {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Value("${local.server.port}")
    int port;

    private FormAuthConfig formConfig;

    @BeforeEach
    public void init() {
        User user = userRepository.findByEmail("test@test.com");
        if (user == null) {
            user = new User();
            user.setFirstName("Test");
            user.setLastName("Test");
            user.setPassword(passwordEncoder.encode("test"));
            user.setEmail("test@test.com");
            user.setEnabled(true);
            userRepository.save(user);
        } else {
            user.setPassword(passwordEncoder.encode("test"));
            userRepository.save(user);
        }

        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        formConfig = new FormAuthConfig("/login", "username", "password");
    }

    @Test
    public void givenLoggedInUser_whenUsernameOrPasswordIsIncorrectAfterMaxAttempt_thenUserBlockFor24Hours() {
        //first request where a user tries several incorrect credential
        for (int i = 0; i < MAX_ATTEMPT - 2; i++) {
            final RequestSpecification requestIncorrect = RestAssured.given().auth().form("testtesefsdt.com" + i, "tesfsdft", formConfig);

            requestIncorrect.when().get("/console").then().assertThat().statusCode(200).and().body(not(containsString("home")));
        }

        //then user tries a correct user
        final RequestSpecification request = RestAssured.given().auth().form("test@test.com", "test", formConfig);

        request.when().get("/console").then().assertThat().statusCode(200).and().body(containsString("home"));

        for (int i = 0; i < 3; i++) {
            final RequestSpecification requestSecond = RestAssured.given().auth().form("testtesefsdt.com", "tesfsdft", formConfig);

            requestSecond.when().get("/console").then().assertThat().statusCode(200).and().body(not(containsString("home")));
        }

        //the third request where we can see that the user is blocked even if he previously entered a correct credential
        final RequestSpecification requestCorrect = RestAssured.given().auth().form("test@test.com", "test", formConfig);

        requestCorrect.when().get("/console").then().assertThat().statusCode(200).and().body(not(containsString("home")));
    }
}