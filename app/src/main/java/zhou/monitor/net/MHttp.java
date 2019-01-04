package zhou.monitor.net;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class MHttp {
	private static Map<String, String> HEADER = new HashMap<>();
	
	static {
		HEADER.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
	}
	
	private CloseableHttpClient client = HttpClients.createDefault();
	private HttpGet httpGet = new HttpGet("");
	private HttpPost httpPost = new HttpPost("");
	
	public MHttp() {
		for(Entry<String, String> en : HEADER.entrySet()) {
			httpGet.addHeader(en.getKey(), en.getValue());
			httpPost.addHeader(en.getKey(), en.getValue());
		}
	}
			
	public String Get(String uri) throws Exception{
		if(!httpGet.getURI().toString().equals(uri)) {
			httpGet.setURI(new URI(uri));
		}
//		System.out.println(httpGet.getURI().toString());
		
		CloseableHttpResponse response = client.execute(httpGet);
		String str = EntityUtils.toString(response.getEntity());
		response.close();
		return str;
	}
	
	public String Post(String uri, Map<String, String> params) throws Exception{
		if(!httpPost.getURI().toString().equals(uri)) {
			httpPost.setURI(new URI(uri));
		}
		
		List<NameValuePair> nvs = null;
		if(null != params) {
			nvs = new ArrayList<>();
			for(Entry<String, String> en : params.entrySet()) {
				nvs.add(new BasicNameValuePair(en.getKey(), en.getValue()));
			}
		}
		if(null != nvs) httpPost.setEntity(new UrlEncodedFormEntity(nvs));
		
		CloseableHttpResponse response = client.execute(httpPost);
		String str = EntityUtils.toString(response.getEntity());
		response.close();
		return str;
	}
	
	public void close() throws Exception{
		this.client.close();
	}
	
	public static void main(String[] args) throws Exception{
		MHttp hp = new MHttp();
		String testUrl = "https://api.binance.com/api/v3/ticker/price?symbol=ETHUSDT";
		System.out.println(" ------------------------------ " + testUrl + " -------------------------");
		System.out.println(hp.Get(testUrl) + "\n\n");
		if(args.length != 0) {
			for(String st : args) {
				System.out.println(" ------------------------------ " + st + " -------------------------");
				System.out.println(hp.Get(st) + "\n\n");
			}
		}
		
	}
}




















