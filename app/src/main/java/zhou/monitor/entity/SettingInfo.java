package zhou.monitor.entity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;

import zhou.monitor.utility.IOUtility;

public class SettingInfo {
    // 响铃
    public static boolean onSound = true;
    // 振动
    public static boolean onVibrate = false;
    // 服务器(地址:端口)
    public static String serveAddr = "172.96.252.209:38201";
    // 价格网址
    public static String priceAddr = "https://api.binance.com/api/v3/ticker/price?symbol=";

    public static void writeToFile(String path){
        try {
            BufferedWriter bw = IOUtility.getBW(path);
            bw.write("onSound " + String.valueOf(onSound) + "\n");
            bw.write("onVibrate " + String.valueOf(onVibrate) + "\n");
            bw.write("serveAddr " + serveAddr + "\n");
            bw.write("priceAddr " + priceAddr);
            bw.close();
        } catch (Exception e){ }

    }

    public static void loadFromFile(String path) throws Exception{
        BufferedReader br = IOUtility.getBR(path);
        String line = null;
        String[] arr = null;
        while((line=br.readLine()) != null){
            arr = line.split(" ");
            switch (arr[0].trim()){
                case "onSound":
                    onSound = Boolean.parseBoolean(arr[1].trim());
                    break;
                case "onVibrate":
                    onVibrate = Boolean.parseBoolean(arr[1].trim());
                    break;
                case "serveAddr":
                    serveAddr = arr[1].trim();
                    break;
                case "priceAddr":
                    priceAddr = arr[1].trim();
                    break;
            }
        }
        br.close();
    }
}
