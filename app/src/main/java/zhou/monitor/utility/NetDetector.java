package zhou.monitor.utility;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import zhou.monitor.service.GlobalService;

public class NetDetector {
    public static boolean isNetworkConnected() {
        return GlobalService.isNetworkConnected();
    }
}
