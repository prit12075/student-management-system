package com.university.sms.controller;

import com.university.sms.core.exception.AttendanceShortageException;
import com.university.sms.core.exception.RecordNotFoundException;
import com.university.sms.core.exception.SmsException;
import com.university.sms.entity.Student;
import com.university.sms.service.UniversityErpService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * UNIT 2 - Centralised exception handling (Anjay).
 *
 * <p>Instead of try/catch in every controller method, exceptions thrown anywhere in a request
 * travel up the call stack (exception propagation) and are caught here. Each
 * {@link ExceptionHandler} method handles one exception type and decides what the user sees.
 * Only the project's own exceptions are handled, so Spring Security's access-denied flow is
 * untouched.</p>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final UniversityErpService erpService;

    public GlobalExceptionHandler(UniversityErpService erpService) {
        this.erpService = erpService;
    }

    @ExceptionHandler(AttendanceShortageException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAttendanceShortage(AttendanceShortageException ex, Model model, Principal principal) {
        log.info("Hall ticket refused: {}", ex.toString());
        model.addAttribute("exception", ex);
        if (principal != null) {
            // @ModelAttribute methods of StudentController do not run for advice handlers,
            // so the sidebar/top-bar data is added here.
            Student student = erpService.getStudentByUsername(principal.getName());
            model.addAttribute("currentStudent", student);
            model.addAttribute("currentUser", student.getUser());
        }
        model.addAttribute("activePage", "hallticket");
        return "error/attendance-shortage";
    }

    @ExceptionHandler(RecordNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(RecordNotFoundException ex, Model model, HttpServletRequest request) {
        model.addAttribute("title", "Record not found");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("path", request.getRequestURI());
        return "error/app-error";
    }

    @ExceptionHandler(SmsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleSmsException(SmsException ex, Model model, HttpServletRequest request) {
        model.addAttribute("title", "Request could not be completed (" + ex.getErrorCode() + ")");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("path", request.getRequestURI());
        return "error/app-error";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleUploadTooLarge(RedirectAttributes redirect) {
        redirect.addFlashAttribute("errorMessage", "Uploaded file is too large (limit 2 MB).");
        return "redirect:/admin/java-lab?tab=io";
    }
}
