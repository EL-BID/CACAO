/*******************************************************************************
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without 
 * restriction, including without limitation the rights to use, copy, modify, merge, publish, 
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or 
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN 
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.utils;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.idb.cacao.web.GenericResponse;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.entities.UserProfile;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

/**
 * Utility methods for Controller implementations in the scope of this web application
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ControllerUtils {

	public static final int MIN_PAGE_SIZE = 5;
	public static final int MAX_PAGE_SIZE = 100;
	public static final int MAX_LENGTH_FOR_AUTO_COMPLETE = 256;

	public static ResponseEntity<Object> returnErrors(BindingResult result, MessageSource messageSource) {
    	List<String> errors = result.getAllErrors().stream().map(e -> {
    		String msg = messageSource.getMessage(e, LocaleContextHolder.getLocale());
    		if (e instanceof FieldError)
    			return "\""+((FieldError)e).getField()+"\" "+msg;
    		else
    			return msg;
    	}).collect(Collectors.toCollection(LinkedList::new));
    	errors.add(0, messageSource.getMessage("op.failed", null, LocaleContextHolder.getLocale()));
    	return ResponseEntity.badRequest().body(String.join("\n",errors));
	}
	
	public static GenericResponse returnErrorsAsGenericResponse(BindingResult result) {
    	List<String> errors = result.getAllErrors().stream().map(e -> {
    		String msg = e.getDefaultMessage();
    		if (e instanceof FieldError)
    			return "\""+((FieldError)e).getField()+"\" "+msg;
    		else
    			return msg;
    	}).collect(Collectors.toCollection(LinkedList::new));
    	return new GenericResponse(
    			String.join("\n",errors));
	}
	
	public static ResponseEntity<Object> returnBadRequest(String message, MessageSource messageSource, Object... args) {
		return ResponseEntity.badRequest().body(Collections.singletonMap("error", 
				messageSource.getMessage(message, args, LocaleContextHolder.getLocale())));
	}
	
	public static boolean isLogged() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth instanceof AnonymousAuthenticationToken) 
    		return false;
    	return true;
	}
	
	public static boolean isSystemAdmin() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth instanceof AnonymousAuthenticationToken) 
    		return false;
    	Collection<? extends GrantedAuthority> roles = auth.getAuthorities();
    	return roles.stream().anyMatch(ga->UserProfile.SYSADMIN.getRole().equalsIgnoreCase(ga.getAuthority()));
	}

	public static boolean isOfficer() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		return false;
    	if (auth instanceof AnonymousAuthenticationToken) 
    		return false;
    	Collection<? extends GrantedAuthority> roles = auth.getAuthorities();
    	return roles.stream().anyMatch(ga->UserProfile.AUTHORITY.getRole().equalsIgnoreCase(ga.getAuthority()));
	}
	
    public static String getAppUrl(HttpServletRequest request) {
    	String protocol = null;
    	String forwarded = request.getHeader("X-Forwarded-Proto");
    	if (forwarded==null)
    		forwarded = request.getHeader("x-forwarded-proto"); // maybe case sensitive?
    	if (forwarded!=null && forwarded.trim().length()>0)
    		protocol = forwarded;
    	if (protocol==null) {
    		if (443==request.getServerPort() || request.isSecure())
    			protocol = "https";
    		else
    			protocol = "http";
    	}
        return protocol + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    }
    
    /**
     * Return detailed information about an incoming HTTP request
     */
    public static String getDebugInfo(HttpServletRequest request) {
    	StringBuilder report = new StringBuilder();
    	try {
    		report.append("INCOMING HTTP ").append(request.getMethod()).append("\n");
	    	report.append("URL: ").append(request.getRequestURI()).append("\n");
	    	if (request.getQueryString()!=null && request.getQueryString().trim().length()>0)
	    		report.append("Query String: ").append(request.getQueryString()).append("\n");
	    	report.append("REMOTE ADDR: ").append(request.getRemoteAddr()).append("\n");
	    	for (String header_name:Collections.list(request.getHeaderNames()) ) {
	    		report.append("HEADER: ").append(header_name).append(" = ").append(request.getHeader(header_name)).append("\n");
	    	}
	    	Map<String,String[]> params = request.getParameterMap();
	    	if (params!=null && !params.isEmpty()) {
	    		for (Map.Entry<String,String[]> entry: params.entrySet()) {
	    			report.append("PARAM: ").append(entry.getKey()).append(" = ").append(String.join(",", entry.getValue())).append("\n");
	    		}
	    	}
    	}
    	catch (Exception ex) {
    		// do not throw exception here
    	}
    	return report.toString();
    }
    
    /**
     * Return detailed information about an outgoing HTTP request
     */
    @SuppressWarnings("unchecked")
	public static String getDebugInfo(URI uri, HttpMethod method, HttpEntity<?> request) {
    	StringBuilder report = new StringBuilder();
    	try {
    		report.append("OUTGOING HTTP ").append(method.name()).append("\n");
	    	report.append("URL: ").append(uri).append("\n");
	    	for (Map.Entry<String,List<String>> entry:request.getHeaders().entrySet() ) {
	    		report.append("HEADER: ").append(entry.getKey()).append(" = ").append(String.join(",",entry.getValue())).append("\n");
	    	}
	    	Object params = request.getBody();
	    	if ((params instanceof MultiValueMap) && !((MultiValueMap<?,?>)params).isEmpty()) {
	    		for (Map.Entry<String,List<String>> entry: ((MultiValueMap<String,String>)params).entrySet()) {
	    			report.append("PARAM: ").append(entry.getKey()).append(" = ").append(String.join(",", entry.getValue())).append("\n");
	    		}
	    	}
	    	else if (params instanceof String) {
	    		report.append("PARAM: "+params);
	    	}
    	}
    	catch (Exception ex) {
    		// do not throw exception here
    	}
    	return report.toString();    	
    }
    
    /**
     * Return detailed information about the response from an outgoing HTTP request
     */
    public static String getDebugInfo(URI uri, HttpMethod method, ResponseEntity<Map<String,String>> response) {
    	StringBuilder report = new StringBuilder();
    	try {
    		report.append("RESPONSE FROM HTTP ").append(method.name()).append("\n");
	    	report.append("URL: ").append(uri).append("\n");
	    	report.append("STATUS CODE: ").append(response.getStatusCodeValue()).append("\n");
	    	report.append("STATUS: ").append(response.getStatusCode().getReasonPhrase()).append("\n");
	    	for (Map.Entry<String,List<String>> entry:response.getHeaders().entrySet() ) {
	    		report.append("HEADER: ").append(entry.getKey()).append(" = ").append(String.join(",",entry.getValue())).append("\n");
	    	}
	    	Map<String,String> params = response.getBody();
	    	if (params!=null && !params.isEmpty()) {
	    		for (Map.Entry<String,String> entry: params.entrySet()) {
	    			report.append("PARAM: ").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
	    		}
	    	}
    	}
    	catch (Exception ex) {
    		// do not throw exception here
    	}
    	return report.toString();    	
    }
    
    /**
     * Execute a search procedure, returning whatever it finds. 
     * Capture error related to non existent index.
     */
    public static <T> Page<T> searchPage(SomeSearchReturnsPage<T> searchCall) {
    	try {
    		return searchCall.search();
    	}
    	catch (Exception ex) {
    		if (ErrorUtils.isErrorNoIndexFound(ex) || ErrorUtils.isErrorNoMappingFoundForColumn(ex)) {
    			return Page.empty();
    		}
    		else {
    			throw ex;
    		}
    	}
    }
    
    /**
     * Any functional interace that returns a 'Page' of some type. It's used with {@link ControllerUtils#searchPage(SomeSearch) searchPage}
     */
    @FunctionalInterface
    public static interface SomeSearchReturnsPage<T> {
    	
    	public Page<T> search();
    	
    }
    
    /**
     * Execute a search procedure, returning whatever it finds. 
     * Capture error related to non existent index.
     */
    public static <T> Optional<T> searchOptional(SomeSearchReturnsOptional<T> searchCall) {
    	try {
    		return searchCall.search();
    	}
    	catch (Exception ex) {
    		if (ErrorUtils.isErrorNoIndexFound(ex) || ErrorUtils.isErrorNoMappingFoundForColumn(ex)) {
    			return Optional.empty();
    		}
    		else {
    			throw ex;
    		}
    	}
    }
    
    /**
     * Any functional interace that returns a 'Page' of some type. It's used with {@link ControllerUtils#searchPage(SomeSearch) searchPage}
     */
    @FunctionalInterface
    public static interface SomeSearchReturnsOptional<T> {
    	
    	public Optional<T> search();
    	
    }

    /**
     * Includes into model a flag indicating the user has already logged in. Useful for conditions in pages that are displayed
     * differently in both logged and non-logged areas.
     */
    public static void tagLoggedArea(Model model) {
    	try {
	    	if (ControllerUtils.isLogged()) {
	    		model.addAttribute("logged_area", Boolean.TRUE);
	    	}
    	}
	    catch (Exception ex) {	    	
	    }
    }

    /**
     * Returns the provided page size, if the first parameter is present, and temporarily stores this information in User object.<BR>
     * If the information was not provided, verifies if we have stored this information in User object (temporarily) and returns it.<BR>
     * Otherwise, return the application default page size.
     */
	public static int getPageSizeForUser(Optional<Integer> pageSize, Environment env) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = (auth==null) ? null : UserUtils.getUser(auth);
		if (user!=null) {
			if (pageSize.isPresent()) {
				user.setPageSize(pageSize.get());
				return pageSize.get();
			}
			Integer psize = user.getPageSize();
			if (psize!=null)
				return Math.min(MAX_PAGE_SIZE, Math.max(psize, MIN_PAGE_SIZE));
		}
		if (pageSize.isPresent()) {
			return pageSize.get();
		}
		return Integer.parseInt(env.getProperty("default.page.size"));
	}

	/**
	 * Check if it's running inside JUnit test
	 */
	public static boolean isJUnitTest() {  
	  for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
	    if (element.getClassName().startsWith("org.junit.")) {
	      return true;
	    }           
	  }
	  return false;
	}
	
	/**
	 * Check for the presence of 'Mocked Elastic Search' component in runtime
	 */
	public static boolean hasMockES() {
		return System.getProperty("MOCKED_ELASTIC_SEARCH")!=null;
	}

	/**
	 * Try to reduce the potential risk related to using an user entry for 'auto complete' purpose.<BR>
	 * Reduce the size if the entry is very big.<BR>
	 * Removes some special characters (such as parenthesis, brackets, dashes, quotes, etc.).
	 */
	public static String treatTermForAutoComplete(String term) {
		if (term==null || term.length()==0)
			return term;
		term = term.trim();
		if (term.length()>MAX_LENGTH_FOR_AUTO_COMPLETE)
			term = term.substring(0, MAX_LENGTH_FOR_AUTO_COMPLETE);		
		term = term.replaceAll("[\\-\\/\\\\<>\\(\\)\\{\\}\\[\\]\"'\\:]", ".");
		return term;
	}
}
