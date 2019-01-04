package zhou.monitor.thread;

import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import zhou.monitor.activity.AddItemActivity;
import zhou.monitor.entity.SettingInfo;
import zhou.monitor.net.Client;

// 连通性测试线程
public class ConnectTest implements Runnable {
    private String result = null;
    private String reqestAddr = null;

    private ConnectTest(String reqestAddr){
        this.reqestAddr = reqestAddr;
    }

    @Override
    public void run() {
        try{
            Client client = new Client(SettingInfo.serveAddr);
            result = client.reqGet(reqestAddr);
            client.close();
        }catch (Exception e){
            result = e.getMessage();
        }
    }

    public static String test(String addr){
        ConnectTest ct = new ConnectTest(addr);
        new Thread(ct).start();
        while (true){
            if(ct.result != null){
                return ct.result;
            }
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
        }
    }
}
