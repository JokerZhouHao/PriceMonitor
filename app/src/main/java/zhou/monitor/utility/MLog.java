package zhou.monitor.utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import zhou.monitor.service.GlobalService;

public class MLog {
    public static void write(String info){
        try {
            BufferedWriter bw = IOUtility.getBW(GlobalService.pathLog(), Boolean.TRUE);
            bw.write(StringFormator.getLogDate());
            bw.write(info);
            bw.close();
        }catch (Exception e){}
    }

    public static void writeLine(String info){
        write(info + "\n");
    }

    public static String loadLog(){
        try {
            BufferedReader br = IOUtility.getBR(GlobalService.pathLog());
            StringBuffer sb = new StringBuffer();
            String line = null;
            while(null != (line = br.readLine())){
                sb.append(line);
                sb.append('\n');
            }
            br.close();
            return sb.toString();
        } catch (Exception e){}
        return "";
    }
}
