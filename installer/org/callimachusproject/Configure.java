/*
 * Copyright (c) 2012 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject;
import com.izforge.izpack.util.*;

public class Configure {

  public void run(AbstractUIProcessHandler handler, String[] args) {

    // Get arguments
    String installdir = args[0];
    String primaryAuthority = args[1];
    String port = args[2];
    String secondaryauthority1 = args[3];
    String secondaryauthority2 = args[4];
    String secondaryauthority3 = args[5];
    String secondaryauthority4 = args[6];
    String secondaryauthority5 = args[7];
    String mailprotocol = args[8];
    String mailfrom = args[9];
    String mailhost = args[10];
    String mailport = args[11];
    String mailauthrequired = args[12];
    String mailusername = args[13];
    String mailpassword = args[14];
    String repository = args[15];
    String daemonuser = args[16];
    String daemongroup = args[17];
    String callimachususername = args[18];
    String callimachuspassword = args[19];

    // Create a log message echoing the arguments to the handler's STDOUT.
    String message = "Installation directory: " + installdir + "\n";
    message += "-----------------------------\n";
    message += "Primary authority: " + primaryAuthority + "\n";
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
    message += "-----------------------------\n";
    message += "Repository: " + repository + "\n";
    message += "Daemon username: " + daemonuser + "\n";
    message += "Daemon group name: " + daemongroup + "\n";
    message += "Repository: " + repository + "\n";
    message += "-----------------------------\n";
    message += "Callimachus username: " + callimachususername + "\n";
    message += "Callimachus password: " + callimachuspassword + "\n";
    handler.logOutput(message, false);
  }

}
