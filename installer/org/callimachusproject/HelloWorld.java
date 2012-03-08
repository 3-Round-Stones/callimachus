package org.callimachusproject;
import com.izforge.izpack.util.AbstractUIProcessHandler;

public class HelloWorld {

  public void run(AbstractUIProcessHandler handler, String[] args) {
    handler.logOutput("Hello, World!", false);
  }

}
