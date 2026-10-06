package com.green.spring_board.controller;

import com.green.spring_board.dto.MyInfoResponse;
import com.green.spring_board.dto.UserUpdateRequest;
import com.green.spring_board.exceptions.ResourceConflictException;
import com.green.spring_board.dto.LoginRequest;
import com.green.spring_board.dto.SignupRequest;
import com.green.spring_board.exceptions.ResourceNotFoundException;
import com.green.spring_board.exceptions.UnauthenticatedException;
import com.green.spring_board.exceptions.UserRequestException;
import com.green.spring_board.repository.UserRepository;
import com.green.spring_board.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping ("/api/user")
@AllArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody SignupRequest signupRequest) {
        userService.signup(signupRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest httpServletRequest
    ){
        int userId = userService.login(loginRequest);
        HttpSession session = httpServletRequest.getSession();
        httpServletRequest.changeSessionId();
        session.setAttribute("userId", userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MyInfoResponse> getCurrenUser(
            HttpServletRequest httpServletRequest
    ){
        // 1. 이 사람의 세션을 가져옴
        HttpSession session = httpServletRequest.getSession(false);

        if(session == null || session.getAttribute("userId") == null) {
            throw new UnauthenticatedException("로그인이 필요합니다");
        }

        // 2. 세션에서 유저 아이디 뽑아옴
        int userId = (int) session.getAttribute("userId");
        MyInfoResponse response = userService.getUserInfo(userId);

        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request
    ){
        HttpSession session = request.getSession(false);

        if(session == null || session.getAttribute("userId") == null){
            throw new UnauthenticatedException("로그인이 필요합니다");
        }

        session.invalidate();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/update")
    public ResponseEntity<Void> updateUserInfo(
            HttpServletRequest request,
            @Valid @RequestBody UserUpdateRequest userUpdateRequest
    ){
        // 이메일 닉네임 업데이트
        // 요청한 세션을 가져옴
        HttpSession session = request.getSession(false);

        if(session == null || session.getAttribute("userId") == null) {
            throw new UnauthenticatedException("로그인이 필요합니다");
        }

        // 세션에서 요청자의 id를 가져옴
        int userId = (int) session.getAttribute("userId");

        userService.updateUserInfo(userId, userUpdateRequest);
        return ResponseEntity.ok().build();
    }

    // 유저 탈퇴
    @DeleteMapping
    public ResponseEntity<Void> deleteUser (
            HttpServletRequest request
    ){
        HttpSession session = request.getSession(false);
        if(session == null || session.getAttribute("userId") == null) {
            throw new UnauthenticatedException("로그인이 필요합니다");
        }
        int userId = (int) session.getAttribute("userId");

        // 1. DB삭제
        userService.deleteUser(userId);
        // 2. 세션 비활성화
        session.invalidate();

        return ResponseEntity.noContent().build();
    }
}
