package thereia.java.chess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

@SpringBootApplication
public class ChessApplication {

    public static void main(String[] args) {
        printServerInfo();
        SpringApplication.run(ChessApplication.class, args);
    }

    private static void printServerInfo() {
        System.out.println("========================================");
        System.out.println("          揭棋对弈服务器启动");
        System.out.println("========================================");
        System.out.println("本机可用IP地址列表：");
        System.out.println("----------------------------------------");
        
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            boolean foundIP = false;
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    String ip = addr.getHostAddress();
                    if (ip.contains(":") || ip.equals("127.0.0.1")) {
                        continue;
                    }
                    System.out.println("  " + ip + "  (" + iface.getDisplayName() + ")");
                    foundIP = true;
                }
            }
            
            if (!foundIP) {
                System.out.println("  未找到可用IP地址");
            }
        } catch (Exception e) {
            System.out.println("  获取IP地址失败: " + e.getMessage());
        }
        
        System.out.println("----------------------------------------");
        System.out.println("WebSocket服务端口: 8887");
        System.out.println("客户端连接地址示例: ws://[IP地址]:8887/ws");
        System.out.println("========================================");
        System.out.println();
    }
}