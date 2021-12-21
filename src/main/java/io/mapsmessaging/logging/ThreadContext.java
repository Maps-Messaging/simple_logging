package io.mapsmessaging.logging;

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


}
