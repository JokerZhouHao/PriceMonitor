package zhou.monitor.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.GridLayout;
import android.widget.TextView;

import zhou.monitor.R;
import zhou.monitor.entity.PriceItem;
import zhou.monitor.utility.StringFormator;

public class WaveView extends GridLayout {
    private PriceItem item = null;
    private TextView curWave = null;
    private TextView curPrice = null;

    public WaveView(Context context, PriceItem item){
        this(context, null, item);
    }

    public WaveView(Context context, AttributeSet attrs, PriceItem item) {
        super(context, attrs);
        this.item = item;
        LayoutInflater.from(context).inflate(R.layout.view_monitor_wave, this);
        curWave = findViewById(R.id.curWave);
        curPrice = findViewById(R.id.curPrice);
        setTransType(item.getTransType());
        refresh(0, 0);
        setMinuteSpan(item.getMinuteSpan());
        setExceptWave(item.getWave());
    }

    /**
     * 刷新视图
     * @param wave
     * @param price
     */
    public void refresh(double wave, double price){
        if(wave > 0)     curWave.setText("+" + StringFormator.formatPercent(wave) + "%");
        else curWave.setText(StringFormator.formatPercent(wave) + "%");
        curPrice.setText(StringFormator.formatPrice(price));
    }

    public void setMinuteSpan(int span){
        ((TextView)findViewById(R.id.waveMinuteSpan)).setText(String.valueOf(span) + "分钟");
    }

    /**
     * 设置期望的wave
     * @param wave
     */
    public void setExceptWave(double wave){
        if(wave > 0) ((TextView)findViewById(R.id.exceptWave)).setText("+" + StringFormator.formatPercent(wave) + "%");
        else ((TextView)findViewById(R.id.exceptWave)).setText(StringFormator.formatPercent(wave) + "%");
    }

    public void setTransType(String type){
        ((TextView)findViewById(R.id.tvType)).setText(type);
    }

    public PriceItem getItem() {
        return item;
    }

    public void setWaveToRed(){
        curWave.setTextColor(0xFFFF0000);
    }

    public void recoverWaveColor(){
        curWave.setTextColor(0xFF555555);
    }
}
