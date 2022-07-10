package io.mapsmessaging.logging;

import java.util.Map;
import org.slf4j.MDC;

public class ThreadContext {

  public static void put(String key, String value){
    MDC.put(key, value);
  }

  public static void remove(String key){
    MDC.remove(key);
  }

  public static void clear(){
    MDC.clear();
  }

  public static void clearMap(){
    MDC.clear();
  }

  public static void clearAll(){
    MDC.clear();
  }

  public static Map<String, String> getContext(){
    return MDC.getCopyOfContextMap();
  }

  public static void putAll(Map<String, String> context){
    MDC.setContextMap(context);
  }

  private ThreadContext(){}
}
