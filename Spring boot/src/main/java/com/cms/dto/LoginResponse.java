package com.cms.dto;



public class LoginResponse {


    private String token;

    private String role;



    public LoginResponse(String token, String role) {

        this.setToken(token);

        this.setRole(role);

    }



	public String getToken() {
		return token;
	}



	public void setToken(String token) {
		this.token = token;
	}



	public String getRole() {
		return role;
	}



	public void setRole(String role) {
		this.role = role;
	}



    // Getters and Setters

}