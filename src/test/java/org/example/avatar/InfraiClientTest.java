package org.example.avatar;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;
public class InfraiClientTest {
 public static void main(String[] args) throws Exception {
  HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  AtomicInteger calls=new AtomicInteger();
  server.createContext("/retry", e->{
   boolean retry=calls.incrementAndGet()<4;
   byte[] body=(retry?"{\"ok\":false,\"error\":{\"code\":\"RATE_LIMITED\"}}":"{\"ok\":true}").getBytes();
   e.getResponseHeaders().add("Retry-After","0"); e.sendResponseHeaders(retry?429:200,body.length);e.getResponseBody().write(body);e.close();
  });
  String[] envelopes = {
   "{\"ok\" : false, \"error\":{\"code\":\"DENIED\"}}",
   "{\"data\":{\"ok\":true},\"ok\":false}",
   "{\"ok\":true}", "not JSON", "{}",
   "{\"ok\":true,\"data\":{\"ok\":false},\"metadata\":[null,1,true]}"
  };
  int[] statuses = {200, 200, 403, 200, 200, 200};
  for (int i=0;i<envelopes.length;i++) {
   final int index=i;
   server.createContext("/case"+i, e->{
    byte[] body=envelopes[index].getBytes(java.nio.charset.StandardCharsets.UTF_8);
    e.sendResponseHeaders(statuses[index],body.length);
    e.getResponseBody().write(body);e.close();
   });
  }
  server.start();
  try {
   var constructor=LayeredConfig.class.getDeclaredConstructor(String.class,String.class); constructor.setAccessible(true);
   var client=new InfraiClient(constructor.newInstance("local-test","http://127.0.0.1:"+server.getAddress().getPort()));
   if(!client.post("/retry","{}").equals("{\"ok\":true}")||calls.get()!=4) throw new AssertionError();
   for (int i=0;i<envelopes.length;i++) {
    boolean rejected=false;
    try { client.post("/case"+i,"{}"); }
    catch (java.io.IOException | IllegalStateException expected) { rejected=true; }
    if (rejected != (i<5)) throw new AssertionError("envelope case "+i);
   }
   System.out.println("429 error envelopes retried successfully through fourth attempt");
  } finally {server.stop(0);}
 }
}
