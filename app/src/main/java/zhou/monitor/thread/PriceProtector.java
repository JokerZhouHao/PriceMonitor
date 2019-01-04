package zhou.monitor.thread;

import zhou.monitor.service.GlobalService;

/**
 * PriceMonitor保护线程
 */
public class PriceProtector implements Runnable {
    @Override
    public void run() {
        // 与PriceMonitor错开运行
        try{
            Thread.sleep(GlobalService.TICK/2);
        } catch (Exception e){}
        while (true){
            if(PriceMonitor.numRequest == Long.MAX_VALUE)    break; // PriceMonitor因为不可抗原因终止
            GlobalService.createPriceMonitor();
            try{
                Thread.sleep(GlobalService.TICK);
            } catch (Exception e){}
        }
    }
}
