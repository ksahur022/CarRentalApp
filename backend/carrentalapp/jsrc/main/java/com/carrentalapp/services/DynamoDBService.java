package com.carrentalapp.services;



import com.carrentalapp.models.Admin;
import com.carrentalapp.models.SupportAgent;
import com.carrentalapp.models.User;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import javax.inject.Inject;
import java.time.Instant;

public class DynamoDBService {
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final DynamoDbTable<SupportAgent> supportAgentTable;
    private final DynamoDbTable<Admin> adminTable;

    @Inject
    public DynamoDBService(DynamoDbEnhancedClient dynamoDbEnhancedClient, DynamoDbTable<SupportAgent> supportAgentTable,DynamoDbTable<Admin> adminTable) {
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
        // Access the support_agent table using the SupportAgent model
        this.supportAgentTable = dynamoDbEnhancedClient.table("SupportAgent", TableSchema.fromBean(SupportAgent.class));
        this.adminTable = dynamoDbEnhancedClient.table("Admin", TableSchema.fromBean(Admin.class));

    }


//    public void saveUser(User user, String cognitoSub, String role) {
//        user.setCognitoSub(cognitoSub);
//        user.setRole(role);
//        user.setCreatedAt(Instant.now().toString());
//        userTable.putItem(PutItemEnhancedRequest.builder(User.class).item(user).build());
//    }

//    public User getUserByEmail(String email) {
//        try {
//            // Assuming email is the partition key
//            Key key = Key.builder().partitionValue(email).build();
//            return userTable.getItem(key);
//
//            // If email is not the partition key but a GSI exists:
//            /*
//            QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
//                    .queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue(email).build()))
//                    .build();
//
//            Iterator<User> results = userTable.index("email-index").query(queryRequest).items().iterator();
//            return results.hasNext() ? results.next() : null;
//            */
//        } catch (Exception e) {
//            System.err.println("Error retrieving user by email: " + e.getMessage());
//            return null;
//        }

    /**
     * Check if the email exists in the Admin table.
     */
//    public boolean isAdmin(String email) {
//        try {
//            Key key = Key.builder()
//                    .partitionValue(email)
//                    .build();
//
//            Admin admin = adminTable.getItem(key);
//            return admin != null;
//        } catch (DynamoDbException e) {
//            System.err.println("Error checking admin table: " + e.getMessage());
//            return false;
//        }
//    }

    /**
     * Add a new admin user to the Admin table.
     */
//    public void addAdmin(String email) {
//        try {
//            Admin admin = new Admin();
//            admin.setEmail(email);
//            adminTable.putItem(admin);
//            System.out.println("Successfully added admin: " + email);
//        } catch (DynamoDbException e) {
//            System.err.println("Error adding admin: " + e.getMessage());
//            throw new RuntimeException("Failed to add admin: " + e.getMessage(), e);
//        }
//    }
    /**
     * Optional: A method to save a SupportAgent (or any other entity) to DynamoDB.
     * @param supportAgent the SupportAgent entity to save
     */
    public void saveSupportAgent(SupportAgent supportAgent) {
        try {
            supportAgentTable.putItem(PutItemEnhancedRequest.builder(SupportAgent.class)
                    .item(supportAgent)
                    .build());
        } catch (DynamoDbException e) {
            // Handle DynamoDB exception
            System.err.println("Error saving support agent: " + e.getMessage());
        }
    }
    /**
     * Check if the email exists in the Admin table.
     */
    public boolean isAdmin(String email) {
        try {
            Key key = Key.builder()
                    .partitionValue(email)
                    .build();

            Admin admin = adminTable.getItem(r -> r.key(key));
            return admin != null;
        } catch (DynamoDbException e) {
            System.err.println("Error checking admin table: " + e.getMessage());
            return false;
        }
    }

    public void addAdmin(String email) {
        try {
            Admin admin = new Admin();
            admin.setEmail(email);
            adminTable.putItem(admin);
            System.out.println("Successfully added admin: " + email);
        } catch (DynamoDbException e) {
            System.err.println("Error adding admin: " + e.getMessage());
            throw new RuntimeException("Failed to add admin: " + e.getMessage(), e);
        }
    }
    public boolean isSupportAgent(String email) {
        try {
            Key key = Key.builder()
                    .partitionValue(email)
                    .build();

            SupportAgent supportAgent = supportAgentTable.getItem(key);
            return supportAgent != null;
        } catch (DynamoDbException e) {
            System.err.println("Error checking support agent table: " + e.getMessage());
            return false;
        }
    }
//    public void addAdmin(String email) {
//        try {
//            Admin admin = new Admin();
//            admin.setEmail(email);
//            adminTable.putItem(admin);
//            System.out.println("Successfully added admin: " + email);
//        } catch (DynamoDbException e) {
//            System.err.println("Error adding admin: " + e.getMessage());
//            throw new RuntimeException("Failed to add admin: " + e.getMessage(), e);
//        }
//    }
    public void removeAdmin(String email) {
        try {
            Key key = Key.builder()
                    .partitionValue(email)
                    .build();

            adminTable.deleteItem(key);
            System.out.println("Successfully removed admin: " + email);
        } catch (DynamoDbException e) {
            System.err.println("Error removing admin: " + e.getMessage());
            throw new RuntimeException("Failed to remove admin: " + e.getMessage(), e);
        }
    }

}


