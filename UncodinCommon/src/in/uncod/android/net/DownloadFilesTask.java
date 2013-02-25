package in.uncod.android.net;

import in.uncod.android.util.threading.TaskWithResultListener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

/**
 * A task for downloading multiple files, that accepts an optional results callback.
 * 
 * Download progress is published with three values: current file index, total file count, and current file progress.
 * Subclasses can override onProgressUpdate in order to e.g. update a progress dialog.
 */
public class DownloadFilesTask extends TaskWithResultListener<URL, Integer, List<File>> {
    private static final String TAG = "DownloadFilesTask";

    private File mDestinationPath;
    private boolean mOverwriteExisting;

    /**
     * Creates a file download task.
     * 
     * @param destinationPath
     *            The destination directory. All files will be downloaded here. Must be an existing directory.
     * @param overwriteExisting
     *            If true, any files with the same name as those being downloaded will be overwritten. Otherwise, the
     *            existing file will be returned with the rest of the download results.
     * @param listener
     *            If not null, the result listener will be activated with the list of downloaded files.
     */
    public DownloadFilesTask(File destinationPath, boolean overwriteExisting,
            OnTaskResultListener<List<File>> listener) {
        super(listener);

        if (destinationPath == null || !destinationPath.exists() || !destinationPath.isDirectory())
            throw new IllegalArgumentException("destinationPath must be an existing directory");

        mDestinationPath = destinationPath;
        mOverwriteExisting = overwriteExisting;
    }

    @Override
    protected List<File> doInBackground(URL... downloadUrls) {
        List<File> results = new ArrayList<File>(downloadUrls.length);

        int currentFileIndex = 0;

        // Initial progress state
        publishProgress(currentFileIndex, downloadUrls.length, 0);

        for (URL url : downloadUrls) {
            try {
                if (url != null) {
                    // Determine destination file
                    String[] urlSplits = url.toString().split("/");
                    String filename = urlSplits[urlSplits.length - 1];
                    File destinationFile = new File(mDestinationPath, filename);

                    if (!mOverwriteExisting && destinationFile.exists()) {
                        // Consider file already downloaded
                        publishProgress(currentFileIndex, downloadUrls.length, 100);
                    }
                    else {
                        OutputStream output = new FileOutputStream(destinationFile);

                        Log.d(TAG, "Downloading file " + url + " to " + destinationFile.getAbsolutePath());

                        downloadFileFromUrl(url, output, currentFileIndex, downloadUrls.length);

                        // Update progress
                        publishProgress(currentFileIndex, downloadUrls.length, 100);
                    }

                    results.add(destinationFile);
                }
            }
            catch (Exception e) {
                results.add(null);

                e.printStackTrace();
            }
            finally {
                currentFileIndex++;
            }
        }

        return results;
    }

    /**
     * Download a file from a URL, and update the total progress if possible
     * 
     * @param url
     *            The remote location of the file
     * @param output
     *            The file contents will be written to this stream
     * @param currentFileIndex
     *            The current index of the file being downloaded (for progress updates)
     * @param totalFileCount
     *            The total number of files being downloaded (for progress updates)
     * 
     * @throws IOException
     *             Thrown if there is an error while downloading the file
     */
    protected void downloadFileFromUrl(URL url, OutputStream output, int currentFileIndex, int totalFileCount)
            throws IOException {
        URLConnection connection = url.openConnection();
        connection.connect();

        // Get file size
        int fileLength = connection.getContentLength();

        // Download the file
        InputStream input = new BufferedInputStream(url.openStream());

        byte data[] = new byte[1024];
        float total = 0;
        int count;
        while ((count = input.read(data)) != -1) {
            total += count;

            output.write(data, 0, count);
        }

        // Update progress
        int currentDownloadPercent = (int) (100 * (total / fileLength));
        publishProgress(currentFileIndex, totalFileCount, currentDownloadPercent);

        output.flush();
        output.close();
        input.close();
    }
}