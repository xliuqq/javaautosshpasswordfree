package com.hongcheng.aibigdata.tools.sshpassowordfree;

import java.util.Objects;

public class HostPasswd {
    String host;
    String password;

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
