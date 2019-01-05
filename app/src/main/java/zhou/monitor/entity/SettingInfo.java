package zhou.monitor.entity;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;

import zhou.monitor.service.GlobalService;
import zhou.monitor.utility.IOUtility;

public class SettingInfo {
    // 响铃
    private static Boolean onSound = null;
    // 振动
    private static Boolean onVibrate = null;
    // 服务器(地址:端口)
    private static String serveAddr = null;
    // 价格网址
    private static String priceAddr = null;

    // 第一次安装时才会执行的代码
    public static void installInit(String pathConfig){
        onSound = Boolean.TRUE;
        onVibrate = Boolean.TRUE;
        serveAddr = "172.96.252.209:38201";
        priceAddr = "https://api.binance.com/api/v3/ticker/price?symbol=";
        writeToFile(pathConfig);
    }

    public static Boolean onSound(){
        if(onSound == null) GlobalService.checkStatus();
        if(onSound == null) return Boolean.TRUE;
        return onSound;
    }

    public static Boolean onVibrate(){
        if(onVibrate == null)   GlobalService.checkStatus();
        if(onVibrate == null)   return Boolean.TRUE;
        return onVibrate;
    }

    public static String serveAddr(){
        if(serveAddr==null) GlobalService.checkStatus();
        if(serveAddr==null) return "172.96.252.209:38201";
        return serveAddr;
    }

    public static String priceAddr(){
        if(priceAddr == null)   GlobalService.checkStatus();
        if(priceAddr == null)   return "https://api.binance.com/api/v3/ticker/price?symbol=";
        return priceAddr;
    }

    public static void updateOnSound(boolean sound){
        if(onSound == null || onVibrate == null || priceAddr==null || serveAddr==null)  GlobalService.checkStatus();
        if(onSound == null || onVibrate == null || priceAddr==null || serveAddr==null) return;
        onSound = sound;
        writeToFile(GlobalService.pathConfig());
    }

    public static void updateOnVibrate(boolean vibrate){
        if(onSound == null || onVibrate == null || priceAddr==null || serveAddr==null)  GlobalService.checkStatus();
        if(onSound == null || onVibrate == null || priceAddr==null || serveAddr==null) return;
        onVibrate = vibrate;
        writeToFile(GlobalService.pathConfig());
    }

    public static void updateServeAddr(String addr){
        if(onSound == null || onVibrate == null || priceAddr==null || serveAddr==null)  GlobalService.checkStatus();
        if(onSound == null || onVibrate == null || priceAddr==null || serveAddr==null) return;
        serveAddr = addr;
        writeToFile(GlobalService.pathConfig());
    }

    public static void updatePriceAddr(String addr){
        if(onSound == null || onVibrate == null || priceAddr==null || serveAddr==null)  GlobalService.checkStatus();
        if(onSound == null || onVibrate == null || priceAddr==null || serveAddr==null) return;
        priceAddr = addr;
        writeToFile(GlobalService.pathConfig());
    }

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

    public static void loadFromFile(String path){
        try{
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
        } catch (Exception e){}
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        GlobalService.checkStatus();
    }
}
