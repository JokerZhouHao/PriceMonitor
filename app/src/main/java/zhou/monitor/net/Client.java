package zhou.monitor.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.NameValuePair;

import zhou.monitor.utility.IOUtility;

import static zhou.monitor.utility.IOUtility.writeString;

public class Client {
	
	private String host = null;
	private int port = 0;
	private Socket socket = null;
	private InputStream ist = null;
	private OutputStream ost = null;

	public Client(String hostAndPort) throws Exception{
		String[] arr = hostAndPort.split(":");
		this.host = arr[0].trim();
		this.port = Integer.parseInt(arr[1]);
		this.connect();
	}

	public Client(String host, int port) throws Exception{
		this.host = host;
		this.port = port;
		this.connect();
	}
	
	private void connect() throws Exception{
		socket = new Socket(host, port);
		ist = socket.getInputStream();
		ost = socket.getOutputStream();
		System.out.println("> 连接服务器<" + host + ", " + port + ">成功");
	}
	
	public String reqGet(String url) throws Exception{
		IOUtility.writeString(ost, "8()get()" + url.trim());
		return IOUtility.readAvailString(ist);
	}
	
	public String reqPost(String url, Map<String, String> nvs) throws Exception{
		String req = "8()post()" + url ;
		if(nvs !=null && !nvs.isEmpty()) {
			for(Entry<String, String> en : nvs.entrySet()) {
				req +=  "()" + en.getKey() + "((" + en.getValue();
			}
		}
		IOUtility.writeString(ost, req);
		return IOUtility.readAvailString(ist);
	}

	public Boolean isClose(){
		return socket.isClosed();
	}

	public void close() throws Exception{
		ist.close();
		ost.close();
		socket.close();
//		System.out.println("> 关闭客户端成功");
	}
	
	public static void main(String[] args) throws Exception {
		Client client = new Client("172.96.252.209", 38201);
//		Client client = new Client("172.96.252.209:38201");
//		Scanner sca = new Scanner(System.in);
//		String line = null;
//		System.out.print("> 请输入: ");
//		while(null != (line = sca.nextLine())) {
//			if(line.equals("0"))	break;
//			System.out.println("> 收到服务器: " + client.reqGet(line));
//			System.out.print("> 请输入: ");
//		}
//		client.close();
    }
}
