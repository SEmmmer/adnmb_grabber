package com.emmmer.adnmb;

import kala.collection.immutable.ImmutableSeq;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

public class Main {
  record Content(
    String time,
    String po,
    String no,
    String content
  ) {

  }

  record Thread(
    Content content,
    ImmutableSeq<Content> reply
  ) {
  }

  static HttpClient CLIENT = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();

  public static void main(String[] args) throws IOException, InterruptedException {
    var thread = getThread("https://adnmb3.com/t/45144493");
    System.out.println(thread);

  }

  private static Document getDoc(String url) throws IOException, InterruptedException {
    var body = CLIENT.send(HttpRequest.newBuilder(URI.create(url))
      .GET()
      .header("content-type", "application/gzip")
      .build(), HttpResponse.BodyHandlers.ofInputStream()).body();
    var gzipIn = new GZIPInputStream(body);
    var text = new String(gzipIn.readAllBytes(), StandardCharsets.UTF_8);
    return Jsoup.parse(text);
  }

  private static Thread getThread(String url) throws IOException, InterruptedException {
    var doc = getDoc(url);
    var thread = doc.select(".h-threads-list").select(".h-threads-item-main");

    var pages = doc.select("ul.uk-pagination > li > a");
    int lastPage = 83;
    var s = IntStream.range(0, lastPage + 1).boxed()
      .collect(ImmutableSeq.factory())
      .flatMap(page -> getReplyInUrl(url, page));
    var x = getContent(Objects.requireNonNull(thread.first()));
    return new Thread(x, s);
  }

  private static ImmutableSeq<Content> getReplyInUrl(String baseUrl, int page) {
    try {
      var doc = getDoc(baseUrl + "?page" + page);
      return getReply(doc);
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("Cannot get reply in page: " + page, e);
    }
  }

  private static ImmutableSeq<Content> getReply(Document doc) {
    return doc.select(".h-threads-item-reply")
      .stream()
      .map(Main::getContent)
      .filter(i -> !i.no.equals("9999999"))
      .collect(ImmutableSeq.factory());
  }

  private static Content getContent(Element thread) {
    var time = thread.select(".h-threads-info-createdat").text();
    var po = thread.select(".h-threads-info-uid").text().replace("ID:", "");
    var no = thread.select(".h-threads-info-id").text().replace("No.", "");
    var content = ImmutableSeq.from(thread.select(".h-threads-content").html().split("<br>")).joinToString("");
    return new Content(time, po, no, content);
  }
}
