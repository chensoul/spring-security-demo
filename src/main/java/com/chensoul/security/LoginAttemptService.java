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
package com.chensoul.security;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPT = 10;

    private LoadingCache<String, Integer> attemptsCache;

    private final HttpServletRequest request;

    public LoginAttemptService(HttpServletRequest request) {
        super();
        this.request = request;
        attemptsCache = Caffeine.newBuilder()
                //创建缓存或者最近一次更新缓存后经过指定时间间隔，刷新缓存；refreshAfterWrite仅支持LoadingCache
                .refreshAfterWrite(1, TimeUnit.DAYS)
                .expireAfterWrite(1, TimeUnit.DAYS)
                .expireAfterAccess(1, TimeUnit.DAYS)
                .maximumSize(10)
                .build(key -> 0);
        ;
    }

    public void loginFailed(final String key) {
        int attempts;
        attempts = attemptsCache.get(key);
        attempts++;
        attemptsCache.put(key, attempts);
    }

    public boolean isBlocked() {
        return attemptsCache.get(getClientIP()) >= MAX_ATTEMPT;
    }

    private String getClientIP() {
        final String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader!=null) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}
