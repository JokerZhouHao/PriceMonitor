package zhou.monitor.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.GridLayout;
import android.widget.TextView;

import zhou.monitor.R;
import zhou.monitor.entity.PriceItem;
import zhou.monitor.utility.StringFormator;

public class PriceView extends GridLayout {
    private PriceItem item = null;
    private TextView curPrice = null;
    private TextView outPrice = null;

    public PriceView(Context context, PriceItem item){
        this(context, null, item);
    }

    public PriceView(Context context, AttributeSet attrs, PriceItem item) {
        super(context, attrs);
        this.item = item;
        LayoutInflater.from(context).inflate(R.layout.view_limit_price, this);
        curPrice = findViewById(R.id.tvCurPrice);
        outPrice = findViewById(R.id.outPrice);
        setOutPrice(item.getOutPrice());
        setTransType(item.getTransType());
    }

    public void refreshCurPrice(double price){
        curPrice.setText(StringFormator.formatPrice(price));
    }

    public PriceItem getItem() {
        return item;
    }

    public void setOutPrice(double price){
        outPrice.setText(StringFormator.formatPrice(price));
    }

    public void setTransType(String type){
        ((TextView)findViewById(R.id.tvType)).setText(type);
    }

    public void setPriceToRed(){
        curPrice.setTextColor(0xFFFF0000);
    }

    public void recovePriceColor(){
        curPrice.setTextColor(0xFF555555);
    }
}
