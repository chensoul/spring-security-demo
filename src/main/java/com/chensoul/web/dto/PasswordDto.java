package com.chensoul.web.dto;

import com.chensoul.validation.ValidPassword;
import lombok.Data;

@Data
public class PasswordDto {

    private String oldPassword;

    private String token;

    @ValidPassword
    private String newPassword;
}
