package com.incture.config;

import org.springframework.web.bind.annotation.RestController;

import com.sap.cloud.security.xsuaa.tokenflows.XsuaaTokenFlows;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.sap.cloud.security.xsuaa.client.OAuth2TokenResponse;
import com.sap.cloud.security.xsuaa.token.Token;
import com.sap.cloud.security.xsuaa.tokenflows.TokenFlowException;
import com.sap.cloud.security.xsuaa.tokenflows.XsuaaTokenFlows;

import static com.sap.cloud.security.xsuaa.token.TokenClaims.CLAIM_USER_NAME;

@RestController
public class TestController {
 
	private static final Logger logger = LoggerFactory.getLogger(TestController.class);
	private XsuaaTokenFlows tokenFlows;
	private DataService dataService;
	
	@Autowired
    public TestController(XsuaaTokenFlows tokenFlows, DataService dataService) {
        this.tokenFlows = tokenFlows;
        this.dataService = dataService;
    }

    @GetMapping("/health")
    public String sayHello() { return "I'm alright"; }
    
    
    @GetMapping("/v1/sayHello")
    public Map<String, String> sayHello(@AuthenticationPrincipal Token token) {

        logger.info("Got the Xsuaa token: {}", token.getAppToken());
        logger.info(token.toString());

        Map<String, String> result = new HashMap<>();
        result.put("grant type", token.getGrantType());
        result.put("client id", token.getClientId());
        result.put("subaccount id", token.getSubaccountId());
        result.put("zone id", token.getZoneId());
        result.put("logon name", token.getLogonName());
        result.put("family name", token.getFamilyName());
        result.put("given name", token.getGivenName());
        result.put("email", token.getEmail());
        result.put("authorities", String.valueOf(token.getAuthorities()));
        result.put("scopes", String.valueOf(token.getScopes()));

        return result;
    }
    
    @GetMapping("/v2/sayHello")
    public String sayHello(@AuthenticationPrincipal Jwt jwt) {

        logger.info("Got the JWT: {}", jwt);
        logger.info(jwt.getClaimAsString(CLAIM_USER_NAME));
        logger.info(jwt.toString());

        return "Hello Jwt-Protected World!";
    }
    @GetMapping("/v1/method")
    @PreAuthorize("hasAuthority('Read')")
    public String callMethodRemotely() {
        return "Read-protected method called!";
    }
    @GetMapping("/v1/getAdminData")
    public String readFromDataService() {
        return dataService.readSensitiveData();
    }
    @GetMapping("/v3/requestClientCredentialsToken")
    public String requestClientCredentialsToken() throws TokenFlowException {

        OAuth2TokenResponse clientCredentialsTokenResponse = tokenFlows.clientCredentialsTokenFlow().execute();
        logger.info("Got the Client Credentials Token: {}", clientCredentialsTokenResponse.getAccessToken());

        return clientCredentialsTokenResponse.getDecodedAccessToken().getPayload();
    }
    @GetMapping("/v3/requestUserToken")
    public String requestUserToken(@AuthenticationPrincipal Token token) throws TokenFlowException {
        OAuth2TokenResponse userTokenResponse = tokenFlows.userTokenFlow()
                .token(token.getAppToken())
                .subdomain(token.getSubdomain())
                .execute();

        logger.info("Got the exchanged token for 3rd party service: {}", userTokenResponse);
        logger.info("You can now call the 3rd party service passing the exchanged token value: {}. ", userTokenResponse);

        return "<p>The access-token (decoded):</p><p>" + userTokenResponse.getDecodedAccessToken().getPayload()
                + "</p><p>The refresh-token: </p><p>" + userTokenResponse.getRefreshToken()
                + "</p><p>The access-token (encoded) can be found in the logs 'cf logs spring-security-xsuaa-usage --recent'</p>";
    }
    @GetMapping("/v3/requestRefreshToken/{refreshToken}")
    public String requestRefreshToken(@AuthenticationPrincipal Jwt jwt, @PathVariable("refreshToken") String refreshToken) throws TokenFlowException {

        OAuth2TokenResponse refreshTokenResponse = tokenFlows.refreshTokenFlow()
        		.refreshToken(refreshToken)
                .execute();
 
        logger.info("Got the access token for the refresh token: {}", refreshTokenResponse.getAccessToken());
        logger.info("You could now inject this into Spring's SecurityContext, using: SpringSecurityContext.init(...).");

        return refreshTokenResponse.getDecodedAccessToken().getPayload();
    }
}
