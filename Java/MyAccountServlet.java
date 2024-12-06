package com.mygenesis.components.core.servlets;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mygenesis.components.core.services.CovisintConfigService;
import com.mygenesis.components.core.services.DecodedPdfServices;
import com.mygenesis.components.core.services.GlobalConfigService;
import com.mygenesis.components.core.services.MyAccountServices;
import com.mygenesis.components.core.services.RecaptchaValidationService;
import com.mygenesis.components.core.services.RequestSecurityService;
import com.mygenesis.components.core.services.ServiceConstants;
import com.mygenesis.components.core.utils.CommonUtils;
import com.mygenesis.components.core.utils.ConnectionUtils;
import com.mygenesis.components.core.utils.CookieUtil;
import com.mygenesis.components.core.utils.Encryption;
import com.mygenesis.components.core.utils.LinkUtils;

@SlingServlet(paths = { "/bin/common/MyAccountServlet" }, metatype = true, extensions = {
                "json" }, label = "My Account Service Servlet")
@Properties({
                @Property(name = "service.pid", value = "com.mygenesis.components.core.servlets", propertyPrivate = false),
                @Property(name = "service.description", value = "Connects to Covisint REST API", propertyPrivate = false),
                @Property(name = "service.vendor", value = "Wipro Technologies", propertyPrivate = false),
                @Property(name = "label", value = "connectRestAPI") })

public class MyAccountServlet extends SlingAllMethodsServlet {
        private static final long serialVersionUID = 1L;

        private static final Logger log = LoggerFactory.getLogger(MyAccountServlet.class);

        @Reference
        MyAccountServices myaccountservices;

        @Reference
        DecodedPdfServices pdfservice;

        @Reference
        private RequestSecurityService reqService;

        @Reference
        protected ResourceResolverFactory resourceResolverFactory;
        
        @Reference
    	public CovisintConfigService covisintConfigService;
        
        @Reference
    	public RecaptchaValidationService recaptchaValidationService;
        
        @Reference
    	private GlobalConfigService globalConfig;

        @Override
        protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {
                try {
                        // Fetch the request parameters from the request

                        String vin = request.getParameter("vin");
                        String username = request.getParameter("username");
                        String token = request.getParameter("token");
                        String service = request.getParameter("service");
                        String domainSelector = StringUtils.EMPTY;
                        String completeURL = request.getParameter(ServiceConstants.URL);

                        // log.debug("VINNNN:" + vin);

                        // log.debug("MyAccountServlet Service Called::" + service);
                        // log.debug("Current page url:" + completeURL);

                        /*--------- Domain value extracted from URL--------- */
                        if (null != completeURL) {
                                domainSelector = ConnectionUtils.domainExtractor(completeURL);
                        }

                        // log.debug("Domain Value ::" + domainSelector);

                        if (service.equals("myaccountservices")) {
                                String password = StringUtils.EMPTY;
                                log.debug("Inside myaccountservices::" + domainSelector);
                                String firstname = request.getParameter("firstname");
                                String lastname = request.getParameter("lastname");
                                String emailaddress = request.getParameter("emailaddress");
                                String securityquestion = request.getParameter("security_question");
                                String securityanswer = request.getParameter("security_answer");
                                String pass_word = request.getParameter("pass_word");
                                boolean validPassword = isvalidPassword(pass_word);
                                if (validPassword) {
                                        password = pass_word;
                                } else {
                                        password = "";
                                }
                                String city = request.getParameter("newusercity");
                                String region = request.getParameter("newuserstate");
                                String zipcode = request.getParameter("zipcode");
                                String flag = request.getParameter("rewardsFlag");
                                String gRecaptchaResponse = request.getParameter("captcharesponse");
                                
								boolean isvalidCaptcha = true;

								if (covisintConfigService.getReCaptchaEnabled().equalsIgnoreCase("true")) {
									isvalidCaptcha = recaptchaValidationService.validate(request,
											gRecaptchaResponse, completeURL);

								}
                                if (!StringUtils.isEmpty(password)) {
                                	if(isvalidCaptcha) {
                                        JSONObject services = myaccountservices.registrationAEM(firstname, lastname,
                                                        emailaddress, password,
                                                        zipcode, city, region, securityquestion, securityanswer, flag,
                                                        domainSelector);

                                        response.getWriter().write(services.toString());
                                	}else {
                                		response.setContentType("application/json");
                    					response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                    							ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                    					response.getWriter().write("Please verify that you are a robot.");
                                	}
								} else {

									response.getWriter()
											.write("Your password doesn't match the criteria. Please try again later.");
								}

							}

                        // code for change_password

						if (service.equals("changePassword")) {
							String password = StringUtils.EMPTY;
							String oldPassword = request.getParameter("oldPassword");
							String newPassword = request.getParameter("newPassword");
							boolean validPassword = isvalidPassword(newPassword);
							if (validPassword) {
								password = newPassword;
							} else {
								password = "";
							}

							if (!StringUtils.isEmpty(password)) {
								JSONObject changePassword = myaccountservices.changePassword(username, oldPassword,
										newPassword, domainSelector);

								response.setContentType("application/json");
								response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
										ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
								response.getWriter().write(changePassword.toString());

							} else {
								response.getWriter()
										.write("Your password doesn't match the criteria. Please try again later.");
							}

						}

                        // code for pinUpdate

                        if (service.equals("pinUpdate")) {
                                String pin = request.getParameter("pin");
                                String oldPin = request.getParameter("oldPin");
                                String questionCode = request.getParameter("questionCode");
                                String answer = request.getParameter("answer");

                                JSONObject pinUpdate = myaccountservices.pinUpdate(username, token, pin, oldPin,
                                                questionCode, answer,
                                                domainSelector);

                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                response.getWriter().write(pinUpdate.toString());
                        }

                        if (service.equals("logOutService")) {

                        		JSONObject isLogout = myaccountservices.isLogout(username, token, domainSelector);

                                Cookie tokenCookie = CookieUtil.clearCookie(request, "jwt_token");
                                Cookie snameCookie = CookieUtil.clearCookie(request, "s_name");
                                Cookie isValidVehCookie = CookieUtil.clearCookie(request, "isValidatedVehicle");

                                response.setContentType("text/html");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                if (tokenCookie != null) {
                                        response.addCookie(tokenCookie);
                                }
                                if (snameCookie != null) {
                                        response.addCookie(snameCookie);
                                }
                                if (isValidVehCookie != null) {
                                        response.addCookie(isValidVehCookie);
                                }

                                String homepage = LinkUtils.getMappedLink(reqService.getLighthomepageHyundaiUri());

                                // response.sendRedirect(homepage);
                                response.getWriter().write("User have been logged out.");
                        }

                        if (service.equals("signoutFromSQA")) {

                                Cookie tokenCookie = CookieUtil.clearCookie(request, "jwt_token");
                                Cookie snameCookie = CookieUtil.clearCookie(request, "s_name");
                                Cookie isValidVehCookie = CookieUtil.clearCookie(request, "isValidatedVehicle");
                                // Cookie visitoridCookie = CookieUtil.clearCookie(request, "visitorid");
                                // Cookie MyHLoginCookie = CookieUtil.clearCookie(request, "MyHyundaiLogin");

                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                if (tokenCookie != null) {
                                        response.addCookie(tokenCookie);
                                }
                                if (snameCookie != null) {
                                        response.addCookie(snameCookie);
                                }
                                if (isValidVehCookie != null) {
                                        response.addCookie(isValidVehCookie);
                                }
                                // response.addCookie(visitoridCookie);
                                // response.addCookie(MyHLoginCookie);
                                response.getWriter().write("Cookie clean up.");

                        }
                        // code for add emergency contact
                        if (service.equals("setEmergencyContact")) {
                                String firstName = request.getParameter("firstName");
                                String lastName = request.getParameter("lastName");
                                String relationship = request.getParameter("relationship");
                                String contactEmail = request.getParameter("contactEmail");
                                String phone1 = request.getParameter("phone1");
                                String phone1Type = request.getParameter("phone1Type");
                                String phone2 = request.getParameter("phone2");
                                String phone2Type = request.getParameter("phone2Type");

                                JSONObject services = myaccountservices.addEmergencyContact(username, token, lastName,
                                                firstName,
                                                relationship, contactEmail, phone1, phone1Type, phone2, phone2Type,
                                                domainSelector);

                                response.getWriter().write(services.toString());
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                        }

                        // code for delete emergency contact

                        if (service.equals("getEmergencyContact")) {
                                String contactId = request.getParameter("contactId");
                                String loginId = contactId;
                                String actionType = request.getParameter("actionType");
                                String lastName = request.getParameter("lastName");
                                String firstName = request.getParameter("firstName");
                                String relationship = request.getParameter("relationship");
                                String email = request.getParameter("email");
                                String phone1 = request.getParameter("phone1");
                                String phone1Type = request.getParameter("phone1Type");
                                String phone2 = request.getParameter("phone2");
                                String phone2Type = request.getParameter("phone2Type");

                                /*
                                 * String services = myaccountservices.addEmergencyContact(username, token,
                                 * loginId, contactId, actionType,
                                 * lastName, firstName, relationship, email, phone1, phone1Type, phone2,
                                 * phone2Type);
                                 */
                                /*
                                 * log.info(services);
                                 * response.getWriter().write("myaccountservices="+services);
                                 */
                        }
                        if (service.equals("getOwnerInfoDashboard")) {
                                JSONObject services = myaccountservices.getOwnerInfoDashboard(username, token, vin,
                                                domainSelector);
                                log.info("owner info dashboard Data - " + services.toString());

                                JSONArray jsondata = services.getJSONArray("RESPONSE_STRING");
                                JSONObject firstVehicle = (JSONObject) jsondata.get(0);

                                if (firstVehicle.has("veh")) {
                                        JSONObject vInfo = firstVehicle.getJSONObject("veh");
                                        ResourceResolver resourceResolver = request.getResourceResolver();

                                        String path = vInfo.getString("Images360URL");
                                        String bodytype = null;

                                        if (vInfo.has("Body")) {
                                                bodytype = vInfo.getString("Body");
                                        }

                                        String csPath = LinkUtils.getAssetPath(path, resourceResolver,
                                                        "ConnectedServices-01.png",
                                                        bodytype);
                                        services.put("ConnectedServicesDefaultURL", csPath);
                                        String dsPath = LinkUtils.getAssetPath(path, resourceResolver,
                                                        "Dashboard-01.png", bodytype);
                                        services.put("DashboardDefaultURL", dsPath);
                                        String msPath = LinkUtils.getAssetPath(path, resourceResolver,
                                                        "ManageSubscription-01.png",
                                                        bodytype);
                                        services.put("ManageSubscriptionDefaultURL", msPath);
                                        String mvhrPath = LinkUtils.getAssetPath(path, resourceResolver, "Mvhr-01.png",
                                                        bodytype);
                                        services.put("MvhrDefaultURL", mvhrPath);
                                        String myVehiclesPath = LinkUtils.getAssetPath(path, resourceResolver,
                                                        "MyVehicles-01.png",
                                                        bodytype);
                                        services.put("MyVehiclesDefaultURL", myVehiclesPath);
                                        String offCanvasPath = LinkUtils.getAssetPath(path, resourceResolver,
                                                        "OffCanvas-01.png", bodytype);
                                        services.put("OffCanvasDefaultURL", offCanvasPath);
                                        String serviceValetPath = LinkUtils.getAssetPath(path, resourceResolver,
                                                        "ServiceValet-01.png",
                                                        bodytype);
                                        services.put("ServiceValetDefaultURL", serviceValetPath);
                                        String vehicleHealthPath = LinkUtils.getAssetPath(path, resourceResolver,
                                                        "VehicleHealth-01.png",
                                                        bodytype);
                                        services.put("VehicleHealthDefaultURL", vehicleHealthPath);
                                        String vehicleHealthGcsPath = LinkUtils.getAssetPath(path, resourceResolver,
                                                        "VehicleHealth-02.png",
                                                        bodytype);
                                        services.put("VehicleHealthGcsDefaultURL", vehicleHealthGcsPath);

                                        /*
                                         * firstVehicle.put("veh",vInfo);
                                         * services.put("RESPONSE_STRING", firstVehicle);
                                         */

                                }
                                response.getWriter().write(services.toString());
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                        }

                        // code for get owner info
                        if (service.equals("getOwnerInfoService")) {
                                JSONObject services = myaccountservices.getOwnerInfo(username, token, domainSelector);

                                // log.debug("Data - "+services);
                                if (services.has("RESPONSE_STRING")) {
                                        JSONObject response_string = services.getJSONObject("RESPONSE_STRING");

                                        if (response_string.has("OwnersVehiclesInfo")) {
                                                JSONArray vehiclesInfo = response_string
                                                                .getJSONArray("OwnersVehiclesInfo");

                                                ResourceResolver resourceResolver = request.getResourceResolver();

                                                // vehiclesInfo.
                                                for (int i = 0; i < vehiclesInfo.length(); i++) {
                                                        JSONObject vInfo = (JSONObject) vehiclesInfo.get(i);
                                                        String path = vInfo.getString("Images360URL");
                                                        String bodytype = null;

                                                        if (vInfo.has("Body")) {
                                                                bodytype = vInfo.getString("Body");
                                                        }

                                                        String csPath = LinkUtils.getAssetPath(path, resourceResolver,
                                                                        "ConnectedServices-01.png",
                                                                        bodytype);
                                                        vInfo.put("ConnectedServicesDefaultURL", csPath);
                                                        String dsPath = LinkUtils.getAssetPath(path, resourceResolver,
                                                                        "Dashboard-01.png",
                                                                        bodytype);
                                                        vInfo.put("DashboardDefaultURL", dsPath);
                                                        String msPath = LinkUtils.getAssetPath(path, resourceResolver,
                                                                        "ManageSubscription-01.png",
                                                                        bodytype);
                                                        vInfo.put("ManageSubscriptionDefaultURL", msPath);
                                                        String mvhrPath = LinkUtils.getAssetPath(path, resourceResolver,
                                                                        "Mvhr-01.png", bodytype);
                                                        vInfo.put("MvhrDefaultURL", mvhrPath);
                                                        String myVehiclesPath = LinkUtils.getAssetPath(path,
                                                                        resourceResolver, "MyVehicles-01.png",
                                                                        bodytype);
                                                        vInfo.put("MyVehiclesDefaultURL", myVehiclesPath);
                                                        String offCanvasPath = LinkUtils.getAssetPath(path,
                                                                        resourceResolver, "OffCanvas-01.png",
                                                                        bodytype);
                                                        vInfo.put("OffCanvasDefaultURL", offCanvasPath);
                                                        String serviceValetPath = LinkUtils.getAssetPath(path,
                                                                        resourceResolver,
                                                                        "ServiceValet-01.png", bodytype);
                                                        vInfo.put("ServiceValetDefaultURL", serviceValetPath);
                                                        String vehicleHealthPath = LinkUtils.getAssetPath(path,
                                                                        resourceResolver,
                                                                        "VehicleHealth-01.png", bodytype);
                                                        vInfo.put("VehicleHealthDefaultURL", vehicleHealthPath);
                                                        String vehicleHealthGcsPath = LinkUtils.getAssetPath(path,
                                                                        resourceResolver,
                                                                        "VehicleHealth-02.png", bodytype);
                                                        vInfo.put("VehicleHealthGcsDefaultURL", vehicleHealthGcsPath);
                                                        vehiclesInfo.put(i, vInfo);
                                                }

                                                response_string.put("OwnersVehiclesInfo", vehiclesInfo);

                                                services.put("RESPONSE_STRING", response_string);

                                        }

                                }
                                response.getWriter().write(services.toString());
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);

                        }

                        // code for get owner multiple vehicles info
                        if (service.equals("getOwnersVehiclesInfoService")) {
                                JSONObject services = myaccountservices.getOwnersVehiclesInfo(username, token,
                                                domainSelector);
                                // log.debug("Data - "+services);

                                if (services.has("OwnerInfo")) {
                                        JSONObject ownerinfo = services.getJSONObject("OwnerInfo");
                                        if (ownerinfo.has("OwnersVehiclesInfo")) {
                                                JSONArray vehiclesInfo = ownerinfo.getJSONArray("OwnersVehiclesInfo");
                                                ResourceResolver resourceResolver = request.getResourceResolver();

                                                // vehiclesInfo.
                                                for (int i = 0; i < vehiclesInfo.length(); i++) {
                                                        JSONObject vInfo = (JSONObject) vehiclesInfo.get(i);
                                                        String path = vInfo.getString("Images360URL");
                                                        String bodytype = null;

                                                        if (vInfo.has("Body")) {
                                                                bodytype = vInfo.getString("Body");
                                                        }
                                                        String csPath = LinkUtils.getAssetPath(path, resourceResolver,
                                                                        "ConnectedServices-01.png",
                                                                        bodytype);
                                                        vInfo.put("ConnectedServicesDefaultURL", csPath);
                                                        String dsPath = LinkUtils.getAssetPath(path, resourceResolver,
                                                                        "Dashboard-01.png",
                                                                        bodytype);
                                                        vInfo.put("DashboardDefaultURL", dsPath);
                                                        String msPath = LinkUtils.getAssetPath(path, resourceResolver,
                                                                        "ManageSubscription-01.png",
                                                                        bodytype);
                                                        vInfo.put("ManageSubscriptionDefaultURL", msPath);
                                                        String mvhrPath = LinkUtils.getAssetPath(path, resourceResolver,
                                                                        "Mvhr-01.png", bodytype);
                                                        vInfo.put("MvhrDefaultURL", mvhrPath);
                                                        String myVehiclesPath = LinkUtils.getAssetPath(path,
                                                                        resourceResolver, "MyVehicles-01.png",
                                                                        bodytype);
                                                        vInfo.put("MyVehiclesDefaultURL", myVehiclesPath);
                                                        String offCanvasPath = LinkUtils.getAssetPath(path,
                                                                        resourceResolver, "OffCanvas-01.png",
                                                                        bodytype);
                                                        vInfo.put("OffCanvasDefaultURL", offCanvasPath);
                                                        String serviceValetPath = LinkUtils.getAssetPath(path,
                                                                        resourceResolver,
                                                                        "ServiceValet-01.png", bodytype);
                                                        vInfo.put("ServiceValetDefaultURL", serviceValetPath);
                                                        String vehicleHealthPath = LinkUtils.getAssetPath(path,
                                                                        resourceResolver,
                                                                        "VehicleHealth-01.png", bodytype);
                                                        vInfo.put("VehicleHealthDefaultURL", vehicleHealthPath);
                                                        // String vehicleHealthGcsPath =
                                                        // LinkUtils.getAssetPath(path,resourceResolver,"VehicleHealth-02.png");
                                                        // vInfo.put("VehicleHealthGcsDefaultURL",
                                                        // vehicleHealthGcsPath);
                                                        vehiclesInfo.put(i, vInfo);
                                                }

                                                ownerinfo.put("OwnersVehiclesInfo", vehiclesInfo);
                                                services.put("OwnerInfo", ownerinfo);

                                        }
                                }

                                /*
                                 * // create MyHyundaiLogin cookie
                                 * if(services.has("OwnerInfo") &&
                                 * services.getJSONObject("OwnerInfo").has("OwnerProfileInfo")){
                                 * String login =
                                 * services.getJSONObject("OwnerInfo").getJSONArray("OwnerProfileInfo").
                                 * getJSONObject(0).getString("Login");
                                 * String firstname =
                                 * services.getJSONObject("OwnerInfo").getJSONArray("OwnerProfileInfo").
                                 * getJSONObject(0).getString("FirstName");
                                 * Cookie MyHyundaiLogin = new Cookie("MyHyundaiLogin",login+"|"+firstname);
                                 * MyHyundaiLogin.setPath(";Path=/;HttpOnly;");
                                 * MyHyundaiLogin.setDomain(".hyundaiusa.com");
                                 * if(request.isSecure()){
                                 * MyHyundaiLogin.setSecure(true);
                                 * }
                                 * response.addCookie(MyHyundaiLogin);
                                 * }
                                 */

                                response.getWriter().write(services.toString());
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);

                        }

                        // code for getVehiclePurchaseHistory

                        if (service.equals("getVehiclePrHistory")) {

                                String ownerId = request.getParameter("ownerid");
                                String userName = request.getParameter("ssnUserName");
                                String snToken = request.getParameter("ssnToken");

                                JSONObject services = myaccountservices.getVehiclePurchaseHistory(username, token,
                                                ownerId,
                                                domainSelector);

                                response.getWriter().write(services.toString());
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);

                        }

                        // code for setVehiclePurchaseStatus
                        if (service.equals("setVehiclePurStatus")) {

                                String VehicleStatus = request.getParameter("VehicleStatus");
                                String ownerId = request.getParameter("ownerId");

                                String services = myaccountservices.updateVehiclePurchaseStatus(username, token,
                                                ownerId, vin,
                                                VehicleStatus, domainSelector);

                                response.getWriter().write("myaccountservices=" + services);
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                        }

                        // code for validate vehicle by vin with-out user info
                        if (service.equals("isValidVin")) {
                                String ownerId = request.getParameter("ownerId");
                                String from = request.getParameter("from");
                                JSONObject isValidVinResult = myaccountservices.isValidVin(vin, from);
                                response.getWriter().write(isValidVinResult.toString());
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                        }

                        // code for validate vehicle by vin

                        if (service.equals("validateVehicleByVIN")) {

                                String ownerId = request.getParameter("ownerId");
                                JSONObject validateVehicleByVinResult = myaccountservices.validateVehicleByVIN(username,
                                                token, vin,
                                                ownerId, domainSelector);

                                Cookie cookie = new Cookie("isValidatedVehicle", "false");
                                if (validateVehicleByVinResult.getString("E_IFRESULT").equals("Z:Success")) {
                                        if (!validateVehicleByVinResult.getJSONObject("RESPONSE_STRING")
                                                        .getBoolean("@IsGenesis")) {
                                                cookie = new Cookie("isValidatedVehicle", "true");
                                        }
                                }
                                // cookie.setSecure(true);
                                cookie.setPath(";Path=/;HttpOnly;");

                                if (request.isSecure()) {
                                        cookie.setSecure(true);
                                }

                                response.addCookie(cookie);
                                response.getWriter().write(validateVehicleByVinResult.toString());
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);

                        }

                        // code for update vehicle nick name

                        if (service.equals("setVehicleNickname")) {
                                String regid = request.getParameter("regid");
                                String idmd = request.getParameter("idmd");

                                String nickName = request.getParameter("nickName");
                                if (nickName != null) {
                                        nickName = nickName.replaceAll("\\<.*?\\>", "");
                                        nickName = nickName.replaceAll("[^a-zA-Z0-9]", " ");
                                }
                                JSONObject result = myaccountservices.updateVehicleNickname(token, idmd, vin, regid,
                                                nickName, username,
                                                domainSelector);
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                response.getWriter().write(result.toString());

                        }

                        // code for remove vehicle on myaccount page

                        if (service.equals("removeVehicle")) {

                                String input = request.getParameter("userparam");
                                JSONObject userParam = new JSONObject(input);
                                JSONObject result = myaccountservices.removeVehicle(username, token, vin, userParam,
                                                domainSelector);
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                response.getWriter().write(result.toString());
                        }

                        // code for adding new vehicle
                        if (service.equals("addVehicle")) {

                                String input = request.getParameter("userparam");
                                JSONObject userParam = new JSONObject(input);

                                JSONObject result = myaccountservices.addVehicle(username, token, vin, userParam,
                                                domainSelector);
                                response.getWriter().write(result.toString());
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                        }
                        // code for getSecondaryDriverList
                        if (service.equals("getSecondaryDriverList")) {

                                JSONObject result = myaccountservices.getPending2ndDriverList(vin, username, token,
                                                domainSelector);
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                response.getWriter().write(result.toString());
                        }

                        // code for set2ndDriverDetails
                        if (service.equals("set2ndDriverDetails")) {
                                String driverFirstName = request.getParameter("driverFirstName");
                                String driverLastName = request.getParameter("driverLastName");
                                String driverEmail = request.getParameter("driverEmail");

                                JSONObject result = myaccountservices.set2ndDriverDetails(vin, username, token,
                                                driverLastName,
                                                driverFirstName, driverEmail, domainSelector);

                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                response.getWriter().write(result.toString());
                        }

                        // code for setFeatureSecondaryDrivers
                        if (service.equals("setFeatureSecondaryDrivers")) {

                                String idmId = request.getParameter("idmId");
                                String regdriverEmail = request.getParameter("regdriverEmail");
                                String input = request.getParameter("userparam");

                                JSONObject userParam = new JSONObject(input);

                                JSONObject result = myaccountservices.setFeatureRegSecondaryDrivers(vin, username,
                                                token, idmId,
                                                regdriverEmail, userParam, domainSelector);

                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                response.getWriter().write(result.toString());
                        }

                        // code for removeSecondaryDriver
                        if (service.equals("removeSecondaryDriver")) {

                                String idmId = request.getParameter("idmId");
                                String regdriverEmail = request.getParameter("regdriverEmail");
                                String input = request.getParameter("userparam");

                                JSONObject userParam = new JSONObject(input);

                                JSONObject result = myaccountservices.setFeatureRegSecondaryDrivers(vin, username,
                                                token, idmId,
                                                regdriverEmail, userParam, domainSelector);

                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                response.getWriter().write(result.toString());
                        }

                        // code for set default vehicle
                        if (service.equals("setDefaultVehicle")) {

                                JSONObject result = myaccountservices.setDefaultVehicle(vin, username, token,
                                                domainSelector);
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                response.getWriter().write(result.toString());
                        }

                        // code for get user profile info secondary driver

                        if (service.equals("getUFinfoSecondaryDriver")) {

                                String idmId = request.getParameter("idmd");
                                JSONObject result = myaccountservices.getUFinfoSecondaryDriver(vin, username, token,
                                                idmId,
                                                domainSelector);
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                response.getWriter().write(result.toString());
                        }

                        if (service.equals("getAddress")) {

							boolean validUsername = CommonUtils.ifUserIsValidForNewEMail(username, request, response);
							boolean isvalidCaptcha = false;
							if (validUsername) {
								String gRecaptchaResponse = request.getParameter("captcharesponse");
								if (gRecaptchaResponse != null || !gRecaptchaResponse.isEmpty()) {						
									String[] bypassUser = globalConfig.getBypassuser();

									if (Arrays.asList(bypassUser).contains(username)) {
										gRecaptchaResponse = "false";
										/*
										 * if (gRecaptchaResponse.equals("ZXNiQ1dQQ1BBa2V5MTCWP==")) {
										 * gRecaptchaResponse = "false"; }
										 */
									}

									if (covisintConfigService.getReCaptchaEnabled().equalsIgnoreCase("true")) {
										isvalidCaptcha = recaptchaValidationService.validate(request,
												gRecaptchaResponse, request.getHeader("referer"));
									}
									if (covisintConfigService.getReCaptchaEnabled().equalsIgnoreCase("false")) {
										isvalidCaptcha = true;
									}
								} else {
									isvalidCaptcha = true;
								}
								if (isvalidCaptcha) {
									JSONObject result = myaccountservices.getAddress(username, domainSelector);
									response.setContentType("application/json");
									response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
											ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
									response.getWriter().write(result.toString());
								} else if (!isvalidCaptcha) {
									response.setContentType("application/json");
									response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
											ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
									response.getWriter().write("Please verify that you are a robot.");
								}
							} else {
								response.getWriter().write("Please check with valid user");
							}
							
                        }

                        // code for get GET_MONRONEY LABEL PDF

                        if (service.equals("getMonroneyLabelPDF")) {

                                JSONObject result = myaccountservices.getMonroneyLabelPDF(vin, username, token,
                                                domainSelector);
                                String response_string = result.getString("RESPONSE_STRING");
                                // log.debug("Response String = "+ response_string);
                                if (response_string != " ") {
                                        // VehicleHealthReport report = PdfHealthReportUtils.json2HealthReport(result);
                                        /*
                                         * byte[] pdf = pdfservice.createPdfReport(response_string);
                                         * HttpServletResponse hresponse = (HttpServletResponse) response;
                                         * hresponse.reset();
                                         * hresponse.setContentType("application/pdf");
                                         * hresponse.addHeader("Content-Type", "application/pdf");
                                         * hresponse.setContentLength(pdf.length);
                                         * hresponse.addHeader("Content-Transfer-Encoding", "base64");
                                         * hresponse.setHeader("Content-disposition",
                                         * "attachment; filename='Sticker.pdf'");
                                         * ServletOutputStream output = hresponse.getOutputStream();
                                         * //FileOutputStream output = new FileOutputStream("out.pdf");
                                         * output.write(pdf);
                                         * output.flush();
                                         * output.close();
                                         */
                                        // pdf.writeTo(response.getOutputStream());

                                        response.setContentType("text/plain");
                                        response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                        ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                        response.getWriter().write(response_string.toString());
                                } else {
                                        response.setContentType("application/json");
                                        response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                        ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                        response.getWriter().write(response_string.toString());
                                }
                        }

                        if (service.equals("userHavePin")) {

                                JSONObject userPin = myaccountservices.userHavePin(username, token, domainSelector);
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                response.getWriter().write(userPin.toString());
                        }

                        if (service.equals("getDriverPermissions")) {
                                String idmid = request.getParameter("idmid");
                                String loginid = request.getParameter("loginid");
                                String ownerid = request.getParameter("ownerid");
                                String regid = request.getParameter("regid");

                                JSONObject services = myaccountservices.getDriverPermissions(username, token, vin,
                                                idmid, ownerid,
                                                loginid, regid, domainSelector);
                                response.getWriter().write(services.toString());
                        }
                        if (service.equals("set2ndDriverPermissionDetails")) {
                                String featureDetails = request.getParameter("userparam");

                                JSONParser parser = new JSONParser();
                                org.json.simple.JSONObject json = (org.json.simple.JSONObject) parser
                                                .parse(featureDetails);

                                JSONObject result = myaccountservices.put2ndDriverFeatures(json, token, vin,
                                                request.getParameter("IDMID"), request.getParameter("driverEmail"),
                                                username, domainSelector);
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                response.getWriter().write(result.toString());
                        }

                        if (service.equals("getWarrantyDate")) {
                                JSONObject result = myaccountservices.getWarrantyDate(vin, domainSelector);
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                response.getWriter().write(result.toString());
                        }
                        if (service.equals("getFinanceInfo")) {
                                String ownerId = request.getParameter("ownerid");

                                JSONObject result = myaccountservices.getFinanceInfo(vin, ownerId, domainSelector);
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                response.getWriter().write(result.toString());
                        }
                        if (service.equals("getPurchaseInfo")) {
                                String ownerId = request.getParameter("ownerid");

                                JSONObject result = myaccountservices.getPurchaseInfo(vin, ownerId, domainSelector);
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                response.getWriter().write(result.toString());
                        }
                        /*--------code for MyH Secondary Permissions---------*/
                        if (service.equals("getPermissionDetailsMyH")) {

                                String gen = request.getParameter("Gen");
                                JSONObject result = myaccountservices.getRoleBasedPermissionDetails(vin, username,
                                                token, gen,
                                                domainSelector);
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                response.getWriter().write(result.toString());

                        }

                        /*--------code BLODS_2NDRY_DRIVER_INVITAION_SEND---------*/
                        if (service.equals("invitationBlods")) {
                        		String featureDetails = request.getParameter("userparam");
                        		String ownerid = request.getParameter("owner"); 
                                String idmid = request.getParameter("IDMID");
                                String regId = request.getParameter("regId");
                                String year = request.getParameter("year");
                                String model = request.getParameter("model");
                                String ssntoken = request.getParameter("token");
                                String vinFromDecrptedData = request.getParameter("vin");
                                String userNameFromDecryptedData = request.getParameter("username");
                                String completeURLFromDecryptedData = request.getParameter(ServiceConstants.URL);
                                String domainSelectorFromDecryptedData = ConnectionUtils.domainExtractor(completeURLFromDecryptedData);
                                if (year == null) {
                                        year = "";
                                }
                                if (model == null) {
                                        model = "";
                                }
								JSONParser parser = new JSONParser();
								org.json.simple.JSONObject jsonDetails = (org.json.simple.JSONObject) parser
										.parse(featureDetails);
									JSONObject result = myaccountservices.saveDriverPermissionBlods(jsonDetails, ssntoken,
											vinFromDecrptedData, idmid, ownerid, regId, userNameFromDecryptedData, domainSelectorFromDecryptedData, year, model);
									response.setContentType("application/json");
									response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL, ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
									response.getWriter().write(result.toString());
									

                        }
                        /*--------code for MyH get Secondary Permissions---------*/
                        if (service.equals("getPermissionDetailsforMyH")) {
                                String idmid = request.getParameter("IDMID");
                                String regemail = request.getParameter("driverEmail");
                                JSONObject result = myaccountservices.getPermissionsForHyundai(username, vin, idmid,
                                                token, regemail,
                                                domainSelector);
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                response.getWriter().write(result.toString());

                        }
                        if (service.equals("CPOPDF")) {
                                JSONObject result = myaccountservices.getCPOpdf(username, vin, token, domainSelector);
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                response.getWriter().write(result.toString());

                        }
                        if (service.equals("getpinstatus")) {
                                JSONObject result = myaccountservices.getpinstatus(username, token, domainSelector);
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                response.getWriter().write(result.toString());

                        }
                        if (service.equals("putupdatewelcome")) {
                                JSONObject result = myaccountservices.putupdatewelcome(token, username, domainSelector);
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                response.getWriter().write(result.toString());

                        }
                        if (service.equals("isOwner")) {
                                JSONObject result = myaccountservices.isOwner(username, domainSelector);
                                response.setContentType("application/json");
                                response.setHeader(ServiceConstants.COVISINT_CACHE_CONTROL,
                                                ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
                                response.getWriter().write(result.toString());

                        }

                } catch (Exception e) {
                        // log.error("Servlet Exception " + e.getMessage());
                }

        }

        public boolean isvalidPassword(String pass_word) {
                if (pass_word != null) {
                        String regex = "^(?=.*[0-9@#$%!-_+)])" + "(?=.*[a-zA-Z])" + "(?!.*[~`^&*/(=[{}]:\\\\<>.?])"
                                        + ".{8,32}$";
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(pass_word);
                        return matcher.matches();

                } else {
					return false;
				}
			}

		}