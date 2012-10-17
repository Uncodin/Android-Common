package in.uncod.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.WindowManager;
import android.widget.ArrayAdapter;

/**
 * Created by IntelliJ IDEA. Date: 2/13/12 Time: 1:47 PM
 */
public class Util {
    /**
     * Adds all given items to the given adapter and notifies that the data set changed
     * 
     * ArrayAdapter.addAll is API level 11+, so our own implementation is needed
     * 
     * @param arrayAdapter
     * @param items
     */
    public static <TItems> void addAllToAdapter(ArrayAdapter<TItems> arrayAdapter, Collection<TItems> items) {
        if (arrayAdapter != null) {
            for (TItems item : items) {
                arrayAdapter.add(item);
            }

            arrayAdapter.notifyDataSetChanged();
        }
    }

    public static void setStatusBarVisibility(Activity activity) {
        if (Build.MODEL.equals("limo") && Build.PRODUCT.equals("limo")) {
            activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public static String md5(String hashMe) {
        byte[] bytesOfMessage;
        try {
            bytesOfMessage = hashMe.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] thedigest = md.digest(bytesOfMessage);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < thedigest.length; i++) {
                sb.append(Integer.toString((thedigest[i] & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        }
        catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;

    }

    public static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public static String padLeft(String s, int n) {
        return String.format("%" + n + "s", s).replace(' ', '0');
    }

    static public boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files == null) {
                return true;
            }
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                }
                else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    public static File getFileFromUri(Activity activity, Uri contentUri) {
        return new File(getFilePathFromUri(activity, contentUri));
    }

    public static String getFilePathFromUri(Activity activity, Uri contentUri) {
        if (contentUri.getScheme().equals("file")) {
            return contentUri.getEncodedSchemeSpecificPart();
        }

        // can post image
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = activity.managedQuery(contentUri, proj, // Which columns to return
                null, // WHERE clause; which rows to return (all rows)
                null, // WHERE clause selection arguments (none)
                null); // Order-by clause (ascending by name)
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();

        return cursor.getString(column_index);
    }

    public static String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;

        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        is.close();

        return sb.toString();
    }
}
