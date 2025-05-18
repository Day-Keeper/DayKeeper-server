package com.shujinko.project.service.user;

import com.shujinko.project.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    
    UserRepository userRepository;
    
    @Autowired
    UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
//    public String join(User user) {
//        if(isNewUser(user)) {userRepository.save(user); return "success";}
//        return "fail";
//    }
    
//    private boolean isNewUser(User user) {
//        return userRepository.findByEmail(user.getGoogle()) == null;
//    }
    
}
