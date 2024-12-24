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

import com.chensoul.config.LoginNotificationConfig;
import com.chensoul.config.TestDbConfig;
import com.chensoul.config.TestIntegrationConfig;
import com.chensoul.persistence.dao.UserRepository;
import com.chensoul.persistence.dao.VerificationTokenRepository;
import com.chensoul.persistence.model.User;
import com.chensoul.persistence.model.VerificationToken;
import com.chensoul.validation.EmailExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestDbConfig.class, TestIntegrationConfig.class, LoginNotificationConfig.class})
@Transactional
class UserIntegrationTest {

    private final VerificationTokenRepository tokenRepository;

    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Long tokenId;
    private Long userId;

    //

    @BeforeEach
    public void givenUserAndVerificationToken() throws EmailExistsException {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("SecretPassword");
        user.setFirstName("First");
        user.setLastName("Last");
        entityManager.persist(user);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, user);
        entityManager.persist(verificationToken);

        entityManager.flush();
        entityManager.clear();

        tokenId = verificationToken.getId();
        userId = user.getId();
    }

    @AfterEach
    public void flushAfter() {
        entityManager.flush();
        entityManager.clear();
    }

    //

    @Test
    void whenContextLoad_thenCorrect() {
        assertTrue(userRepository.count() > 0);
        assertTrue(tokenRepository.count() > 0);
    }

    // @Test(expected = Exception.class)
    @Test
    @Disabled("needs to go through the service and get transactional semantics")
    void whenRemovingUser_thenFkViolationException() {
        userRepository.deleteById(userId);
    }

    @Test
    void whenRemovingTokenThenUser_thenCorrect() {
        tokenRepository.deleteById(tokenId);
        userRepository.deleteById(userId);
    }

}
