package com.campusfix.controller;

import com.campusfix.model.AppUser;
import com.campusfix.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

import java.util.Locale;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserRepository userRepository;

    public GlobalControllerAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Makes the logged-in user available to every template as ${currentUser}. */
    @ModelAttribute("currentUser")
    public AppUser currentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return null;
        return userRepository.findByEmail(auth.getName().toLowerCase(Locale.ROOT)).orElse(null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView tooLarge() {
        ModelAndView mv = new ModelAndView("error");
        mv.setStatus(HttpStatus.PAYLOAD_TOO_LARGE);
        mv.addObject("status", 413);
        mv.addObject("error", "Image too large");
        mv.addObject("message", "The image is larger than 5 MB. Go back and choose a smaller file.");
        return mv;
    }
}
