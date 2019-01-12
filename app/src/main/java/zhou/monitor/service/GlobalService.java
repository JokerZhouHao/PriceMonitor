package zhou.monitor.service;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.GridLayout;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

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
import zhou.monitor.utility.MLog;
import zhou.monitor.view.MainView;

/**
 * 由于android会以优先级随时清除activity和线程，而前台服务（创造了通知的服务）优先级最高，
 * 故自定义了个前台服务，存放所有全局变量，同时，该服务也是查询价格线程与activity交互，其中
 * 价格线程负责查询价格，判断是否有满足条件的item，而activity只负责显示内容。
 */
public class GlobalService extends Service {
    // 服务名
    public static final String ACTION = "zhou.monitor.service.GlobalService";

    // IntentFilter
    private static final IntentFilter screenFilter = new IntentFilter();

    // 供外部使用的服务
    public static GlobalService globalSer = null;

    private static boolean noInit = true;

    // 基本目录
    private static  String pathBase = null;

    // 日志文件路径
    private static String pathLog = null;
    public final static long logMaxLength = 1024 * 150; // 日志文件的最大大小

    // 配置文件路径
    private static String pathConfig = null;

    // 记录添加的条目信息文件路径
    private static String pathItems = null;

    // Application, MainActivity, MainView
    public static MainActivity mainAct = null;
    public static MainView mainView = null;

    // 价格监视器
    private PriceMonitor priceMonitor = null;
    private PriceProtector priceProtector = null;
//    private WakeUpThread wakeUpThread = null;

    // 铃声、振动
    private MediaPlayer mediaPlayer = null;
    private Vibrator vbt = null;

    // 电源锁
    private PowerManager.WakeLock wakeLock = null;
    private PowerManager.WakeLock screemLock = null;

    // 监控器参数
    public static final int TICK = 60000;   // 查询间隔时间
    public static final int NUMPRICE = 200; // PriceRecoder能记录的price数
    public static final int TEMPTICK = 1500; // 为避免被封ip故每次查询的每个url相隔时间
    public static final int RETRYTICK = 30000; // 创建client失败，过多久后尝试

    // 标记是否有通知
    public static boolean hasNotification = false;
    // 标记屏幕是否开着
    public static boolean hasScreenOn = true;

    // 线程池
    private static  final ThreadPoolExecutor fixedThreadPool = (ThreadPoolExecutor)Executors.newFixedThreadPool(1);

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
            pathLog = pathBase + "log.txt";
            file = new File(pathLog);
            if(!file.exists())  file.createNewFile();
            else if(file.length() >= logMaxLength){ // 已超过限制大小
                file.delete();
                file.createNewFile();
            }

            // 处理配置文件
            pathConfig = pathBase + "config.txt";
            file = new File(pathConfig);
            if(file.exists()){
                SettingInfo.loadFromFile(pathConfig);
            } else{
                SettingInfo.installInit(pathConfig);
            }

            // 处理条目信息文件
            pathItems = pathBase + "items.txt";
            file = new File(pathItems);
            if(!file.exists())  file.createNewFile();
        }
    }

    public static String pathLog(){
        if(pathLog == null) checkStatus();
        return pathLog;
    }

    public static String pathItems(){
        if(pathItems == null) checkStatus();
        return pathItems;
    }

    public static String pathConfig(){
        if(pathConfig==null)    checkStatus();
        return pathConfig;
    }

    public static void checkStatus(){
        if(globalSer != null)   globalSer.inCheckStatus();
    }

    public void inCheckStatus(){
        noInit = Boolean.FALSE;
        if(null == pathBase)    pathBase = this.getFilesDir().getAbsolutePath() + File.separator;
        if(null == pathLog) pathLog = pathBase + "log.txt";
        if(null == pathConfig){
            pathConfig = pathBase + "config.txt";
            SettingInfo.loadFromFile(pathConfig);
        }
        if(null == pathItems)   pathItems = pathBase + "items.txt";
    }

    /**
     * 创建PriceMonitor
     */
    private PriceMonitor innerCreatePriceMonitor(){
        if(null != mainView){
            priceMonitor = new PriceMonitor(SettingInfo.serveAddr(), mainView);
        } else priceMonitor = new PriceMonitor(SettingInfo.serveAddr(), pathItems());
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

        // 将创建的Service赋值静态变量，便于使用
        globalSer = this;

        // 创建价格监视器
        innerCreatePriceMonitor();
        fixedThreadPool.execute(priceMonitor);
//        new Thread(priceMonitor).start();

        // 创建价格监视器保护线程
//        innerCreateProtector();
//        new Thread(priceProtector).start();

        // 创建唤醒线程
//        wakeUpThread = new WakeUpThread();
//        wakeUpThread.start();

        // 创建屏幕事件监听器
        createScreenListener();

        // 创建周期时钟
//        GlobalServiceAlarm.startRepeatServiceAlarm(this, 45, GlobalService.class, GlobalService.ACTION);

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true){
//                    try {
//                        Thread.sleep(20000);
//                    } catch (Exception e){}
//                    GlobalService.this.holdScreemLock();
//                    GlobalService.this.innerReleaseScreemLock();
//                }
//            }
//        }).start();
    }

    // 持有wakelock
    public PowerManager.WakeLock holdWakeLock(){
        if(null == wakeLock){
            PowerManager powerM = (PowerManager)getSystemService(POWER_SERVICE);
            if(powerM == null)    return null;
            wakeLock = powerM.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, getClass().getCanonicalName());
            if(null != wakeLock) {
                wakeLock.acquire();
            }
        }
        return wakeLock;
    }

    // 持有wakelock
    public void releaseWakeLock(){
        if(null != wakeLock && wakeLock.isHeld()){
            wakeLock.release();
        }
        wakeLock = null;
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
        // 避免被回收
        if(globalSer != this){
            globalSer = this;
        }
//        wakeUpThread.interrupt();

        // 判断线程是否还活着
        if(fixedThreadPool != null && fixedThreadPool.getActiveCount() == 0){
            this.innerHoldScreemLock();
            MLog.writeLine("PriceMonitor被杀死 ！！！！！！！！");
            innerCreatePriceMonitor();
            fixedThreadPool.execute(priceMonitor);
            this.innerReleaseScreemLock();
        }

        // 获取锁
        this.holdWakeLock();
        return super.onStartCommand(intent, flags, startId);
    }


    // 创建监听屏幕的事件
    public void createScreenListener(){
        // 屏幕灭屏广播
        screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
        // 屏幕亮屏广播
        screenFilter.addAction(Intent.ACTION_SCREEN_ON);
        // 解锁广播
        screenFilter.addAction(Intent.ACTION_USER_PRESENT);
        BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                if(mainAct == null) return;
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    hasScreenOn = true;
                } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
//                    MLog.writeLine("ACTION_SCREEN_OFF");
                    hasScreenOn = false;
                    hasNotification = false;
                    showMainActivity();  // 锁屏将MainAct推到前台
                } else if(Intent.ACTION_USER_PRESENT.equals(action)) {
//                    MLog.writeLine("解锁");
                    if(!hasNotification && !PriceMonitor.signTryConnect)    mainAct.moveTaskToBack(true); //移到后台
                    else{
                        showMainActivity();
                        hasNotification = false;   // 重置状态
                    }
                }
            }
        };
        registerReceiver(mBatInfoReceiver, screenFilter);
    }

    // 获得屏幕锁
    public static void holdScreemLock(){
        if(globalSer == null)   return;
        globalSer.innerHoldScreemLock();
    }

    private void innerHoldScreemLock(){
        // 获取电源管理器对象
        if(screemLock == null){
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if(pm==null)    return;
            // 获取PowerManager.WakeLock对象,后面的参数|表示同时传入两个值,最后的是LogCat里用的Tag
            screemLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK,
                    getClass().getCanonicalName());
            if(null != screemLock)  screemLock.acquire(); // 点亮屏幕
        }
    }

    // 释放屏幕锁
    public static  void releaseScreemLock(){
        if(globalSer == null)   return;
        globalSer.innerReleaseScreemLock();
    }

    private void innerReleaseScreemLock(){
        if(screemLock !=null && screemLock.isHeld()){
            screemLock.release();
        }
        screemLock = null;
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
        if(SettingInfo.onSound() && null==mediaPlayer){
            createMediaPlayer();
            try {
                mediaPlayer.prepareAsync();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer.start();
        }
        if(SettingInfo.onVibrate() && null == vbt){
            createVibrate();
            if(vbt.hasVibrator()){
                long[] freq = {100, 200, 250, 200, 300, 400};
                vbt.vibrate(freq, 0);
            }
        }
    }

    // 关闭反馈
    public void innerCloseFeedback(){
        if(mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        mediaPlayer = null;
        if(vbt != null){
            vbt.cancel();
        }
        vbt = null;
    }

    /**
     * 打开MainActivity
     */
    public static void showMainActivity(){
        if(globalSer == null)   return;
        if(null == mainAct || !mainAct.isFront()){
            Intent intent = new Intent(globalSer, MainActivity.class);
//            startActivity(intent);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setAction(Intent.ACTION_MAIN);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            globalSer.startActivity(intent);
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

    // 将MainActivity移到后台
    public static  void moveMainActToBack(){
        if(null != mainAct) mainAct.moveTaskToBack(true);
    }

    /**
     * 设置MainActivity的标题
     * @param title
     */
    public static void setMainActTitle(String title){
        if(null != mainAct)    mainAct.setTitle(title);
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
        if(null != mainView && mainAct != null){
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
        if(null == GlobalService.mainView)  return "MainView is null";
        if(null == GlobalService.mainAct)   return "MainAct is null";
        GlobalService.mainView.addWaveItem(transType, minuteSpan, wave);
        return "ok";
    }

    /**
     * add PriceItem
     * @param transType
     * @param isBigger
     * @param outPrice
     * @return
     */
    public static String addPriceItem(String transType, boolean isBigger, double outPrice){
        if(null == GlobalService.mainView)  return "MainView is null";
        if(null == GlobalService.mainAct)   return "MainAct is null";
        GlobalService.mainView.addPriceItem(transType, isBigger, outPrice);
        return "ok";
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
