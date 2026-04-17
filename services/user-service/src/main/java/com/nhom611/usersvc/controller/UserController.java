package com.nhom611.usersvc.controller;

import com.nhom611.usersvc.dto.AuthDtos;
import com.nhom611.usersvc.service.UserQueryService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UserController {

    private final UserQueryService userQueryService;

    public UserController(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    // Auth required (SecurityConfig). MVP: không giới hạn role để tiện test.
    @GetMapping("/users")
    public Map<String, Object> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<AuthDtos.UserResponse> p = userQueryService.listUsers(page, size);
        return Map.of(
                "items", p.getContent(),
                "page", p.getNumber(),
                "size", p.getSize(),
                "totalElements", p.getTotalElements(),
                "totalPages", p.getTotalPages()
        );
    }
}
