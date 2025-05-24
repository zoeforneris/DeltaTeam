/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model.entity;

/**
 *
 * @author zoeforneris
 */
public class Admin {

    public String username;
    public String password;

    public Admin(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
        public Admin(String username) {
        this.username = username;
    }
    
    
    public Admin(){
        
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
