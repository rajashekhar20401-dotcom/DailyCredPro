package com.unqiuehire.kashflow.dto.requestdto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationUpdateRequestDto {
    private Double latitude;
    private Double longitude;
    private Boolean consentGiven;
}
