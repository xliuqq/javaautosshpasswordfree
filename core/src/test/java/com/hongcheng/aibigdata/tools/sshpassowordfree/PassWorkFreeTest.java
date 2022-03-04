package com.hongcheng.aibigdata.tools.sshpassowordfree;

import static org.junit.jupiter.api.Assertions.*;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.junit.jupiter.api.Test;

class PassWorkFreeTest {

    @Test
    void login() throws JSchException, IOException, InterruptedException {
        JSch jsch = new JSch();
        Session session = jsch.getSession("experiment", "172.16.2.135", 22);
        session.setPassword("hcdsj2022");
        // skip host-key check
        session.setConfig("StrictHostKeyChecking", "no");

        session.connect(30000);

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setPty(false);
        channel.setCommand("scp -f ~/.ssh/id_rsa.pub .");

        InputStream in = channel.getInputStream();
        InputStream er = channel.getErrStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(er, StandardCharsets.UTF_8));

        channel.connect();

        StringBuilder builder = new StringBuilder();
        String buf;
        while ((buf = reader.readLine()) != null) {
            builder.append(buf).append("\n");
        }
        String errbuf;
        while ((errbuf = errorReader.readLine()) != null) {
            builder.append(errbuf).append("\n");
        }

        // 必须等待执行结果全部读取完之后，才能拿到状态
        int status = channel.getExitStatus();
        System.out.println(status);

        channel.disconnect();
        session.disconnect();

        System.out.println(builder.toString());
    }
}
