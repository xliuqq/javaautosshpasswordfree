package com.hongcheng.aibigdata.tools.sshpassowordfree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SshExec {

    public static int run(Session session, String command) throws JSchException, IOException {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setPty(false);
        channel.setCommand(command);

        InputStream in = channel.getInputStream();
        InputStream er = channel.getErrStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(er, StandardCharsets.UTF_8));

        channel.connect(3000);

        //        StringBuilder builder = new StringBuilder();
        String buf;
        while ((buf = reader.readLine()) != null) {
            //            builder.append(buf).append("\n");
        }
        String errbuf;
        while ((errbuf = errorReader.readLine()) != null) {
            //            builder.append(errbuf).append("\n");
        }
        // 必须等待执行结果全部读取完之后，才能拿到状态
        int status = channel.getExitStatus();

        channel.disconnect();

        //        System.out.println(builder.toString());

        return status;
    }


    public static int runWithOut(Session session, String command, StringBuilder out) throws JSchException, IOException {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setPty(false);
        channel.setCommand(command);

        InputStream in = channel.getInputStream();
        InputStream er = channel.getErrStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(er, StandardCharsets.UTF_8));

        channel.connect(3000);

        String buf;
        while ((buf = reader.readLine()) != null) {
            out.append(buf).append("\n");
        }
        String errbuf;
        while ((errbuf = errorReader.readLine()) != null) {
            out.append(errbuf).append("\n");
        }
        // 必须等待执行结果全部读取完之后，才能拿到状态
        int status = channel.getExitStatus();

        channel.disconnect();

        return status;
    }
}
