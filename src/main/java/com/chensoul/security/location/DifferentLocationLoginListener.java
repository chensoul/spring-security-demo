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
package com.chensoul.security.location;

import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class DifferentLocationLoginListener implements ApplicationListener<OnDifferentLocationLoginEvent> {
    private final MessageSource messages;
    private final JavaMailSender mailSender;
    private final Environment env;

    @Override
    public void onApplicationEvent(final OnDifferentLocationLoginEvent event) {
        final String enableLocUri = event.getAppUrl() + "/user/enableNewLoc?token=" + event.getToken()
                .getToken();
        final String changePassUri = event.getAppUrl() + "/changePassword.html";
        final String recipientAddress = event.getUsername();
        final String subject = "Login attempt from different location";
        final String message = messages.getMessage("message.differentLocation", new Object[]{new Date().toString(), event.getToken()
                .getUserLocation()
                .getCountry(), event.getIp(), enableLocUri, changePassUri}, event.getLocale());

        final SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipientAddress);
        email.setSubject(subject);
        email.setText(message);
        email.setFrom(env.getProperty("support.email"));
        System.out.println(message);
        mailSender.send(email);
    }

}
