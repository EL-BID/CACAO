/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.ui;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.idb.cacao.web.GenericResponse;
import org.idb.cacao.web.controllers.services.IConfigEMailService;
import org.idb.cacao.web.dto.PasswordDto;
import org.idb.cacao.web.entities.ConfigEMail;
import org.idb.cacao.web.entities.PasswordResetToken;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.repositories.PasswordResetTokenRepository;
import org.idb.cacao.web.repositories.UserRepository;
import org.idb.cacao.web.sec.ISecurityUserService;
import org.idb.cacao.web.utils.ControllerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller class for all endpoints related to user interface regarding password recovery.
 * 
 * @author Gustavo Figueiredo
 *
 */
@Controller
public class PasswordRecoveryUIController {

	private static final Logger log = Logger.getLogger(PasswordRecoveryUIController.class.getName());

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordResetTokenRepository passwordTokenRepository;

    @Autowired
    private ISecurityUserService securityUserService;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private IConfigEMailService configEmailService;

    /**
     * STEP 01 OF PASSWORD RENEWAL:<BR>
     * Generates token and send to user's e-mail.
     */
    @PostMapping("/resetPassword")
    @ResponseBody
    public GenericResponse resetPassword(HttpServletRequest request, 
      @RequestParam("email") String userEmail) {
        User user = userRepository.findByLoginIgnoreCase(userEmail);
        if (user == null) {
            throw new UserNotFoundException(userEmail);
        }
        String token = UUID.randomUUID().toString();
        createPasswordResetTokenForUser(user, token);
        mailSender.send(constructResetTokenEmail(ControllerUtils.getAppUrl(request), 
          request.getLocale(), token, user));
        log.log(Level.INFO, "Sent e-mail to "+user.getLogin()+" requesting to change password");
        return new GenericResponse(
          messageSource.getMessage("pwd.reset.password.email", null, 
          request.getLocale()));
    }

    /**
     * STEP 02 OF PASSWORD RENEWAL:<BR>
     * Validates token (user probably clicked link in e-mail) and redirects to page for new password.
     */
    @GetMapping("/updatePassword")
    public ModelAndView showChangePasswordPage(final HttpServletRequest request, final ModelMap model, 
    		@RequestParam("token") final String token, @RequestParam("messageKey" ) final Optional<String> messageKey) {
    	
        Locale locale = request.getLocale();
        model.addAttribute("lang", locale.getLanguage());

        messageKey.ifPresent( key -> {
            String message = messageSource.getMessage(key, null, locale);
            model.addAttribute("message", message);
        });

        final String result = securityUserService.validatePasswordResetToken(token);

        if(result != null) {
            model.addAttribute("messageKey", "auth.message." + result);
            return new ModelAndView("redirect:/login", model);
        } else {
            model.addAttribute("token", token);
            return new ModelAndView("login/updatePassword", model);
        }
    }

    /**
     * STEP 03 OF PASSWORD RENEWAL:<BR>
     * User submit form with new password.
     */
    @PostMapping("/savePassword")
    @ResponseBody
    public GenericResponse savePassword(final Locale locale, @Valid PasswordDto passwordDto) {

        final String result = securityUserService.validatePasswordResetToken(passwordDto.getToken());

        if(result != null) {
            return new GenericResponse(messageSource.getMessage("auth.message." + result, null, locale));
        }

        Optional<User> user = getUserByPasswordResetToken(passwordDto.getToken());
        if(user.isPresent()) {
            changeUserPassword(user.get(), passwordDto.getNewPassword());
            return new GenericResponse(messageSource.getMessage("pwd.reset.password.suc", null, locale));
        } else {
            return new GenericResponse(messageSource.getMessage("login.failed", null, locale));
        }
    }
    
    public void createPasswordResetTokenForUser(final User user, final String token) {
        final PasswordResetToken myToken = new PasswordResetToken(token, user);
        passwordTokenRepository.save(myToken);
    }
    
    public Optional<User> getUserByPasswordResetToken(final String token) {
    	PasswordResetToken prt = passwordTokenRepository.findByToken(token);
    	if (prt==null || prt.getUserId()==null)
    		return Optional.empty();
    	return userRepository.findById(prt.getUserId());
    }

    private SimpleMailMessage constructResetTokenEmail(final String contextPath, final Locale locale, final String token, final User user) {
        final String url = contextPath + "/updatePassword?token=" + token;
        final String message = messageSource.getMessage("pwd.reset.password", null, locale);
        return constructEmail("Reset Password", message + " \r\n" + url, user);
    }

    private SimpleMailMessage constructEmail(String subject, String body, User user) {
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setSubject(subject);
        email.setText(body);
        email.setTo(user.getLogin());
        ConfigEMail config = configEmailService.getActiveConfig();
        if (config!=null && config.getSupportEmail().trim().length()>0) {
        	email.setFrom(config.getSupportEmail());
        }
        return email;
    }

    public void changeUserPassword(final User user, final String password) {
        user.setPassword(new BCryptPasswordEncoder(11).encode(password));
        userRepository.saveWithTimestamp(user);
    }


}
