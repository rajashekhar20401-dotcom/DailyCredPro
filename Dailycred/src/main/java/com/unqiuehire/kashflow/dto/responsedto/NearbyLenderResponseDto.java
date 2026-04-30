package com.unqiuehire.kashflow.dto.responsedto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NearbyLenderResponseDto {
    private Long lenderId;
    private String lenderName;
    private String pincode;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
}