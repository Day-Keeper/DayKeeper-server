package com.shujinko.project.controller.user;


import com.shujinko.project.domain.dto.user.UserDto;
import com.shujinko.project.domain.dto.user.UserPartialUpdateDto;
import com.shujinko.project.domain.entity.user.User;
import com.shujinko.project.service.user.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/user")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {
    private final UserService userService;
    UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping
    @ResponseBody
    public ResponseEntity<UserDto> getUser(Authentication authentication) {
        String uid = authentication.getName();
        User user = userService.getUser(uid);
        if(user == null){
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(user.toUserDto());
        }
    }
    
    @PatchMapping
    @ResponseBody
    public ResponseEntity<UserDto> updateUser(Authentication authentication, @RequestBody UserPartialUpdateDto partialUpdateDto) {
        String uid = authentication.getName();
        User user = userService.updateUser(uid, partialUpdateDto);
        if(user == null){
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(user.toUserDto());
        }
    }
    
    @DeleteMapping
    @ResponseBody
    public ResponseEntity<String> logoutUser(Authentication authentication) {
        String uid = authentication.getName();
        boolean success = userService.logoutUser(uid);
        if(success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
