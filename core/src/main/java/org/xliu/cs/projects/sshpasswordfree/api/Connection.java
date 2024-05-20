package org.xliu.cs.projects.sshpasswordfree.api;

import com.jcraft.jsch.Session;

import java.io.Closeable;
import java.io.IOException;

public interface Connection extends Closeable {


    ExecResult runCmd(String command) throws IOException;

    ExecResult upload(String localFilePath, String remoteFilePath, int fileMode) throws IOException;


    ExecResult download(String localFilePath, String remoteFilePath) throws IOException;
}
