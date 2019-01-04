package zhou.monitor.entity;

import java.util.Objects;

/**
 * 存放要提醒的信息
 */
public class PriceItem {
    public static final int PRICE = 0;
    public static final int WAVE = 1;
    private int id;
    private int storeType = PRICE;
    // transcation type
    private String transType = "";
    // price
    private boolean isBigger = true;
    private double outPrice = 0.0;
    // wave
    private int minuteSpan = 0;
    private double wave = 0.0;

    public PriceItem(int id, String transType, boolean isBigger, double outPrice){
        this.id = id;
        this.storeType = PRICE;
        this.transType = transType;
        this.isBigger = isBigger;
        this.outPrice = outPrice;
    }

    public PriceItem(int id, String transType, int minuteSpan, double wave){
        this.id = id;
        this.transType = transType;
        this.storeType = WAVE;
        this.minuteSpan = minuteSpan;
        this.wave = wave;
    }

    public Boolean isPrice(){
        return storeType==PRICE;
    }

    public Boolean isWave(){
        return storeType==WAVE;
    }

    public double getOutPrice() {
        return outPrice;
    }

    public int getMinuteSpan() {
        return minuteSpan;
    }

    public String getTransType() {
        return transType;
    }

    public boolean inWave(double wave){
        if(this.wave < 0) return wave <= this.wave;
        else return  wave >= this.wave;
    }

    public boolean inOutPrice(double price){
        if(isBigger)    return price >= this.outPrice;
        else return price <= this.outPrice;
    }

    public static boolean isPriceEncode(String st){
        return st.startsWith("PRICE");
    }

    public static boolean isWaveEncode(String st){
        return st.startsWith("WAVE");
    }

    public int getItemId() {
        return id;
    }

    public String encode(){
        if(isPrice()){
            return "PRICE " + transType + " " + String.valueOf(isBigger) + " " + String.valueOf(outPrice);
        } else {
            return "WAVE " + transType + " " + String.valueOf(minuteSpan) + " " + String.valueOf(wave);
        }
    }

    public static PriceItem decode(int id,String str){
        String[] arr = null;
        if(str.startsWith("PRICE")){
            arr = str.split(" ");
            return new PriceItem(id, arr[1], Boolean.parseBoolean(arr[2]), Double.parseDouble(arr[3]));
        } else if(str.startsWith("WAVE")){
            arr = str.split(" ");
            return new PriceItem(id, arr[1], Integer.parseInt(arr[2]), Double.parseDouble(arr[3]));
        } else return null;
    }

    public double getWave() {
        return wave;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriceItem priceItem = (PriceItem) o;
        return id == priceItem.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static void main(String[] args){
        PriceItem pInfo = new PriceItem(1, "ETH/USDT", true, 1.2);
        String st1 = pInfo.encode();
        System.out.println(st1);
        System.out.println(PriceItem.decode(1,st1).encode());

        pInfo = new PriceItem(1,"ETH/USDT", 12, 1.4);
        st1 = pInfo.encode();
        System.out.println(st1);
        System.out.println(PriceItem.decode(1, st1).encode());

        String str = "a/b";
        System.out.println(str.replace("/", ""));
    }
}
