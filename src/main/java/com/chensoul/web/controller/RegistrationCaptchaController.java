package com.chensoul.web.controller;

import com.chensoul.captcha.CaptchaServiceV3;
import com.chensoul.captcha.ICaptchaService;
import com.chensoul.persistence.model.User;
import com.chensoul.registration.OnRegistrationCompleteEvent;
import com.chensoul.service.IUserService;
import com.chensoul.web.dto.UserDto;
import com.chensoul.web.util.GenericResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class RegistrationCaptchaController {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final IUserService userService;
    private final ICaptchaService captchaService;
    private final ICaptchaService captchaServiceV3;
    private final ApplicationEventPublisher eventPublisher;

    // Registration
    @PostMapping("/user/registrationCaptcha")
    public GenericResponse captchaRegisterUserAccount(@Valid final UserDto accountDto, final HttpServletRequest request) {

        final String response = request.getParameter("g-recaptcha-response");
        captchaService.processResponse(response);

        return registerNewUserHandler(accountDto, request);
    }


    // Registration reCaptchaV3
    @PostMapping("/user/registrationCaptchaV3")
    public GenericResponse captchaV3RegisterUserAccount(@Valid final UserDto accountDto, final HttpServletRequest request) {

        final String response = request.getParameter("response");
        captchaServiceV3.processResponse(response, CaptchaServiceV3.REGISTER_ACTION);

        return registerNewUserHandler(accountDto, request);
    }

    private GenericResponse registerNewUserHandler(final UserDto accountDto, final HttpServletRequest request) {
        LOGGER.debug("Registering user account with information: {}", accountDto);

        final User registered = userService.registerNewUserAccount(accountDto);
        eventPublisher.publishEvent(new OnRegistrationCompleteEvent(registered, request.getLocale(), getAppUrl(request)));
        return new GenericResponse("success");
    }


    private String getAppUrl(HttpServletRequest request) {
        return "http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    }

}
