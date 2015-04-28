package gr.com.ist.commun.web.controller;

import static gr.com.ist.commun.utils.SecurityUtils.getCurrentUser;
import gr.com.ist.commun.core.domain.security.AuthorityRole;
import gr.com.ist.commun.core.domain.security.User;
import gr.com.ist.commun.core.domain.security.UserProfile;
import gr.com.ist.commun.core.repository.security.AuthoritiesRepository;
import gr.com.ist.commun.core.repository.security.UsersRepository;
import gr.com.ist.commun.core.validation.CompositeValidator;
import gr.com.ist.commun.core.validation.ValidationErrorsException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SecurityController {
    
    @Autowired
    UsersRepository usersRepository;
    
    @Autowired
    CompositeValidator validator;
   
    @ResponseBody
    @RequestMapping(value="/security/userDetails", method = RequestMethod.GET, produces = {
    "application/json" })
    public User getUserDetails() {
        return getCurrentUser();
    }
    
    @Autowired
    private AuthoritiesRepository authoritiesRepository;
    
    @RequestMapping(value="/authorities/query", produces = {MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Page<AuthorityRole>> searchByKeyword(@RequestParam(value="q", required=false) String term, Pageable pageable) {
        return new ResponseEntity<>(authoritiesRepository.searchTerm(term, pageable), HttpStatus.OK);
    }
    
    @ResponseBody
    @RequestMapping(value="/security/userProfile", method = RequestMethod.GET, produces = {
    "application/json" })
    public UserProfile getUserProfile() {
        User user = getCurrentUser();
        UserProfile userProfile = new UserProfile();
        userProfile.setAvatar(user.getAvatar());
        userProfile.setEmail(user.getEmail());
        userProfile.setUsername(user.getUsername());
        userProfile.setPassword(user.getPassword());
        userProfile.setFirstName(user.getFirstName());
        userProfile.setLastName(user.getLastName());
        userProfile.setFullName(user.getFullName());
        return userProfile;
    }
    
    @ResponseBody
    @RequestMapping(value="/security/userProfile", method = RequestMethod.PUT, consumes = {
    "application/json" })
    public void updateUserProfile(@RequestBody UserProfile userProfile, BindingResult result) {
        User user = getCurrentUser();
        user.setAvatar(userProfile.getAvatar());
        user.setEmail(userProfile.getEmail());
        user.setUsername(userProfile.getUsername());
        user.setPassword(userProfile.getPassword());
        user.setFirstName(userProfile.getFirstName());
        user.setLastName(userProfile.getLastName());
        user.setFullName(userProfile.getFullName());
        validator.validate(user, result);
        if (result.hasErrors()) {
            throw new ValidationErrorsException(result, result);
        }
        usersRepository.save(user);
    }
}
