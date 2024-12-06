package com.mygenesis.components.core.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adaptables ={Resource.class,SlingHttpServletRequest.class},defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class CreateAccountModel {

    @Default(values = "MyHyundai")
    @ValueMapValue
    private String heading;

    @Default(values = "Create account")
    @ValueMapValue
    private String pageTitle;

    @ValueMapValue
    private String nextPageButtonText;

    @ValueMapValue
    private String previousPageButtonText;

    @ValueMapValue
    private String rightSectionImage;

    @ValueMapValue
    private String rightSectionImageAltText;

    @ValueMapValue
    private String alreadyAccountText;

    @ValueMapValue
    private String loginText;

    @ValueMapValue
    private String loginLink;

    @ValueMapValue
    private String loginLinkTarget;

    @ValueMapValue
    private String emailPlaceholderText;

    @ValueMapValue
    private String passwordPlaceholderText;

    @Default(values = "*required")
    @ValueMapValue
    private String requiredText;

    @ValueMapValue
    private String firstName;

    @ValueMapValue
    private String lastName;

    @ValueMapValue
    private String zipCode;

    @ValueMapValue
    private String mobilePhone;

    @ValueMapValue
    private String accountVerificationMsg;

    @ValueMapValue
    private String agreeText;

    @ValueMapValue
    private String termsOfusetext;

    @ValueMapValue
    private String termsOfUseLink;

    @ValueMapValue
    private String termsOfUseTarget;

    @ValueMapValue
    private String termErrorMsgTxt;

    @ValueMapValue
    private String codeVerificationText;

    @ValueMapValue
    private String codeVerificationErrorText;

    @ValueMapValue
    private String resendCodeText;

    @ValueMapValue
    private String createAccountButtonText;

    @ValueMapValue
    private String emailValidationTxt;

    @ValueMapValue
    private String emailConfirmationTxt;

    public String getEmailValidationTxt() {
        return emailValidationTxt;
    }

    public String getEmailConfirmationTxt() {
        return emailConfirmationTxt;
    }

    public String getHeading() {
        return heading;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public String getNextPageButtonText() {
        return nextPageButtonText;
    }

    public String getPreviousPageButtonText() {
        return previousPageButtonText;
    }

    public String getRightSectionImage() {
        return rightSectionImage;
    }

    public String getRightSectionImageAltText() {
        return rightSectionImageAltText;
    }

    public String getAlreadyAccountText() {
        return alreadyAccountText;
    }

    public String getLoginText() {
        return loginText;
    }

    public String getLoginLink() {
        return loginLink;
    }

    public String getLoginLinkTarget() {
        return loginLinkTarget;
    }

    public String getEmailPlaceholderText() {
        return emailPlaceholderText;
    }

    public String getPasswordPlaceholderText() {
        return passwordPlaceholderText;
    }

    public String getRequiredText() {
        return requiredText;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public String getAccountVerificationMsg() {
        return accountVerificationMsg;
    }

    public String getAgreeText() {
        return agreeText;
    }

    public String getTermsOfusetext() {
        return termsOfusetext;
    }

    public String getTermsOfUseLink() {
        return termsOfUseLink;
    }

    public String getTermsOfUseTarget() {
        return termsOfUseTarget;
    }

    public String getTermErrorMsgTxt() {
        return termErrorMsgTxt;
    }

    public String getCodeVerificationText() {
        return codeVerificationText;
    }

    public String getCodeVerificationErrorText() {
        return codeVerificationErrorText;
    }

    public String getResendCodeText() {
        return resendCodeText;
    }

    public String getCreateAccountButtonText() {
        return createAccountButtonText;
    }
}
