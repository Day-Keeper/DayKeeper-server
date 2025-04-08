package com.shujinko.project.controller;


import com.shujinko.project.domain.entity.User;
import com.shujinko.project.domain.dto.UserDto;
import com.shujinko.project.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class UserController {
    
    
    private final UserService userService;
    
    UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping
    public String createForm(){
        return "createMemberForm";
    }
    @PostMapping("/members/new")
    @ResponseBody
    public String createNewUser(@RequestBody UserDto userDto) {
            User user = userDto.toEntity();
            return userService.join(user);
    }
}
