package zhou.monitor.utility;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import zhou.monitor.service.GlobalService;

public class GlobalServiceAlarm {
    private static AlarmManager manager = null;
    private static PendingIntent pendingIntent = null;


    //开启周期启动Service的时钟
    public static void startRepeatServiceAlarm(Context context, int seconds, Class<?> cls, String action) {
        //获取AlarmManager系统服务
        if (GlobalServiceAlarm.manager==null)
            manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if(GlobalServiceAlarm.pendingIntent == null){
            //包装需要执行Service的Intent
            Intent intent = new Intent(context, cls);
            intent.setAction(action);
            pendingIntent = PendingIntent.getService(context, 0,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        //触发服务的起始时间
        long triggerAtTime = SystemClock.elapsedRealtime();

        //使用AlarmManger的setRepeating方法设置定期执行的时间间隔（seconds秒）和需要执行的Service
        manager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime,
                seconds * 1000, pendingIntent);
    }

    // 开启一次性闹钟
    public static void startOneTimeServiceAlarm(Context context, long offetMill, Class<?> cls, String action) {
        if (GlobalServiceAlarm.manager==null)
            manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if(GlobalServiceAlarm.pendingIntent == null){
            //包装需要执行Service的Intent
            Intent intent = new Intent(context, cls);
            intent.setAction(action);
            pendingIntent = PendingIntent.getService(context, 0,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        //触发服务的起始时间
        long triggerAtTime = SystemClock.elapsedRealtime();

        //使用AlarmManger的setRepeating方法设置一次执行的时间间隔（seconds秒）和需要执行的Service
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime + offetMill, pendingIntent);
    }


    //停止定时启动Service的闹钟
    public static void stopService(Context context, Class<?> cls,String action) {
        if (GlobalServiceAlarm.manager==null)
            manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if(GlobalServiceAlarm.pendingIntent == null){
            //包装需要执行Service的Intent
            Intent intent = new Intent(context, cls);
            intent.setAction(action);
            pendingIntent = PendingIntent.getService(context, 0,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        //取消正在执行的服务
        manager.cancel(pendingIntent);
    }
}
