package zhou.monitor.thread;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL;

import zhou.monitor.entity.PriceItem;
import zhou.monitor.entity.PriceRecoder;
import zhou.monitor.entity.SettingInfo;
import zhou.monitor.net.Client;
import zhou.monitor.service.GlobalService;
import zhou.monitor.utility.IOUtility;
import zhou.monitor.utility.MLog;
import zhou.monitor.utility.NetDetector;
import zhou.monitor.utility.StringFormator;
import zhou.monitor.view.MainView;
import zhou.monitor.view.PriceView;
import zhou.monitor.view.WaveView;

public class PriceMonitor extends Thread {

    // 表示当前线程的状态，若不小于0，则该次请求已完成，否则当前请求为完成，需要持有WakeLock
    // 1：表示本次查询成功执行     2：表示当前无网络      3：表示当前无条目
    // Integer.max：表示线程已结束
    // -1：为供轮询线程状态的对象，能够对该线程设置的状态码
    private int statusCode = 1;
    public static long numRequest = 1;  // 注意为Long.max线程因无法恢复，而终止
    public static long sleepTime = 0; // 用于告知外界该线程会休眠多久

    private static final List<PriceItem> allItems = new ArrayList<>();
    private static final Map<String, String> type2Type = new HashMap<>();    // 记录ETH/USDT --> ETHUSDT映射
    private static final HashMap<String, PriceRecoder> type2Prices = new HashMap<>();
    private static String serverAddrStr = null;
    private static String serverIP = null;
    private static int port = 0;
    private static Client client = null;
    private static final Map<String, Double> pricePeek = new HashMap<>();    // 价格板
    public static boolean signTryConnect =false;

    private PriceMonitor(String serverAddrStr){
        this.serverAddrStr = serverAddrStr;
        fomatAddr(serverAddrStr);
    }

    public PriceMonitor(String serverAddrStr, MainView mainView){
        this(serverAddrStr);
        setAllTypes(mainView);
    }

    public PriceMonitor(String serverAddrStr, String itemPath){
        this(serverAddrStr);
        loadAllTypeFromFile(itemPath);
    }

    /**
     * 从文件初始化所有与type相关的数据
     * @param itemPath
     */
    public void loadAllTypeFromFile(String itemPath){
        BufferedReader br = null;
        try {
            allItems.clear();
            br = IOUtility.getBR(itemPath);
            String line = null;
            PriceItem item = null;
            while(null != (line = br.readLine())){
                item = PriceItem.decode(-1, line);  // PriceMonitor初始化的Item的id都为-1
                allItems.add(item);
            }
            br.close();
        } catch (Exception e){
        } finally {
            try{
                br.close();
            } catch (Exception e){}
        }
        // 更新type2Type
        type2Type.clear();
        for(PriceItem item : allItems){
            type2Type.put(item.getTransType(), item.getTransType().replace("/", ""));
        }
        // 移除不存在的类型
        List<String> li = new ArrayList<>();
        for(Map.Entry<String, PriceRecoder> en : type2Prices.entrySet()){
            if(!type2Type.containsKey(en.getKey())) li.add(en.getKey());
        }
        for(String st : li)     type2Prices.remove(st);
    }

    /**
     * 设置所有与交易类型相关的数据
     * @param mainView
     */
    public void setAllTypes(MainView mainView){
        synchronized (this){
            // 更新allItems
            allItems.clear();
            Map<Integer, View> id2Items = mainView.getAllViews();
            for(Map.Entry<Integer, View> en : id2Items.entrySet()){
                if(en.getValue() instanceof PriceView)  allItems.add(((PriceView)en.getValue()).getItem());
                else allItems.add(((WaveView)en.getValue()).getItem());
            }
            // 更新type2Type
            type2Type.clear();
            for(PriceItem item : allItems){
                type2Type.put(item.getTransType(), item.getTransType().replace("/", ""));
            }
            // 移除不存在的类型
            List<String> li = new ArrayList<>();
            for(Map.Entry<String, PriceRecoder> en : type2Prices.entrySet()){
                if(!type2Type.containsKey(en.getKey())) li.add(en.getKey());
            }
            for(String st : li)     type2Prices.remove(st);
        }
    }

    @Override
    public void run(){
        int tick = GlobalService.TICK;
        int rollSpeed = 0;
        int rollTime = 0;
        PriceRecoder recoder = null;
        if(!createClient(1)){
            numRequest = Long.MAX_VALUE;
            this.releaseWakeLock(-1);
            return; // 创建client失败了
        }
        while (true){
            MLog.writeLine("PriceMonitor.numRequest = " + String.valueOf(numRequest));
            try {
                if(numRequest % 25 == 0){    // 每隔25次就重新创建一次socket
                    if(!createClient(1)){
                        numRequest = Long.MAX_VALUE;
                        this.releaseWakeLock(-1);
                        return; // 创建client失败了
                    }
                }
                // 检查保护线程
//                GlobalService.createProtector();
                // 检查网络是否已连接
                if(!NetDetector.isNetworkConnected()){
                    GlobalService.setMainActTitle("无网络");
                    if(null != client && !client.isClose()){
                        try {
                            client.close();
                        } catch (Exception e) {}
                    }
                    sleepTime = GlobalService.TICK;
                    numRequest++;
                    this.releaseWakeLock(sleepTime);
                    try {
                        Thread.sleep(sleepTime);
                    } catch (Exception e){}
                    continue;
                }
                // 检查是否有交易类型需查找
                if(type2Type.isEmpty()){
                    GlobalService.setMainActTitle("当前无条目");
                    if(null != client && !client.isClose()){
                        try {
                            client.close();
                        } catch (Exception e){}
                    }
                    sleepTime = GlobalService.TICK;
                    numRequest++;
                    this.releaseWakeLock(sleepTime);
                    try {
                        Thread.sleep(GlobalService.TICK);
                    } catch (Exception e){}
                    continue;
                }
                // 必要时恢复连接
                if((null==client || client.isClose()) && !createClient(1)){
                    numRequest = Long.MAX_VALUE;
                    this.releaseWakeLock(-1);
                    return;
                }
                // 检查服务器地址是否已变
                if(!serverAddrStr.equals(SettingInfo.serveAddr())){
                    GlobalService.setMainActTitle("服务器地址已变");
                    // 关闭连接
                    try {
                        client.close();
                    } catch (Exception e) {}
                    // 创建连接
                    if(!createClient(1)){
                        numRequest = Long.MAX_VALUE;
                        this.releaseWakeLock(-1);
                        return; //  创建失败
                    }
                }
                synchronized (this){
                    tick = GlobalService.TICK;
                    // 开始查询
                    JSONObject json = null;
                    double price = 0.0;
                    for(Map.Entry<String, String> en : type2Type.entrySet()){
                        String response = client.reqGet(SettingInfo.priceAddr() + en.getValue());
                        try {
                            json = new JSONObject(response);
                        } catch (Exception e){
                            GlobalService.feedback();
                            MLog.writeLine("json解析response异常：" + response);
                            GlobalService.setMainActTitle("json解析response异常：" + response);
                            numRequest = Long.MAX_VALUE;
                            this.releaseWakeLock(-1);
                            return; //  失败
                        }
                        // 参数错误，停止查询，强制要求用户检查
                        if(!json.has("price")){
                            GlobalService.feedback();
                            MLog.writeLine("请求地址错误：" + response);
                            GlobalService.setMainActTitle("请求地址错误：" + response);
                            numRequest = Long.MAX_VALUE;
                            this.releaseWakeLock(-1);
                            return;
                        }
                        // 处理请求结果
                        try {
                            price = Double.parseDouble(json.getString("price"));
                        } catch (Exception e){
                            GlobalService.feedback();
                            MLog.writeLine("json解析price异常：" + response);
                            GlobalService.setMainActTitle("json解析price异常：" + response);
                            numRequest = Long.MAX_VALUE;
                            this.releaseWakeLock(-1);
                            return; //  创建失败
                        }
                        // 添加价格
                        if(null == (recoder = type2Prices.get(en.getKey()))){
                            recoder = new PriceRecoder(GlobalService.NUMPRICE);
                            type2Prices.put(en.getKey(), recoder);
                        }
                        recoder.add(price);

                        // 避免请求太快，被封ip
                        try {
                            Thread.sleep(GlobalService.TEMPTICK);
                        } catch (Exception e){}
                        tick -= GlobalService.TEMPTICK;
                    }
                    // 检查是否有满足条件的item
                    if(check()){
                        GlobalService.feedback();
                        GlobalService.hasNotification = true;
                        GlobalService.showMainActivity();
                    }
                    // 刷新价格板变量
                    refreshPricePeek();
                }
                // 刷新MainView
                GlobalService.refreshMainViewUI(type2Prices);
                if(!pricePeek.isEmpty()){
                    for(Map.Entry<String, Double> en : pricePeek.entrySet()){
                        GlobalService.setMainActTitle(en.getKey() + ":" + StringFormator.formatPrice(en.getValue()));
                        break;
                    }
                }
                sleepTime = tick;
                numRequest++;
                this.releaseWakeLock(sleepTime);
                try {
                    Thread.sleep(tick);
                } catch (Exception e){}
            } catch (Exception e){
                GlobalService.setMainActTitle("PriceMonitor : " + e.getMessage());
                MLog.writeLine("PriceMonitor.run.catch: " + e.getMessage());
                if(!createClient(1)) {
                    numRequest = Long.MAX_VALUE;
                    this.releaseWakeLock(-1);
                    return; //  创建失败
                }
            }
        }
    }

    /**
     * 设置价格板，之所单独创建个价格板，是为了防止在遍历type2Prices，刚好MainView试图更新它
     */
    private void refreshPricePeek(){
        pricePeek.clear();
        for(Map.Entry<String, PriceRecoder> en : type2Prices.entrySet()){
            pricePeek.put(en.getKey(), en.getValue().now());
        }
    }

    /**
     * 格式化地址串
     */
    private void fomatAddr(String addrPort){
        String arr[] = null;
        arr = serverAddrStr.split(":");
        serverIP = arr[0].trim();
        port = Integer.parseInt(arr[1].trim());
    }

    /**
     * android的网络请求不能在主线程中，必须在新创建的线程中创建client
     */
    private boolean createClient(int times){
        if(times == 11){
            GlobalService.setMainActTitle("创建client " + String.valueOf(times-1) + "次失败 !");
//            GlobalService.feedback();
            GlobalService.releaseScreemLock();
            return false;
        }
        // 第3次尝试会点亮屏幕
        if(times==3){
            GlobalService.holdScreemLock();
            GlobalService.showMainActivity();
            signTryConnect = true;
        }
        // 若socket存在，先需关闭socket
        if(null != client){
            try{
                client.close();
            } catch (Exception e){
                MLog.writeLine("createClient第1次关闭client异常: " + e.getMessage());
            }
        }
        boolean hasException = false;
        try {
            fomatAddr(SettingInfo.serveAddr());
            client = new Client(serverIP, port);
        } catch (Exception e){
            MLog.writeLine("createClient new client异常" + String.valueOf(times) + "次: " + e.getMessage());
            hasException = true;
        }
        if(hasException){
            GlobalService.setMainActTitle("创建client" + String.valueOf(times) + "次失败，trying " + String.valueOf(times + 1));
            try{
                Thread.sleep(GlobalService.RETRYTICK);
            }catch (Exception e){
            }finally {
                return createClient(times + 1);
            }
        } else{
            if(signTryConnect){
                GlobalService.moveMainActToBack();
                signTryConnect = false;
            }
            GlobalService.releaseScreemLock();  // 释放屏幕锁
            return true;
        }
    }

    /**
     * 检查
     * @return
     */
    public Boolean check(){
        PriceRecoder recoder = null;
        double precent = 0.0;
        for(PriceItem item : allItems){
            recoder = type2Prices.get(item.getTransType());
            if(item.isPrice()){
                if(Boolean.TRUE == item.inOutPrice(recoder.now())) return Boolean.TRUE;
            } else {
                precent = ((recoder.now() - recoder.get(item.getMinuteSpan() + 1))/recoder.get(item.getMinuteSpan() + 1)) * 100;
                if(Boolean.TRUE == item.inWave(precent))
                    return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    // 释放锁并添加时钟
    private void releaseWakeLock(long sleepTime){
        if(sleepTime > 0){
            // 因为MainAct在前台，时钟才能在后台执行
//            if(!GlobalService.hasNotification && !GlobalService.hasScreenOn)   GlobalService.showMainActivity();
//            GlobalService.startOneTimeServiceAlarm(sleepTime/5*4); // 添加服务时钟
//            if(!GlobalService.hasNotification && !GlobalService.hasScreenOn){
//                try{
//                    Thread.sleep(1000);
//                } catch (Exception e){}
//                GlobalService.moveMainActToBack();
//            }
        }
        GlobalService.globalSer.releaseWakeLock(); // 释放锁
    }
}
