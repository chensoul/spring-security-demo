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

import com.chensoul.persistence.model.NewLocationToken;
import com.chensoul.service.IUserService;
import com.chensoul.web.error.UnusualLocationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class DifferentLocationChecker implements UserDetailsChecker {
    private final ConfigurableApplicationContext context;
    private final HttpServletRequest request;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void check(UserDetails userDetails) {
        IUserService userService = context.getBean(IUserService.class);
        final String ip = getClientIP();
        final NewLocationToken token = userService.isNewLoginLocation(userDetails.getUsername(), ip);
        if (token!=null) {
            final String appUrl = "http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
            eventPublisher.publishEvent(new OnDifferentLocationLoginEvent(request.getLocale(), userDetails.getUsername(), ip, token, appUrl));
            throw new UnusualLocationException("unusual location");
        }
    }

    private String getClientIP() {
        final String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader==null || xfHeader.isEmpty() || !xfHeader.contains(request.getRemoteAddr())) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
