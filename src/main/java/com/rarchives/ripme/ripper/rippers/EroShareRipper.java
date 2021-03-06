/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rarchives.ripme.ripper.rippers;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.utils.Http;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EroShareRipper extends AbstractHTMLRipper {

    public EroShareRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getDomain() {
        return "eroshare.com";
    }

    @Override
    public String getHost() {
        return "eroshare";
    }

    @Override
    public void downloadURL(URL url, int index) {
        addURLToDownload(url);
    }

    @Override
    public List<String> getURLsFromPage(Document doc) {
        List<String> urls = new ArrayList<>();
        //Pictures
        Elements imgs = doc.getElementsByTag("img");
        for (Element img : imgs) {
            if (img.hasClass("album-image")) {
                String imageURL = img.attr("src");
                imageURL = "https:" + imageURL;
                urls.add(imageURL);
            }
        }

        //Videos
        Elements vids = doc.getElementsByTag("video");
        for (Element vid : vids) {
            if (vid.hasClass("album-video")) {
                Elements source = vid.getElementsByTag("source");
                String videoURL = source.first().attr("src");
                urls.add(videoURL);
            }
        }

        return urls;
    }

    @Override
    public Document getFirstPage() throws IOException {
        Response resp = Http.url(this.url).ignoreContentType().response();
        return resp.parse();
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://[w.]*eroshare.com/([a-zA-Z0-9\\-_]+)/?$");
        Matcher m = p.matcher(url.toExternalForm());

        if (m.matches())
            return m.group(1);

        throw new MalformedURLException("eroshare album not found in " + url + ", expected https://eroshare.com/album");
    }

    public static List<URL> getURLs(URL url) throws IOException {
        Response resp = Http.url(url).ignoreContentType().response();
        Document doc = resp.parse();

        List<URL> urls = new ArrayList<>();
        //Pictures
        Elements imgs = doc.getElementsByTag("img");
        for (Element img : imgs) {
            if (img.hasClass("album-image")) {
                String imageURL = img.attr("src");
                imageURL = "https:" + imageURL;
                urls.add(new URL(imageURL));
            }
        }

        //Videos
        Elements vids = doc.getElementsByTag("video");
        for (Element vid : vids) {
            if (vid.hasClass("album-video")) {
                Elements source = vid.getElementsByTag("source");
                String videoURL = source.first().attr("src");
                urls.add(new URL(videoURL));
            }
        }

        return urls;
    }
}