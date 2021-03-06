package org.openmrs.module.loginapp.page.controller;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Location;
import org.openmrs.Role;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.emrapi.EmrApiConstants;
import org.openmrs.module.referenceapplication.ReferenceApplicationConstants;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.openmrs.ui.framework.page.PageRequest;
import org.openmrs.util.OpenmrsConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestParam;

public class AfyaEhmsLoginPageController {

	private static final String COOKIE_NAME_LAST_SESSION_LOCATION = "referenceapplication.lastSessionLocation";
	private static final String SESSION_ATTRIBUTE_REDIRECT_URL = "_REFERENCE_APPLICATION_REDIRECT_URL_";
	private static final String REQUEST_PARAMETER_NAME_REDIRECT_URL = "redirectUrl";
	private static final String SESSION_ATTRIBUTE_ERROR_MESSAGE = "_REFERENCE_APPLICATION_ERROR_MESSAGE_";
	private static final int UNKNOWN_LOCATION_ID = 1;
	
	protected Logger log = LoggerFactory.getLogger(AfyaEhmsLoginPageController.class);

	public String get(
			PageModel model,
			PageRequest pageRequest,
			UiUtils ui,
			@CookieValue(value = COOKIE_NAME_LAST_SESSION_LOCATION, required = false) String lastSessionLocatonId
			){
		if (Context.isAuthenticated()) {
			return "redirect:" + ui.pageLink(ReferenceApplicationConstants.MODULE_ID, "home");
		}
		
		String redirectUrl = (String) pageRequest.getRequest().getSession().getAttribute(SESSION_ATTRIBUTE_REDIRECT_URL);
		pageRequest.getRequest().getSession().removeAttribute(SESSION_ATTRIBUTE_REDIRECT_URL);
		
		model.addAttribute(REQUEST_PARAMETER_NAME_REDIRECT_URL, redirectUrl);
		
		return null;
	}

	public String post(
			@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "password", required = false) String password,
			@CookieValue(value = COOKIE_NAME_LAST_SESSION_LOCATION, required = false) String lastSessionLocationId,
			@SpringBean("locationService") LocationService locationService, 
			UiUtils ui, 
			PageRequest pageRequest,
			UiSessionContext sessionContext
			) {
		String redirectUrl = pageRequest.getRequest().getParameter(REQUEST_PARAMETER_NAME_REDIRECT_URL);
		
		try {
			Context.authenticate(username, password);
			
			if (Context.isAuthenticated()) {
				Integer defaultLocationId = null;
				Location location = null;
				Location lastSessionLocation = null;
				try {
					String userlocationstr = OpenmrsConstants.USER_PROPERTY_DEFAULT_LOCATION;
					String defaultLocationString = Context.getAuthenticatedUser().getUserProperty(OpenmrsConstants.USER_PROPERTY_DEFAULT_LOCATION);

					if(StringUtils.isNotEmpty(lastSessionLocationId)){
						//fetch previous location from sessions
						location = locationService.getLocation(Integer.valueOf(lastSessionLocationId));
					}
					else if (StringUtils.isEmpty(defaultLocationString))
					{
						location = Context.getLocationService().getLocation(UNKNOWN_LOCATION_ID);
					}
					else{
						location = Context.getLocationService().getLocation(Integer.parseInt(defaultLocationString));
					}

					pageRequest.setCookieValue(COOKIE_NAME_LAST_SESSION_LOCATION, location.getLocationId().toString());
					sessionContext.setSessionLocation(location);

					if (StringUtils.isNotBlank(redirectUrl)) {
						//don't redirect back to the login page on success nor an external url
						if (!redirectUrl.contains("login.")) {
							if (log.isDebugEnabled())
								log.debug("Redirecting user to " + redirectUrl);

							return "redirect:" + redirectUrl;
						} else {
							if (log.isDebugEnabled())
								log.debug("Redirect contains 'login.', redirecting to home page");
						}
					}
					return "redirect:" + ui.pageLink(ReferenceApplicationConstants.MODULE_ID, "home");
				} catch (Exception e) {
					Context.logout();
					e.printStackTrace();
					pageRequest.getSession().setAttribute(SESSION_ATTRIBUTE_ERROR_MESSAGE,
							ui.message("afyaehms.error.login.fail"));
				}
			}
			
		} catch (ContextAuthenticationException e) {
			if (log.isDebugEnabled())
				log.debug("Failed to authenticate user");
			
			pageRequest.getSession().setAttribute(SESSION_ATTRIBUTE_ERROR_MESSAGE,
				ui.message("afyaehms.error.login.fail"));
		}
		
		if (log.isDebugEnabled())
			log.debug("Sending user back to login page");
		
		pageRequest.getSession().setAttribute(SESSION_ATTRIBUTE_REDIRECT_URL, redirectUrl);
		
		return "redirect:" + ui.pageLink(ReferenceApplicationConstants.MODULE_ID, "login");
	}

}
