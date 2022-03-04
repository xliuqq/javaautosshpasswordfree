package com.hongcheng.aibigdata.tools.sshpassowordfree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PassWorkFree {
    private static final Logger LOG = LogManager.getLogger(PassWorkFree.class);

    private static final JSch J_SCH = new JSch();

    private static boolean generateRSAKey(String username, Set<HostPasswd> hosts) {
        return hosts.stream().allMatch(host -> generateHostRSAKey(username, host));
    }

    private static boolean generateHostRSAKey(String username, HostPasswd hostPasswd) {
        String host = hostPasswd.host;

        LOG.info("Generate key for host begin: {}", host);
        try {
            Session session = getSession(username, hostPasswd);

            // 如果密钥已存在，则先删除
            boolean status = SshExec.run(session, "rm -rf ~/.ssh/* && ssh-keygen -t rsa -N '' -f ~/.ssh/id_rsa -q") == 0;

            session.disconnect();
            return status;
        } catch (JSchException | IOException e) {
            LOG.error("Generate key for host [{}] error: [{}]", host, e);
            return false;
        } finally {
            LOG.info("Generate key for host finished: {}", host);
        }

    }

    private static void generateKnownHosts(String username, Set<HostPasswd> hostPasswds) {
        try {
            HostPasswd mainHostPasswd = hostPasswds.iterator().next();
            Session session = getSession(username, mainHostPasswd);

            for (HostPasswd hostPasswd : hostPasswds) {
                LOG.error("Test ssh from {} to  {}.", mainHostPasswd.host, hostPasswd.host);
                int status = SshExec.run(session, "ssh -o StrictHostKeyChecking=no " + hostPasswd.host + " ls");
                if (status != 0) {
                    LOG.error("ssh from {} to  {} id_rsa.pub file error.", mainHostPasswd.host, hostPasswd.host);
                    return;
                }
            }

            String tmpFile = "tmp_known_host.tmp";
            LOG.info("Download known hosts from [{}] to local file [{}].", mainHostPasswd.host, tmpFile);
            RemoteScpExec.download(session, tmpFile, "/home/" + username + "/.ssh/known_hosts");
            session.disconnect();

            for (HostPasswd hostPasswd : hostPasswds) {
                LOG.error("Send known host from local file [{}]  to [{}].", tmpFile, hostPasswd.host);
                Session hostSession = getSession(username, hostPasswd);
                RemoteScpExec.upload(hostSession, tmpFile, "/home/" + username + "/.ssh/known_hosts", 00644);
                hostSession.disconnect();
            }

            new File(tmpFile).deleteOnExit();
        } catch (JSchException | IOException | SftpException e) {
            LOG.error("Test ssh to host error:", e);
        }
    }

    private static Session getSession(String username, HostPasswd hostPasswd) throws JSchException {
        Session session = J_SCH.getSession(username, hostPasswd.host);
        session.setPassword(hostPasswd.password);
        // skip host-key check
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(3000);
        return session;
    }

    private static boolean copyRsaKeyToHosts(String username, Set<HostPasswd> hostPasswds) {
        // collect each host rsa.pub as a single file
        String tmpFileName = "tmp_rsa.txt";
        try (FileOutputStream fileOutputStream = new FileOutputStream(new File(tmpFileName))) {
            for (HostPasswd hostPasswd : hostPasswds) {
                LOG.info("Collect pub key for host begin: {}", hostPasswd.host);
                Session session = getSession(username, hostPasswd);

                StringBuilder builder = new StringBuilder();
                int status = SshExec.runWithOut(session, "cat ~/.ssh/id_rsa.pub", builder);
                if (status != 0) {
                    LOG.info("Get host [{}] %s id_rsa.pub file error：, {}", hostPasswd.host, builder.toString());
                    return false;
                }
                fileOutputStream.write(builder.toString().getBytes(StandardCharsets.UTF_8));
                session.disconnect();
                LOG.info("Collect pub key for host end: {}", hostPasswd.host);
            }
        } catch (IOException | JSchException e) {
            LOG.error("Get host pub rsa key error:", e);
            return false;
        }

        // scp file to all host
        try {
            for (HostPasswd hostPasswd : hostPasswds) {
                LOG.info("Scp all pub key for host begin: {}", hostPasswd.host);
                Session session = getSession(username, hostPasswd);

                RemoteScpExec.upload(session, tmpFileName, "/home/" + username + "/.ssh/authorized_keys", 00600);

                session.disconnect();
                LOG.info("Scp all pub key for host end: {}", hostPasswd.host);
            }
        } catch (JSchException | SftpException e) {
            LOG.error("Scp pub rsa key to host error:", e);
            return false;
        }

        new File(tmpFileName).deleteOnExit();

        return true;
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("输入3个参数：\n" +
                                   "第一个为用户名 \n" +
                                   "第二个为密码（节点通用）\n" +
                                   "第三个为节点列表的文件, 格式为 'host[ password]', 每个节点可以不同密码（替换通用密码）\n");
        }

        String username = args[0];
        String password = args[1];
        String fileName = args[2];

        Set<HostPasswd> hostPasswds = readHostPasswdsFromFile(password, fileName);

        LOG.info("Read hosts are: {}", hostPasswds.toString());

        if (hostPasswds.size() < 1) {
            LOG.error("Host parsed failed!");
            return;
        }

        // Step 1：针对所有节点生成 RSA 密钥
        if (!generateRSAKey(username, hostPasswds)) {
            return;
        }

        // Step 2: 进行密钥分发
        if (!copyRsaKeyToHosts(username, hostPasswds)) {
            return;
        }

        // Step 3: 测试免密登录
        generateKnownHosts(username, hostPasswds);
    }

    private static Set<HostPasswd> readHostPasswdsFromFile(String originPassword, String file) {
        Set<HostPasswd> hostPasswds = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // 忽略注释
                if (line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split(" ");
                if (parts.length > 2) {
                    LOG.error("File [{}] line [{}] format error: only support host and password with space separated", file, line);
                    return Collections.emptySet();
                }

                HostPasswd hostPasswd = new HostPasswd();
                hostPasswd.host = parts[0];
                hostPasswd.password = originPassword;

                if (parts.length == 2) {
                    hostPasswd.password = parts[1];
                }
                if (hostPasswds.contains(hostPasswd)) {
                    LOG.warn("File has multiple same name host, use the last. [{}]", hostPasswd.host);
                }
                hostPasswds.add(hostPasswd);
            }
            return hostPasswds;
        } catch (IOException ioException) {
            LOG.error("Parse file [{}] occurs exception: {}", file, ioException);
        }

        return Collections.emptySet();
    }

}
