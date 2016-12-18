package me.legrange.yaml.app.config.snippets;

import me.legrange.yaml.app.config.annotation.NotBlank;
import me.legrange.yaml.app.config.annotation.NotNull;


/**
 *
 * @since 1.0
 * @author Gideon le Grange https://github.com/GideonLeGrange
 */
public class Database {

    public void setDriver(String driver)  {
        this.driver = driver;
    }

    public String getDriver() {
        return driver;
    }

    public String getUrl() {
        return url;
    }


    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    
    @NotBlank
    private String driver;
    @NotBlank
    private String host;
    @NotBlank
    private String database;
    @NotBlank
    private String user;
    @NotNull
    private String password;
    @NotBlank
    private String url;
}
