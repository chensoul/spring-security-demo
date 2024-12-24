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
import com.chensoul.persistence.model.User;
import com.chensoul.persistence.model.VerificationToken;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

@RequiredArgsConstructor
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { Application.class, TestDbConfig.class, TestIntegrationConfig.class }, webEnvironment = WebEnvironment.RANDOM_PORT)
@Transactional
class RegistrationControllerIntegrationTest {

    private final WebApplicationContext webApplicationContext;

    @PersistenceContext
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private String token;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        User user = new User();
        user.setEmail(UUID.randomUUID().toString() + "@example.com");
        user.setPassword(UUID.randomUUID().toString());
        user.setFirstName("First");
        user.setLastName("Last");

        entityManager.persist(user);
        token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, user);
        verificationToken.setExpiryDate(Date.from(Instant.now().plus(2, ChronoUnit.DAYS)));

        entityManager.persist(verificationToken);

        /*
            flush managed entities to the database to populate identifier field
         */
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void testRegistrationConfirm() throws Exception {
        ResultActions resultActions = this.mockMvc.perform(get("/registrationConfirm?token=" + token));
        resultActions.andExpect(status().is3xxRedirection());
        resultActions.andExpect(model().attribute("messageKey", "message.accountVerified"));
        resultActions.andExpect(view().name("redirect:/console"));
    }

    @Test
    void testRegistrationValidation() throws Exception {

        final MultiValueMap<String, String> param = new LinkedMultiValueMap<>();
        param.add("firstName", "");
        param.add("lastName", "");
        param.add("email", "");
        param.add("password", "");
        param.add("matchingPassword", "");

        ResultActions resultActions = this.mockMvc.perform(post("/user/registration").params(param));
        resultActions.andExpect(status().is(400));
        resultActions.andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.error", is("InvaliduserDto")))
                .andExpect(jsonPath("$.message", containsString("{\"field\":\"lastName\",\"defaultMessage\":\"Length must be greater than 1\"}")));
    }
}
