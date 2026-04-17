package com.nhom611.usersvc.service;

import com.nhom611.usersvc.domain.User;
import com.nhom611.usersvc.dto.AuthDtos;
import com.nhom611.usersvc.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class UserQueryService {

    private final UserRepository userRepository;

    public UserQueryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Page<AuthDtos.UserResponse> listUsers(int page, int size) {
        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.min(Math.max(1, size), 100);

        PageRequest pr = PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return userRepository.findAll(pr).map(UserQueryService::toUserResponse);
    }

    private static AuthDtos.UserResponse toUserResponse(User user) {
        return new AuthDtos.UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getStatus());
    }
}
