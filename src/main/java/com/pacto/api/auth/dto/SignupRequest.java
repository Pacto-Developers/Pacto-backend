package com.pacto.api.auth.dto;

import lombok.Getter;

@Getter
public class SignupRequest {

    private String email;
    private String password;
}
