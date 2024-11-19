package com.chensoul.security;

public interface ISecurityUserService {

    String validatePasswordResetToken(String token);

}
