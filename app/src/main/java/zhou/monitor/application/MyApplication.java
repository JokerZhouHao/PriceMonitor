package zhou.monitor.application;

import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;

import zhou.monitor.MainActivity;
import zhou.monitor.entity.SettingInfo;
import zhou.monitor.service.GlobalService;

public class MyApplication extends Application {
    private MediaPlayer mediaPlayer = null;
    private Vibrator vbt = null;

    @Override
    public void onCreate() {
        super.onCreate();
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
    public void feedback(){
        if(SettingInfo.onSound && null==mediaPlayer){
            createMediaPlayer();
            try {
                mediaPlayer.prepare();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer.start();
        }
        if(SettingInfo.onVibrate && null == vbt){
            createVibrate();
            if(vbt.hasVibrator()){
                long[] freq = {100, 200, 250, 200, 300, 400};
                vbt.vibrate(freq, 0);
            }
        }

//        if(!Global.mainAct.isFront()){
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
//        }
    }

    // 关闭反馈
    public void closeFeedback(){
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
}
