package com.carrentalapp.services;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class CognitoService {
    private final CognitoIdentityProviderClient cognitoClient;
    private final String userPoolId;
    private final String clientId;
    private final DynamoDBService dynamoDBService;
    @Inject
    public CognitoService(CognitoIdentityProviderClient cognitoClient,DynamoDBService dynamoDBService) {
        this.cognitoClient = cognitoClient;
        this.dynamoDBService = dynamoDBService;
        this.userPoolId = System.getenv("COGNITO_ID");
        this.clientId = System.getenv("CLIENT_ID");
    }

    //    public String registerUser(String email, String password, String firstName, String lastName) {
//        SignUpRequest request = SignUpRequest.builder()
//                .clientId(clientId)
//                .username(email)
//                .password(password)
//                .userAttributes(
//                        AttributeType.builder().name("email").value(email).build(),
//                        AttributeType.builder().name("given_name").value(firstName).build(),
//                        AttributeType.builder().name("family_name").value(lastName).build()
//                )
//                .build();
//        cognitoClient.signUp(request);
//
//        return confirmUserSignUp(email, password);
//    }
   /* public String registerUser(String email, String password, String firstName, String lastName) {
        // 1. Sign up the user
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .clientId(clientId)
                .username(email)
                .password(password)
                .userAttributes(
                        AttributeType.builder().name("email").value(email).build(),
                        AttributeType.builder().name("given_name").value(firstName).build(),
                        AttributeType.builder().name("family_name").value(lastName).build()
                )
                .build();
        cognitoClient.signUp(signUpRequest);

        // 2. Confirm the user programmatically (critical fix)
        AdminConfirmSignUpRequest confirmRequest = AdminConfirmSignUpRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .build();
        cognitoClient.adminConfirmSignUp(confirmRequest);

        // 3. Authenticate to get the ID token
        return confirmUserSignUp(email, password);
    }*/
    public String registerUser(String email, String password, String firstName, String lastName,String role) {
        // 1. Create user with email_verified = true
        AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .userAttributes(
                        AttributeType.builder().name("email").value(email).build(),
                        AttributeType.builder().name("email_verified").value("true").build(),
                        AttributeType.builder().name("given_name").value(firstName).build(),
                        AttributeType.builder().name("family_name").value(lastName).build(),
                        AttributeType.builder().name("custom:role").value(role).build()
                )
                .temporaryPassword(password) // sets a temp password
                .messageAction(MessageActionType.SUPPRESS) // don't send email
                .build();
        cognitoClient.adminCreateUser(createUserRequest);

        // 2. Set the password permanently
        AdminSetUserPasswordRequest setPasswordRequest = AdminSetUserPasswordRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .password(password)
                .permanent(true)
                .build();
        cognitoClient.adminSetUserPassword(setPasswordRequest);

        // 3. Authenticate and get token
        return confirmUserSignUp(email, password);
    }


    private String confirmUserSignUp(String email, String password) {
        try {
            AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                    .userPoolId(userPoolId)
                    .clientId(clientId)
                    .authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                    .authParameters(Map.of("USERNAME", email, "PASSWORD", password))
                    .build();

            AdminInitiateAuthResponse authResponse = cognitoClient.adminInitiateAuth(authRequest);
            return authResponse.authenticationResult().idToken();
        } catch (Exception e) {
            throw new RuntimeException("Error confirming user sign-up: " + e.getMessage(), e);
        }
    }

    public boolean isEmailRegistered(String email) {
        try {
            cognitoClient.adminGetUser(AdminGetUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .build());
            return true;
        } catch (UserNotFoundException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Error checking email registration: " + e.getMessage(), e);
        }

    }

    //login code

    public Map<String, Object> authenticateUser(String email, String password) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Create authentication request
            AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                    .userPoolId(userPoolId)
                    .clientId(clientId)
                    .authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                    .authParameters(Map.of(
                            "USERNAME", email,
                            "PASSWORD", password
                    ))
                    .build();

            // Perform authentication
            AdminInitiateAuthResponse authResponse = cognitoClient.adminInitiateAuth(authRequest);

            // Extract authentication result
            AuthenticationResultType authResult = authResponse.authenticationResult();

            // Add tokens to result
            result.put("idToken", authResult.idToken());
            result.put("accessToken", authResult.accessToken());
            result.put("refreshToken", authResult.refreshToken());
            result.put("expiresIn", authResult.expiresIn());

            return result;

        } catch (NotAuthorizedException e) {
            // Incorrect username or password
            result.put("error", "Incorrect username or password");
            return result;
        } catch (UserNotFoundException e) {
            // User does not exist
            result.put("error", "User not found");
            return result;
        } catch (UserNotConfirmedException e) {
            // User exists but isn't confirmed
            result.put("error", "User is not confirmed");
            return result;
        } catch (Exception e) {
            // Other errors
            result.put("error", "Authentication failed: " + e.getMessage());
            return result;
        }
    }

    public Map<String, String> getUserAttributes(String email) {
        try {
            AdminGetUserRequest userRequest = AdminGetUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .build();

            AdminGetUserResponse userResponse = cognitoClient.adminGetUser(userRequest);

            // Convert list of attributes to a map
            return userResponse.userAttributes().stream()
                    .collect(Collectors.toMap(
                            AttributeType::name,
                            AttributeType::value
                    ));

        } catch (Exception e) {
            throw new RuntimeException("Error retrieving user attributes: " + e.getMessage(), e);
        }
    }

    public void updateUserRole(String email, String role) {
        try {
            // Create the attribute to update
            AttributeType roleAttribute = AttributeType.builder()
                    .name("custom:role")
                    .value(role)
                    .build();

            // Create the update request
            AdminUpdateUserAttributesRequest updateRequest = AdminUpdateUserAttributesRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .userAttributes(roleAttribute)
                    .build();

            // Execute the update
            cognitoClient.adminUpdateUserAttributes(updateRequest);
            System.out.println("Successfully updated role for user: " + email + " to " + role);
        } catch (Exception e) {
            System.err.println("Error updating user role: " + e.getMessage());
            throw new RuntimeException("Failed to update user role: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the user ID (sub) from an ID token
     * @param idToken The ID token from the Authorization header
     * @return The user ID extracted from the token
     */
    public String getUserIdFromToken(String idToken) {
        try {
            // For ID tokens, we need to decode and parse the JWT
            // ID tokens are JWTs with three parts separated by dots
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new RuntimeException("Invalid token format");
            }

            // Decode the payload (second part)
            // Add padding if needed
            String base64Payload = parts[1];
            while (base64Payload.length() % 4 != 0) {
                base64Payload += "=";
            }

            String payload = new String(Base64.getUrlDecoder().decode(base64Payload), StandardCharsets.UTF_8);

            // Parse the JSON
            JsonObject jsonPayload = new Gson().fromJson(payload, JsonObject.class);

            // Extract the "sub" claim which contains the user ID
            if (jsonPayload.has("sub")) {
                return jsonPayload.get("sub").getAsString();
            }

            throw new RuntimeException("User ID not found in token");
        } catch (Exception e) {
            throw new RuntimeException("Error validating token: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the email from an ID token
     * @param idToken The ID token from the Authorization header
     * @return The email extracted from the token
     */
    public String getEmailFromToken(String idToken) {
        try {
            // For ID tokens, we need to decode and parse the JWT
            // ID tokens are JWTs with three parts separated by dots
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new RuntimeException("Invalid token format");
            }

            // Decode the payload (second part)
            // Add padding if needed
            String base64Payload = parts[1];
            while (base64Payload.length() % 4 != 0) {
                base64Payload += "=";
            }

            String payload = new String(Base64.getUrlDecoder().decode(base64Payload), StandardCharsets.UTF_8);

            // Parse the JSON
            JsonObject jsonPayload = new Gson().fromJson(payload, JsonObject.class);

            // Extract the "email" claim which contains the user's email
            if (jsonPayload.has("email")) {
                return jsonPayload.get("email").getAsString();
            }

            throw new RuntimeException("Email not found in token");
        } catch (Exception e) {
            throw new RuntimeException("Error extracting email from token: " + e.getMessage(), e);
        }
    }
}