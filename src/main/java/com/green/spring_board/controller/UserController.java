package com.green.spring_board.controller;

import com.green.spring_board.ResourceConflictException;
import com.green.spring_board.dto.SignupRequest;
import com.green.spring_board.exceptions.UserRequestException;
import com.green.spring_board.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping ("/api/user")
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<Void> signup(@RequestBody SignupRequest signupRequest) {
        try {
            userService.signup(signupRequest);
            return ResponseEntity.ok().build();
        }catch (ResourceConflictException e){
            // DB에 중복된 값이 이미 있을 때 (409 conflict code)
            // 409 code는 별도 메서드를 지원하지 않아 직접 정해줌
            return ResponseEntity.status(409).build();
        }catch (UserRequestException e){
            return ResponseEntity.badRequest().build();
        }catch (Exception e){
            return ResponseEntity.internalServerError().build();
        }
    }
}
