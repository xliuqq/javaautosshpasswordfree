package org.xliu.cs.projects.sshpasswordfree;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xliu.cs.projects.sshpasswordfree.api.Connection;
import org.xliu.cs.projects.sshpasswordfree.api.ExecResult;
import org.xliu.cs.projects.sshpasswordfree.impl.JshConnection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);

    private static boolean generateRSAKey(String username, Set<HostPasswd> hosts) {
        return hosts.stream().allMatch(host -> generateHostRSAKey(username, host));
    }

    private static boolean generateHostRSAKey(String username, HostPasswd hostPasswd) {
        String host = hostPasswd.getHost();

        LOG.info("Generate key for host begin: {}", host);
        try (Connection connection = new JshConnection(username, hostPasswd)) {
            // 如果密钥已存在，则先删除
            ExecResult execResult = connection.runCmd("rm -rf ~/.ssh/* && ssh-keygen -t rsa -N '' -f ~/.ssh/id_rsa -q");
            boolean isSuccess = execResult.getExitCode() == 0;
            if (!isSuccess) {
                LOG.error("Generate rsa key failed, stdout and stderr are as below.");
                LOG.error("Execute stdout: {}", execResult.getOut());
                LOG.error("Execute stderr: {}", execResult.getErr());
            }
            return isSuccess;
        } catch (IOException e) {
            LOG.error("Generate key for host [{}] error: [{}]", host, e);
            return false;
        } finally {
            LOG.info("Generate key for host finished: {}", host);
        }

    }

    private static void generateKnownHosts(String username, Set<HostPasswd> hostPasswds) {
        String tmpFile = "tmp_known_host.tmp";

        HostPasswd mainHostPasswd = hostPasswds.iterator().next();
        try (Connection connection = new JshConnection(username, mainHostPasswd)) {
            for (HostPasswd hostPasswd : hostPasswds) {
                LOG.info("Test ssh from {} to  {}.", mainHostPasswd.getHost(), hostPasswd.getHost());
                ExecResult execResult = connection.runCmd("ssh -o StrictHostKeyChecking=no " + hostPasswd.getHost() + " ls");
                if (execResult.getExitCode() != 0) {
                    LOG.error("ssh from {} to  {} id_rsa.pub file error.", mainHostPasswd.getHost(), hostPasswd.getHost());
                    return;
                }
            }
            LOG.info("Download known hosts from [{}] to local file [{}].", mainHostPasswd.getHost(), tmpFile);
            ExecResult result = connection.download(tmpFile, getHome(username) + "/.ssh/known_hosts");
            if (result.getExitCode() != 0) {
                LOG.error("Download file failed, stdout and stderr are as below.");
                LOG.error("Execute stdout: {}", result.getOut());
                LOG.error("Execute stderr: {}", result.getErr());
                return;
            }
        } catch (IOException e) {
            LOG.error("Test ssh to host error:", e);
            return;
        }

        new File(tmpFile).deleteOnExit();

        for (HostPasswd hostPasswd : hostPasswds) {
            LOG.info("Send known host from local file [{}]  to [{}].", tmpFile, hostPasswd.getHost());
            try (Connection connection = new JshConnection(username, hostPasswd)) {
                ExecResult result = connection.upload(tmpFile, getHome(username) + "/.ssh/known_hosts", 384);
                if (result.getExitCode() != 0) {
                    LOG.error("Send file failed, stdout and stderr are as below.");
                    LOG.error("Execute stdout: {}", result.getOut());
                    LOG.error("Execute stderr: {}", result.getErr());
                    return;
                }
            } catch (IOException e) {
                LOG.error("Test ssh to host error:", e);
                return;
            }
        }
    }

    private static boolean copyRsaKeyToHosts(String username, Set<HostPasswd> hostPasswds) {
        // collect each host rsa.pub as a single file
        String tmpFileName = "tmp_rsa.txt";
        try (FileOutputStream fileOutputStream = new FileOutputStream(tmpFileName)) {
            for (HostPasswd hostPasswd : hostPasswds) {
                LOG.info("Collect pub key for host begin: {}", hostPasswd.getHost());

                try (Connection connection = new JshConnection(username, hostPasswd)) {
                    ExecResult execResult = connection.runCmd("cat ~/.ssh/id_rsa.pub");
                    if (execResult.getExitCode() != 0) {
                        LOG.error("Get host [{}] %s id_rsa.pub file failed.", hostPasswd.getHost());
                        LOG.error("stdout is: {}", execResult.getOut());
                        LOG.error("stderr is: {}", execResult.getErr());
                        return false;
                    }
                    fileOutputStream.write(execResult.getOut().getBytes(StandardCharsets.UTF_8));
                }

                LOG.info("Collect pub key for host end: {}", hostPasswd.getHost());
            }
        } catch (IOException e) {
            LOG.error("Get host pub rsa key error:", e);
            return false;
        }

        // scp file to all host
        for (HostPasswd hostPasswd : hostPasswds) {
            LOG.info("Scp all pub key for host begin: {}", hostPasswd.getHost());
            try (Connection connection = new JshConnection(username, hostPasswd)) {
                // 00600 = 384
                ExecResult upload = connection.upload(tmpFileName, getHome(username) + "/.ssh/authorized_keys", 384);
                if (upload.getExitCode() != 0) {
                    LOG.error("Scp pub rsa key to host [{}] failed.", hostPasswd.getHost());
                    LOG.error("stdout is: {}", upload.getOut());
                    LOG.error("stderr is: {}", upload.getErr());
                    return false;
                }
            } catch (IOException e) {
                LOG.error("Scp pub rsa key to host error:", e);
                return false;
            }
            LOG.info("Scp all pub key for host end: {}", hostPasswd.getHost());
        }

        new File(tmpFileName).deleteOnExit();

        return true;
    }

    private static String getHome(String username) {
        if ("root".equals(username)) {
            return "/root";
        }
        return "/home/" + username;
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("输入3个参数：\n" +
                    "第一个为用户名 \n" +
                    "第二个为密码（节点通用）\n" +
                    "第三个为节点列表的文件, 格式为 'host [password]', 每个节点可以不同密码（替换通用密码）\n");
        }

        String username = args[0];
        String password = args[1];
        String fileName = args[2];

        Set<HostPasswd> hostPasswds = FileUtil.readHostPasswdsFromFile(password, fileName);
        LOG.info("Read hosts are: {}", hostPasswds);

        if (hostPasswds.isEmpty()) {
            LOG.error("Host parsed failed!");
            return;
        }
        if (hostPasswds.size() == 1) {
            LOG.error("Hosts should be greater than 1.");
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

}
