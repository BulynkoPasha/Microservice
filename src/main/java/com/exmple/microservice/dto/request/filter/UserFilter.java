package com.exmple.microservice.dto.request.filter;

import lombok.Data;

@Data
public class UserFilter {
    private String name;
    private String surname;
    private int page = 0;
    private int size = 20;

}
