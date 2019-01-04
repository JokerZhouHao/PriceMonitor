package zhou.monitor.utility;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StringFormator {
    private static DecimalFormat df = new DecimalFormat("0.00");
    private static DecimalFormat dfPrice = new DecimalFormat("##.#########");
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String formatPercent(double x){
        if(df==null) df = new DecimalFormat("0.00");
        return df.format(x);
    }

    public static String formatPrice(double x){
        if(dfPrice == null) dfPrice = new DecimalFormat("##.#########");
        return dfPrice.format(x);
    }

    public static String formatDate(Date date){
        if(sdf==null)   sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    public static String getLogDate(){
        return "[" + formatDate(new Date()) + "]";
    }

    public static void main(String[] args){
//        System.out.println(StringFormator.formatPercent(-1.232));
        System.out.println(StringFormator.formatPrice(-789.2));
    }
}
