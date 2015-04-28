package gr.com.ist.commun.core.domain.security;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name="PERSISTENT_LOGINS")
public class PersistentLogin {
    
    @NotNull
    @Column(length=64)
    private String username;
    @Column(length=64)
    @Id
    private String series;
    @NotNull
    @Column(length=64)
    private String token;
    @NotNull
    private Date lastUsed;
    
    public PersistentLogin() {
        super();
    }
    
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getSeries() {
        return series;
    }
    public void setSeries(String series) {
        this.series = series;
    }
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
    public Date getLastUsed() {
        return lastUsed;
    }
    public void setLastUsed(Date lastUsed) {
        this.lastUsed = lastUsed;
    }
    
}
