package com.pacto.api.mission.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MissionRequestDto {
    private String submittedUrl;
    private String reason;
}