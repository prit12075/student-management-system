package com.university.sms.controller;

import com.university.sms.core.collections.SeatingPlan;
import com.university.sms.core.risk.RiskReport;
import com.university.sms.entity.User;
import com.university.sms.service.SmartCampusService;
import com.university.sms.service.UniversityErpService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLException;
import java.util.List;

/**
 * Admin "Smart Campus" page: the four novel features
 * (Risk Radar - Prit, Marksheet Verification - Anjay, Alert Engine - Vivekanand, Exam Seating - Arindam).
 */
@Controller
@RequestMapping("/admin/smart")
public class SmartCampusController {

    private final SmartCampusService smart;
    private final UniversityErpService erpService;

    public SmartCampusController(SmartCampusService smart, UniversityErpService erpService) {
        this.smart = smart;
        this.erpService = erpService;
    }

    @ModelAttribute
    public void common(Authentication authentication, Model model) {
        if (authentication != null) {
            User admin = erpService.getUserByUsername(authentication.getName());
            model.addAttribute("currentUser", admin);
        }
        model.addAttribute("activePage", "smart");
    }

    @GetMapping
    public String page(@RequestParam(defaultValue = "risk") String tab, Model model) {
        model.addAttribute("tab", tab);
        List<RiskReport> reports = smart.riskRadar();
        model.addAttribute("risk", reports);
        model.addAttribute("riskSummary", smart.riskSummary(reports));
        model.addAttribute("riskFactors", smart.riskFactorNames());
        model.addAttribute("scheduler", smart.getAlertScheduler());
        model.addAttribute("recentNotifications", smart.recentNotifications());
        model.addAttribute("notificationsSent", smart.notificationsSent());
        model.addAttribute("plan", smart.getLastPlan());
        model.addAttribute("students", erpService.getAllStudents());
        return "admin/smart";
    }

    @PostMapping("/seating")
    public String seating(@RequestParam(defaultValue = "End-Semester Examination Nov 2026") String examName,
                          @RequestParam(defaultValue = "2") int rooms,
                          @RequestParam(defaultValue = "3") int rows,
                          @RequestParam(defaultValue = "4") int cols,
                          Authentication auth, RedirectAttributes ra) {
        try {
            SeatingPlan plan = smart.generateSeating(examName, clamp(rooms, 1, 10), clamp(rows, 1, 12), clamp(cols, 1, 12), auth.getName());
            String msg = plan.seated() + " students seated, " + plan.sameDepartmentNeighbours()
                    + " same-department neighbours, plan saved to SQLite and every student notified.";
            if (plan.unseated().isEmpty()) {
                ra.addFlashAttribute("successMessage", msg);
            } else {
                ra.addFlashAttribute("errorMessage", msg + " Not enough seats for: " + plan.unseated());
            }
        } catch (SQLException e) {
            ra.addFlashAttribute("errorMessage", "Could not save the seating plan: " + e.getMessage());
        }
        return "redirect:/admin/smart?tab=seating";
    }

    @PostMapping("/alerts/scan")
    public String scan(RedirectAttributes ra) {
        int n = smart.runAlertScanNow();
        ra.addFlashAttribute("successMessage", "Scan finished: " + n + " new alert(s) delivered to student inboxes.");
        return "redirect:/admin/smart?tab=alerts";
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
