package com.university.sms.controller;

import com.university.sms.service.SmartCampusService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Public marksheet verification page (Anjay - novel feature). No login needed: an employer or
 * another university types the roll number and the code printed on the marksheet PDF.
 */
@Controller
public class VerificationController {

    private final SmartCampusService smart;

    public VerificationController(SmartCampusService smart) {
        this.smart = smart;
    }

    @GetMapping("/verify")
    public String verify(@RequestParam(required = false) String roll,
                         @RequestParam(required = false) String code, Model model) {
        model.addAttribute("roll", roll);
        model.addAttribute("code", code);
        if (roll != null && !roll.isBlank() && code != null && !code.isBlank()) {
            model.addAttribute("result", smart.verify(roll, code));
        }
        return "verify";
    }
}
