/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.voting.conference;

import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author surcadmin
 */
public class Conference implements Serializable {
    private ArrayList<Principal> principals;
    private String question;
    private final int confId;
    //collection for ignor list
    //collection for already vote
    //collection for discard members
    
    public Conference(String question, Principal initiator, int id) {
        this.question = question;
        principals = new ArrayList<Principal>();
        principals.add(initiator);
        confId = id;
    }
    
    public Conference(Conference c) {
        this.question = new String(c.getQuestion());
        this.principals = new ArrayList<Principal>(c.getPrincipals());
        this.confId = c.getId();
    }
    
    public boolean addMember(Principal p) {
        if (!memberContains(p)) {
            //Principal p1 = new Principal(p); 
            this.principals.add(p);
            return true;
        }
        return false;
    }
    
    public boolean removeMember(Principal p) {
        if (memberContains(p)) {
            //Principal p1 = new Principal(p); 
            this.principals.remove(p);
            return true;
        }
        return false;
    }
    
    public boolean memberContains(Principal p) {
        for (int i=0; i<principals.size(); i++) {
            if (principals.get(i).equals(p)) {
                return true;
            }
        }
        return false;
    }
    
    public int getId() {
        return this.confId;
    }
    
    public String getQuestion() {
	return this.question;
    }
    
    public ArrayList<Principal> getPrincipals() {
	return this.principals;
    }
    
    public String toString() {
        return "Voting: " + this.question + "\nInitiator: " + this.principals.get(0).getName() + "; mailto: " + this.principals.get(0).getEmail();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + this.confId;
        return hash;
    }
    
    @Override
    public boolean equals(Object c) {
	if (c instanceof Conference) {
	    return (this.confId == ((Conference) c).getId());
	}
	return false;
    }
}