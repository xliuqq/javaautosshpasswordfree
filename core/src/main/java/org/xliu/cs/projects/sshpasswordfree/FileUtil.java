package org.xliu.cs.projects.sshpasswordfree;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FileUtil {
    private static final Logger LOG = LogManager.getLogger(FileUtil.class);

    private FileUtil() {
    }

    /**
     * Read Host and Password from file.
     *
     * @param defaultPassword the default password.
     * @param file            each line contains a host with an optional password.
     * @return if parse failed, will return empty set.
     */
    static Set<HostPasswd> readHostPasswdsFromFile(String defaultPassword, String file) {
        Set<HostPasswd> hostPasswds = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(file))))) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // 忽略注释
                if (line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split(" ");
                if (parts.length > 2) {
                    LOG.error("File [{}] line [{}] format error: only support host and password with space separated.", file, line);
                    return Collections.emptySet();
                }

                String password = defaultPassword;
                if (parts.length == 2) {
                    password = parts[1].trim();
                }
                HostPasswd hostPasswd = new HostPasswd(parts[0].trim(), password);
                if (hostPasswds.contains(hostPasswd)) {
                    LOG.warn("File has multiple same name host, use the last. [{}]", hostPasswd.getHost());
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
