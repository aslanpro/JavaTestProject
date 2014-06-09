/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.voting.conference;

import java.io.Serializable;
import java.util.Arrays;

/**
 *
 * @author surcadmin
 */
public class Principal implements Serializable{

    private String name;
    private String eMail;
    private byte[] id;
    private boolean middleTrust = false;
    public static final int ID_SIZE = 16;

    public Principal() {
        name = null;
        id = null;
        eMail = null;
    }

    public Principal(String name, String eMail, byte[] id, boolean trust) {
        this.name = name;
        this.eMail = eMail;
        this.id = Arrays.copyOfRange(id, 0, ID_SIZE);
        this.middleTrust = trust;
    }
    
    public Principal(Principal p) {
        this.name = new String(p.getName());
        this.eMail = new String(p.getEmail());
        this.id = Arrays.copyOfRange(p.getID(), 0, ID_SIZE);
        this.middleTrust = p.getTrust();
    }

    public String getName() {
        return this.name;
    }

    public String getEmail() {
        return this.eMail;
    }

    public byte[] getID() {
        return this.id;
    }
    
    public boolean getTrust() {
        return this.middleTrust;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String eMail) {
        this.eMail = eMail;
    }

    public void setID(byte[] id) {
        this.id = Arrays.copyOfRange(id, 0, ID_SIZE);
    }
    
    public void setTrust(boolean trust) {
        this.middleTrust = trust;
    }
    
    @Override
    public String toString() {
        if (this.middleTrust) 
            return this.name + "; " + this.eMail + "; " + "middle trust";
        return this.name + "; " + this.eMail + "; " + "high trust"; 
    }
    
    @Override
    public boolean equals(Object p) {
        if (p instanceof Principal) {
            if ((this.id != null) && (((Principal) p).getID() != null)) {
                return Arrays.equals(this.id, ((Principal) p).getID());
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 19 * hash + Arrays.hashCode(this.id);
        return hash;
    }
}