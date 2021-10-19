package io.mapsmessaging.logging;

/**
 * This is an arbitrary category that makes sense to the application, it is useful to map specific log entries to specific modules
 * within your application
 */
public interface Category {

  String getDescription();
}
