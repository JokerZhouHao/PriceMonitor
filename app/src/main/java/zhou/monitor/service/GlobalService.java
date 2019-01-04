package zhou.monitor.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;
import java.util.HashMap;

import zhou.monitor.MainActivity;
import zhou.monitor.R;
import zhou.monitor.application.MyApplication;
import zhou.monitor.entity.PriceRecoder;
import zhou.monitor.entity.SettingInfo;
import zhou.monitor.thread.PriceMonitor;
import zhou.monitor.thread.PriceProtector;
import zhou.monitor.thread.WakeUpThread;
import zhou.monitor.utility.AlarmUtility;
import zhou.monitor.utility.GlobalServiceAlarm;
import zhou.monitor.view.MainView;

/**
 * 由于android会以优先级随时清除activity和线程，而前台服务（创造了通知的服务）优先级最高，
 * 故自定义了个前台服务，存放所有全局变量，同时，该服务也是查询价格线程与activity交互，其中
 * 价格线程负责查询价格，判断是否有满足条件的item，而activity只负责显示内容。
 */
public class GlobalService extends Service {
    // 服务名
    public static final String ACTION = "zhou.monitor.service.GlobalService";

    // 供外部使用的服务
    public static GlobalService globalSer = null;

    private static boolean noInit = true;

    // 基本目录
    public static  String pathBase = null;

    // 日志文件路径
    public static String pathLog = "log.txt";
    public static long logMaxLength = 1024 * 100; // 日志文件的最大大小

    // 配置文件路径
    public static String pathConfig = "config.txt";
    // 记录添加的条目信息文件路径
    public static String pathItems = "items.txt";

    // Application, MainActivity, MainView
    private static MyApplication myApp = null;
    public static MainActivity mainAct = null;
    public static MainView mainView = null;

    // 价格监视器
    private PriceMonitor priceMonitor = null;
    private PriceProtector priceProtector = null;
    private WakeUpThread wakeUpThread = null;

    // 铃声、振动
    private MediaPlayer mediaPlayer = null;
    private Vibrator vbt = null;

    // 电源锁
    private PowerManager.WakeLock wakeLock = null;

    // 监控器参数
    public static final int TICK = 60000;   // 查询间隔时间
    public static final int NUMPRICE = 200; // PriceRecoder能记录的price数
    public static final int TEMPTICK = 2000; // 为避免被封ip故每次查询的每个url，隔2秒钟
    public static final int RETRYTICK = 60000; // 创建client失败，过多久后尝试
    public static final int LONGESTFEEDBACKTIME = 600000; // 反馈的最长时间

    /**
     * 初始化各变量
     * @param mainAct
     * @throws Exception
     */
    public static void init(MainActivity mainAct) throws Exception{
        GlobalService.mainAct = mainAct;
        if(noInit){
            noInit = false;
            // 用mainAct初始化路径
            pathBase = mainAct.getFilesDir().getAbsolutePath() + File.separator;

            File file = null;
            // 处理日志文件
            pathLog = pathBase + pathLog;
            file = new File(pathLog);
            if(!file.exists())  file.createNewFile();
            else if(file.length() >= logMaxLength){ // 已超过限制大小
                file.delete();
                file.createNewFile();
            }

            // 处理配置文件
            pathConfig = pathBase + pathConfig;
            file = new File(pathConfig);
            if(file.exists())   SettingInfo.loadFromFile(pathConfig);
            else{
                SettingInfo.writeToFile(pathConfig);
            }

            // 处理条目信息文件
            pathItems = pathBase + pathItems;
            file = new File(pathItems);
            if(!file.exists())  file.createNewFile();
        }
    }

    /**
     * 创建PriceMonitor
     */
    private PriceMonitor innerCreatePriceMonitor(){
        if(null != mainView){
            priceMonitor = new PriceMonitor(SettingInfo.serveAddr, mainView);
        } else priceMonitor = new PriceMonitor(SettingInfo.serveAddr, pathItems);
        return priceMonitor;
    }

    public static Boolean isRunMonitor(){
        if(null == globalSer)   return Boolean.TRUE;
        else if(null == globalSer.priceMonitor) return Boolean.FALSE;
        else return Boolean.TRUE;
    }

    public static void createPriceMonitor(){
        if(null != globalSer && null == globalSer.priceMonitor){
            globalSer.innerCreatePriceMonitor();
            new Thread(globalSer.priceMonitor).start();
        }
    }

    /**
     * 创建价格线程保护者
     * @return
     */
    private PriceProtector innerCreateProtector(){
        priceProtector = new PriceProtector();
        return  priceProtector;
    }

    public static void createProtector(){
        if(null != globalSer && null == globalSer.priceProtector){
            globalSer.innerCreateProtector();
            new Thread(globalSer.priceMonitor).start();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 添加进行中的通知，以将该服务变成前台服务，提高优先级
        NotificationManager nManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.ic_grade_black);
        mBuilder.setContentTitle("Don't look ME");
        mBuilder.setContentText("Do you work !");
//        RemoteViews rView = new RemoteViews(getPackageName(), R.layout.view_limit_price);
//        mBuilder.setContent(rView);
        Intent intent0 = new Intent(this, MainActivity.class);
        intent0.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);
        Notification notification = mBuilder.build();
        notification.flags = Notification.FLAG_NO_CLEAR|Notification.FLAG_ONGOING_EVENT;
        startForeground(1, notification);
//        startForeground(1, mBuilder.build());

        // 将创建的Service赋值静态变量，便于使用
        globalSer = this;

//        // 获取电源锁
//        if(null == wakeLock){
//            PowerManager powerM = (PowerManager)getSystemService(POWER_SERVICE);
//            wakeLock = powerM.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, getClass().getCanonicalName());
//            if(null != wakeLock) {
//                wakeLock.acquire();
//            }
//        }

        // 打开启动服务的时钟
//        AlarmUtility.startRepeatServiceAlarm(this, 40, GlobalService.class, GlobalService.ACTION);

        // 创建价格监视器
        innerCreatePriceMonitor();
        new Thread(priceMonitor).start();

        // 创建价格监视器保护线程
        innerCreateProtector();
        new Thread(priceProtector).start();

        // 创建唤醒线程
        wakeUpThread = new WakeUpThread(this);
        wakeUpThread.start();

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                for(int i=0; i<3; i++){
//                    GlobalServiceAlarm.startOneTimeServiceAlarm(GlobalService.this, 10000, GlobalService.class, GlobalService.ACTION);
//                    try {
//                        Thread.sleep(10000);
//                    } catch (Exception e)   {}
//                }
//            }
//        }).start();


    }

    public PowerManager.WakeLock holdWakeLock(){
        if(null == wakeLock){
            PowerManager powerM = (PowerManager)getSystemService(POWER_SERVICE);
            wakeLock = powerM.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, getClass().getCanonicalName());
            if(null != wakeLock) {
                wakeLock.acquire();
            }
        }
        return wakeLock;
    }

    public void releaseWakeLock(){
        if(null != wakeLock && wakeLock.isHeld()){
            wakeLock.release();
            wakeLock = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 释放锁
        if(null != wakeLock && wakeLock.isHeld()){
            wakeLock.release();
            wakeLock = null;
        }
        // 取消时钟
        AlarmUtility.stopService(this, GlobalService.class, GlobalService.ACTION);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 每出现一次"启动成功"，就创建了一个MainActivity
//        innerfeedback();
//        Toast.makeText(this, "启动成功", Toast.LENGTH_SHORT).show();
        if(wakeUpThread.isSleep())  wakeUpThread.interrupt();

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 获取的是铃声的Uri
     * @param ctx
     * @param type
     * @return
     */
    private Uri getDefaultRingtoneUri(Context ctx, int type) {
        return RingtoneManager.getActualDefaultRingtoneUri(ctx, type);
    }

    // 创建媒体播放器
    private void createMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this, getDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE));
        mediaPlayer.setLooping(true);
    }

    // 创建震动器
    private void createVibrate(){
        vbt = (Vibrator)getApplicationContext().getSystemService(Service.VIBRATOR_SERVICE);
    }

    // 反馈
    public void innerfeedback(){
        if(SettingInfo.onSound && null==mediaPlayer){
            createMediaPlayer();
//            try {
//                mediaPlayer.prepare();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
            mediaPlayer.start();
        }
        if(SettingInfo.onVibrate && null == vbt){
            createVibrate();
            if(vbt.hasVibrator()){
                long[] freq = {100, 200, 250, 200, 300, 400};
                vbt.vibrate(freq, 0);
            }
        }
        showMainActivity();
    }

    // 关闭反馈
    public void innerCloseFeedback(){
        if(mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if(vbt != null){
            vbt.cancel();
            vbt = null;
        }
    }

    /**
     * 打开MainActivity
     */
    public void showMainActivity(){
        if(null == mainAct || !mainAct.isFront()){
            Intent intent = new Intent(this, MainActivity.class);
//            startActivity(intent);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setAction(Intent.ACTION_MAIN);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
//            Intent intent = new Intent("zhou.monitor.MainActivity");
//            PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//            try {
//                pi.send(this, 0, intent);
//            }catch (Exception e){}

//            intent.setFlags(Intent.FLAG_FROM_BACKGROUND);
//            intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
//            startActivity(intent);
        }

    }

    /**
     * 设置MainActivity的标题
     * @param title
     */
    public static void setMainActTitle(String title){
        if(null != globalSer && null != mainAct)    mainAct.setTitle(title);
    }

    public static void feedback(){
        if(null != globalSer)   globalSer.innerfeedback();
    }

    public static void closeFeedback(){
        if(null != globalSer)   globalSer.innerCloseFeedback();
    }


    public static void updateMonitorAllTypes(MainView mainView){
        if(null != globalSer.priceMonitor){
            globalSer.priceMonitor.setAllTypes(mainView);
        }
    }

    public static void refreshMainViewUI(HashMap<String, PriceRecoder> type2Recoder){
        if(null != mainView){
            mainView.refreshAllItemsUI(type2Recoder);
        }
    }

    /**
     * 检查是否已联网
     * @return
     */
    public static boolean isNetworkConnected() {
        if(null != globalSer){
            ConnectivityManager mConnectivityManager = (ConnectivityManager) globalSer
                    .getSystemService(globalSer.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    /**
     * add WaveItem
     * @param transType
     * @param minuteSpan
     * @param wave
     * @return
     */
    public static  String addWaveItem(String transType, int minuteSpan, double wave){
        if(null != GlobalService.mainView){
            GlobalService.mainView.addWaveItem(transType, minuteSpan, wave);
            return "ok";
        } else return "MainView is null";
    }

    /**
     * add PriceItem
     * @param transType
     * @param isBigger
     * @param outPrice
     * @return
     */
    public static String addPriceItem(String transType, boolean isBigger, double outPrice){
        if(null != GlobalService.mainView){
            GlobalService.mainView.addPriceItem(transType, isBigger, outPrice);
            return "ok";
        } else return "MainView is null";
    }

    public static void startOneTimeServiceAlarm(long offetMill) {
        if(globalSer != null){
            GlobalServiceAlarm.startOneTimeServiceAlarm(globalSer, offetMill, GlobalService.class, GlobalService.ACTION);
        }
    }

    public static void showToast(String str){
        if(globalSer != null){
            Toast.makeText(globalSer, str, Toast.LENGTH_SHORT).show();
        }
    }

}
