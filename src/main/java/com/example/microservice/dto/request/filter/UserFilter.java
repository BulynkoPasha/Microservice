package com.example.microservice.dto.request.filter;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserFilter {
    private String name;
    private String surname;
    private int page = 0;
    private int size = 20;

}
