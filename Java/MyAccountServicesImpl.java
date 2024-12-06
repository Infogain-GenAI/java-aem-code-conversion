package com.mygenesis.components.core.services.impl;

import com.adobe.granite.crypto.CryptoSupport;
import com.mygenesis.components.core.services.ConfigExtractor;
import com.mygenesis.components.core.services.CovisintConfigService;
import com.mygenesis.components.core.services.MyAccountServices;
import com.mygenesis.components.core.services.ServiceConstants;
import com.mygenesis.components.core.utils.ConnectionUtils;
import com.mygenesis.components.core.utils.GenesisConstants;
import com.mygenesis.components.core.utils.LinkUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;


@Component(label = "Covisint MyAccount Service", description = "Covisint MyAccount Service", metatype = true, immediate = true)
@Service({MyAccountServices.class})
public class MyAccountServicesImpl implements MyAccountServices {

	private static final String FEATURE_BY_ROLE = "FeatureByRole";
	private static final String GEN = "GEN";
	private static final String BILLING_IFID = "BillingIFID";
	private static final String BILLING = "Billing";
	private static final String _2ND_DRIVER_FEATURES = "2ndDriverFeatures";
	private static final String VEHICLE_PURCHASE_INFO_IFID = "VehiclePurchaseInfoIFID";
	private static final String VEHICLE_PURCHASE_INFO = "VehiclePurchaseInfo";
	private static final String GET_DRIVER_PERMISSIONS_IFID = "getDriverPermissionsIFID";
	private static final String GET_DRIVER_PERMISSIONS = "getDriverPermissions";
	private static final String GET_WARRANTY_DATE_IFID = "getWarrantyDateIFID";
	private static final String GET_WARRANTY_DATE = "getWarrantyDate";
	private static final String GET_FINANCE_IFID = "getFinanceIFID";
	public static final String GET_FINANCE = "getFinance";
	public static final String PUT_UPDATE_WELCOME = "updatewelcome";

	private static final Logger log = LoggerFactory.getLogger(MyAccountServicesImpl.class);
	private static final String COVISINT_CUSTOMERID = "CUSTOMERID";

	@Reference
	private CovisintConfigService CovisintConfigService;

	@Reference
	private CryptoSupport cryptoSupport;
	
	@Reference
	private ConfigExtractor config;

	protected ResourceResolver adminResourceResolver;

	private String result = null;

	/**
	 *
	 *  Method uses configExtractor file to get domain specific configurable values
	 *  to authorize user for registrationAEM
	 * @param userid
	 * @param domainSelector
	 * @param firstname
	 * @param lastname
	 * @param emailaddress
	 * @param pass_word
	 * @param zipcode
	 * @param city
	 * @param region
	 * @param securityquestion
	 * @param securityanswer
	 * @return
	 * @throws IOException
	 */

	@Override
	public JSONObject registrationAEM(String firstname, String lastname, String emailaddress, String pass_word,
									  String zipcode, String city,String region,String securityquestion, String securityanswer,String flag,String domainSelector) {

		log.debug("Enter registrationAEM:: "+domainSelector);

		log.debug("Enter registrationAEM:FLAG: "+flag);

		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in = null;
		try {

			String param = "/vsEnrollment/v1.0/REGISTRATION";
			JSONObject userParam = new JSONObject();
			JSONObject userinfovalue = new JSONObject();
			userinfovalue.put("userId", emailaddress);
			userinfovalue.put("notificationEmail", emailaddress);
			userinfovalue.put("password", pass_word);
			userinfovalue.put("firstName", firstname);
			userinfovalue.put("lastName", lastname);
			userinfovalue.put("middleInitial", "");
			userinfovalue.put("prefix", "");
			userinfovalue.put("suffix", "");
			userinfovalue.put("birthMonth", "-1");
			userinfovalue.put("birthDay", "-1");
			//userinfovalue.put("registrationDate:","2016-07-19T12:25:47.4514784-07:00");
			userinfovalue.put("role", "PROSPECT");
			userinfovalue.put("enrollmentType", "INDIVIDUAL");
			//userinfovalue.put("contact","");
			userinfovalue.put("securityAnswers", "");

			JSONArray jsonArr = new JSONArray();
			JSONObject jobj = new JSONObject();

			jobj.put("type", "PRIMARY");
			//jobj.put("street", "1 DENVOR ROAD");
			//jobj.put("additional","");
			jobj.put("city", city);
			jobj.put("region", region);
			jobj.put("postalCode", zipcode);
			jsonArr.put(jobj);
			userinfovalue.put("address", jsonArr);

			JSONArray jsonArr1 = new JSONArray();
			JSONObject jobj1 = new JSONObject();

			jobj1.put("questionCode", securityquestion);
			//jobj.put("street", "1 DENVOR ROAD");
			//jobj.put("additional","");
			//jobj.put( "city", "DUBLIN");
			//jobj.put( "region", "OH");
			jobj1.put("answer", securityanswer);
			jsonArr1.put(jobj1);
			userinfovalue.put("securityAnswers", jsonArr1);


			userParam.put("userInfo", userinfovalue);
			userParam.put("preferredDealerCode", "");
			userParam.put("assistedDealerCode", "");
			userParam.put("overrideFlag", false);
			//userParam.put("salesman","null");

			URL url = new URL(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector +ServiceConstants.REGISTER_AEM));

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector +ServiceConstants. REGISTER_AEMIFID));
			// conn.setRequestProperty(ServiceConstants.COVISINT_ACCESSTOKEN,decrypted_token);
			conn.setRequestProperty(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			// conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME,"emailaddress");
			// conn.setRequestProperty(ServiceConstants.COVISINT_VIN,"KMHC05LH8HU000209");
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			// conn.setRequestProperty("LOGIN_ID","ae000209@hmausa.com");
			// conn.setRequestProperty(ServiceConstants.COVISINT_CACHE_CONTROL,ServiceConstants.COVISINT_CACHE_CONTROL_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION,
					(String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			conn.setRequestProperty("LOGIN_ID", emailaddress);
			if(domainSelector == "genesis"){
				conn.setRequestProperty(ServiceConstants.COVISINT_TRANSACTION_ID, new Random().nextInt(999999) + "_G"); // Need
			}else{
				conn.setRequestProperty(ServiceConstants.COVISINT_TRANSACTION_ID, new Random().nextInt(999999) + "_H"); // Need with _H
			}
			if(null != flag){
				conn.setRequestProperty(ServiceConstants.ENROLL, flag); // Need
			}

			conn.setDoOutput(true);
			conn.getOutputStream().write(userParam.toString().getBytes());

			int responseCode = conn.getResponseCode();
			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			if (responseCode != HttpStatus.SC_OK) {
				log.debug("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;

	}

	public JSONObject changePassword(String username, String oldPassword, String newPassword,String domainSelector) {

		log.debug("Enter changePassword:: "+domainSelector);

		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in = null;
		try {
			Map<String, Object> params = new LinkedHashMap<String, Object>();
			HttpResponse response = null;
			JSONObject userParam = new JSONObject();
			userParam.put("idpUserID", username);
			userParam.put("oldPassword", oldPassword);
			userParam.put("newPassword", newPassword);

			URL url = new URL(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + ServiceConstants.CHANGE_PASSWORD));

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_PUT);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			// conn.setRequestProperty(ServiceConstants.COVISINT_IFID,
			// "OP_OUSR003");
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + ServiceConstants.CHANGE_PASSWORD_IFID));
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, username);
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			conn.setDoOutput(true);
			conn.getOutputStream().write(userParam.toString().getBytes());
			int responseCode = conn.getResponseCode();
			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}
		return json;
	}

	public JSONObject generateNewCode(String scenarioId, String byEmail, String bySMS, String mobilePhone, String userId, String deviceId){
		log.debug("Method :: generateNewCode");

		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in=null;
		HttpResponse response = null;

		String param = "/vsUser_MyH/MFACODEGENERATION";

		try {
			JSONObject bodyJson = new JSONObject();
			JSONObject  requestBodyJson = new JSONObject();

			requestBodyJson.put("scenarioId",scenarioId);
			requestBodyJson.put("byEmail",byEmail);
			requestBodyJson.put("bySMS",bySMS);
			requestBodyJson.put("mobilePhone",mobilePhone);
			bodyJson.put("requestBody",requestBodyJson);

			URL url = new URL(CovisintConfigService.getRESTURL() + param);
			log.debug("Request URL :: {}", url);

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);
			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_VALUE_ESB);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_VALUE_OP);
			conn.setRequestProperty(ServiceConstants.MFA_DEVICE_ID,deviceId);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, CovisintConfigService.getCodeGenerateIFID());
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, CovisintConfigService.getAuthorization());
			conn.setRequestProperty(ServiceConstants.COVISINT_USER_ID, userId);
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);

			conn.setDoOutput(true);
			conn.getOutputStream().write(bodyJson.toString().getBytes());
			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.debug("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error :: {}",e.getStackTrace());
			String jsonString = "{\"status\" : \"ServerError\"}";
			try {
				json = new JSONObject(jsonString);
			} catch (JSONException ex) {
				throw new RuntimeException(ex);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;
	}

	public JSONObject newPassword(String requestBody, String deviceId, String userId){
		log.debug("Method :: newPassword");

		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in=null;
		HttpResponse response = null;

		String param = "/vsUser_MyH/MFANEWPASSWORD";

		try {
			URL url = new URL(CovisintConfigService.getRESTURL() + param);
			log.debug("Request URL :: {}", url);

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);
			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_VALUE_ESB);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_VALUE_OP);
			conn.setRequestProperty(ServiceConstants.MFA_DEVICE_ID,deviceId);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, CovisintConfigService.getNewPasswordIFID());
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, CovisintConfigService.getAuthorization());
			conn.setRequestProperty(ServiceConstants.COVISINT_USER_ID, userId);
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);

			conn.setDoOutput(true);
			conn.getOutputStream().write(requestBody.getBytes());
			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.debug("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error :: {}",e.getStackTrace());
			String jsonString = "{\"status\" : \"ServerError\"}";
			try {
				json = new JSONObject(jsonString);
			} catch (JSONException ex) {
				throw new RuntimeException(ex);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;
	}

	@Override
	public JSONObject changePasswordNew(String accessToken, String userId, String deviceId, String requestBody) {
		log.debug("Method :: changePasswordNew");

		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in=null;
		HttpResponse response = null;

		String param = "/vsUser_MyH/PASSWORD";

		try {
			URL url = new URL(CovisintConfigService.getRESTURL() + param);
			log.debug("Request URL :: {}", url);

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);
			conn.setRequestMethod(ServiceConstants.COVISINT_PUT);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_VALUE_ESB);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_VALUE_OP);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, "OP_IUSR003_MyH");
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, CovisintConfigService.getAuthorization());
			conn.setRequestProperty(ServiceConstants.USERID, userId);
			conn.setRequestProperty(ServiceConstants.MFA_DEVICE_ID,deviceId);
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESS_TOKEN, accessToken);
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);

			conn.setDoOutput(true);
			conn.getOutputStream().write(requestBody.getBytes());

			int responseCode = conn.getResponseCode();
			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			e.printStackTrace();
			String jsonString = "{\"status\" : \"ServerError\"}";
			try {
				json = new JSONObject(jsonString);
			} catch (JSONException ex) {
				throw new RuntimeException(ex);
			}
		} finally {       
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;

	}

	@Override
	public JSONObject userRegisterNew(String deviceId, String body) {
		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in=null;
		HttpResponse response = null;

		String param = "/vsEnrollment_MyH/REGISTRATION";

		try {

			URL url = new URL(CovisintConfigService.getRESTURL() + param);
			log.debug("Request URL :: {}", url);

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);
			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_VALUE_ESB);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_VALUE_OP);
			conn.setRequestProperty(ServiceConstants.DEVICE_ID,deviceId);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, "OP_IENR010_MyH");
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, CovisintConfigService.getAuthorization());
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_TRANSACTION_ID, new Random().nextInt(999999) + "_H");
			conn.setDoOutput(true);
			conn.getOutputStream().write(body.getBytes());
			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.debug("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error :: {}",e.getStackTrace());
			String jsonString = "{\"status\" : \"ServerError\"}";
			try {
				json = new JSONObject(jsonString);
			} catch (JSONException ex) {
				throw new RuntimeException(ex);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;
	}

	@Override
	public JSONObject updateEmail(String accessToken, String userId, String deviceId, String requestBody) {
		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in=null;
		HttpResponse response = null;

		String param = "/vsUserAEM_MyH/LOGIN";

		try {
			URL url = new URL(CovisintConfigService.getRESTURL() + param);
			log.debug("Request URL :: {}", url);

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);
			conn.setRequestMethod(ServiceConstants.COVISINT_PUT);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_VALUE_ESB);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_VALUE_OP);
			conn.setRequestProperty(ServiceConstants.MFA_DEVICE_ID, deviceId);
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, userId);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID,"OP_IUSR029_MyH");
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESS_TOKEN, accessToken);
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, CovisintConfigService.getAuthorization());
			conn.setRequestProperty(ServiceConstants.COVISINT_TRANSACTION_ID, new Random().nextInt(999999) + "_H");
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);

			conn.setDoOutput(true);
			conn.getOutputStream().write(requestBody.toString().getBytes());
			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.debug("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error :: {}",e.getStackTrace());
			String jsonString = "{\"status\" : \"ServerError\"}";
			try {
				json = new JSONObject(jsonString);
			} catch (JSONException ex) {
				throw new RuntimeException(ex);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;
	}

	@Override
	public JSONObject updateProfile(String accessToken,String userId, String deviceId, String requestBody) {
		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in=null;
		HttpResponse response = null;

		String param = "/vsUser_MyH/PROFILE";

		try {
			URL url = new URL(CovisintConfigService.getRESTURL() + param);
			log.debug("Request URL :: {}", url);

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);
			conn.setRequestMethod(ServiceConstants.COVISINT_PUT);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_VALUE_ESB);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_VALUE_OP);
			conn.setRequestProperty(ServiceConstants.MFA_DEVICE_ID,deviceId);
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME,userId);
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESS_TOKEN, accessToken);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, "OP_IUSR035_MyH");
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, CovisintConfigService.getAuthorization());
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);

			conn.setDoOutput(true);
			conn.getOutputStream().write(requestBody.getBytes());
			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.debug("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error :: {}",e.getStackTrace());
			String jsonString = "{\"status\" : \"ServerError\"}";
			try {
				json = new JSONObject(jsonString);
			} catch (JSONException ex) {
				throw new RuntimeException(ex);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;
	}

	@Override
	public JSONObject resetPinNew(String userId, String deviceId, String accessToken, String requestBody) {
		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in=null;
		HttpResponse response = null;

		String param = "/vsUser_MyH/PIN";

		try {
			URL url = new URL(CovisintConfigService.getRESTURL() + param);
			log.debug("Request URL :: {}", url);

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);
			conn.setRequestMethod(ServiceConstants.COVISINT_PUT);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_VALUE_ESB);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_VALUE_OP);
			conn.setRequestProperty(ServiceConstants.MFA_DEVICE_ID,deviceId);
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESSTOKEN,accessToken);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, "OP_IUSR002_MyH");
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, CovisintConfigService.getAuthorization());
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, userId);
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);

			conn.setDoOutput(true);
			conn.getOutputStream().write(requestBody.getBytes());
			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.debug("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error :: {}",e.getStackTrace());
			String jsonString = "{\"status\" : \"ServerError\"}";
			try {
				json = new JSONObject(jsonString);
			} catch (JSONException ex) {
				throw new RuntimeException(ex);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;
	}

	@Override
	public JSONObject getCommunicationMethod(String userId, String scenarioId, String deviceId, String mobilePhone) {
		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in=null;
		HttpResponse response = null;

		String param = "/vsUser_MyH/MFACOMMUNICATIONMETHODS";

		try {

			JSONObject requestBodyJson = new JSONObject("{}");
			URL url = new URL(CovisintConfigService.getRESTURL()+param);
			log.debug("Request URL :: {}", url);
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);
			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.REQUESTMETHOD,"CODE_RECIPIENT");
			conn.setRequestProperty(ServiceConstants.SCENARIOID,scenarioId);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_VALUE_ESB);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_VALUE_OP);
			conn.setRequestProperty(ServiceConstants.MFA_DEVICE_ID,deviceId);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, "OP_IUSR060_MyH");
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, CovisintConfigService.getAuthorization());
			conn.setRequestProperty(ServiceConstants.COVISINT_USER_ID,userId);
			if(mobilePhone != null) {
				conn.setRequestProperty("PHONENUMBER", mobilePhone);
			}
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);

			conn.setDoOutput(true);
			conn.getOutputStream().write(requestBodyJson.toString().getBytes());
			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.debug("Response Code is " + responseCode);
				String jsonString = "{\"status\" : \"ServerError\"}";
				json = new JSONObject(jsonString);
				return json;
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error :: {}",e.getStackTrace());
			String jsonString = "{\"status\" : \"error\",}";
			try {
				json = new JSONObject(jsonString);
			} catch (JSONException ex) {
				throw new RuntimeException(ex);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;
	}

	@Override
	public JSONObject userLoginNew(String deviceId, String requestBody) {
		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in=null;
		HttpResponse response = null;

		String param = "/vsUser_MyH/MFAMYHLOGIN";


		try {
			URL url = new URL(CovisintConfigService.getRESTURL() + param);
			log.debug("Request URL :: {}", url);

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);
			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_VALUE_ESB);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_VALUE_OP);
			conn.setRequestProperty(ServiceConstants.MFA_DEVICE_ID,deviceId);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, "OP_IUSR063_MyH");
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, CovisintConfigService.getAuthorization());
			conn.setRequestProperty(ServiceConstants.VERIFICATION_METHOD, "sms");
			conn.setRequestProperty(ServiceConstants.COVISINT_BRANDID, "H");
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);

			conn.setDoOutput(true);
			conn.getOutputStream().write(requestBody.getBytes());
			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.debug("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error :: {}",e.getStackTrace());
			String jsonString = "{\"status\" : \"ServerError\"}";
			try {
				json = new JSONObject(jsonString);
			} catch (JSONException ex) {
				throw new RuntimeException(ex);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;
	}

	@Override
	public JSONObject mfaCodeGenerate(String userId, String deviceId, String requestBody) {
		log.debug("Method :: mfaCodeGenerate");

		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in=null;
		HttpResponse response = null;

		String param = "/vsUser_MyH/MFACODEGENERATION";

		try {

			URL url = new URL(CovisintConfigService.getRESTURL() + param);
			log.debug("Request URL :: {}", url);

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);
			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_VALUE_ESB);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_VALUE_OP);
			conn.setRequestProperty(ServiceConstants.MFA_DEVICE_ID,deviceId);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, CovisintConfigService.getCodeGenerateIFID());
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, CovisintConfigService.getAuthorization());
			conn.setRequestProperty(ServiceConstants.COVISINT_USER_ID, userId);
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);

			conn.setDoOutput(true);
			conn.getOutputStream().write(requestBody.toString().getBytes());
			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.debug("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error :: {}",e.getStackTrace());
			String jsonString = "{\"status\" : \"ServerError\"}";
			try {
				json = new JSONObject(jsonString);
			} catch (JSONException ex) {
				throw new RuntimeException(ex);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;
	}

	@Override
	public JSONObject verifyPasswordPolicy(String deviceId, String userId, String requestBody) {

		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in=null;
		HttpResponse response = null;

		String param = "/vsUser_MyH/MFAVERIFYPASSWORDPOLICY";


		try {

			URL url = new URL(CovisintConfigService.getRESTURL() + param);
			log.debug("Request URL :: {}", url);

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);
			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_VALUE_ESB);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_VALUE_OP);
			conn.setRequestProperty(ServiceConstants.MFA_DEVICE_ID,deviceId);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, "OP_IUSR064_MyH");
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, CovisintConfigService.getAuthorization());
			conn.setRequestProperty(ServiceConstants.USERID,userId);
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			conn.setDoOutput(true);
			conn.getOutputStream().write(requestBody.getBytes());
			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.debug("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error :: {}",e.getStackTrace());
			String jsonString = "{\"status\" : \"ServerError\"}";
			try {
				json = new JSONObject(jsonString);
			} catch (JSONException ex) {
				throw new RuntimeException(ex);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;
	}

	@Override
	public JSONObject verifyToken(String deviceId, String userId,String accessToken, String requestBody) {
		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in=null;
		HttpResponse response = null;

		String param = "/vsUser_MyH/MFANEWVERIFIEDTOKEN";


		try {

			URL url = new URL(CovisintConfigService.getRESTURL() + param);
			log.debug("Request URL :: {}", url);

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);
			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_VALUE_ESB);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_VALUE_OP);
			conn.setRequestProperty(ServiceConstants.MFA_DEVICE_ID,deviceId);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, "OP_IUSR064_MyH");
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, CovisintConfigService.getAuthorization());
			conn.setRequestProperty(ServiceConstants.USERID,userId);
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESS_TOKEN, accessToken);
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);

			conn.setDoOutput(true);
			conn.getOutputStream().write(requestBody.getBytes());
			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.debug("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error :: {}",e.getStackTrace());
			String jsonString = "{\"status\" : \"ServerError\"}";
			try {
				json = new JSONObject(jsonString);
			} catch (JSONException ex) {
				throw new RuntimeException(ex);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;
	}

	@Override
	public JSONObject resendLink(String userId, String access_token, String firstName, String lastName) {

		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in=null;
		HttpResponse response = null;

		String param = "/vsUser_MyH/MFARESENDLINK";


		try {

			JSONObject requestBodyJson = new JSONObject("{}");

			URL url = new URL(CovisintConfigService.getRESTURL() + param);
			log.debug("Request URL :: {}", url);

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);
			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_VALUE_ESB);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_VALUE_OP);
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESS_TOKEN, access_token);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, "OP_IUSR066_MyH");
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, CovisintConfigService.getAuthorization());
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, userId);
			if(firstName != null) {
				conn.setRequestProperty(ServiceConstants.FIRST_NAME, firstName);
			}
			if(lastName != null){
				conn.setRequestProperty(ServiceConstants.LAST_NAME, lastName);
			}
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);

			conn.setDoOutput(true);
			conn.getOutputStream().write(requestBodyJson.toString().getBytes());
			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.debug("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error :: {}",e.getStackTrace());
			String jsonString = "{\"status\" : \"ServerError\"}";
			try {
				json = new JSONObject(jsonString);
			} catch (JSONException ex) {
				throw new RuntimeException(ex);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;
	}


	@Override
	public JSONObject validateCode(String scenarioId, String otpCode, String token, String userId, String deviceId) {
		log.debug("Method :: validateCode");

		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in=null;
		HttpResponse response = null;

		String param = "/vsUser_MyH/MFACODEVALIDATION";


		try {
			JSONObject bodyJson = new JSONObject();
			JSONObject  requestBodyJson = new JSONObject();

			requestBodyJson.put("scenarioId",scenarioId);
			requestBodyJson.put("mfa_code",otpCode);
			requestBodyJson.put("mfa_token",token);

			bodyJson.put("requestBody",requestBodyJson);

			URL url = new URL(CovisintConfigService.getRESTURL() + param);
			log.debug("Request URL :: {}", url);

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);
			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_VALUE_ESB);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_VALUE_OP);
			conn.setRequestProperty(ServiceConstants.MFA_DEVICE_ID,deviceId);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, CovisintConfigService.getCodeValidationIFID());
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, CovisintConfigService.getAuthorization());
			conn.setRequestProperty(ServiceConstants.COVISINT_USER_ID, userId);
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);

			conn.setDoOutput(true);
			conn.getOutputStream().write(bodyJson.toString().getBytes());

			int responseCode = conn.getResponseCode();
			log.debug("ifid ::{}",CovisintConfigService.getCodeValidationIFID());
			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			log.debug("point 1");
			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error :: {}",e.getStackTrace());
			String jsonString = "{\"status\" : \"ServerError\"}";
			try {
				json = new JSONObject(jsonString);
			} catch (JSONException ex) {
				throw new RuntimeException(ex);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;

	}

	@Override
	public JSONObject changePin(String accessToken,String userId,String deviceId, String requestBody){
		log.debug("Method :: changePin");

		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in=null;
		HttpResponse response = null;

		String param = "/vsUser_MyH/PIN";

		try {

			URL url = new URL(CovisintConfigService.getRESTURL() + param);
			log.debug("Request URL :: {}", url);

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);
			conn.setRequestMethod(ServiceConstants.COVISINT_PUT);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_VALUE_ESB);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_VALUE_OP);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, "OP_IUSR002_MyH");
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, CovisintConfigService.getAuthorization());
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, userId);
			conn.setRequestProperty(ServiceConstants.MFA_DEVICE_ID, deviceId);
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESS_TOKEN, accessToken);
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);

			conn.setDoOutput(true);
			conn.getOutputStream().write(requestBody.getBytes());

			int responseCode = conn.getResponseCode();
			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			if (responseCode != HttpStatus.SC_OK) {
				log.debug("Response Code is ::{}",responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			e.printStackTrace();
			String jsonString = "{\"status\" : \"ServerError\"}";
			try {
				json = new JSONObject(jsonString);
			} catch (JSONException ex) {
				throw new RuntimeException(ex);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;

	}

	public JSONObject pinUpdate(String username, String token, String pin, String oldPin, String questionCode,
								String answer,String domainSelector) {

		log.debug("Method :: pinUpdate");

		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in = null;
		try {
			Map<String, Object> params = new LinkedHashMap<String, Object>();
			HttpResponse response = null;

			String param = "/vsUser/v1.0/PIN";

			JSONObject userParam = new JSONObject();
			userParam.put("idpUserId", username);
			userParam.put("username", username);
			userParam.put("pin", pin);

			JSONArray qanswer = new JSONArray();
			JSONObject qanswerobj = new JSONObject();
			if (oldPin != null && !oldPin.equals("")) {
				userParam.put("oldPin", oldPin);
			} else if (questionCode != null && !questionCode.equals("")) {
				qanswerobj.put("questionCode", questionCode);
				qanswerobj.put("answer", answer);
				qanswer.put(qanswerobj);
				userParam.put("securityAnswers", qanswer);
			}

			URL url = new URL(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + ServiceConstants.PIN_UPDATE));

			String decrypted_token = token;

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_PUT);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			// conn.setRequestProperty(ServiceConstants.COVISINT_IFID,
			// "OP_OUSR002");
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + ServiceConstants.PIN_UPDATE_IFID));
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			conn.setRequestProperty(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, username);
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_TIMESTAMP, "TIMESTAMP");

			conn.setDoOutput(true);
			conn.getOutputStream().write(userParam.toString().getBytes());

			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;
	}

	/**
	 * method to add emergency contact
	 *
	 * @param
	 * @return
	 * @throws IOException
	 */


	@Override
	public JSONObject addEmergencyContact(String username, String token, String lastName, String firstName,
										  String relationship, String contactEmail, String phone1, String phone1Type, String phone2,
										  String phone2Type,String domainSelector) {


		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in = null;
		try {

			JSONObject addContactJsonObj = new JSONObject();
			JSONArray addJsonArray = new JSONArray();

			JSONObject addData = new JSONObject();
			addData.put("contactId", "0");
			addData.put("actionType", "SAVE");
			addData.put("lastName", lastName);
			addData.put("firstName", firstName);
			addData.put("relationship", relationship);
			addData.put("email", contactEmail);
			addData.put("phone1", phone1);
			addData.put("phone1Type", phone1Type);
			addData.put("phone2", phone2);
			addData.put("phone2Type", phone2Type);

			// add json data to json array
			addJsonArray.put(addData);

			// add json array to json object
			addContactJsonObj.put("loginId", username);
			addContactJsonObj.put("contacts", addJsonArray);

			String decrypted_token = token;

			//String param = "/vsUser/v1.0/CONTACT/EMERGENCY";
			//URL url = new URL(CovisintConfigService.getRESTURL() + param);
			URL url = new URL(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + ServiceConstants.ADD_EMERGENCY_CONTACT));
			log.debug("url--"+url);
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + ServiceConstants.ADD_EMERGENCY_CONTACT_IFID));
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			conn.setRequestProperty(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, username);
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE, ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			conn.setDoOutput(true);
			conn.getOutputStream().write(addContactJsonObj.toString().getBytes());

			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;
	}


	@Override
	public String deleteEmergencyContact(String username, String token, String loginId, String contactId,
										 String actionType, String lastName, String firstName, String relationship, String email, String phone1,
										 String phone1Type, String phone2, String phone2Type,String domainSelector) {

		log.debug("Enter deleteEmergencyContact CALLED::");

		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in = null;
		try {

			JSONObject addContactJsonObj = new JSONObject();
			JSONArray addJsonArray = new JSONArray();

			JSONObject addData = new JSONObject();
			addData.put("contactId", contactId);
			addData.put("actionType", actionType);
			addData.put("lastName", lastName);
			addData.put("firstName", firstName);
			addData.put("relationship", relationship);
			addData.put("email", email);
			addData.put("phone1", phone1);
			addData.put("phone1Type", phone1Type);
			addData.put("phone2", phone2);
			addData.put("phone2Type", phone2Type);

			// add json data to json array
			addJsonArray.put(addData);

			// add json array to json object
			addContactJsonObj.put("loginId", email);
			addContactJsonObj.put("contacts", addJsonArray);

			String decrypted_token = token;

			String param = "/vsUser_MyH/v1.0/CONTACT/EMERGENCY";
			URL url = new URL(CovisintConfigService.getRESTURL() + param);

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, "OP_IUSR020_MyH");
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			conn.setRequestProperty(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, username);
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			conn.setDoOutput(true);
			conn.getOutputStream().write(addContactJsonObj.toString().getBytes());

			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

			result = json.get("E_IFRESULT").toString();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return result;

	}


	/*
	 * method to getVehiclePurchaseHistory for Authorized Drivers in MY vehicle
	 * screen
	 *
	 */

	@Override

	public JSONObject getVehiclePurchaseHistory(String username, String token, String ownerId,String domainSelector) {

		log.debug("Enter getVehiclePurchaseHistory::"+ domainSelector);

		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;

		try {

			String decrypted_token = token;
			HttpClient httpclient = HttpClientBuilder.create().build();

			httpGet = new HttpGet(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + ServiceConstants.VEHICLE_PR_HISTORY)+ownerId);

			httpGet.addHeader(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			httpGet.addHeader(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + ServiceConstants.VEHICLE_PR_HISTORY_IFID));
			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_USERNAME, username);
			httpGet.addHeader(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			httpGet.addHeader("OWNERID", ownerId);

			response = httpclient.execute(httpGet);

			// verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}

			String getResult = EntityUtils.toString(response.getEntity());
			json = (JSONObject) new JSONTokener(getResult).nextValue();

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}

		// TODO Auto-generated method stub
		return json;

	} // getVehiclePurchaseHistory method end



	/*code for getOwnerMethod*/

	@Override
	public JSONObject getOwnerInfo(String username, String token,String domainSelector) {



		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;

		String brand = "H";
		JSONObject result = null;

		try {

			String decrypted_token = token;

			HttpClient httpclient = HttpClientBuilder.create().build();
			httpGet = new HttpGet(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector +ServiceConstants.OWNER_INFO)+"&LOGIN_NAME="+username+"&BRANDID="+brand);

			//log.info("getOwnerInfo  URL::" + CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + ServiceConstants.OWNER_INFO)+"&LOGIN_NAME="+username+"&BRANDID="+brand);

			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + "OwnerInfoIFID"));
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			httpGet.addHeader(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_USERNAME, username);
			httpGet.addHeader(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			//httpGet.addHeader(ServiceConstants.COVISINT_BRANDID, (String) config.getPropValue(domainSelector + ServiceConstants.COVISINT_BRANDID));
			httpGet.addHeader(ServiceConstants.COVISINT_BRANDID, "H");


			response = httpclient.execute(httpGet);

			// verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}

			String getResult = EntityUtils.toString(response.getEntity());
			json = (JSONObject) new JSONTokener(getResult).nextValue();

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}

		// TODO Auto-generated method stub getOwnerInfo
		return json;

	}
	/*code for getOwnerInfo method for dashboard Light API*/

	@Override
	public JSONObject getOwnerInfoDashboard(String username, String token, String vin, String domainSelector) {



		HttpResponse response = null;
		HttpGet httpGet = null;

		JSONObject json = null;

		String param = "/vsAccount_MyH/v1.0/OWNER/GETOWNERINFODASHBOARD";


		try {

			String decrypted_token = token;

			HttpClient httpclient = HttpClientBuilder.create().build();
			//httpGet = new HttpGet(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector +ServiceConstants.OWNER_INFO_DASHBOARD));
			httpGet = new HttpGet(CovisintConfigService.getRESTURL() + param);


			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + "OwnerInfoDashboardIFID"));
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			httpGet.addHeader(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_USERNAME, username);
			httpGet.addHeader(ServiceConstants.COVISINT_VIN, vin);
			httpGet.addHeader(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			httpGet.addHeader(ServiceConstants.COVISINT_BRANDID, (String) config.getPropValue(domainSelector + ServiceConstants.COVISINT_BRANDID));
			httpGet.addHeader(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);

			response = httpclient.execute(httpGet);

			// verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}

			String getResult = EntityUtils.toString(response.getEntity());
			json = (JSONObject) new JSONTokener(getResult).nextValue();

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}

		// TODO Auto-generated method stub getOwnerInfo
		return json;

	}



	/*code for getOwnerMethod*/

	@Override
	public JSONObject getOwnersVehiclesInfo(String username, String token, String domainSelector) {

		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;
		JSONObject result = null;

		try {

			log.debug("Enter getOwnersVehiclesInfo::" + domainSelector);

			//String decrypted_token = token;

			HttpClient httpclient = HttpClientBuilder.create().build();

			//httpGet = new HttpGet("http://hmaidinteai01.hke.local:5160/rest/WM9RESTFULProvider01/OPI/V1/ACCOUNT/OWNER/VEHICLE/DETAILS");
			httpGet = new HttpGet(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + ServiceConstants.OWNERS_VEHICLES_INFO));
			long startTime = System.currentTimeMillis();
			httpGet.addHeader(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector +ServiceConstants.OWNERS_VEHICLES_INFO_IFID));
			httpGet.addHeader(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));

			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_BRANDID, (String) config.getPropValue(domainSelector + ServiceConstants.COVISINT_BRANDID));
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_ACCESSTOKEN, token);
			httpGet.addHeader(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_USERNAME, username);
			httpGet.addHeader(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);

			response = httpclient.execute(httpGet);
			long elapsedTime = System.currentTimeMillis() - startTime;
			log.debug("Total elapsed http request/response time in milliseconds Get OWNERVehicle Info: " + elapsedTime);

			// verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}

			String getResult = EntityUtils.toString(response.getEntity());
			json = (JSONObject) new JSONTokener(getResult).nextValue();
			long jsonTime = System.currentTimeMillis()-startTime;
			log.debug("jsonTime for getOwnersVehiclesInfo--"+jsonTime);
			if (json.getString("E_IFRESULT").equals("Z:Success")) {

				if (json.getString("VehicleInfo").length() > 0) {
					JSONObject vehicleInfo = json.getJSONObject("VehicleInfo");
					JSONObject vehicleDetails = vehicleInfo.getJSONObject("vehicleDetails");

					String vin = vehicleDetails.getString("vin");
					json.put("s_name", cryptoSupport.protect(username));
				} else {
					json.put("s_name", cryptoSupport.protect(username));
				}
			}


		} catch (Exception e) {

		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}

		// TODO Auto-generated method stub getOwnerInfo
		return json;

	}



	@Override
	public JSONObject getVinInfo(String username, String token) {

		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;
		String param = "/vsAccount_MyH/v1.0/OWNER?LOAD_ALL_VEHICLES=false&BRANDID="+ServiceConstants.COVISINT_BRANDID_HYUNDAI_VALUE+"&LOGIN_NAME=" + username;
		JSONObject result = null;

		try {


			String decrypted_token = token;


			HttpClient httpclient = HttpClientBuilder.create().build();
			httpGet = new HttpGet(CovisintConfigService.getRESTURL()+ param);
			long startTime = System.currentTimeMillis();
			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_IFID, "OP_OACC008_MyH");
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			httpGet.addHeader(ServiceConstants.COVISINT_USERNAME, username);
			httpGet.addHeader(ServiceConstants.COVISINT_AUTHORIZATION, CovisintConfigService.getAuthorization());

			response = httpclient.execute(httpGet);
			long elapsedTime = System.currentTimeMillis() - startTime;
			log.debug("Total elapsed http request/response time in milliseconds Get VIN Info: " + elapsedTime);

			// verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {

			}

			String getResult = EntityUtils.toString(response.getEntity());
			json = (JSONObject) new JSONTokener(getResult).nextValue();
			long jsonTime = System.currentTimeMillis()-startTime;
			log.debug("jsonTime for get VIN info--"+jsonTime);
		} catch (Exception e) {
			log.error(e.getMessage());
		}finally{
			if(httpGet!=null){
				httpGet.releaseConnection();
			}
		}

		// TODO Auto-generated method stub   getOwnerInfo
		return json;

	}


	@Override
	public JSONObject getJWTTokenStatus(String username, String JwtToken, String domainSelector, String password) {


		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in = null;
		try {

			JSONObject addContactJsonObj = new JSONObject();
			log.debug("addContactJsonObj--"+addContactJsonObj);


			String param = "/vsUserAEM_MyH/v1.0/JWTTOKEN";
			URL url = new URL(CovisintConfigService.getRESTURL() + param);
			//URL url = new URL(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + ServiceConstants.ADD_EMERGENCY_CONTACT));
			log.debug("url--"+url);
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY,"HMA");
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER,"OP MyH");
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER,"ESB");
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, "OP_IUSR051_MyH");
			//conn.setRequestProperty(ServiceConstants.COVISINT_JWTTOKEN, JwtToken);

			//conn.setRequestProperty(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			conn.setRequestProperty("USERNAME", username);
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE, ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			conn.setDoOutput(true);
			addContactJsonObj.put("username", username);
			addContactJsonObj.put("password", password);
			addContactJsonObj.put("jwttoken", JwtToken);
			conn.getOutputStream().write(addContactJsonObj.toString().getBytes());

			int responseCode = conn.getResponseCode();

			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			} else {
				in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
				String jsonText = ConnectionUtils.readAll(in);
				json = new JSONObject(jsonText);
				json.put("erroCode", "200");
				json.put("s_name", cryptoSupport.protect(username));
			}



		} catch (Exception e) {
			// TODO Auto-generated catch block

		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {

				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;
	}

	@Override
	public JSONObject getVehiclesInfo(String username, String token) {

		log.debug("Enter getVehiclesInfo::" );

		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;
		String param = "/vsAccount_MyH/v1.0/OWNER/VEHICLE/DETAILS";
		JSONObject result = null;

		try {

			String decrypted_token = token;

			HttpClient httpclient = HttpClientBuilder.create().build();
			httpGet = new HttpGet(CovisintConfigService.getRESTURL() + param);

			httpGet.addHeader(ServiceConstants.COVISINT_IFID, "OP_OACC028_MyH");
			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_USERNAME, username);
			httpGet.addHeader(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			//httpGet.addHeader(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE );
			httpGet.addHeader(ServiceConstants.COVISINT_BRANDID, "H");
			httpGet.addHeader(ServiceConstants.AUTHORIZATION, "Basic TXlIT3dBRU1Vc2VyOmFsOFZyU056XkVNa2NR");
			response = httpclient.execute(httpGet);

			// verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}

			String getResult = EntityUtils.toString(response.getEntity());
			json = (JSONObject) new JSONTokener(getResult).nextValue();
		} catch (Exception e) {

		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}

		// TODO Auto-generated method stub getOwnerInfo
		return json;

	}

	@Override
	public JSONObject isOwner(String username, String domainSelector) {

		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;
		String param = "/vsAccount_MyH/v1.0/ISOWNER";
		JSONObject result = null;

		try {

			HttpClient httpclient = HttpClientBuilder.create().build();
			httpGet = new HttpGet(CovisintConfigService.getRESTURL()+ param);

			httpGet.addHeader(ServiceConstants.COVISINT_IFID, "OP_OACC092_MyH");
			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_USERNAME, username);
			httpGet.addHeader(ServiceConstants.COVISINT_AUTHORIZATION, CovisintConfigService.getAuthorization());

			response = httpclient.execute(httpGet);

			// verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}

			String getResult = EntityUtils.toString(response.getEntity());
			json = (JSONObject) new JSONTokener(getResult).nextValue();

		} catch (Exception e) {

		}finally{
			if(httpGet!=null){
				httpGet.releaseConnection();
			}
		}
		return json;
	}

	@Override
	public JSONObject isLogout(String username, String token, String domainSelector) {


		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in = null;
		try {

			JSONObject addContactJsonObj = new JSONObject();
			log.debug("addContactJsonObj--"+addContactJsonObj);

			String decrypted_token = token;

			String param = "/vsUser_MyH/LOGOUT";
			URL url = new URL(CovisintConfigService.getRESTURL() + param);
			//URL url = new URL(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + ServiceConstants.ADD_EMERGENCY_CONTACT));
			log.debug("url--"+url);
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY,"HMA");
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER,"OP MyH");
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER,"ESB");
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, "OP_IUSR048_MyH");
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);

			//conn.setRequestProperty(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			conn.setRequestProperty("USERID", username);
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE, ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			conn.setDoOutput(true);
			conn.getOutputStream().write(addContactJsonObj.toString().getBytes());

			int responseCode = conn.getResponseCode();

			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			} else {
				in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
				String jsonText = ConnectionUtils.readAll(in);
				json = new JSONObject(jsonText);
			}



		} catch (Exception e) {
			// TODO Auto-generated catch block

		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {

				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;
	}
	@Override
	public JSONObject getSSOAccessToken(String username, String token, String jwttoken, String domainSelector) {


		JSONObject json_sso = null;
		HttpURLConnection conn = null;
		JSONObject enrollmentDetails = null;
		Reader in = null;
		try {

			JSONObject addContactJsonObj = new JSONObject();
			log.debug("getSSOAccessToken--"+addContactJsonObj);

			String param = "/vsUser_MyH/v1.0/OAUTH/TOKEN/VALIDATESSO";

			URL url = new URL(CovisintConfigService.getRESTURL() + param);
			log.debug("url--"+url);
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY,"HMA");
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER,"OP MyH");
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER,"ESB");
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, "OP_IUSR049_MyH");
			conn.setRequestProperty("JWTTOKEN", jwttoken);
			conn.setRequestProperty("SSOTOKEN", token);

			//conn.setRequestProperty(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			conn.setRequestProperty("USERNAME", username);
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE, ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			conn.setDoOutput(true);
			conn.getOutputStream().write(addContactJsonObj.toString().getBytes());

			int responseCode = conn.getResponseCode();

			if (responseCode != HttpStatus.SC_OK) {

			} else {
				in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
				String jsonText = ConnectionUtils.readAll(in);

				json_sso = new JSONObject(jsonText);

				String token_exists = json_sso.getString("RESPONSE_STRING");

				// if(json.has("Token")){
				if (token_exists.equals("")) {

					enrollmentDetails = json_sso;

				} else {
					json_sso.put("s_name", username);
					String token_sso = json_sso.getJSONObject("RESPONSE_STRING").getString("jwt_id");
					json_sso.getJSONObject("RESPONSE_STRING").put("jwt_token", token_sso);
					log.debug("json_sso"+json_sso);
					enrollmentDetails = json_sso;
				}
			}



		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return enrollmentDetails;
	}

	@Override
	public JSONObject getChatbotAccessToken(String username, String token, String jwttoken, String domainSelector) {


		JSONObject json_sso = null;
		HttpURLConnection conn = null;
		JSONObject enrollmentDetails = null;
		Reader in = null;
		try {

			JSONObject addContactJsonObj = new JSONObject();
			log.debug("getSSOAccessToken--"+addContactJsonObj);

			String param = "/vsUser_MyH/v1.0/OAUTH/TOKEN/VALIDATESSO";

			URL url = new URL(CovisintConfigService.getRESTURL() + param);
			log.debug("url--"+url);
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY,"HMA");
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER,"OP MyH");
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER,"ESB");
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, "OP_IUSR049_MyH");
			conn.setRequestProperty("JWTTOKEN", jwttoken);
			conn.setRequestProperty("SSOTOKEN", token);

			//conn.setRequestProperty(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			conn.setRequestProperty("USERNAME", username);
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE, ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			conn.setDoOutput(true);
			conn.getOutputStream().write(addContactJsonObj.toString().getBytes());

			int responseCode = conn.getResponseCode();

			if (responseCode != HttpStatus.SC_OK) {

			} else {
				in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
				String jsonText = ConnectionUtils.readAll(in);

				json_sso = new JSONObject(jsonText);


			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json_sso;
	}
	@Override
	public String updateVehiclePurchaseStatus(String username, String token, String ownerId, String vin,
											  String VehicleStatus, String domainSelector) {

		log.debug("Enter updateVehiclePurchaseStatus::" + domainSelector);

		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in = null;
		try {


			JSONObject userInput = new JSONObject();
			userInput.put(ServiceConstants.COVISINT_VIN, vin);
			userInput.put(ServiceConstants.VEHICLE_STATUS, VehicleStatus);
			JSONArray userInputArray = new JSONArray();
			userInputArray.put(userInput);
			JSONObject userData = new JSONObject();
			userData.put(ServiceConstants.VEHICLE_LIST, userInputArray);
			String decrypted_token = token;

			log.debug("TEST LOGIC"+userData.toString());

			URL url = new URL(CovisintConfigService.getRESTURL() + (String) config.getPropValue(domainSelector + ServiceConstants.UPDATE_VEHICLE_PR_HISTORY));

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_PUT);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + ServiceConstants.UPDATE_VEHICLE_PR_HISTORY_IFID));
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			conn.setRequestProperty(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, username);
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty("OWNERID", ownerId);
			conn.setDoOutput(true);
			conn.getOutputStream().write(userData.toString().getBytes());

			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

			result = json.get("E_IFRESULT").toString();

		} catch (Exception e) {

		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {

				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		// TODO Auto-generated method stub
		return result;

	}

	@Override
	public JSONObject isValidVin(String vin, String from) {
		JSONObject response_string = null;
		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;
		try {
			HttpClient httpclient = HttpClientBuilder.create().build();
			httpGet = new HttpGet(CovisintConfigService.getRESTURL() + CovisintConfigService.getValidateVinApiUrl());
			httpGet.addHeader(ServiceConstants.COVISINT_IFID, CovisintConfigService.getValidateVinApiIFID());
			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, "MYH");
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_VIN, vin);
			httpGet.addHeader(ServiceConstants.COVISINT_FROM, from);
			httpGet.addHeader(GenesisConstants.AUTHORIZATION,
					GenesisConstants.BASIC + LinkUtils.getAuthorizationHeader(CovisintConfigService.getValidateVinApiUsername(),
							CovisintConfigService.getValidateVinApiPassword()));
			response = httpclient.execute(httpGet);
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}
			String getResult = EntityUtils.toString(response.getEntity());
			json = (JSONObject) new JSONTokener(getResult).nextValue();
			response_string = json;
		} catch (Exception e) {

		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}
		return response_string;
	}

	@Override
	public JSONObject validateVehicleByVIN(String username, String token, String vin, String ownerId,String domainSelector) {

		log.debug("Enter validateVehicleByVIN::" + domainSelector);
		// TODO Auto-generated method stub
		JSONObject response_string = null;
		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;

		try {

			//String decrypted_token = token;

			HttpClient httpclient = HttpClientBuilder.create().build();
			httpGet = new HttpGet(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + ServiceConstants.VALIDATE_VEHICLE_BY_VIN));
			httpGet.addHeader(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + ServiceConstants.VALIDATE_VEHICLE_BY_VINIFID));
			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_USERNAME, username);
			//httpGet.addHeader(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			httpGet.addHeader(ServiceConstants.COVISINT_OWNERID, ownerId);
			httpGet.addHeader(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			httpGet.addHeader(ServiceConstants.COVISINT_VIN, vin);
			response = httpclient.execute(httpGet);

			// verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}

			String getResult = EntityUtils.toString(response.getEntity());
			json = (JSONObject) new JSONTokener(getResult).nextValue();

			response_string = json;

		} catch (Exception e) {
		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}

		return response_string;
	}


	@Override
	public JSONObject addVehicle(String username, String token, String vin, JSONObject userparam,String domainSelector) {

		log.debug("Enter addVehicle::" + domainSelector);

		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in = null;
		try {

			String decrypted_token = token;

			URL url = new URL(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + ServiceConstants.ADD_VEHICLE));

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);

			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + ServiceConstants.ADD_VEHICLE_IFID));

			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, username);
			conn.setRequestProperty(ServiceConstants.COVISINT_VIN, vin);
			conn.setRequestProperty(ServiceConstants.TRANSACTION_ID, "76678686678");
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.ISBLUELINK, ServiceConstants.TRUE);
			conn.setDoOutput(true);
			conn.getOutputStream().write(userparam.toString().getBytes());

			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {

		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {

				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}


		return json;

	}


	@Override
	public JSONObject removeVehicle(String username, String token, String vin, JSONObject userparam,String domainSelector) {

		log.debug("Enter removeVehicle::" + domainSelector);
		// TODO Auto-generated method stub

		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in = null;
		try {

			String decrypted_token = token;

			URL url = new URL(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + ServiceConstants.REMOVE_VEHICLE));
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_PUT);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + ServiceConstants.REMOVE_VEHICLE_IFID));
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, username);
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty("DRIVERTYPE", "SD");
			conn.setRequestProperty(ServiceConstants.TRANSACTION_ID, "2");
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));

			conn.setDoOutput(true);
			conn.getOutputStream().write(userparam.toString().getBytes());

			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;

	}


	@Override
	public JSONObject updateVehicleNickname(String token, String idmd, String vin, String regid, String nickName,
											String username,String domainSelector) {

		log.debug("Enter updateVehicleNickname::" + domainSelector);
		// TODO Auto-generated method stub

		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in = null;
		try {

			JSONObject addData1 = new JSONObject();

			JSONObject addData2 = new JSONObject();
			addData2.put("LOGINID", username);
			addData2.put("IDMID", idmd);
			addData2.put("VIN", vin);
			addData2.put("REGID", regid);
			addData2.put("NICKNAME", nickName);

			addData1.put("IN_DATA", addData2);

			String decrypted_token = token;

			URL url = new URL(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + ServiceConstants.UPDATE_VEHICLE_NAME));

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_PUT);
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + ServiceConstants.UPDATE_VEHICLE_NAME_IFID));
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, username);
			conn.setRequestProperty(ServiceConstants.COVISINT_VIN, vin);
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			conn.setDoOutput(true);
			conn.getOutputStream().write(addData1.toString().getBytes());

			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {

				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;

	}


	// code for getPending2ndDriverList

	public JSONObject getPending2ndDriverList(String vin, String username, String token,String domainSelector) {

		log.debug("Enter getPending2ndDriverList::" + domainSelector);

		// TODO Auto-generated method stub
		String result = null;

		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;

		try {

			String decrypted_token = token;

			HttpClient httpclient = HttpClientBuilder.create().build();
			httpGet = new HttpGet(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + ServiceConstants.PENDING_SEC_DRIVER));
			httpGet.addHeader(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + ServiceConstants.PENDING_SEC_DRIVER_IFID));
			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_USERNAME, username);
			httpGet.addHeader(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			httpGet.addHeader(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			httpGet.addHeader(ServiceConstants.COVISINT_VIN, vin);
			response = httpclient.execute(httpGet);

			// verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}

			String getResult = EntityUtils.toString(response.getEntity());
			json = (JSONObject) new JSONTokener(getResult).nextValue();

		} catch (Exception e) {

		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}
		return json;

	}

	// code for save2nddriver details,
	public JSONObject set2ndDriverDetails(String vin, String username, String token, String driverLastName,
										  String driverFirstName, String driverEmail,String domainSelector) {

		log.debug("Enter set2ndDriverDetails::" + domainSelector);
		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in = null;
		try {

			JSONObject inputObject = new JSONObject();
			JSONObject userInput = new JSONObject();
			userInput.put("invitationId", "0");
			userInput.put("driverLastName", driverLastName);
			userInput.put("driverFirstName", driverFirstName);
			userInput.put("driverType", "SD");
			userInput.put("driverEmail", driverEmail);

			JSONArray userData = new JSONArray();
			userData.put(userInput);

			inputObject.put("driverDetails", userData);

			String decrypted_token = token;

			URL url = new URL(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + ServiceConstants.SAVE_SECOND_DRIVER));

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + ServiceConstants.SAVE_SECOND_DRIVER_IFID));
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, username);
			conn.setRequestProperty(ServiceConstants.COVISINT_VIN, vin);
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			conn.setRequestProperty(ServiceConstants.ACTION_TYPE, "SAVE");
			conn.setDoOutput(true);
			conn.getOutputStream().write(inputObject.toString().getBytes());

			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {

				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;

	}

	// code for set featureSecondarydriver details


	public JSONObject setFeatureRegSecondaryDrivers(String vin, String username, String token, String idmId,
													String regdriverEmail, JSONObject userParam,String domainSelector) {

		log.debug("Enter setFeatureRegSecondaryDrivers::" + domainSelector);

		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in = null;
		try {

			String decrypted_token = token;

			URL url = new URL(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + ServiceConstants.SET_FEATURE_REG_SEC_DRIVER));

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_PUT);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + ServiceConstants.SET_FEATURE_REG_SEC_DRIVER_IFID));
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, username);
			conn.setRequestProperty(ServiceConstants.COVISINT_VIN, vin);
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			conn.setRequestProperty(ServiceConstants.REGISTERED_SECONDRY_DRIVER_EMAIL, regdriverEmail);
			conn.setRequestProperty("IDMID", idmId);
			conn.setDoOutput(true);
			conn.getOutputStream().write(userParam.toString().getBytes());

			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			// TODO Auto-generated catch block

		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {

				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;

	}

	//code for edit permissions of registered secondary driver

	public JSONObject editPermission2ndDriver(String vin, String username, String token, String idmId,
											  String secdriverid) {
		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;
		try {

			String decrypted_token = token;

			String param = "/vsSecondaryDrivers/v1.0/FEATURES_REG_SECOND_DRIVER";
			URL url = new URL(CovisintConfigService.getRESTURL() + param);

			HttpClient httpclient = HttpClientBuilder.create().build();
			httpGet = new HttpGet(CovisintConfigService.getRESTURL() + param);
			httpGet.addHeader(ServiceConstants.COVISINT_AUTHORIZATION, CovisintConfigService.getAuthorization());
			httpGet.addHeader(ServiceConstants.COVISINT_IFID, "OP_OACC046_MyG");
			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_USERNAME, username);
			httpGet.addHeader(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			httpGet.addHeader(ServiceConstants.COVISINT_VIN, vin);
			httpGet.addHeader(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_IDMID, idmId);
			httpGet.addHeader(ServiceConstants.COVISINT_REGISTERED_SECONDRY_DRIVER_EMAIL, secdriverid);

			response = httpclient.execute(httpGet);

			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}

			String getResult = EntityUtils.toString(response.getEntity());
			json = (JSONObject) new JSONTokener(getResult).nextValue();

		} catch (Exception e) {
			// TODO Auto-generated catch block

		}
		return json;

	}

	// code  for  remove secondary driver


	// code for set featureSecondarydriver details


	public JSONObject removeSecondaryDriver(String vin, String username, String token, String idmId,
											String regdriverEmail,String domainSelector) {

		log.debug("Enter removeSecondaryDriver::" + domainSelector);
		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in = null;
		try {

			JSONObject inputData = new JSONObject();
			inputData.put("primaryDriverLoginId", username);
			inputData.put("vin", vin);
			inputData.put("driverLoginId", "{{registeredSecondaryDriverIdmId}}");

			String decrypted_token = token;


			URL url = new URL(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + "RemoveSecDriver"));

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + "RemoveSecDriverIFID"));
			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, username);
			conn.setRequestProperty(ServiceConstants.COVISINT_VIN, vin);
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			conn.setRequestProperty(ServiceConstants.REGISTERED_SECONDRY_DRIVER_EMAIL, regdriverEmail);
			conn.setRequestProperty("IDMID", idmId);
			conn.setDoOutput(true);
			conn.getOutputStream().write(inputData.toString().getBytes());

			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			// TODO Auto-generated catch block

		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {

				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;

	}


	//code for setDefault vehicle
	public JSONObject setDefaultVehicle(String vin, String username, String token,String domainSelector) {

		log.debug("Enter setDefaultVehicle::" + domainSelector);
		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in = null;
		try {

			JSONObject inputData = new JSONObject();
			inputData.put("primaryDriverLoginId", username);

			String decrypted_token = token;

			URL url = new URL(CovisintConfigService.getRESTURL() +config.getPropValue(domainSelector + ServiceConstants.SET_DEFAULT_VEHICLE));

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + ServiceConstants.SET_DEFAULT_VEHICLE_IFID));
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, username);
			conn.setRequestProperty(ServiceConstants.COVISINT_VIN, vin);
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);

			conn.setDoOutput(true);
			conn.getOutputStream().write(inputData.toString().getBytes());

			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {

		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {

				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;

	}



	// code for getPending2ndDriverList

	public JSONObject getUFinfoSecondaryDriver(String vin, String username, String token, String idmId, String domainSelector) {

		log.debug("Enter getUFinfoSecondaryDriver::" + domainSelector);

		// TODO Auto-generated method stub
		String result = null;

		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;

		try {

			String decrypted_token = token;

			HttpClient httpclient = HttpClientBuilder.create().build();
			httpGet = new HttpGet(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + ServiceConstants.U_FINFO_SEC_DRIVER));
			httpGet.addHeader(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + ServiceConstants.U_FINFO_SEC_DRIVER_IFID));
			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			httpGet.addHeader(ServiceConstants.COVISINT_USERNAME, username);
			httpGet.addHeader(ServiceConstants.COVISINT_VIN, vin);
			httpGet.addHeader(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			httpGet.addHeader("IDMID", idmId);
			httpGet.addHeader(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));

			response = httpclient.execute(httpGet);

			// verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {

			}

			String getResult = EntityUtils.toString(response.getEntity());
			json = (JSONObject) new JSONTokener(getResult).nextValue();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}
		return json;

	}
	/**
	 *
	 *  Method uses configExtractor file to get domain specific configurable values
	 *  to authorize user for getAddress
	 * @param username
	 * @param domainSelector
	 * @return
	 * @throws IOException
	 */
	@Override
	public JSONObject getAddress(String username, String domainSelector) {

		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;
		try {

			log.debug("Enter getAddress::" + domainSelector);


			HttpClient httpclient = HttpClientBuilder.create().build();

			httpGet = new HttpGet(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + ServiceConstants.GET_ADDRESS) + ServiceConstants.SERVICE + "&FORMAT="
					+ ServiceConstants.FORMAT + "&EMAIL=" + username + "&COMPANY1=" + ServiceConstants.COMPANY1
					+ "&CONTRACT=" + ServiceConstants.CONTRACT);

			httpGet.addHeader(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + ServiceConstants.GET_ADDRESS_IFID));
			httpGet.addHeader(ServiceConstants.COVISINT_AUTHORIZATION,
					(String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));

			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);

			response = httpclient.execute(httpGet);

			// verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}

			String getResult = EntityUtils.toString(response.getEntity());
			json = (JSONObject) new JSONTokener(getResult).nextValue();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}
		return json;

	}

	// code for getPending2ndDriverList

	public JSONObject getMonroneyLabelPDF(String vin, String username, String token,String domainSelector) {

		log.debug("Enter getMonroneyLabelPDF::" + domainSelector);

		String result = null;

		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;

		try {

			String decrypted_token = token;

			HttpClient httpclient = HttpClientBuilder.create().build();
			httpGet = new HttpGet(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + ServiceConstants.MONOREY_PDF));

			httpGet.addHeader(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			httpGet.addHeader(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + ServiceConstants.MONOREY_PDFIFID));
			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			httpGet.addHeader(ServiceConstants.COVISINT_USERNAME, username);
			httpGet.addHeader(ServiceConstants.COVISINT_VIN, vin);

			response = httpclient.execute(httpGet);

			// verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}

			String getResult = EntityUtils.toString(response.getEntity());
			getResult = getResult.replaceAll("\\r\\n|\\r|\\n", " ");

			json = (JSONObject) new JSONTokener(getResult).nextValue();

		} catch (Exception e) {

		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}

		// TODO Auto-generated method stub getOwnerInfo
		return json;

	}

	@Override
	public boolean isValidToken(String token) {
		// Stub implementation
		return true;
	}

	@Override
	public JSONObject getBillingInfo(String vin, String token, String regid, String username, String domainSelector) {

		log.debug("Enter getBillingInfo::" + domainSelector);

		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;

		JSONObject result = null;

		try {

			String decrypted_token = token;

			HttpClient httpclient = HttpClientBuilder.create().build();
			httpGet = new HttpGet(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector +BILLING));

			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector +BILLING_IFID));
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			httpGet.addHeader(ServiceConstants.COVISINT_CONTENTTYPE, ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			httpGet.addHeader(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_USERNAME, username);
			httpGet.addHeader(ServiceConstants.COVISINT_VIN, vin);
			httpGet.addHeader(ServiceConstants.COVISINT_REGISTRATIONID, regid);


			response = httpclient.execute(httpGet);

			// verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}

			String getResult = EntityUtils.toString(response.getEntity());
			json = (JSONObject) new JSONTokener(getResult).nextValue();

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}

		return json;

	}

	@Override
	public JSONObject placeSubscriptionOrder(String vin, String token, String userName, String dealercode,
											 JSONObject enrollInfo,String domainSelector) {

		log.debug("Enter placeSubscriptionOrder::"+domainSelector);

		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in = null;
		try {


			String decrypted_token = token;

			URL url = new URL(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + "PlaceSubs"));

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + "PlaceSubsIFID"));
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, userName);
			conn.setRequestProperty(ServiceConstants.COVISINT_VIN, vin);
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_DEALER_CODE, dealercode);// Need
			// dealer
			// code
			conn.setDoOutput(true);
			conn.getOutputStream().write(enrollInfo.toString().getBytes());

			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {

		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {

				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		// TODO Auto-generated method stub
		return json;

	}

	@Override
	public JSONObject cancelSubscriptionOrder(String vin, String token, String userName,String domainSelector) {

		log.debug("Enter cancelSubscriptionOrder::"+domainSelector);

		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in = null;
		try {

			JSONObject jsonObject=new JSONObject();
			String decrypted_token = token;
			log.debug("Decrypted Token ="+ decrypted_token);
			URL url = new URL(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + "CancelSubs"));
			log.debug("URL = "+ url.toString());
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + "CancelSubsIFID"));
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_HMA_COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, userName);
			conn.setRequestProperty(ServiceConstants.COVISINT_VIN, vin);
			conn.setDoOutput(true);
			conn.getOutputStream().write(jsonObject.toString().getBytes());

			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {

		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {

				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		// TODO Auto-generated method stub
		log.debug("cancelSubscriptionOrder JSON - " + json.toString());
		return json;

	}


	@Override
	public JSONObject put2ndDriverFeatures(org.json.simple.JSONObject featureDetails, String token, String vin,
										   String idmid, String driver2ndEmail, String userName, String domainSelector) {

		log.debug("Enter put2ndDriverFeatures::"+domainSelector);
		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in = null;
		try {

			String decrypted_token = token;


			URL url = new URL(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + _2ND_DRIVER_FEATURES));

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_PUT);
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);

			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, userName);
			conn.setRequestProperty(ServiceConstants.COVISINT_VIN, vin);
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			conn.setRequestProperty(ServiceConstants.REGISTERED_SECONDRY_DRIVER_EMAIL, driver2ndEmail);
			conn.setRequestProperty("IDMID", idmid);
			conn.setDoOutput(true);
			conn.getOutputStream().write(featureDetails.toString().getBytes());

			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);



		} catch (Exception e) {

		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {

				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		// TODO Auto-generated method stub
		return json;

	}

	public JSONObject getDriverPermissions(String username, String token, String vin, String idmid, String ownerid,
										   String loginid, String regid,String domainSelector) {

		log.debug("Enter getDriverPermissions::"+domainSelector);
		JSONObject driver_json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;

		try {

			String decrypted_token = token;

			HttpClient httpclient = HttpClientBuilder.create().build();

			httpGet = new HttpGet(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + GET_DRIVER_PERMISSIONS));
			httpGet.addHeader(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + GET_DRIVER_PERMISSIONS_IFID));
			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_CONTENTTYPE, ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			httpGet.addHeader("CUSTOMERID", ownerid);
			httpGet.addHeader(ServiceConstants.COVISINT_LOGIN_ID, loginid);
			httpGet.addHeader(ServiceConstants.COVISINT_IDMID, idmid);
			httpGet.addHeader(ServiceConstants.COVISINT_VIN, vin);
			httpGet.addHeader(ServiceConstants.COVISINT_REGISTRATION_ID, regid);
			httpGet.addHeader(ServiceConstants.COVISINT_USERNAME, username);
			httpGet.addHeader(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			httpGet.addHeader(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));

			response = httpclient.execute(httpGet);

			// verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}

			String getResult = EntityUtils.toString(response.getEntity());
			driver_json = (JSONObject) new JSONTokener(getResult).nextValue();

		} catch (Exception e) {

		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}

		return driver_json;
	}

	public JSONObject userHavePin(String username, String token, String domainSelector) {
		JSONObject userPinJson = null;
		HttpURLConnection conn = null;
		Reader in = null;

		log.debug("Enter userHavePin::"+domainSelector);
		try {
			Map<String, Object> params = new LinkedHashMap<String, Object>();

			URL url = new URL(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + ServiceConstants.PIN_VERIFICATION));

			String decrypted_token = token;

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + ServiceConstants.PIN_VERIFICATION_IFID));
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, username);
			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);

			JSONObject jsonobject = new JSONObject();

			conn.setDoOutput(true);
			conn.getOutputStream().write(jsonobject.toString().getBytes());

			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			userPinJson = new JSONObject(jsonText);

		} catch (ProtocolException pe) {

		} catch (JSONException je) {

		} catch (IOException io) {

		}  finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {

				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return userPinJson;
	}

	public JSONObject getWarrantyDate(String vin,String domainSelector){
		log.debug("Enter getWarrantyDate::" + domainSelector);

		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;

		try {

			HttpClient httpclient = HttpClientBuilder.create().build();
			httpGet = new HttpGet(CovisintConfigService.getRESTURL()+config.getPropValue(domainSelector + GET_WARRANTY_DATE));

			httpGet.addHeader(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + GET_WARRANTY_DATE_IFID));
			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_CONTENTTYPE,ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_VIN, vin);
			httpGet.addHeader(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));

			response = httpclient.execute(httpGet);

			// verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}

			String getResult = EntityUtils.toString(response.getEntity());
			json = (JSONObject) new JSONTokener(getResult).nextValue();

		}catch (Exception e) {

		} finally{
			if(httpGet!=null){
				httpGet.releaseConnection();
			}
		}

		return json;
	}

	public JSONObject getFinanceInfo(String vin, String ownerId,String domainSelector){

		log.debug("Enter getFinanceInfo::" + domainSelector);
		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;

		try {

			HttpClient httpclient = HttpClientBuilder.create().build();
			httpGet = new HttpGet(CovisintConfigService.getRESTURL()+config.getPropValue(domainSelector + GET_FINANCE));

			httpGet.addHeader(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + GET_FINANCE_IFID));
			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_CONTENTTYPE,ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_VIN, vin);
			httpGet.addHeader(COVISINT_CUSTOMERID, ownerId);
			httpGet.addHeader(ServiceConstants.COVISINT_AUTHORIZATION, (String)config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));

			response = httpclient.execute(httpGet);

			// verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}

			String getResult = EntityUtils.toString(response.getEntity());
			json = (JSONObject) new JSONTokener(getResult).nextValue();

		}catch (Exception e) {

		} finally{
			if(httpGet!=null){
				httpGet.releaseConnection();
			}
		}

		return json;
	}
	public JSONObject getPurchaseInfo(String vin, String ownerId,String domainSelector){
		log.debug("Enter getPurchaseInfo::" + domainSelector);

		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;

		try {

			HttpClient httpclient = HttpClientBuilder.create().build();
			httpGet = new HttpGet(CovisintConfigService.getRESTURL()+config.getPropValue(domainSelector + VEHICLE_PURCHASE_INFO));

			httpGet.addHeader(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + VEHICLE_PURCHASE_INFO_IFID));
			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_CONTENTTYPE,ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_VIN, vin);
			httpGet.addHeader(COVISINT_CUSTOMERID, ownerId);
			httpGet.addHeader(ServiceConstants.COVISINT_AUTHORIZATION, (String)config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));

			response = httpclient.execute(httpGet);

			// verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}

			String getResult = EntityUtils.toString(response.getEntity());
			json = (JSONObject) new JSONTokener(getResult).nextValue();

		}catch (Exception e) {

		} finally{
			if(httpGet!=null){
				httpGet.releaseConnection();
			}
		}

		return json;
	}
	/*
	 * MyAccountServices#
	 * getRoleBasedPermissionDetails method get the role based permission .
	 *
	 *
	 */
	@Override
	public JSONObject getRoleBasedPermissionDetails(String vin, String username, String token, String gen,
													String domainSelector) {

		log.debug("-----Enter getRoleBasedPermissionDetails::-----" + domainSelector);

		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;

		try {
			String decrypted_token = token;

			HttpClient httpclient = HttpClientBuilder.create().build();
			httpGet = new HttpGet(CovisintConfigService.getRESTURL()+config.getPropValue(domainSelector + FEATURE_BY_ROLE));

			httpGet.addHeader(ServiceConstants.COVISINT_IFID, "OP_OENR007_MyH");
			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SESSIONID, ServiceConstants.COVISINT_SESSIONID_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_USERNAME, username);
			httpGet.addHeader(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			httpGet.addHeader(ServiceConstants.COVISINT_VIN, vin);

			httpGet.addHeader(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);

			httpGet.addHeader(GEN, gen);

			httpGet.addHeader(ServiceConstants.COVISINT_AUTHORIZATION, (String)config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));

			response = httpclient.execute(httpGet);

			// verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}

			String getResult = EntityUtils.toString(response.getEntity());

			json = (JSONObject) new JSONTokener(getResult).nextValue();

		}catch (Exception e) {

		} finally{
			if(httpGet!=null){
				httpGet.releaseConnection();
			}
		}

		return json;
	}

	/*
	 * MyAccountServices#	saveDriverPermissionBlods method saves the permission details
	 *
	 */
	/*------------------Code in PROGRESS------------*/
	@Override
	public JSONObject saveDriverPermissionBlods(org.json.simple.JSONObject jsonDetails, String token, String vin, String idmid,
												String ownerid, String regId, String username, String domainSelector, String year, String model) {


		log.debug("--Enter saveDriverPermissionBlodsffffff::----" + domainSelector+token+vin+idmid+regId+username+ownerid);

		JSONObject json = null;
		JSONObject results = null;
		HttpURLConnection conn = null;
		Reader in = null;
		String param="/vsSecondaryDrivers_MyH/v1.0/INVITATION_BLODS";

		try {

			String decrypted_token = token;

			URL url = new URL(CovisintConfigService.getRESTURL() + param);

			log.debug("-----saveDriverPermissionBlodsURL-----"+CovisintConfigService.getRESTURL() + param);

			conn = (HttpURLConnection) url.openConnection();

			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, "OP_IACC069_MyH");
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, username);
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty("OWNERID", ownerid);
			conn.setRequestProperty("IDMID", idmid);
			conn.setRequestProperty("REGID", regId);
			conn.setRequestProperty("VIN", vin);
			conn.setRequestProperty("YEAR", year);
			conn.setRequestProperty("MODEL", model);
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setDoOutput(true);


			conn.getOutputStream().write(jsonDetails.toString().getBytes());


			int responseCode=0;;
			try {
				responseCode = conn.getResponseCode();
			} catch (Exception e) {
				// TODO Auto-generated catch block

			}
			log.info("Response Code is " + responseCode);

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			}
			String jsonText = ConnectionUtils.readAll(in);

			json = new JSONObject(jsonText);

			//results = (JSONObject) json.get("E_IFRESULT");

		} catch (Exception e) {

		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {

				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;
	}

	@Override
	public JSONObject getPermissionsForHyundai( String userName,String vin, String idmid,String token,String driver2ndEmail,String domainSelector)
	{

		log.debug("Enter getPermissionsForHyundai::"+domainSelector);
		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;
		Reader in = null;
		try {

			String decrypted_token = token;


			HttpClient httpclient = HttpClientBuilder.create().build();
			httpGet = new HttpGet(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + _2ND_DRIVER_FEATURES));

			httpGet.addHeader(ServiceConstants.COVISINT_IFID, "OP_OACC046_MyH");
			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_USERNAME, userName);
			httpGet.addHeader(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			httpGet.addHeader(ServiceConstants.COVISINT_VIN, vin);

			httpGet.addHeader(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);
			httpGet.addHeader("IDMID", idmid);
			httpGet.addHeader(ServiceConstants.REGISTERED_SECONDRY_DRIVER_EMAIL, driver2ndEmail);

			httpGet.addHeader(ServiceConstants.COVISINT_AUTHORIZATION, (String)config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));

			response = httpclient.execute(httpGet);

			// verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}

			String getResult = EntityUtils.toString(response.getEntity());

			json = (JSONObject) new JSONTokener(getResult).nextValue();

		}catch (Exception e) {
			log.error(e.getMessage());
		} finally{
			if(httpGet!=null){
				httpGet.releaseConnection();
			}
		}

		return json;
	}

	@Override
	public JSONObject getCPOpdf(String username, String vin, String token, String domainSelector) {

		log.debug("-----Enter getCPOpdf::-----" + domainSelector);

		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;

		try {
			String decrypted_token = token;

			HttpClient httpclient = HttpClientBuilder.create().build();
			httpGet = new HttpGet(CovisintConfigService.getRESTURL()+config.getPropValue(domainSelector + "cpoindicator"));

			httpGet.addHeader(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + "cpoindicatorIFID"));
			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_USERNAME, username);
			httpGet.addHeader(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			httpGet.addHeader(ServiceConstants.COVISINT_VIN, vin);
			log.debug("-----Enter VIN::-----" + vin);
			httpGet.addHeader(ServiceConstants.AUTHORIZATION, (String)config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));

			response = httpclient.execute(httpGet);
			log.debug("-----Enter response::-----" + response);

			// verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}
			String getResult = EntityUtils.toString(response.getEntity());
			json = (JSONObject) new JSONTokener(getResult).nextValue();



		}catch (Exception e) {
			log.error(e.getMessage());
		} finally{
			if(httpGet!=null){
				httpGet.releaseConnection();
			}
		}

		return json;
	}
	public JSONObject getpinstatus(String username, String token, String domainSelector) {
		log.debug("-----get pin status::-----" + domainSelector);

		JSONObject json = null;
		HttpResponse response = null;
		HttpGet httpGet = null;

		try {
			String decrypted_token = token;

			HttpClient httpclient = HttpClientBuilder.create().build();
			httpGet = new HttpGet(CovisintConfigService.getRESTURL()+config.getPropValue(domainSelector + "getpinstatus"));

			httpGet.addHeader(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + "pinstatusIFID"));
			httpGet.addHeader(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			httpGet.addHeader(ServiceConstants.COVISINT_USERNAME, username);
			httpGet.addHeader(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);
			httpGet.addHeader(ServiceConstants.COVISINT_CONTENTTYPE,ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);
			httpGet.addHeader(ServiceConstants.AUTHORIZATION, (String)config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));

			response = httpclient.execute(httpGet);
			log.debug("-----Enter response::-----" + response);


			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + statusCode);
			}
			String getResult = EntityUtils.toString(response.getEntity());
			json = (JSONObject) new JSONTokener(getResult).nextValue();

		}catch (Exception e) {
			log.error(e.getMessage());
		} finally{
			if(httpGet!=null){
				httpGet.releaseConnection();
			}
		}

		return json;
	}
	@Override
	public JSONObject putupdatewelcome(String token, String username,String domainSelector) {

		log.debug("put update welcome:"+domainSelector);
		JSONObject json = null;
		HttpURLConnection conn = null;
		JSONObject userParam = new JSONObject();
		Reader in = null;
		try {

			String decrypted_token = token;

			URL url = new URL(CovisintConfigService.getRESTURL() + config.getPropValue(domainSelector + PUT_UPDATE_WELCOME));

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);

			conn.setRequestMethod(ServiceConstants.COVISINT_PUT);
			conn.setRequestProperty(ServiceConstants.AUTHORIZATION, (String) config.getPropValue(domainSelector + ServiceConstants.AUTHORIZATION));
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, (String) config.getPropValue(domainSelector + ServiceConstants.PUT_UPDATE_WELCOME_IFID));
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_SENDER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_RECEIVER_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESSTOKEN, decrypted_token);

			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, username);

			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);

			conn.setDoOutput(true);
			conn.getOutputStream().write(userParam.toString().getBytes());

			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			if (responseCode != HttpStatus.SC_OK) {
				log.info("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);



		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		// TODO Auto-generated method stub
		return json;

	}

	@Override
	public JSONObject verifyEmailLink(String userId, String verify_email_token,String accessToken) {


		JSONObject json = null;
		HttpURLConnection conn = null;
		Reader in=null;
		HttpResponse response = null;

		String param = "/vsUser_MyH/MFAEMAILTOKENVERIFY";

		try {

			JSONObject requestBodyJson = new JSONObject("{}");
			URL url = new URL(CovisintConfigService.getRESTURL() + param);
			log.debug("Request URL :: {}", url);

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
			conn.setReadTimeout(ConnectionUtils.READ_TIMEOUT);
			conn.setRequestMethod(ServiceConstants.COVISINT_POST);
			conn.setRequestProperty(ServiceConstants.COVISINT_COMPANY, ServiceConstants.COVISINT_COMPANY_VALUE);
			conn.setRequestProperty(ServiceConstants.COVISINT_SENDER, ServiceConstants.COVISINT_VALUE_ESB);
			conn.setRequestProperty(ServiceConstants.COVISINT_RECEIVER, ServiceConstants.COVISINT_VALUE_OP);
			conn.setRequestProperty(ServiceConstants.COVISINT_ACCESS_TOKEN, accessToken);
			conn.setRequestProperty(ServiceConstants.EMAIL_VERIFY_TOKEN, verify_email_token);
			conn.setRequestProperty(ServiceConstants.COVISINT_IFID, "OP_IUSR067_MyH");
			conn.setRequestProperty(ServiceConstants.COVISINT_AUTHORIZATION, CovisintConfigService.getAuthorization());
			conn.setRequestProperty(ServiceConstants.COVISINT_USERNAME, userId);
			conn.setRequestProperty(ServiceConstants.COVISINT_FROM, ServiceConstants.COVISINT_FROM_VALUE);

			conn.setRequestProperty(ServiceConstants.COVISINT_CONTENTTYPE,
					ServiceConstants.COVISINT_CONTENT_TYPE_VALUE);

			conn.setDoOutput(true);
			conn.getOutputStream().write(requestBodyJson.toString().getBytes());
			int responseCode = conn.getResponseCode();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			if (responseCode != HttpStatus.SC_OK) {
				log.debug("Response Code is " + responseCode);
			}

			String jsonText = ConnectionUtils.readAll(in);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error :: {}",e.getStackTrace());
			String jsonString = "{\"status\" : \"ServerError\"}";
			try {
				json = new JSONObject(jsonString);
			} catch (JSONException ex) {
				throw new RuntimeException(ex);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}

			if (conn != null) {
				conn.disconnect();
			}
		}

		return json;
	}


}