# Simple Logging

This very simple wrapper over Log4J2 is not meant to take away or add any logging features, instead, it simply provides a single class for all the log messages. 
The actual log code then is simply a call to the log message enum that needs to be logged. This becomes handy when looking for specific log messages or if you want to 
change the text of the message etc.
