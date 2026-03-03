package kr.co.choi.property_manager.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeContorller {

    @GetMapping("/")
    public String home(@RequestParam(required = false) Long focusId, Model model) {
        model.addAttribute("focusId", focusId);
        return "home";
    }


}
