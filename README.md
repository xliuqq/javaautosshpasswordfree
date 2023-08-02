# javaAutoSshPasswordFree

Java 自动配置免密登录

## 使用

1. jar包获取

- 使用 assembly 打包出来的jar-with-dependencies包 

2. 执行
- 将 java-auto-ssh-password-free-assembly-1.0.0-jar-with-dependencies.jar 放到某个目录
- 执行 `java -cp . com.hongcheng.aibigdata.tools.sshpassowordfree.PassWorkFree $username $password $host_file`

说明：
- `$username` : 用户名
- `$password` ：密码，如果是通用密码，则设置，如果每个节点密码不一样，则该值随意设置（必须设置）
- `$host_file` : 主机文件，形式如下

```properties
# 节点和密码，用空格分隔
node131 password1
node131 password1
# 如果该节点是通用密码，则密码部分可以不填
node131
```
