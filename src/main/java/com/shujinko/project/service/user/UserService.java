package com.shujinko.project.service.user;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.auth.oauth2.AccessToken;
import com.shujinko.project.domain.dto.user.UserDto;
import com.shujinko.project.domain.dto.user.UserPartialUpdateDto;
import com.shujinko.project.domain.entity.user.User;
import com.shujinko.project.repository.RefreshTokenRepository;
import com.shujinko.project.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class UserService {
    
    UserRepository userRepository;
    RefreshTokenRepository refreshTokenRepository;
    Logger logger = LoggerFactory.getLogger(UserService.class);
    @Autowired
    UserService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }
    
    public User getUser(String uid) {
        User user = userRepository.findByUid(uid);
        if (user == null) {
            logger.error("getUser :User not found with uid: {}", uid);
            return null;
        }else{
            logger.info("getUser : User found with uid: {}", uid);
            return user;
        }
    }
    
    @Transactional
    public User updateUser(String uid, UserPartialUpdateDto userPartialUpdateDto) {
        User user = userRepository.findByUid(uid);
        if (user == null) {
            logger.error("updateUser : User not found with uid: {}", uid);
            return null;
        }
        if (userPartialUpdateDto.getName() != null) {
            user.setName(userPartialUpdateDto.getName());
        }
        if (userPartialUpdateDto.getBirthday() != null) {
            user.setBirthday(LocalDate.parse(userPartialUpdateDto.getBirthday()));
        }
        //JPA가 알아서 저장함.
        logger.info("updateUser : User updated with uid: {}", uid);
        return user;
    }
    
    @Transactional
    public boolean logoutUser(String uid) {
        User user = userRepository.findByUid(uid);
        if(user == null) {
            logger.error("logoutUser : User not found with uid: {}", uid);
            return false;
        }
        if(refreshTokenRepository.findByUid(uid).isEmpty()) {
            logger.error("logoutUser : Refresh token not found with uid: {}", uid);
            return false;
        }
        refreshTokenRepository.delete(refreshTokenRepository.findByUid(uid).get());
        return true;
    }
}
