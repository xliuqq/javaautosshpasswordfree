package org.xliu.cs.projects.sshpasswordfree;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HostPasswdTest {

    @Test
    void testEquals() {
        HostPasswd hostPasswd = new HostPasswd("hostA", "pass");
        HostPasswd hostPasswd2 = new HostPasswd("hostA", null);
        HostPasswd hostPasswd3 = new HostPasswd("hostB", "pass");

        assertEquals(hostPasswd, hostPasswd2);
        assertNotEquals(hostPasswd, hostPasswd3);
    }
}