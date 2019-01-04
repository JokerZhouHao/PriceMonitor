package zhou.monitor.thread;

import android.os.PowerManager;
import android.util.Log;

import zhou.monitor.service.GlobalService;

public class WakeUpThread extends Thread {

    private GlobalService service = null;
    private long numRequest = PriceMonitor.numRequest;
    private Boolean isSleep = Boolean.FALSE;

    public WakeUpThread(GlobalService ser){
        this.service = ser;
    }

    @Override
    public void run() {
        try{
            Thread.sleep(8000);
        } catch (Exception e){}
        while (true){
            isSleep = Boolean.FALSE;
            if(PriceMonitor.numRequest != Long.MAX_VALUE && numRequest >= PriceMonitor.numRequest){ // PriceMonitor的状态未向前推进
                service.holdWakeLock(); // 请求锁
                while(PriceMonitor.numRequest != Long.MAX_VALUE && numRequest >= PriceMonitor.numRequest){  // 轮询
                    try{
                        Thread.sleep(1000);
                    } catch (Exception e){}
                }
            }
            if(PriceMonitor.numRequest == Long.MAX_VALUE){  // 线程已终止
                service.releaseWakeLock();  // 释放锁
                return;
            } else {
                numRequest = PriceMonitor.numRequest;
                GlobalService.startOneTimeServiceAlarm(PriceMonitor.sleepTime); // 添加服务时钟
                service.releaseWakeLock(); // 释放锁
            }
//            Log.d("--------- > WakeUp : ", "running . . . . . . . . . . . .. . . ");
            try {
                isSleep = Boolean.TRUE;
                sleep(6000000);
            } catch (Exception e){}
        }
    }

    public Boolean isSleep() {
        return isSleep;
    }
}
