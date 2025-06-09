package com.shujinko.project.repository.user;

import com.shujinko.project.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    public User findByUid(String uid);
    public User findByRefreshToken(String refreshToken);
}