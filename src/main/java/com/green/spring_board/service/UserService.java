package com.green.spring_board.service;

import com.green.spring_board.dto.MyInfoResponse;
import com.green.spring_board.dto.UserUpdateRequest;
import com.green.spring_board.exceptions.ResourceConflictException;
import com.green.spring_board.exceptions.UnauthenticatedException;
import com.green.spring_board.dto.LoginRequest;
import com.green.spring_board.dto.SignupRequest;
import com.green.spring_board.entity.User;
import com.green.spring_board.exceptions.ResourceNotFoundException;
import com.green.spring_board.exceptions.UserRequestException;
import com.green.spring_board.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.Optional;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void signup(SignupRequest signupRequest) {
        // 이메일과 비밀번호가 공백이 아닌지 확인
        if(signupRequest.getEmail().isBlank()
                ||signupRequest.getPassword().isBlank()) {
            throw new UserRequestException("Email or password cannot be blank");
        }
        // 이메일이 사용중인지 확인
        if(userRepository.existsByEmail(signupRequest.getEmail())){
            throw new ResourceConflictException("Email already exists");
        }

        // 비밀번호 해싱
        String hashedPassword = passwordEncoder.encode(signupRequest.getPassword());


        // db save
        User user = new User();
        user.setEmail(signupRequest.getEmail());
        user.setPassword(hashedPassword);
        user.setNickname(signupRequest.getNickname());
        userRepository.save(user);
    }
    public int login(LoginRequest loginRequest) {
        // 1. 이메일 존재하는건지 확인
        Optional<User> userOptional = userRepository.findAllByEmail(loginRequest.getEmail());

        if(userOptional.isEmpty()){
            throw new ResourceNotFoundException("User not found");
        }

        User user = userOptional.get(); // 이 이메일 사용자 정보

        // 2. 비밀번호가 올바른지 확인
        if(!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new UnauthenticatedException("Wrong password");
        }

        // 3. 로그인 성공
        return user.getId();
    }

    public MyInfoResponse getUserInfo(int userId){
        Optional<User> userOptional = userRepository.findById(userId);
        if(userOptional.isEmpty()) {
            throw new ResourceNotFoundException("User not found");
        }
        User user = userOptional.get();

        // 3. 유저 아이디로 DB 조회함
        // 4. DB에서 이 유저의 닉네임과 이메일을 받아옴
        String email = user.getEmail();
        String nickname = user.getNickname();

        // 5. 돌려줌.
        MyInfoResponse myInfoResponse = new MyInfoResponse();
        myInfoResponse.setEmail(email);
        myInfoResponse.setNickname(nickname);

        return myInfoResponse;
    }

    // 수정
    public void updateUserInfo(int userId, UserUpdateRequest userUpdateRequest) {
        // 1. 대상 유저 조회
        Optional<User> userOptional = userRepository.findById(userId);

            // 유저를 못찾을 경우
        if (userOptional.isEmpty()) {
            throw new ResourceNotFoundException("유저를 찾을 수 없습니다.");
        }

        User user = userOptional.get();

        // 사용자가 올린 요청으로 덮어씌운다
        // 보드 했던것처럼 null 이면 수정하지 않기!
        // 2. 이메일 수정
        if (userUpdateRequest.getEmail() != null && !userUpdateRequest.getEmail().isBlank()) {
            if (!userUpdateRequest.getEmail().equals(user.getEmail())) {
                if (userRepository.existsByEmail(userUpdateRequest.getEmail())) {
                    throw new ResourceConflictException("이미 사용 중인 이메일입니다.");
                }
                user.setEmail(userUpdateRequest.getEmail());
            }
        }
        // 3. 닉네임 수정
        if (userUpdateRequest.getNickname() != null && !userUpdateRequest.getNickname().isBlank()) {
            user.setNickname(userUpdateRequest.getNickname());
        }

        // 4. DB 저장
        userRepository.save(user);
    }

    // 유저 탈퇴
    public void deleteUser(int userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if(userOptional.isEmpty()){
            throw new ResourceNotFoundException("User not found");
        }
        User user = userOptional.get();
        userRepository.delete(user);
    }
}
