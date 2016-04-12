package org.carlspring.maven.artifact.downloader;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author carlspring
 */
public class ArtifactDownloader
{

    private boolean verbose;

    private ProgressOutputMode outputMode = ProgressOutputMode.PERCENT;


    public void download(URL url, File file)
            throws IOException,
                   URISyntaxException
    {
//        FileOutputStream fos = new FileOutputStream(file);
//        download(url, fos);
        if(!file.exists())
            file.mkdir();

        process(url, file);
    }

    private void process(URL url, File file)
            throws IOException,
            URISyntaxException
    {
        String urlStr = url.toString();

        if(!urlStr.endsWith("/"))
        {
            FileOutputStream fos = new FileOutputStream(file);
            download(url, fos);
        }
        else
        {
            org.jsoup.nodes.Document doc = Jsoup.connect(urlStr).get();
            Elements elements = doc.select("a[href]");

            for (Element el : elements)
            {
                if(el.text().equals("../")) continue;

                process(new URL(urlStr + el.text()), new File(file.getAbsolutePath() + "/" + el.text()));
            }
        }

    }

    public DownloadResult download(URL url, OutputStream os)
            throws IOException,
                   URISyntaxException
    {
        DownloadResult result = new DownloadResult();

        CloseableHttpClient client = null;

        try
        {
            client = HttpClients.createDefault();

            HttpGet httpget = new HttpGet(url.toURI());
            HttpResponse response = client.execute(httpget);
            HttpEntity entity = response.getEntity();
            if (entity != null)
            {
                InputStream is = entity.getContent();

                long contentLength = entity.getContentLength();

                int numberOfBytesRead;
                int totalBytesRead = 0;
                byte[] bytes = new byte[4096];

                long startTime = System.currentTimeMillis();

                while ((numberOfBytesRead = is.read(bytes, 0, bytes.length)) != -1)
                {
                    // Write the artifact
                    os.write(bytes, 0, numberOfBytesRead);

                    totalBytesRead += numberOfBytesRead;

                    if (verbose && contentLength > 0)
                    {
                        if (totalBytesRead != contentLength)
                        {
                            if (outputMode.equals(ProgressOutputMode.BYTES))
                            {
                                System.out.print("\rDownloaded " + totalBytesRead + "/" + contentLength + " bytes...");
                            }
                            else
                            {
                                BigDecimal percent = new BigDecimal((100d * totalBytesRead) / contentLength);

                                System.out.print("\rDownloaded " + percent.intValue() + "/100 %...");
                            }
                        }
                        else
                        {
                            long duration = System.currentTimeMillis() - startTime;

                            result.setContentLength(contentLength);
                            result.setDownloadLength(totalBytesRead);
                            result.setDuration(duration);
                            result.setResponseCode(response.getStatusLine().getStatusCode());

                            System.out.println("\rDownloaded " + totalBytesRead + "/" + contentLength + " bytes in " +
                                               duration + " ms " +
                                               "from " + url.toString() + " " +
                                               "at an average speed of " + (contentLength / (duration / 1000)) + " bytes/s.");
                        }
                    }

                    os.flush();
                }
            }
        }
        finally
        {
            if (os != null)
            {
                try
                {
                    os.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            if (client != null)
            {
                try
                {
                    client.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    public boolean isVerbose()
    {
        return verbose;
    }

    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
    }

}
