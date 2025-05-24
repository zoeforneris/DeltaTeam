/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model.entity;

/**
 *
 * @author zoeforneris
 */
public class User {
    
    public String username;
    public String password;
    
    public User(String username, String password){
        this.username = username;
        this.password = password;
    }
    
       public User(String username){
        this.username = username;
    }
    public User(){
        
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
