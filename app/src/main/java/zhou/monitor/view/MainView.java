package zhou.monitor.view;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import zhou.monitor.MainActivity;
import zhou.monitor.R;
import zhou.monitor.entity.PriceItem;
import zhou.monitor.entity.PriceRecoder;
import zhou.monitor.service.GlobalService;
import zhou.monitor.utility.IOUtility;

public class MainView extends ScrollView {
    private int curItemId = 0;
    private TreeMap<Integer, View> allViews = new TreeMap<>(); // 使item显示与添加的顺序一样，用map便于移除item
    private LinearLayout lLayout = null;
    private ItemLongClickListener itemListener = new ItemLongClickListener();
    private ItemClickListener itemClickListener = new ItemClickListener();

    private HashSet<View> activatedViews = new HashSet<>(); // 记录报警的item

    private MainViewHandler handler = new MainViewHandler();
    private static final String keyType2Recoder = "type2Recoder";

    // 处理请求
    class MainViewHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            if(bundle.containsKey(keyType2Recoder)){
                MainView.this.refreshUI((HashMap<String, PriceRecoder>)bundle.getSerializable(keyType2Recoder));
            }
        }
    }

    public MainView(Context context, AttributeSet attrs) throws Exception{
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.view_scroll, this);
        lLayout = (LinearLayout)findViewById(R.id.mylinear);
        loadItems();
    }

    public MainView(Context context) throws Exception{
        this(context, null);
    }

    // item长按监听器
    class ItemLongClickListener implements OnLongClickListener{
        @Override
        public boolean onLongClick(View view) {
            GlobalService.mainAct.setPressedItem(view);
            return false;
        }
    }

    // item点击监听器
    class ItemClickListener implements OnClickListener{
        @Override
        public void onClick(View view) {
            if(activatedViews.contains(view)){
                if(view instanceof PriceView) ((PriceView)view).recovePriceColor();
                else ((WaveView)view).recoverWaveColor();
                activatedViews.remove(view);
            }
        }
    }

    /**
     * 从文件读取素有items
     * @throws Exception
     */
    private void loadItems() throws Exception{
        BufferedReader br = IOUtility.getBR(GlobalService.pathItems());
        String line = null;
        PriceItem item = null;
        View view = null;
        while(null != (line = br.readLine())){
            item = PriceItem.decode(curItemId, line);
            if(item.isPrice()) view = new PriceView(GlobalService.mainAct, item);
            else view = new WaveView(GlobalService.mainAct, item);
            view.setOnLongClickListener(itemListener);
            view.setOnClickListener(itemClickListener);
            GlobalService.mainAct.registerForContextMenu(view);
            allViews.put(curItemId, view);
            lLayout.addView(view);
            curItemId++;
        }
        br.close();
    }

    /**
     * 将所有item信息写入文件
     */
    private void writeToFile(){
        try {
            BufferedWriter bw = IOUtility.getBW(GlobalService.pathItems());
            for(Map.Entry<Integer, View> en : allViews.entrySet()){
                if(en.getValue() instanceof PriceView){
                    bw.write(((PriceView)en.getValue()).getItem().encode());
                } else {
                    bw.write(((WaveView)en.getValue()).getItem().encode());
                }
                bw.write('\n');
            }
            bw.close();
        } catch (Exception e){}
    }

    /**
     * 添加价格条目
     * @param transType
     * @param isBigger
     * @param outPrice
     */
    public void addPriceItem(String transType, boolean isBigger, double outPrice){
        PriceItem item = new PriceItem(curItemId, transType, isBigger, outPrice);
        View view = new PriceView(GlobalService.mainAct, item);
        view.setOnLongClickListener(itemListener);
        view.setOnClickListener(itemClickListener);
        allViews.put(curItemId, view);
        lLayout.addView(view);
        GlobalService.mainAct.registerForContextMenu(view);
        curItemId++;
        writeToFile();
        GlobalService.updateMonitorAllTypes(this);
    }

    /**
     * 添加振幅条目
     * @param transType
     * @param minuteSpan
     * @param wave
     */
    public void addWaveItem(String transType, int minuteSpan, double wave){
        PriceItem item = new PriceItem(curItemId, transType, minuteSpan, wave);
        View view = new WaveView(GlobalService.mainAct, item);
        view.setOnLongClickListener(itemListener);
        view.setOnClickListener(itemClickListener);
        allViews.put(curItemId, view);
        lLayout.addView(view);
        GlobalService.mainAct.registerForContextMenu(view);
        curItemId++;
        writeToFile();
        GlobalService.updateMonitorAllTypes(this);
    }

    /**
     * 移除条目
     * @param item
     */
    public void removeItem(View item){
        lLayout.removeView(item);
        int id = 0;
        if(item instanceof PriceView)   id = ((PriceView)item).getItem().getItemId();
        else id = ((WaveView)item).getItem().getItemId();
        allViews.remove(id);
        writeToFile();
        GlobalService.updateMonitorAllTypes(this);
    }

    /**
     * 通知刷新MainView
     * @param type2Recoder
     */
    public void refreshAllItemsUI(HashMap<String, PriceRecoder> type2Recoder){
        Message msg = Message.obtain();
        Bundle bundle = null;
        if(null == (bundle = msg.getData())){
            bundle = new Bundle();
            msg.setData(bundle);
        }
        bundle.putSerializable(keyType2Recoder, type2Recoder);
        if(null == handler) handler = new MainViewHandler();
        handler.sendMessage(msg);
    }

    /**
     * 通过查询到的价格更新各个Item
     * @param type2Recoder
     */
    private void refreshUI(Map<String, PriceRecoder> type2Recoder){
        if(allViews == null)    return;
        View view = null;
        PriceView priceV = null;
        WaveView waveV = null;
        PriceItem item = null;
        PriceRecoder recoder = null;
        double percent = 0.0;
        for(Map.Entry<Integer, View> en : allViews.entrySet()){
            view = en.getValue();
            if(view instanceof PriceView){  // PriceView
                priceV = ((PriceView)view);
                item = priceV.getItem();
                recoder = type2Recoder.get(item.getTransType());
                priceV.refreshCurPrice(recoder.now());
                if(item.inOutPrice(recoder.now())){
                    activatedViews.add(priceV);
                    priceV.setPriceToRed();
                }
            } else {    //
                waveV = ((WaveView)view);
                item = waveV.getItem();
                recoder = type2Recoder.get(item.getTransType());
                percent = (recoder.now() - recoder.get(item.getMinuteSpan() + 1))/(recoder.get(item.getMinuteSpan() + 1)) * 100;    // get是从1【表示现在】开始编号
                waveV.refresh(percent, recoder.now());
                if(item.inWave(percent)){
                    activatedViews.add(waveV);
                    waveV.setWaveToRed();
                }
            }
        }
    }

    public TreeMap<Integer, View> getAllViews() {
        return allViews;
    }
}
