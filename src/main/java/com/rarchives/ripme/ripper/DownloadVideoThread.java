package com.rarchives.ripme.ripper;

import com.rarchives.ripme.ui.RipStatusMessage.STATUS;
import com.rarchives.ripme.utils.Utils;
import org.apache.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Thread for downloading files.
 * Includes retry logic, observer notifications, and other goodies.
 */
public class DownloadVideoThread extends Thread {

    private static final Logger LOGGER = Logger.getLogger(DownloadVideoThread.class);

    private URL url;
    private File saveAs;
    private String prettySaveAs;
    private AbstractRipper observer;
    private int retries;

    public DownloadVideoThread(URL url, File saveAs, AbstractRipper observer) {
        super();
        this.url = url;
        this.saveAs = saveAs;
        this.prettySaveAs = Utils.removeCWD(saveAs);
        this.observer = observer;
        this.retries = Utils.getConfigInteger("download.retries", 1);
    }

    /**
     * Attempts to download the file. Retries as needed.
     * Notifies observers upon completion/error/warn.
     */
    @Override
    public void run() {
        try {
            observer.stopCheck();
        } catch (IOException e) {
            observer.downloadErrored(url, "Download interrupted");
            return;
        }
        if (saveAs.exists()) {
            if (Utils.getConfigBoolean("file.overwrite", false)) {
                LOGGER.info("[!] Deleting existing file" + prettySaveAs);
                saveAs.delete();
            } else {
                LOGGER.info("[!] Skipping " + url + " -- file already exists: " + prettySaveAs);
                observer.downloadExists(url, saveAs);
                return;
            }
        }

        int bytesTotal;
        int bytesDownloaded = 0;
        try {
            bytesTotal = getTotalBytes(this.url);
        } catch (IOException e) {
            LOGGER.error("Failed to get file size at " + this.url, e);
            observer.downloadErrored(this.url, "Failed to get file size of " + this.url);
            return;
        }
        observer.setBytesTotal(bytesTotal);
        observer.sendUpdate(STATUS.TOTAL_BYTES, bytesTotal);
        LOGGER.debug("Size of file at " + this.url + " = " + bytesTotal + "b");

        int tries = 0; // Number of attempts to download
        do {
            InputStream bis = null;
            OutputStream fos = null;
            byte[] data = new byte[1024 * 256];
            int bytesRead;
            try {
                LOGGER.info("    Downloading file: " + url + (tries > 0 ? " Retry #" + tries : ""));
                observer.sendUpdate(STATUS.DOWNLOAD_STARTED, url.toExternalForm());

                // Setup HTTP request
                HttpURLConnection huc;
                if (this.url.toString().startsWith("https"))
                    huc = (HttpsURLConnection) this.url.openConnection();
                else
                    huc = (HttpURLConnection) this.url.openConnection();

                huc.setInstanceFollowRedirects(true);
                huc.setConnectTimeout(0); // Never timeout
                huc.setRequestProperty("accept", "*/*");
                huc.setRequestProperty("Referer", this.url.toExternalForm()); // Sic
                huc.setRequestProperty("User-agent", AbstractRipper.USER_AGENT);
                tries += 1;

                LOGGER.debug("Request properties: " + huc.getRequestProperties().toString());
                huc.connect();
                // Check status code
                bis = new BufferedInputStream(huc.getInputStream());
                fos = new FileOutputStream(saveAs);

                while ((bytesRead = bis.read(data)) != -1) {
                    try {
                        observer.stopCheck();
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                        observer.downloadErrored(url, "Download interrupted");
                        return;
                    }

                    fos.write(data, 0, bytesRead);
                    bytesDownloaded += bytesRead;
                    observer.setBytesCompleted(bytesDownloaded);
                    observer.sendUpdate(STATUS.COMPLETED_BYTES, bytesDownloaded);
                }

                bis.close();
                fos.close();
                break; // Download successful: break out of infinite loop
            } catch (IOException e) {
                LOGGER.error("[!] Exception while downloading file: " + url + " - " + e.getMessage(), e);
            } finally {
                // Close any open streams
                try {
                    if (bis != null)
                        bis.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }

                try {
                    if (fos != null)
                        fos.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }

            if (tries > this.retries) {
                LOGGER.error("[!] Exceeded maximum retries (" + this.retries + ") for URL " + url);
                observer.downloadErrored(url, "Failed to download " + url.toExternalForm());
                return;
            }

        } while (true);
        observer.downloadCompleted(url, saveAs);
        LOGGER.info("[+] Saved " + url + " as " + this.prettySaveAs);
    }

    private int getTotalBytes(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("HEAD");
        conn.setRequestProperty("accept", "*/*");
        conn.setRequestProperty("Referer", this.url.toExternalForm()); // Sic
        conn.setRequestProperty("User-agent", AbstractRipper.USER_AGENT);
        return conn.getContentLength();
    }

}