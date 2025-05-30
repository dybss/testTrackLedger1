package com.ncusoft.myapplication7.service;

import com.ncusoft.myapplication7.entity.User;

public interface UserService {
    boolean existsByUsername(String username);
    boolean register(User user);
    User findByUsername(String username);
}
