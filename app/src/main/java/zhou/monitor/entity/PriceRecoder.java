package zhou.monitor.entity;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import zhou.monitor.utility.StringFormator;

/**
 * 以分钟为单位记录价格，为节省空间，使用一个数组存放价格
 */
public class PriceRecoder implements Serializable {
    private int numPrice = 0;
    private int head = 0;
    private int tail = 0;
    private double[] allPrices = null;

    public PriceRecoder(int capacity){
        allPrices = new double[capacity + 1];   // +1是为了能区分首尾
    }

    /**
     * 添加价格
     * @param price
     */
    public void add(double price){
        if(numPrice == allPrices.length - 1){
            if((++head)==allPrices.length)  head = 0;
            allPrices[tail] = price;
            if((++tail)==allPrices.length)  tail = 0;
        } else {
            allPrices[tail] = price;
            if((++tail)==allPrices.length)  tail = 0;
            numPrice++;
        }
    }

    /**
     * 获得当前价格
     * @return
     */
    public double now(){
        if(tail==0) return allPrices[allPrices.length - 1];
        else return allPrices[tail-1];
    }

    /**
     * 获得时间
     * @param offset    必须大于0，表示从现在（<现在>用1表示）开始，倒数第几分的价格
     * @return
     */
    public double get(int offset){
        if (offset <= 0)    return Double.MAX_VALUE;
        else if (offset >= numPrice)  return allPrices[head];
        int index = tail - 1;
        if(index < 0)   index = allPrices.length - 1;
        while (offset > 1){
            offset--;
            if(index == 0)  index = allPrices.length - 1;
            else index--;
        }
        return allPrices[index];
    }

    /**
     * 字符串化
     * @return
     */
    public String toString(){
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        if(tail < head){
            for(int i=head; i<allPrices.length; i++)   sb.append(String.valueOf(allPrices[i]) + ", ");
            for(int i=0; i<tail; i++) sb.append(String.valueOf(allPrices[i]) + ", ");
        } else {
            for(int i=head; i<tail; i++) sb.append(String.valueOf(allPrices[i]) + ", ");
        }
        sb.append("]");
        return sb.toString();
    }

    public int num(){
        return numPrice;
    }

    public static void main(String[] args) {
//        PriceRecoder pr = new PriceRecoder(3);
//        pr.add(1);
//        pr.add(2);
//        pr.add(3);
//        System.out.println(pr);
//        System.out.println(pr.get(1));
//        pr.add(4);
//        System.out.println(pr);
//        System.out.println(pr.get(3));
//        pr.add(5);
//        System.out.println(pr);
//        System.out.println(pr.get(5));
//
//        DecimalFormat df = new DecimalFormat("##.########");
//        System.out.println(StringFormator.formatPercent(1.2));
//        System.out.println(df.format(784512112.022000));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(sdf.format(new Date()));
        Object obj = new Object();
        System.out.println(obj.toString());
    }
}
