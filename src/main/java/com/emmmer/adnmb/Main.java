package com.emmmer.adnmb;

import com.google.gson.Gson;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Time;
import java.time.Instant;
import java.util.List;
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
    @Override public String toString() {
      return """
        ==============================
        %s\t%s\t%s
        %s
        ==============================
        """.formatted(time, po, no, content);
    }
  }

  record Thread(
    Content content,
    List<Content> reply
  ) {
    @Override public String toString() {
      return ImmutableSeq.from(reply).prepended(content).joinToString("\n");
    }
  }

  static HttpClient CLIENT = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();

  public static void main(String[] args) throws IOException, InterruptedException {
    var str = "45141418";
    var thread = getThread("https://adnmb3.com/t/" + str);
    var json = new Gson().toJson(thread);
    var dir = Time.from(Instant.now()).toString();
    Files.createDirectories(Path.of("/Users/emmmer/Downloads/", dir));
    Files.writeString(Path.of("/Users/emmmer/Downloads", dir, str + ".txt"), thread.toString());
    Files.writeString(Path.of("/Users/emmmer/Downloads", dir, str + ".json"), json);
  }

  private static Document getDoc(String url) throws IOException, InterruptedException {
    var body = CLIENT.send(HttpRequest.newBuilder(URI.create(url))
      .GET()
      .header("content-type", "application/gzip")
      .header("cookie", System.getenv("cookie"))
      .build(), HttpResponse.BodyHandlers.ofInputStream()).body();
    var gzipIn = new GZIPInputStream(body);
    var text = new String(gzipIn.readAllBytes(), StandardCharsets.UTF_8);
    return Jsoup.parse(text);
  }

  private static Thread getThread(String url) throws IOException, InterruptedException {
    var doc = getDoc(url);
    var thread = doc.select(".h-threads-list").select(".h-threads-item-main");
    var pages = doc.select("ul.uk-pagination > li > a");
    var lastPage = pages.stream().map(i -> i.attr("href").split("page=")[1]).collect(ImmutableSeq.factory()).map(Integer::parseInt).max();
    var replies = IntStream.range(1, lastPage + 1).boxed()
      .collect(ImmutableSeq.factory())
      .flatMap(page -> getReplyInUrl(url, page));
    var main = getContent(Objects.requireNonNull(thread.first()));
    return new Thread(main, replies.asJava());
  }

  private static ImmutableSeq<Content> getReplyInUrl(String baseUrl, int page) {
    try {
      java.lang.Thread.sleep(1000);
      var doc = getDoc(baseUrl + "?page=" + page);
      var date = Time.from(Instant.now()).toString();
      System.out.printf("[%s]Get page %s \n", date, page);
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
    var content = ImmutableSeq.from(thread.select(".h-threads-content").html().split("<br>"))
      .map(i -> i.replace("<font color=\"#789922\">", "").replace("</font>", ""))
      .map(i -> i.replaceAll("&gt;", ">"))
      .joinToString("");
    return new Content(time, po, no, content);
  }
}
