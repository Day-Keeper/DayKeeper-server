package com.shujinko.project.controller.user;


import com.shujinko.project.service.user.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
    public String createForm(){
        return "createMemberForm";
    }
//    @PostMapping("/members/new")
//    @ResponseBody
////    public String createNewUser(@RequestBody UserDto userDto) {
////            User user = userDto.toEntity();
////            return userService.join(user);
////    }
}
