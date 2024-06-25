package apijson.boot;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import java.io.IOException;
import java.io.InputStream;

public class SSHJExample {

    private static final String HOST = "192.168.21.146";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";
    private static final String COMMAND = "ls -l";

    public static void main(String[] args) {
        try (SSHClient ssh = new SSHClient()) {
            // 使用 PromiscuousVerifier 来跳过主机密钥验证
            // 在生产环境中，应该适当验证主机密钥
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.connect(HOST);
            ssh.authPassword(USERNAME, PASSWORD);
            executeCommand(ssh, COMMAND);
        } catch (IOException e) {
            System.err.println("SSH 连接或认证失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void executeCommand(SSHClient ssh, String command) {
        try (Session session = ssh.startSession()) {
            // 在远程主机上执行命令
            Session.Command cmd = session.exec(command);
            printStream(cmd.getInputStream(), System.out);
            printStream(cmd.getErrorStream(), System.err);
            cmd.join(5, java.util.concurrent.TimeUnit.SECONDS); // 等待命令执行完成
            System.out.println("\n** 退出状态: " + cmd.getExitStatus());
        } catch (IOException e) {
            System.err.println("执行命令失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printStream(InputStream stream, java.io.PrintStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = stream.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }
}
