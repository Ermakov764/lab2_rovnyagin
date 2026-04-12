package ru.hse.lab2.api.request;

public class ViewerRequest {

    private String name;
    private String email;

    public ViewerRequest() {
    }

    public ViewerRequest(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
