package com.ncusoft.myapplication7.controller;

import com.ncusoft.myapplication7.entity.User;
import com.ncusoft.myapplication7.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        if (userService.existsByUsername(user.getUsername())) {
            result.put("success", false);
            result.put("message", "用户已存在");
        } else {
            boolean ok = userService.register(user);
            result.put("success", ok);
            result.put("message", ok ? "注册成功" : "注册失败");
        }
        return result;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        User dbUser = userService.findByUsername(user.getUsername());
        if (dbUser != null && dbUser.getPassword().equals(user.getPassword())) {
            result.put("success", true);
            result.put("message", "登录成功");
            result.put("userId", dbUser.getUserId());
        } else {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
        }
        return result;
    }
}
