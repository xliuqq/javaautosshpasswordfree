# JavaAutoSshPasswordFree

Java 自动配置 Linux 节点间的 SSH 免密登录
- 支持幂等执行（会先删除之前的`~/.ssh`）


## 使用
> 要求所有节点都配置了 Hosts，可以互相访问。

1. jar包获取

- 使用 assembly 打包出来的jar包 
- 或从 Release 页面下载

2. 执行
- 将 java-auto-ssh-password-free-core-1.0.0.jar 放到某个目录
- 执行 `java -cp . org.xliu.cs.projects.sshpasswordfree.Main $username $password $host_file`

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

## 原理

Step 1：针对所有节点生成 RSA 密钥(会先删除存在的`~/.ssh`目录)， 生成id_rsa, id_rsa.pub

Step 2: 进行密钥分发，生成 authorized_keys

Step 3: 访问节点，生成 known_hosts （避免 StrictHostKeyChecking）