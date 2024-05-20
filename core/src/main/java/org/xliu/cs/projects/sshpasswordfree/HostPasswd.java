package org.xliu.cs.projects.sshpasswordfree;

import java.util.Objects;

public class HostPasswd {
    private String host;
    private String password;

    public HostPasswd(String host, String password) {
        this.host = host;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        HostPasswd that = (HostPasswd) o;
        return Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host);
    }

    @Override
    public String toString() {
        return host + " " + password;
    }
}
