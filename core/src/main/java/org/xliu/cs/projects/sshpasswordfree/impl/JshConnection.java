package org.xliu.cs.projects.sshpasswordfree.impl;

import com.jcraft.jsch.*;
import org.xliu.cs.projects.sshpasswordfree.HostPasswd;
import org.xliu.cs.projects.sshpasswordfree.api.Connection;
import org.xliu.cs.projects.sshpasswordfree.api.ExecResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class JshConnection implements Connection {
    private static final JSch J_SCH = new JSch();
    private final Session session;

    public JshConnection(String username, HostPasswd hostPasswd) throws IOException {
        session = getSession(username, hostPasswd);
    }


    private Session getSession(String username, HostPasswd hostPasswd) throws IOException {
        try {
            Session tmpSession = J_SCH.getSession(username, hostPasswd.getHost(), 22);
            tmpSession.setPassword(hostPasswd.getPassword());
            tmpSession.setConfig("StrictHostKeyChecking", "no");
            tmpSession.connect(3000);
            return tmpSession;
        } catch (JSchException e) {
            throw new IOException(String.format("Get session for %s error", hostPasswd.getHost()), e);
        }

    }


    @Override
    public ExecResult runCmd(String command) throws IOException {
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setPty(false);
            channel.setCommand(command);

            InputStream in = channel.getInputStream();
            InputStream er = channel.getErrStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(er, StandardCharsets.UTF_8));

            channel.connect(3000);

            StringBuilder out = new StringBuilder();
            StringBuilder err = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append("\n");
            }
            while ((line = errorReader.readLine()) != null) {
                err.append(line).append("\n");
            }
            // 必须等待执行结果全部读取完之后，才能拿到状态
            int exitStatus = channel.getExitStatus();

            return new ExecResult(exitStatus, out.toString(), err.toString());
        } catch (JSchException e) {
            throw new IOException(e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }


    @FunctionalInterface
    interface SftpCallable {
        void call(ChannelSftp channel) throws SftpException;
    }

    @Override
    public ExecResult upload(String localFilePath, String remoteFilePath, int fileMode) throws IOException {

        return getExecResult(channel -> {
            channel.put(localFilePath, remoteFilePath);
            channel.chmod(fileMode, remoteFilePath);
        });
    }

    @Override()
    public ExecResult download(String localFilePath, String remoteFilePath) throws IOException {
        return getExecResult(channel -> {
            channel.get(remoteFilePath, localFilePath);
        });
    }


    private ExecResult getExecResult(SftpCallable callable) throws IOException {
        // exec 'scp -t rfile' remotely
        ChannelSftp channel = null;
        try {
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();

            // callable 如果执行出问题，会抛异常，因此不需要判断返回结构
            callable.call(channel);

            return new ExecResult(0, null, null);
        } catch (JSchException | SftpException e) {
            throw new IOException(e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (session != null) {
            session.disconnect();
        }
    }
}
