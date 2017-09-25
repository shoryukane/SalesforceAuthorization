package com.ambarkaar.database;

import java.sql.*;

public class SalesforceTokenDatabase {
	
	public Connection conn = null;
	public PreparedStatement preparedStmt = null;
	public ResultSet resultSet;
	public String query = null;
	
	public SalesforceTokenDatabase() {
		String myDriver = "org.postgresql.Driver";
		String myUrl = "jdbc:postgresql://localhost:5432/salesforcedb";
		try {
			Class.forName(myDriver);
			this.conn = DriverManager.getConnection(myUrl, "postgres", "sanjay123");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void insertIntoDatabase(String accessToken, String instanceUrl, String refreshToken, String signature, String scope, String idToken, String salesforceId, String tokenType, String issuedAt, String orgId, String userId) {
		try {
			
			query = "INSERT INTO salesforce_table (access_token, instance_url, refresh_token, signature, scope, id_token, salesforce_id, token_type, issued_at, org_id, user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			
			preparedStmt = this.conn.prepareStatement(query);
			preparedStmt.setString(1, accessToken);
			preparedStmt.setString(2, instanceUrl);
			preparedStmt.setString(3, refreshToken);
			preparedStmt.setString(4, signature);
			preparedStmt.setString(5, scope);
			preparedStmt.setString(6, idToken);
			preparedStmt.setString(7, salesforceId);
			preparedStmt.setString(8, tokenType);
			preparedStmt.setString(9, issuedAt);
			preparedStmt.setString(10, orgId);
			preparedStmt.setString(11, userId);
			preparedStmt.execute();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void removeFromDatabase() throws SQLException {
		resultSet = this.getFromDatabase();
		if(resultSet.next() != false ) {
			Statement statement = this.conn.createStatement();
			statement.executeUpdate("TRUNCATE salesforce_table");
		}
	}
	
	public ResultSet getFromDatabase() throws SQLException{
		query = "SELECT * FROM salesforce_table";
		Statement statement = this.conn.createStatement();
		resultSet = statement.executeQuery(query);
		
		return resultSet;
	}
	
	public ResultSet getByOrgId(String orgId) throws SQLException{
		query = "SELECT * FROM salesforce_table WHERE org_id = ?";
		preparedStmt = this.conn.prepareStatement(query);
		preparedStmt.setString(1, orgId);
		resultSet = preparedStmt.executeQuery();
		
		return resultSet;
	}
	
	public ResultSet getByOrgIdAndUserId(String orgId, String userId) throws SQLException{
		query = "SELECT * FROM salesforce_table WHERE org_id = ? AND user_id = ?";
		preparedStmt = this.conn.prepareStatement(query);
		preparedStmt.setString(1, orgId);
		preparedStmt.setString(2, userId);
		resultSet = preparedStmt.executeQuery();
		
		return resultSet;
	}
	
	public int countOrgIdData(String orgId) throws SQLException{
		int count = 0;
		query = "SELECT COUNT(*) FROM salesforce_table WHERE org_id = ?";
		preparedStmt = this.conn.prepareStatement(query);
		preparedStmt.setString(1, orgId);
		resultSet = preparedStmt.executeQuery();
		
		while(resultSet.next()) {
			count = resultSet.getInt(1);
		}
		return count;
	}
	
	public void updateAccessTokenUsingRefreshToken(String accessToken, String refreshToken) {
		query = "UPDATE salesforce_table SET access_token = ? WHERE refresh_token = ?";
		try {
			preparedStmt = this.conn.prepareStatement(query);
			preparedStmt.setString(1, accessToken);
			preparedStmt.setString(2, refreshToken);
			preparedStmt.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
	}
	
	public ResultSet getByRefreshToken(String refreshToken) throws SQLException{
		query = "SELECT * FROM salesforce_table WHERE refresh_token = ?";
		preparedStmt = this.conn.prepareStatement(query);
		preparedStmt.setString(1, refreshToken);
		resultSet = preparedStmt.executeQuery();
		
		return resultSet;
	}
}
