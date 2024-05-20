package org.xliu.cs.projects.sshpasswordfree.api;

public class ExecResult {
    private int exitCode;
    private String out;
    private String err;

    public ExecResult(int exitCode, String out, String err) {
        this.exitCode = exitCode;
        this.out = out;
        this.err = err;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public String getOut() {
        return out;
    }

    public void setOut(String out) {
        this.out = out;
    }

    public String getErr() {
        return err;
    }

    public void setErr(String err) {
        this.err = err;
    }

}
