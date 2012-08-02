package in.uncod.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import android.app.Activity;
import android.content.ContentResolver;
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

    /**
     * Resolves a file by URI and copies it to the destination
     * 
     * @param activity
     *            The Activity whose context will be used for getting the ContentResolver
     * @param origin
     *            The origin file's URI (will be resolved via ContentResolver if necessary)
     * @param destination
     *            The destination File (should point to an actual file, not a directory)
     * 
     * @throws IOException
     */
    public static void copyFile(Activity activity, Uri origin, File destination) throws IOException {
        InputStream fileIs = null;

        if (origin.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver resolver = activity.getContentResolver();

            try {
                fileIs = resolver.openInputStream(origin);
            }
            catch (FileNotFoundException e) {
                // load media on the DoD streak, content provider is broken... bullshit :/
                File file = getImageFromUri(activity, origin);
                fileIs = new FileInputStream(file);
            }
        }
        else if (origin.getScheme().equals(ContentResolver.SCHEME_FILE)) {
            fileIs = new FileInputStream(origin.getPath());
        }

        if (fileIs == null)
            throw new IllegalArgumentException("origin does not appear to be a valid file for copying");

        FileOutputStream fos = new FileOutputStream(destination);

        int read;
        byte[] bytes = new byte[1024];

        while ((read = fileIs.read(bytes)) != -1) {
            fos.write(bytes, 0, read);
        }
        fos.flush();
        fos.close();
        fileIs.close();
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

    public static File getImageFromUri(Activity context, Uri contentUri) {
        return new File(getRealImagePathFromUri(context, contentUri));
    }

    public static String getRealImagePathFromUri(Activity context, Uri contentUri) {
        if (contentUri.getScheme().equals("file")) {
            return contentUri.getEncodedSchemeSpecificPart();
        }

        // can post image
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = context.managedQuery(contentUri, proj, // Which columns to return
                null, // WHERE clause; which rows to return (all rows)
                null, // WHERE clause selection arguments (none)
                null); // Order-by clause (ascending by name)
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();

        return cursor.getString(column_index);
    }
}
