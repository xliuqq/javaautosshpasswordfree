package com.hongcheng.aibigdata.tools.sshpassowordfree;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class RemoteScpExec {

    public static void upload(Session session, String localFilePath, String remoteFilePath, int permission) throws JSchException, SftpException {
        // exec 'scp -t rfile' remotely
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");

        channel.connect();

        channel.put(localFilePath, remoteFilePath);
        channel.chmod(permission, remoteFilePath);

        channel.disconnect();
    }

    public static void download(Session session, String localFilePath, String remoteFilePath) throws JSchException, SftpException {
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");

        channel.connect();

        channel.get(remoteFilePath, localFilePath);

        channel.disconnect();
    }
}
