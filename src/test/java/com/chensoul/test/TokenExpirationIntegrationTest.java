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

import com.chensoul.config.TestDbConfig;
import com.chensoul.config.TestTaskConfig;
import com.chensoul.persistence.dao.UserRepository;
import com.chensoul.persistence.dao.VerificationTokenRepository;
import com.chensoul.persistence.model.User;
import com.chensoul.persistence.model.VerificationToken;
import com.chensoul.task.TokensPurgeTask;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { TestDbConfig.class, TestTaskConfig.class })
@Transactional
class TokenExpirationIntegrationTest {

    private final UserRepository userRepository;

    private final VerificationTokenRepository tokenRepository;

    private final TokensPurgeTask tokensPurgeTask;

    @PersistenceContext
    private EntityManager entityManager;

    private Long token_id;
    private Long user_id;

    //

    @BeforeEach
    public void givenUserWithExpiredToken() {

        // we need a clear token repository
        tokenRepository.deleteAll();

        User user = new User();
        user.setEmail(UUID.randomUUID().toString() + "@example.com");
        user.setPassword(UUID.randomUUID().toString());
        user.setFirstName("First");
        user.setLastName("Last");

        entityManager.persist(user);
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, user);
        verificationToken.setExpiryDate(Date.from(Instant.now().minus(2, ChronoUnit.DAYS)));

        entityManager.persist(verificationToken);

        /*
            flush managed entities to the database to populate identifier field
         */
        entityManager.flush();

        /*
            remove managed entities from the persistence context
            so that subsequent SQL queries hit the database
         */
        entityManager.clear();

        token_id = verificationToken.getId();
        user_id = user.getId();
    }

    @Test
    void whenContextLoad_thenCorrect() {
    	assertNotNull(user_id);
    	assertNotNull(token_id);
    	assertTrue(userRepository.findById(user_id).isPresent());

        Optional<VerificationToken> verificationToken = tokenRepository.findById(token_id);
        assertTrue(verificationToken.isPresent());
        assertTrue(tokenRepository.findAllByExpiryDateLessThan(Date.from(Instant.now())).anyMatch((token) -> token.equals(verificationToken.get())));
    }

    @AfterEach
    public void flushAfter() {
        entityManager.flush();
    }

    @Test
    void whenRemoveByGeneratedQuery_thenCorrect() {
        tokenRepository.deleteByExpiryDateLessThan(Date.from(Instant.now()));
        assertEquals(0, tokenRepository.count());
    }

    @Test
    void whenRemoveByJPQLQuery_thenCorrect() {
        tokenRepository.deleteAllExpiredSince(Date.from(Instant.now()));
        assertEquals(0, tokenRepository.count());
    }

    @Test
    void whenPurgeTokenTask_thenCorrect() {
        tokensPurgeTask.purgeExpired();
        assertFalse(tokenRepository.findById(token_id).isPresent());
    }
}
