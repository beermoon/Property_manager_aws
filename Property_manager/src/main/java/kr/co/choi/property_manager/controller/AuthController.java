package kr.co.choi.property_manager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 로그인 화면 라우팅.
 *
 * <p>Spring Security의 {@code formLogin().loginPage("/login")}을 사용하면
 * "/login" GET을 직접 처리해 폼 화면을 보여줘야 한다(Spring이 기본 폼을 제공해주지 않음).
 */
@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
