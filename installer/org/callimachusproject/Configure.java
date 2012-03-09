package org.callimachusproject;
import com.izforge.izpack.util.*;

public class Configure {

  public void run(AbstractUIProcessHandler handler, String[] args) {

    // Get arguments
    String primaryAuthority = args[0];
    String port = args[1];
    String secondaryauthority1 = args[2];
    String secondaryauthority2 = args[3];
    String secondaryauthority3 = args[4];
    String secondaryauthority4 = args[5];
    String secondaryauthority5 = args[6];
    String mailprotocol = args[7];
    String mailfrom = args[8];
    String mailhost = args[9];
    String mailport = args[10];
    String mailauthrequired = args[11];
    String mailusername = args[12];
    String mailpassword = args[13];

    // Create a log message echoing the arguments to the handler's STDOUT.
    String message = "Primary authority: " + primaryAuthority + "\n";
    message += "Port: " + port + "\n";
    message += "Secondary authority 1: " + secondaryauthority1 + "\n";
    message += "Secondary authority 2: " + secondaryauthority2 + "\n";
    message += "Secondary authority 3: " + secondaryauthority3 + "\n";
    message += "Secondary authority 4: " + secondaryauthority4 + "\n";
    message += "Secondary authority 5: " + secondaryauthority5 + "\n";
    message += "-----------------------------\n";
    message += "Mail protocol: " + mailprotocol + "\n";
    message += "Mail from address: " + mailfrom + "\n";
    message += "Mail host: " + mailhost + "\n";
    message += "Mail port: " + mailport + "\n";
    message += "Mail authentication required: " + mailauthrequired + "\n";
    message += "Mail username: " + mailusername + "\n";
    message += "Mail password: " + mailpassword + "\n";
    handler.logOutput(message, false);
  }

}
