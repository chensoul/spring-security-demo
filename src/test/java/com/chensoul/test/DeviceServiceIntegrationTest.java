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
import com.chensoul.persistence.dao.DeviceMetadataRepository;
import com.chensoul.persistence.dao.UserRepository;
import com.chensoul.persistence.model.DeviceMetadata;
import com.chensoul.persistence.model.User;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.response.Response;
import java.util.Collections;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@ExtendWith(SpringExtension.class)
@Transactional
@SpringBootTest(classes = {Application.class, TestDbConfig.class, TestIntegrationConfig.class},
        properties = "geo.ip.lib.enabled=true", webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DeviceServiceIntegrationTest {

    private final UserRepository userRepository;

    @MockBean
    private DeviceMetadataRepository deviceMetadataRepository;

    private final PasswordEncoder passwordEncoder;

    private final JavaMailSender mailSender;

    @Value("${local.server.port}")
    int port;

    private Long userId;

    @BeforeEach
    public void init() {
        User user = userRepository.findByEmail("test@test.com");
        if (user==null) {
            user = new User();
            user.setFirstName("Test");
            user.setLastName("Test");
            user.setPassword(passwordEncoder.encode("test"));
            user.setEmail("test@test.com");
            user.setEnabled(true);
            user = userRepository.save(user);
        } else {
            user.setPassword(passwordEncoder.encode("test"));
            user = userRepository.save(user);
        }

        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        userId = user.getId();
    }

    @Test
    public void givenValidLoginRequest_whenNoPreviousKnownDevices_shouldSendLoginNotification() {
        final Response response = given()
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36")
                .header("X-Forwarded-For", "88.198.50.103") // Nuremberg
                .formParams("username", "test@test.com", "password", "test")
                .post("/login");

        assertEquals(302, response.statusCode());
        assertEquals("http://localhost:" + port + "/console", response.getHeader("Location"));
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    public void givenValidLoginRequest_whenUsingKnownDevice_shouldNotSendLoginNotification() {
        DeviceMetadata existingDeviceMetadata = new DeviceMetadata();
        existingDeviceMetadata.setUserId(userId);
        existingDeviceMetadata.setLastLoggedIn(new Date());
        existingDeviceMetadata.setLocation("Nuremberg");
        existingDeviceMetadata.setDeviceDetails("Chrome 71.0 - Mac OS X 10.14");
        when(deviceMetadataRepository.findByUserId(userId)).thenReturn(Collections.singletonList(existingDeviceMetadata));

        final Response response = given()
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36")
                .header("X-Forwarded-For", "88.198.50.103") // Nuremberg
                .formParams("username", "test@test.com", "password", "test")
                .post("/login");

        assertEquals(302, response.statusCode());
        assertEquals("http://localhost:" + port + "/console", response.getHeader("Location"));
        verify(mailSender, times(0)).send(any(SimpleMailMessage.class));
    }

    @Test
    public void givenValidLoginRequest_whenUsingNewDevice_shouldSendLoginNotification() {
        DeviceMetadata existingDeviceMetadata = new DeviceMetadata();
        existingDeviceMetadata.setUserId(userId);
        existingDeviceMetadata.setLastLoggedIn(new Date());
        existingDeviceMetadata.setLocation("Nuremberg");
        existingDeviceMetadata.setDeviceDetails("Chrome 71.0 - Mac OS X 10.14");
        when(deviceMetadataRepository.findByUserId(userId)).thenReturn(Collections.singletonList(existingDeviceMetadata));

        final Response response = given()
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.0 Safari/605.1.15")
                .header("X-Forwarded-For", "88.198.50.103") // Nuremberg
                .formParams("username", "test@test.com", "password", "test")
                .post("/login");

        assertEquals(302, response.statusCode());
        assertEquals("http://localhost:" + port + "/console", response.getHeader("Location"));
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    public void givenValidLoginRequest_whenUsingKnownDeviceFromDifferentLocation_shouldSendLoginNotification() {
        DeviceMetadata existingDeviceMetadata = new DeviceMetadata();
        existingDeviceMetadata.setUserId(userId);
        existingDeviceMetadata.setLastLoggedIn(new Date());
        existingDeviceMetadata.setLocation("Nuremberg");
        existingDeviceMetadata.setDeviceDetails("Chrome 71.0 - Mac OS X 10.14");
        when(deviceMetadataRepository.findByUserId(userId)).thenReturn(Collections.singletonList(existingDeviceMetadata));

        final Response response = given()
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36")
                .header("X-Forwarded-For", "81.47.169.143") // Barcelona
                .formParams("username", "test@test.com", "password", "test")
                .post("/login");

        assertEquals(302, response.statusCode());
        assertEquals("http://localhost:" + port + "/console", response.getHeader("Location"));
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

}
