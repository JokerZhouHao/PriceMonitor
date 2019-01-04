package zhou.monitor.utility;

import android.content.res.Resources;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class IOUtility {
	public static BufferedReader getBR(InputStream ist, String charset) throws Exception{
		return new BufferedReader(new InputStreamReader(new BufferedInputStream(ist), charset));
	}
	
	public static BufferedReader getBR(InputStream ist) throws Exception{
		return new BufferedReader(new InputStreamReader(new BufferedInputStream(ist), "utf-8"));
	}

	public static BufferedReader getBR(int resId) throws Exception{
		return getBR(Resources.getSystem().openRawResource(resId));
	}

	public static BufferedWriter getBW(OutputStream ost, String charset) throws Exception{
		return new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(ost), charset));
	}
	
	public static BufferedWriter getBW(OutputStream ost) throws Exception{
		return new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(ost), "utf-8"));
	}

	public static BufferedWriter getBW(String path, boolean append) throws Exception{
		return new BufferedWriter(new FileWriter(path, append));
	}
	public static BufferedWriter getBW(String path) throws Exception{
		return getBW(path, Boolean.FALSE);
	}

	public static BufferedReader getBR(String path) throws Exception{
		return new BufferedReader(new FileReader(path));
	}

	public static String readAvailString(InputStream ist) throws Exception {
		byte[] bys = new byte[1024];
		int n = 0, len = 0;
		StringBuffer sb = new StringBuffer();
		while(true) {
			len = ist.read(bys, 0, bys.length);
			sb.append(new String(bys, 0, len, "utf-8"));
			n = ist.available();
			if(0==n)	break;
		}
		return sb.toString();
	}
	
	public static void writeString(OutputStream ost, String str) throws Exception{
		ost.write(str.getBytes("utf-8"));
		ost.flush();
	}

	public static  String getLogContent(String path){
		StringBuffer sb = new StringBuffer();
		try {
			BufferedReader br = getBR(path);
			String line = null;
			while(null != (line =br.readLine())){
				sb.append(line);
				sb.append('\n');
			}
			br.close();
		}catch (Exception e){}
		return sb.toString();
	}
}
