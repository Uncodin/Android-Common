package in.uncod.android.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ConnectivityAgent implements IConnectivityStatus {
    private Context mContext;
    private ConnectivityManager mConnectMgr;

    public ConnectivityAgent(Context context) {
        mContext = context;

        mConnectMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public boolean canConnectToNetwork() {
        NetworkInfo info = mConnectMgr.getActiveNetworkInfo();

        if (info == null)
            return false;

        if (info.getState() == NetworkInfo.State.CONNECTED) {
            return true;
        }

        if (info.getState() == NetworkInfo.State.DISCONNECTED) {
            return false;
        }

        return false;
    }
}
