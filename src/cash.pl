#!/usr/bin/perl -w

############################################################
#  cash.pl
#
#  David Wood (david at http://3roundstones.com)
#  April 2012
#
#  History:
#    29Aug2012: Checked into Callimachus svn
#
#  A shell for the Callimachus Project (http://callimachusproject.org)
#  that implements a client for the Callimachus REST API.
#
#  Usage: perldoc cash.pl
#
#  Copyright 2012 3 Round Stones Inc.  Licensed under the Apache License,
#  Version 2.0 (the "License"); you may not use this file except in compliance
#  with the License. You may obtain a copy of the License at
#  http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
#  or agreed to in writing, software distributed under the License is
#  distributed
#  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
#  express or implied. See the License for the specific language governing
#  permissions and limitations under the License.
#
###########################################################
use strict;
require 5.10.0;
use feature qw(switch say);

# Load modules. Exit with a better error message if some can't be found.
BEGIN {
      my($found, $error, @DBs, @badmodules, $mod, $badmod);
      $found = 0;
      $error = 0;
      @DBs = ('Term::ReadLine', 'LWP::UserAgent', 'LWP::Authen::Digest', 'File::HomeDir', 'Getopt::Long', 'XML::Simple', 'Data::Dumper');
      for $mod (@DBs) {
        if (eval "require $mod") {
          $mod-> import();
          $found += 1;
          #print "$mod loaded\n";
        } else {
          push @badmodules, $mod;
          $error += 1;
          #print "$mod failed to load\n";
        }
      }
      #print "$found Modules loaded: @DBs\n" unless $error;
      if ($error) {
        my $plural = ''; $plural = 's' if $error > 1;
        print "The following module$plural failed to load:\n";
        foreach $badmod (@badmodules) {
          print "  $badmod\n";
        }
        print "Please install them before running $0.  Exiting.\n";
        exit 1;
      }
}

# Module configuration.
Getopt::Long::Configure( qw(gnu_getopt) );

# Globals
my $version = "0.14";
my $authority;
my $host;
my $debug = 0;
my $autols = 0;
my $term;
my $server = new Server;
my $ua = new LWP::UserAgent;
$ua->agent("CallimachusShell/$version " . $ua->agent);
my $username;
my $password;
my $exitstatus = 0; # Used to provide a return code to calling processes. 

# Command-line Options
my $interactive = 0;
my $execute = "";
my $silent = 0;
my $usage = 0;
my $versionreq = "";
GetOptions  ("i|interactive"  => \$interactive,
            "e|execute=s" => \$execute,
            "s|silent" => \$silent,
            "u|usage" => \$usage,
            "v|version=s" => \$versionreq
            ) or die ("ERROR: Command line options could not be parsed");

if ($usage) {
    exec ("perldoc $0") or usage();
}

if ($versionreq) {
    reportVersion();
}

my $OUT = *STDOUT;

# Commands are processed in the following order:
# 1. Commands provided on the command line (via -e)
# 2. Interactive commands (via -i)
# 3. Commands provided on STDIN only if not interactive and no commands provided on the command line

# Process commands from the command line.
if ($execute) {
    process($execute);
}

# Process information from the user prompt.
if ($interactive) {
    $term = new Term::ReadLine 'Callimachus Shell';
    my $prompt = "callimachus> ";
    $OUT = $term->OUT || *STDOUT;

    # Get features of this Readline implementation.
    # Note that "addhistory" needs to be present for history to work (it isn't on Macs)
    if ($debug > 2) {
        say $term->ReadLine;
        my $features = $term->Features;
        foreach my $feature (keys %$features) {
                say "     Feature: $feature";
        }
    }
    
    print $OUT "Welcome to the Callimachus Shell.  Enter 'help' for a list of commands.\n";
    while ( defined ($_ = $term->readline($prompt)) ) {
        process($_);
    }    
}

unless ($execute or $interactive) {
    # Process information from STDIN (presumed to be a Callimachus Shell script)
    $OUT = *STDOUT;
    while ( <> ) {
        process($_);
    }
}

# Processing complete.  Exit with appropriate return code.
exit $exitstatus;

############
# End main #
############


# Process shell commands. Commands may be compounded with ';' separation.
sub process {
    my $execute = shift(@_);  
    my @commands = split(';', $execute);
    foreach my $com (@commands) {
        $com =~ s/^\s*(.*)\s*/$1/; # Trim leading and trailing whitespace.
        processCmd($com);
        # Exit on any error if not in interactive mode.
        if ( $exitstatus and not $interactive) {
            say STDERR "Exiting on error (not in interactive mode).";
            exit($exitstatus);
        }
    }
}

# Process an individual command
sub processCmd {
    my $command = shift(@_);
    warn $@ if $@;
    
    # FEATURE REQUESTS:
    # TODONEXT: Allow full-text searches of labels.
    # TODONEXT: Allow SPARQL queries.
    # TODO: Tweak get/put/modify functionality for RDF content (BLOBs done first).
    # TODO: Delete files in the active folder (rm --files).
    # TODO: Delete folders in the active folder (rm --folders).
    # TODO: Handle mv for folders.  Check to ensure the input is a folder.
    # TODO: Allow the 'ls' command to take an optional (server/)folder path.
    # TODO: Modify a file (Need to use PUT if filename already exists).
    # TODO: Allow a get/upload/delete/etc to operate on a full URL.
    
    # REFACTORING:
    # TODO: Survey all action methods for consistency of var checks, state checks, login checks...
    # TODO: Review all commands requiring authentication to ensure checks are made (consistently).
    # TODO: Use parseFolderPath() in deleteFile(), etc.
    # TODO: Refactor chDir() and parseDirPath(). Should they be the same method??
    # MAYBE: Have a single 'when' clause per command prefix. Maybe use parser-generator.
    # MAYBE: Refactor login to provide just username and ask for password (won't work without a better libreadline on Macs).  See http://www.perlmonks.org/?node_id=352298 and http://www.trinitycore.info/How-to:Mac#Installing_new_libraries.
    given ($command) {
        when (/^\s*$/) { }
        when (/^\#/) { }
        when (/^cat/i) { $command =~ m/^cat\s+(.*)$/i; retrieveFile("cat", $1) unless $@; }
        when (/^cd\s+(.*)$/i) { $command =~ m/^cd\s+(.*)\s*$/i; parseDirPath($1) unless $@; }
        when (/^cd/i) { chHomeDir("1") unless $@; }
        when (/^debug/i) { reportState() unless $@; }
        when (/^echo/i) { $command =~ m/^echo\s+(.*)$/i; echo($1) unless $@; }
        when (/^exec/i) { $command =~ m/exec\s+(.*)\s*$/i; execCommand($1) unless $@; }
        when (/^export/i) { $command =~ m/^(export\s+car|export)\s+(.*)\s*$/i; exportCar($2) unless $@; }
        when (/^get/i) { $command =~ m/^get\s+(.*)$/i; retrieveFile("get", $1) unless $@; }
        when (/^help\s+(.*)$/i)  { $command =~ m/^help\s+(.*)\s*$/i; commandhelp($1) unless $@; }
        when (/^help/i)  { help() unless $@; }
        when (/^import/i) { $command =~ m/^(import\s+car|import)\s+(.*)\s*$/i; importCar($2) unless $@; }
        when (/^login/i) { $command =~ /^login\s+(.*?)\s+(.*)\s*$/i; login($1, $2) unless $@; }
        when (/^logout/i) { logout() unless $@; }
        when (/^ls$/i) { ls() unless $@; }
        when (/^mkdir/i) { $command =~ m/^mkdir\s+(.*)\s*$/i; makeDir($1) unless $@; }
        when (/^mv/i) { $command =~ /^mv\s+(.*?)\s+(.*)\s*$/i; moveFile($1, $2) unless $@; }
        when (/^put/i) { $command =~ m/^put\s+(.*)$/i; putFile($1) unless $@; }
        when (/^pwd$/i) { pwd() unless $@; }
        when (/^rm\s+/i) { $command =~ m/^rm\s+(.*)$/i; deleteFile($1) unless $@; }
        when (/^rmdir/i) { $command =~ m/^rmdir\s+(.*)$/i; deleteFolder($1) unless $@; }
        when (/^server/i) { $command =~ m/^server\s+(.*)\s*$/i; server($1) unless $@; }
        when (/^set/i) { $command =~ m/^set\s+(.*?)\s+(.*)\s*$/i; setOptions($1, $2) unless $@; }
        when (/^ver/i) { reportVersion() unless $@; }
        when (/^\s*(exit|quit)/i) { exit(0); }
        default { print $OUT "Command not found.  Try 'help' for suggestions.\n" unless $@; }
    }

    if ($interactive and $term and $_) { $term->addhistory($command) if /\S/; }
}

# Send the contents of a file to STDOUT.
sub catFile {
    my $content = shift(@_);
    unless ($content) {
        say $OUT "Error: No content found!";
        $exitstatus++;
        return 0;
    }
    print STDOUT $$content;
    return 1;
}

# Change the active folder, given a folder name.
sub chDir {
    # Take the name of a folder and change to it via its URL.
    my $folderName = shift(@_);
    say $OUT "In chDir()\n   Attempting to cd to \"$folderName\"." if $debug > 2;
    unless ( serverSet() ) { return 0; }
    if ( $folderName eq "/" ) { chHomeDir("1"); }
    
    my $folder = $server->folders->{$folderName};
    if ( $folder ) {
        say $OUT "   Setting the folder to " . $server->folders->{$folderName} if $debug > 2;
        $server->folder($server->folders->{$folderName});
        my $folders = $server->folderHistory;
        push( @$folders, $folderName);
        $server->folderHistory( $folders );
        &getFolderContents();
        return 1;
    }
    say $OUT "There is no folder called $folderName." unless $silent;
    $exitstatus++;
    return 0;
}

# Check a Callimachus server's HTTP authority for sanity.
sub checkAuthority {
    my $authority = shift(@_);
    if ( $authority =~ m/^http:\/\// ) {
        # TODO: Should check response from server before returning true.
        # TODO: Should ensure authority has trailing /
        return 1;
    }    
    $exitstatus++;
    return 0;
}

# Check to see if an active Callimachus server has been configured.
sub serverSet {
    # Don't process unless the server has been set.
    if ( $server->folder and $server->authority ) {
        return 1;
    }
    say $OUT "Please set the server first.";
    commandhelp("server");
    $exitstatus++;
    return 0;
}

# Change the active folder to the home folder for the active server.
sub chHomeDir {
    # Return to the home folder.
    unless ( serverSet() ) { return 0; }
    my $shouldReport = shift (@_);
    $server->folder($server->homeFolder);
    my @folders = ("/");
    $server->folderHistory( \@folders );
    &getFolderContents();
    &ls() if ($autols and $shouldReport);
}

# Report extended help for a given command.
sub commandhelp {
    my $helpterm = lc(shift(@_));
    given ($helpterm) {
        when (/\#/) { say $OUT "Lines beginning with # are considered comments and are not processed." }
            when (/^cat/) { say $OUT "cat <file title>:  Retrieves the designated file and send it to the standard output.  The file title must be exactly as it appears in a folder listing, including spaces.  This action may require authorization (see 'help login')." }
        when (/^cd/) { say $OUT "cd <folder title>: Changes the active folder to the name given.  The title provided may be relative to the current folder (without a leading '/') or absolute to the top folder for the active server (with a leading '/').  Leading '..' characters refer to the parent folder. If the folder title is omitted, the folder will be changed to the top level ('home') folder." }
        when (/^debug/) { say $OUT "debug: Report the state of the shell's server object.  This command is primarily used for debugging the shell."; }
        when (/^echo/) { say $OUT "echo <string>: Echo a string to STDOUT.  This command may be used (e.g.) to mark up output when run with -e or a script file."; }
        when (/^exec/) { say $OUT "exec <command>: Issues a command to the calling shell.  The exec command allows users to run external commands without exiting the Callimachus Shell." }
        when (/^exit/) { say $OUT "exit: Exits the shell." }
        when (/^export/) { say $OUT "export [CAR] [<filename>]: Exports the contents of the active folder into a Callimachus Archive (CAR) file.  If a filename is not provided, the server's suggested filename (consisting of path-to-folder-servername.car) will be used.  This action requires authorization (see 'help login')." }
        when (/^get/) { say $OUT "get <file title>: Retrieves the designated file and saves it to the local file system.  The file title must be exactly as it appears in a folder listing, including spaces.  This action may require authorization (see 'help login')." }
        when (/^help/) { say $OUT "help: Provides a list of built-in commands." }
        when (/^import/) { say $OUT "import [CAR] <filename>: Imports the contents of the filename into the active folder.  The file must be a Callimachus Archive (CAR).  This action replaces the contents of the active folder(!). This action requires authorization (see 'help login')." }
        when (/^login/) { say $OUT "login <username> <password>: Logs into the active server so actions requiring authentication may proceed, such as changing the server state." }
        when (/^logout/) { say $OUT "logout: Logs out of the active server.  Actions requiring authentication will no longer work." }
        when (/^ls/) { say $OUT "ls: Lists the contents of the active folder.  Subfolder names are followed by a / character, e.g. 'rdf/'.  Files are followed by their type in parentheses, e.g. 'helloworld (graph)'." }
        when (/^mkdir/) { say $OUT "mkdir <foldername>: Make a new folder called foldername in the active folder or as provided by a path.  The folder name given may be a URL, path or simple alphanumeric name.  Folders will be created along a given path, as required.  This action requires authorization (see 'help login')." }
        when (/^mv/) { say $OUT "mv <filename1> <filename2>: Move filename1 to filename2.  Filenames may be  simple names, which will rename a file in the active folder, paths relative to the active server or fully-qualified URLs." }
        when (/^put/) { say $OUT "put <filename>: Puts the designated file onto the server in the active folder.  The filename will become the filename on the server, but will be changed to lower case.  This action requires authorization (see 'help login')." }
        when (/^pwd/) { say $OUT "pwd: Returns the path of the active folder." }
        when (/^quit/) { say $OUT "quit: Exits the shell." }
        when (/^rm\s+/) { say $OUT "rm <file title>: Deletes the designated file from the active folder.  The file title must be exactly as it appears in a folder listing, including spaces.  This action requires authorization (see 'help login')." }
        when (/^rmdir/) { say $OUT "rmdir <folder title>: Deletes the designated folder from the active folder.  The folder title must be exactly as it appears in a folder listing, including spaces.  This action requires authorization (see 'help login')." }
        when (/^server/) { say $OUT "server <url> or server -p <proxy> <url>: Sets the Callimachus server authority.  For example, 'server http://localhost:8080/' creates a server object with that base HTTP authority.  The server URL must refer to a Callimachus instance.  Further commands will relate to the last set server authority.  Optionally set an HTTP proxy with -p to allow connection to a Callimachus server behind a proxy or running a DNS name different from its HTTP authority.  The <proxy> field must contain a URL and may contain an optional port number (e.g. http://www.example.com:8080).  The proxy port defaults to 1080 if not provided.  The <proxy> field must contain 'http://'." }
        when (/^set/) { say $OUT "set <option> <value>:  Set a shell option to the specified value.  Current options are 'debug', which may be set to a non-negative integer value to cause an increasing level of additional information to be displayed, and 'autols', which may be set to 1 to cause an 'ls' command to be issued after every 'cd' command."}
        when (/^ver/) { say $OUT "version: Report the version number of this shell." }
        default { say $OUT "No help for term \"$helpterm\"." };   
    }
}

# Delete a file from the active folder.
sub deleteFile {
    unless ( serverSet() ) { return 0; }
    unless ( $server->loggedIn ) {
        say $OUT "Deleting a file may only be performed by an authenticated user.  Please log in first.";
        commandhelp("login");
        return;
    }
    
    my $title = shift(@_);
    unless ($title) {
        say $OUT "Error: The rm command requires a filename to delete.";
        &commandhelp("rm");
        $exitstatus++;
        return 0;
    }
    
    my $url = $server->files->{$title}->{src};
    say $OUT "Attempting to delete URL: $url" if $debug;
    unless ($url) {
        say $OUT "Error: Could not determine URL for file '$title'.";
        $exitstatus++;
        return 0;
    }
    my $req = new HTTP::Request DELETE => $url;
    $req->header( "Host" => $host );
    say $OUT "REQUEST:" if $debug;
    say $OUT $req->as_string if $debug;
    
    # Pass request to the user agent and get a response back
    my $res = $ua->request($req);
    # Check the outcome of the response
    if ($res->is_success) {
        say $OUT "Delete request resulted in:  " . $res->status_line if $debug;
        parseDirPath( ".", "suppress display" );
        say $OUT "Deleted file." if $debug;
        return 1;
    } elsif ( $res->status_line =~ m/401/ ) {
        unless ( deleteFile($title) ) {
            say $OUT "Error: Failed to delete file after two tries.";
            $exitstatus++;
            return 0;
        }
        return 1;
    } else {
        say $OUT "Error: Failed to delete file $title.  The server reported: " . $res->status_line;
        print $OUT $res->as_string if $debug;
        $exitstatus++;
        return 0;
    }
}


# Delete a subfolder (and its contents) from the active folder.
sub deleteFolder {

    unless ( serverSet() ) { return 0; }
    unless ( $server->loggedIn ) {
        say $OUT "Deleting a folder may only be performed by an authenticated user.  Please log in first.";
        commandhelp("login");
        return;
    }
    
    my $title = shift(@_);
    $title =~ s/\/$// if ($title =~ m/\/$/);
    unless ($title) {
        say $OUT "Error: The rmdir command requires a folder name to delete.";
        &commandhelp("rmdir");
        $exitstatus++;
        return 0;
    }
    
    my $url = $server->folders->{$title};
    say $OUT "Attempting to delete URL: $url" if $debug;
    unless ($url) {
        say $OUT "Error: Could not determine URL for file '$title'.";
        $exitstatus++;
        return 0;
    }
    my $req = new HTTP::Request DELETE => $url;
    $req->header( "Host" => $host );
    say $OUT "REQUEST:" if $debug;
    say $OUT $req->as_string if $debug;
    
    # Pass request to the user agent and get a response back
    my $res = $ua->request($req);
    # Check the outcome of the response
    if ($res->is_success) {
        say $OUT "Delete request resulted in:  " . $res->status_line if $debug;
        parseDirPath( ".", "suppress display" );
        say $OUT "Deleted folder and its contents." if $debug;
        return 1;
    } elsif ( $res->status_line =~ m/401/ ) {
        unless ( deleteFolder($title) ) {
            say $OUT "Error: Failed to delete folder after two tries.";
            $exitstatus++;
            return 0;
        }
        return 1;
    } else {
        say $OUT "Error: Failed to delete folder.  The server reported: " . $res->status_line;
        print $OUT $res->as_string if $debug;
        $exitstatus++;
        return 0;
    }
}


# Echo a string to STDOUT.
sub echo {
    my $string = shift(@_);
    $string = "" unless $string;
    say $OUT $string;
}

# Execute a command on the calling (local) shell.
sub execCommand {
    my $command = shift(@_);
    unless ($command) {
        say $OUT "Error: The exec command requires a command to execute.";
        &commandhelp("exec");
        $exitstatus++;
        return 0;
    }
    system($command);
    return 1;
}

# Download a Callimachus Archive File (CAR) from the active folder.
sub exportCar {
    unless ( serverSet() ) { return 0; }
    unless ( $server->loggedIn ) {
        say $OUT "Exporting a CAR file may only be performed by an authenticated user.  Please log in first.";
        commandhelp("login");
        return;
    }
    my $filename = shift(@_);
    my $links = getFolderLinks($server->folder);
    unless ($links) {
        say $OUT "Error: Failed to get links from the active folder.";
        $exitstatus++;
        return 0;
    }
    
    my $url = $links->{'http://callimachusproject.org/rdf/2009/framework#archive'}->{url};
    say $OUT "Attempting to resolve URL: $url" if $debug;
    unless ($url) {
        say $OUT "Error: Could not determine archive URL for the active folder.";
        $exitstatus++;
        return 0;
    }
    my $req = new HTTP::Request GET => $url;
    $req->header( "Host" => $host );
    $req->header( "Accept" => "application/zip" );
    $req->header( "Accept-Encoding" => "*;q=0" );
    $req->protocol('HTTP/1.1');
    say $OUT "REQUEST:" if $debug;
    say $OUT $req->as_string if $debug;
    
    # Pass request to the user agent and get a response back
    my $res = $ua->request($req);
    # Check the outcome of the response
    if ($res->is_success) {
        say $OUT "CAR file request resulted in:  " . $res->status_line if $debug;
        my $content = $res->content;
        my $size = length $content;
        say $OUT "The CAR file is $size bytes in length." if $debug;
        say $OUT "The filename is " . $res->filename if $debug;
        say $OUT $res->as_string if ($debug>2) and ($size < 1000);
        say $OUT "Export successful" unless $silent;
        
        # Save the content to a filehandle.
        my $fh;
        $filename = $res->filename unless ($filename);
        say "Exporting CAR for folder " . $server->folder . " into $filename" if $debug;
        open($fh, ">", "$filename");
        unless ($fh) {
            say $OUT "Error: Cannot open $filename for writing: $!";
            $exitstatus++;
            return 0;
        }
        print $fh $content;
        close($fh);
        return 1;
        
    } else {
        say $OUT "Error: Failed to download CAR file.  The server reported: " . $res->status_line;
        print $OUT $res->as_string if $debug;
        $exitstatus++;
        return 0;
    }
}

# Get a MIME type from a file extension.
sub getContentType {
    my $filename = shift(@_);
    my $ext;
    if ( $filename =~ m/\.(.*)$/ ) {
        $ext = $1;
    } else {    
        $exitstatus++;
        return 0;
    }
    given ($ext) {
        when (/css$/) { return "text/css"; }
        when (/docbook$/) { return "application/docbook+xml"; }
        when (/fnt$/) { return "application/font-woff"; }
        when (/gif$/) { return "image/gif"; }
        when (/html$/) { return "text/html"; }
        when (/ico$/) { return "image/vnd.microsoft.icon"; }
        when (/jpg$|jpeg$/) { return "image/jpeg"; }
        when (/js$/) { return "text/javascript"; }
        when (/owl$/) { return "application/rdf+xml"; }
        when (/pdf$/) { return "application/pdf"; }
        when (/png$/) { return "image/png"; }
        when (/rdf$/) { return "application/rdf+xml"; }
        when (/rq$/) { return "application/sparql-query"; }
        when (/svg$/) { return "image/svg+xml"; }
        when (/ttl$/) { return "text/turtle"; }
        when (/txt$/) { return "text/plain"; }
        when (/xhtml$/) { return "application/xhtml+xml"; }
        when (/xpl$/) { return "application/xproc+xml"; }
        when (/xsl$/) { return "text/xsl"; }
    }    
    $exitstatus++;
    return 0;
}

# Retreive the contents of the active folder from the active server.
sub getFolderContents {
    unless ( serverSet() ) { return; }
    # Create a request
    my $url = $server->folder;
    say $OUT "In getFolderContents()\n   URL: $url" if $debug > 1;
    my $req = new HTTP::Request GET => $url;
    $req->header( "Host" => $host );
    $req->header( "Accept" => "application/xml" );
    say $OUT "Request:\n" . $req->as_string . "\n" if $debug >2;
    
    # Pass request to the user agent and get a response back
    my $res = $ua->request($req);
    # Check the outcome of the response
    if ($res->is_success) {
        
        # Check and hack to tell when Callimachus is (incorrectly) sending
        # multiple Client-Transfer-Encoding headers.
        unless ($res->content) {
            say $OUT "Error: Cannot process folder due to multiple Client-Transfer-Encoding headers in response.";
            say $OUT "       Folder set to home.";
            chHomeDir();
            return 0;
        }
        
        my $xs = XML::Simple->new(ForceArray => 1, KeyAttr => []);
        my $xml = $xs->parse_string($res->content);
        my %folders;
        my %files;
        print $OUT Dumper($xml) if $debug > 2;
        my $i = 0;
        my $furl = "";
        while ( $xml->{entry}[$i] ) {
            # Identify folders.
            my $folderid = "";
            my $j = 0;
            while ( $xml->{entry}[$i]->{link}[$j] ) {
                if ( $xml->{entry}[$i]->{link}[$j]->{rel} eq "contents" ) {
                    $furl = $xml->{entry}[$i]->{link}[$j]->{href};
                    $folderid = "/";
                }
                $j++;
            }
            # Store folder and file metadata for use by the 'cd' command.
            my $title = $xml->{entry}[$i]->{title}[0];
            my $icon = $xml->{entry}[$i]->{icon}[0];
            my $src = $xml->{entry}[$i]->{content}->{src};
            my $type = $xml->{entry}[$i]->{content}->{type};
            
            if ( $icon ) {
                $icon =~ s/^.*\/(.*?)\.\w+$/ ($1)/;
                if ( $icon =~ m/folder/ ) { $icon = ""; }
            } else { $icon = ""; }
            
            if ( $title and $furl ) { # Only for folders
                $folders{$title} = $furl;
                $furl = "";
            } elsif ( $title ) { # Only for files
                my $label = $title;
                if ($src) {
                    $label = $src;
                    $label =~ s/http:\/\/.*\/(.*)$/$1/;
                }
                my %filecontents = ( label => $label . $icon, src => $src, type => $type );
                $files{$label} = \%filecontents;
            }
            $i++;
        }
        
        # Store the subfolders and files for the active folder.
        if (\%folders) {
            $server->folders(\%folders);
        } else {
            $server->folders(undef);
            say $OUT "No subfolders found in this folder." if $debug > 2;
        }
        if (\%files) {
            $server->files(\%files);
        } else {
            $server->files(undef);
            say $OUT "No files found in this folder." if $debug > 2;
        }
        
    } else {    
        # TODO: Handle this error condition?
        say $OUT "Error: Could not resolve folder contents.";
        $@ = "";
    }
}

# Get links from a folder URL.  Returns a hash reference containing the links.
sub getFolderLinks {
    my $url = shift(@_);
    
    # TODO: Is there a better way to get this URL?
    if ( $url =~ m/\?\w+$/ ) { $url =~ s/^(.*)\?\w+$/$1/; }
    
    # Create a request
    my $req = new HTTP::Request OPTIONS => $url;
    $req->header( "Host" => $host );
    # Pass request to the user agent and get a response back
    my $res = $ua->request($req);
    if ($debug) {
        say $OUT "Request:";
        say $OUT $req->as_string;
        say $OUT "Response:";
        say $OUT $res->as_string;
    }
    
    # Check the outcome of the response
    if ($res->is_success) {
        my %links = ();
        # TODO: Should this also check $res->header("link") for a non-empty contents link?
        #       Note that content links won't show up on a Callimachus server when hit via
        #       the wrong authority.
        unless ( $res->header("link") ) {
            say $OUT "Received an unexpected response from the server: " . $res->status_line;
            say $OUT "Tried to reach folder URL: " . $url if $debug;
            $exitstatus++;
            return 0;
        }
        my $callimachus_version;
        if ( $res->header("server") ) {
            # Find and store the Callimachus version.
            $callimachus_version = $res->header("server");
            unless ( $callimachus_version =~ m/^Callimachus/ ) {
                say $OUT "Error: The server does not seem to be a Callimachus.\n  The server header was: $callimachus_version";
                $exitstatus++;
                return 0;
            }
        } else {
            say $OUT "Error: No server header found.  The server does not seem to be a Callimachus.";
            $exitstatus++;
            return 0;
        }
        $callimachus_version =~ s/^Callimachus\///; # Store just the version number.
        $server->version($callimachus_version);
        
        my @linkStrings = split(',', $res->header("link"));
        foreach my $linkString (@linkStrings) {
            my $url = "";
            my $rel = "";
            my $types = "";
            my @typeslist = ();
            my %link;
            if ( $linkString =~ m/<(.*?)>/ ) { $url = $1; }
            if ( $linkString =~ m/rel=\"(.*?)\"/ ) { $rel = $1; }
            if ( $linkString =~ m/type=\"(.*?)\"/ ) { $types = $1; }
            if ( $types =~ m/\s/ ) {
                @typeslist = split(' ', $types);
            } else {
                $typeslist[0] = $types;
            }
            
            # Store each link.
            %link = (url => $url);
            @{$link{types}} = @typeslist; # Carefully cast.
            %{$links{$rel}} = %link;
        }
        return \%links; # Return the hash reference, not the hash.
    } else {
        $server->authority("");
        say $OUT "Error: Invalid URL: $url.  No Callimachus instance found at that address.";
        commandhelp("server");
        $@ = "";
        $exitstatus++;
        return 0;
    }
}

# Returns the active folder path.
sub getPwd {
    unless ( serverSet() ) { return; }
    my $serverfolders = $server->folderHistory;
    my $folderName = "";
    foreach my $entry ( @$serverfolders ) {
        $entry .= '/' unless $entry =~ /\//;
        $folderName .= $entry;
    }
    return $folderName;
}

# Report general help.
sub help {
    # TODO: Include a hyperlink to online docs
    print $OUT <<'ENDOFHELP';
Cash is a shell for the Callimachus Project (http://callimachusproject.org).

Valid shell commands (use 'help <command>' to get details for a particular command):

#                           Comment
cd <folder>                 Change the active folder.
debug                       Report the state of the shell's server object.
echo <string>               Echo a string to STDOUT.
exec <command>              Execute a command on the calling shell.
exit                        Exit the shell.
export [CAR] [<filename>]   Exports the contents of the active folder into a CAR file.
get <file title>            Retreive a file from the active folder and save it.
help                        Get this help message.
import [CAR] <filename>     Imports the contents of the filename (a CAR file) into the active folder.
login <user> <pass>         Login to the active server.
logout                      Log out of the active server.
ls                          List the contents of the active folder.
mkdir <foldername>          Make a new folder.
mv <filename1> <filename2>  Move filename1 to filename2.
put <filename>              Store a file in the active folder.
pwd                         Return the path of the active folder.
quit                        Exit the shell.
rm <filename>               Delete a file from the active folder.
rmdir <folder name>         Delete a folder from the active folder.
server <url>                Set the Callimachus server authority. Optionally set an HTTP proxy (-p <proxy>).
set <option> <value>        Set a shell option.
version                     Report the version of this shell.
ENDOFHELP
}

# Upload a Callimachus Archive File (CAR) into the active folder.
sub importCar {
    unless ( serverSet() ) { return 0; }
    unless ( $server->loggedIn ) {
        say $OUT "Importing a CAR file may only be performed by an authenticated user.  Please log in first.";
        commandhelp("login");
        return;
    }
    my $filename = shift(@_);
    my $contentRef = shift(@_);
    my $content;
    unless ($filename) {
        say $OUT "Importing a CAR file requires a filename to import from.  Please provide a filename of a CAR archive.";
        commandhelp("import");
        return;
    }
    
    # Read file from the local file system.
    if ( $contentRef ) {
            $content = $$contentRef;
    } else {
        unless ( open (CARIN, "<", $filename) ) {
            say $OUT "Error:  Cannot read file $filename from filesystem: $!";
            $exitstatus++;
            return 0;
        }
        while (<CARIN>) {
            $content .= $_;
        }
        close (CARIN) or
            warn "Warning: Could not close the filehandle: $!";
    }
    
    my $links = getFolderLinks($server->folder);
    unless ($links) {
        say $OUT "Error: Failed to get links from the active folder.";
        $exitstatus++;
        return 0;
    }
    
    my $url = $links->{'http://callimachusproject.org/rdf/2009/framework#archive'}->{url};
    say $OUT "Attempting to resolve URL: $url" if $debug;
    unless ($url) {
        say $OUT "Error: Could not determine archive URL for the active folder.";
        $exitstatus++;
        return 0;
    }
    my $headers = HTTP::Headers->new;
    $headers->header( "Host" => $host );
    $headers->header( "Content-Type" => "application/zip" );
    my $req = HTTP::Request->new("PUT", $url, $headers, $content);
    $req->protocol('HTTP/1.1');
    say $OUT "REQUEST:" if $debug;
    say $OUT $req->as_string if $debug;
    
    # Pass request to the user agent and get a response back
    my $res = $ua->request($req);
    # Check the outcome of the response
    if ($res->is_success) {
        say $OUT "CAR file import request resulted in:  " . $res->status_line if $debug;
        say $OUT "Import successful" unless $silent;
        parseDirPath( ".", "suppress display" );
        return 1;
    } elsif ( $res->status_line =~ m/401/ ) {
        unless ( importCar($filename, $contentRef) ) {
            say $OUT "Error: Failed to import CAR file after responding to Digest request.  Authentication credentials may be invalid.  Trying logging in again.";
            $exitstatus++;
            return 0;
        }
        return 1;
    } else {
        say $OUT "Error: Failed to import CAR file.  The server reported: " . $res->status_line;
        print $OUT $res->as_string if $debug;
        $exitstatus++;
        return 0;
    }
}

# Perform an HTTP Digest authentication against the active server.
sub login {
    unless ( serverSet() ) { return 0; }
    $username = shift(@_);
    $password = shift(@_);
    my $realm;
    say "In login:" if $debug;
    unless ($username and $password) {
        say $OUT "The login command requires a username and a password.";
        commandhelp("login");
        $exitstatus++;
        return 0;
    }
    
    my $authority = $server->authority;
    $authority =~ s/^http.*:\/\/(.*?)\/.*$/$1/;
    if ( $authority !~ m/\:/ ) { $authority .= ":80"; }
    say $OUT "  Found authority: $authority" if $debug;
    
    # Get realm from server.
    my $realmurl;
    my $server_version = $server->version;
    unless ($server_version) {
        say $OUT "Error: Cannot find server version.";
        $exitstatus++;
        return 0;
    }
    $server_version =~ s/-.*$//; # Grab just the base version number.
    if ( $server_version =~ m/^\./ ) {
        # Older Callimachus
        $realmurl = $server->authority . "accounts?describe";
    } else {
        # For Callimachus 1.0 and above
        $realmurl = $server->authority . 'auth/digest+account?describe';
    }
    my $realmreq = new HTTP::Request GET => $realmurl;
    $realmreq->header( "Host" => $host );
    $realmreq->header( "Accept" => "application/rdf+xml" );
    # Pass request to the user agent and get a response back
    my $realmres = $ua->request($realmreq);
    # Check the outcome of the response
    if ($realmres->is_success) {
        my $xs = XML::Simple->new(ForceArray => 1, KeyAttr => []);
        my $xml = $xs->parse_string($realmres->content);
        print $OUT Dumper($xml) if $debug > 2;
        if ( $server_version =~ m/^\./ and $server_version ne "0.18" ) {
            # Older Callimachus
            $realm = $xml->{'rdf:Description'}[0]->{'calli:authName'}[0]->{'rdf:resource'};
        } else {
            # For Callimachus 0.18 and above:
            $realm = $xml->{'rdf:Description'}[0]->{'calli:authName'}[0];
        }
        say $OUT "  Found realm: $realm" if $debug;
        print $OUT $realmres->as_string if $debug > 1;
    } else {
        say $OUT "Error: Failed to get Digest realm from server.  Could not log in.  Status code " . $realmres->status_line . " returned from server.";
        $exitstatus++;
        return 0;
    }
    die ("Error: Failed to get Digest realm from server.  Could not log in.") unless ($realm);
    
    $ua->credentials($authority, $realm, $username, $password);
    $server->loggedIn(1);
    say $OUT "Credentials accepted." unless $silent;
    return 1;
}

sub logout {
    $ua = new LWP::UserAgent;
    $ua->agent("CallimachusShell/0.1 " . $ua->agent);
    $server->loggedIn(0);
    say $OUT "Logged out." unless $silent;
}

# List the contents of the active folder.
sub ls {
    unless ( serverSet() ) { return 0; }
    # Display the folder and file titles.
    my $serverfolders = $server->folders;
    foreach my $key (sort keys %$serverfolders) {
        $key .= '/' unless ($key =~ m/\/$/);
        say $OUT "$key";
    }
    my $serverfiles = $server->files;
    foreach my $key (sort keys %$serverfiles) {
        say $OUT "$key";
    }
}

# Make a new folder.
sub makeDir {
    unless ( serverSet() ) { return 0; }
    unless ( $server->loggedIn ) {
        say $OUT "Making a folder may only be performed by an authenticated user.  Please log in first.";
        commandhelp("login");
        return;
    }

    my $folderpath = shift(@_);
    $folderpath =~ s/\/$// if ($folderpath =~ m/\/$/); # Temporarily remove ending slash to allow use of parseFolderUrl.
    my $localpath;
    my($parenturl, $foldername) = parseFolderUrl($folderpath);
    my $folderslug = $foldername;
    $foldername .= '/'; # Folders must end with a slash, slugs must not.
    my $host = $server->authority;
    $host =~ s/http:\/\/(.*)\/$/$1/;
    
    # Determine whether the folder already exists.
    my $serverfolders = $server->folders;
    foreach my $key (sort keys %$serverfolders) {
        if ( $key eq $foldername ) {
            say $OUT "$foldername already exists.";
            #$exitstatus++;
            return 1;
        }
    }
    
    # Create the folder.
    $localpath = getPwd();
    my $content = <<"ENDOFMKDIRTTL";
BASE <$parenturl>
prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
prefix calli: <http://callimachusproject.org/rdf/2009/framework#>

INSERT DATA {
    <$localpath$foldername> a calli:Folder, </callimachus/Folder> ;
        rdfs:label "$folderslug" .
}
ENDOFMKDIRTTL

    # Create request headers.
    my $headers = HTTP::Headers->new;
    $headers->header( "Content-Type" => "application/sparql-update" );
    
    # Get the parent folder's 'describedby' URL.
    my $links = getFolderLinks($server->folder);
    unless ($links) {
        say $OUT "Error: Failed to get links from the active folder.";
        $exitstatus++;
        return 0;
    }
    my $parentcreateurl = $links->{'describedby'}->{url};
    
    # Create request.
    my $req = HTTP::Request->new("POST", $parentcreateurl, $headers, $content);
    $req->header( "Host" => $host );
    $req->protocol('HTTP/1.1');
    say $OUT "REQUEST:" if ($debug > 1);
    say $OUT $req->as_string if ($debug > 1);
    
    # Pass request to the user agent and get a response back
    my $res = $ua->request($req);
    # Check the outcome of the response
    if ($res->is_success) {
        say $OUT "Create folder request resulted in:  " . $res->status_line if $debug;
        say $OUT "Folder created." if $debug;
        parseDirPath( ".", "suppress display" );
        return 1;
    } elsif ( $res->status_line =~ m/401/ ) {
        unless ( makeDir($folderpath) ) {
            say $OUT "Error: Failed to create folder after responding to Digest request.  Authentication credentials may be invalid.  Trying logging in again.";
            $exitstatus++;
            return 0;
        }
        return 1;
    } else {
        say $OUT "Error: Failed to create folder.  The server reported: " . $res->status_line;
        print $OUT $res->as_string if $debug > 2;
        $exitstatus++;
        return 0;
    }
}

# Move a file from one location to another.
sub moveFile {
    my $oldpath = shift(@_);
    my $newpath = shift(@_);
    my $basefolder = getPwd();
    # TODONEXT: Refactor to allow both locations to be full URLs.
    #           Need to save login details per server to home dir file to support multiple server mv.
    #           $home = File::HomeDir->my_home;
    
    # Check inputs.
    unless ($oldpath and $newpath) {
        say $OUT "Error: The mv command requires two file names, paths or URLs as arguments.";
        commandhelp('mv');
        $exitstatus++;
        return 0;
    }
    
    # cd to the first file's server and folder.
    my ($oldfolder, $oldfile);
    if ( $oldpath =~ m/^(.*)\/((\w|\d)+\.(\w+))$/ ) {
        $oldfolder = $1;
        $oldfile = $2;
        if ( parseDirPath($oldfolder) ) {
            $oldfolder = getPwd(); # Account for relative path components ('.' and '..').
        } else {
            say $OUT "Error: Could not change to $oldfolder";
            parseDirPath($basefolder); # Return to the base folder.
            $exitstatus++;
            return 0;
        }
    } else {
        $oldfolder = getPwd();
        $oldfile = $oldpath;
    }
    
    # Get the file contents.
    my $content = retrieveFile("return", $oldfile);
    unless ( $content ) {
        say $OUT "       $oldfile has no content.";
        parseDirPath($basefolder); # Return to the base folder.
        $exitstatus++;
        return 0;
    }
    
    # cd to the target's server and folder.
    parseDirPath($basefolder); # Return to the original folder where this command was executed
                               # to account for relative paths.
    my ($newfolder, $newfile);
    if ( $newpath =~ m/^(.*)\/((\w|\d)+\.(\w+))$/ ) {
        $newfolder = $1;
        $newfile = $2;
        
        # DBG: Remove
        say $OUT "Attempting to change to folder '$newfolder' and write file '$newfile'." if $debug;
        
        unless ( parseDirPath($newfolder) ) {
            say $OUT "Error: Could not change to $newfolder";
            parseDirPath($basefolder); # Return to the base folder.
            $exitstatus++;
            return 0;
        }
    } else {
        # TODONEXT: Maybe.  $newfile might also need to be the old filename and the folder given.
        #                   Account for that case!
        $newfile = $newpath;
    }
    
    # Put the file to the new location.
    if ( putFile($newfile, $content) ) {
        say $OUT "Created $newpath" if $debug;
    } else {
        say $OUT "Error: Could not create $newpath.";
        parseDirPath($basefolder); # Return to the base folder.
        $exitstatus++;
        return 0;
    }
    
    # Change back to the old folder and delete the old file.
    if ($oldfolder) {
        parseDirPath($basefolder);
        unless ( parseDirPath($oldfolder) ) {
            say $OUT "Error: Could not change folder to $oldfolder";
            say $OUT "       $oldfile has been retained.";
            parseDirPath($basefolder); # Return to the base folder.
            $exitstatus++;
            return 0;
        }
    } else {
        parseDirPath($basefolder);
    }
    
    say $OUT "About to delete: *" . $oldfile . "*" if $debug;
    unless ( deleteFile($oldfile) ) {
        say $OUT "Error: Could not delete $oldfile";
        say $OUT "       $oldfile has been retained.";
        parseDirPath($basefolder); # Return to the base folder.
        $exitstatus++;
        return 0;
    }
    
    # Finally, return to the base folder.
    parseDirPath($basefolder);
    return 1;
}

# Attempt to change the active folder to the path provided.
sub parseDirPath {
    my $desiredFolderPath = shift(@_);
    my $suppressDisplay = shift(@_);
    say $OUT "In parseDirPath()\n   Attempting to cd to \"$desiredFolderPath\"." if $debug;
    
    if ( $desiredFolderPath =~ m/^(http:\/\/.*?)\/(.*)$/ ) {
        my $authority = $1 . '/';
        my $folder = '/' . $2;
        server($1);
        return 1 if ( parseDirPath($folder) );
        $exitstatus++;
        return 0;
    }
    unless ( serverSet() ) { return 0; }
    my $folderHistory = $server->folderHistory;
    
    # Record the current folder in case we need to roll back.
    my $currentFolder = getPwd();
    say $OUT "The current folder is $currentFolder" if $debug;
    
    # Handle leading '.' folderName.
    if ( $desiredFolderPath =~ m/^\.$/ ) {
        return 1 if ( parseDirPath($currentFolder) );
        $exitstatus++;
        return 0;
    } elsif ( $desiredFolderPath =~ m/^\.\// ) {
        # Remove the leading './' to make the path relative.
        $desiredFolderPath =~ s/^\.\///;
        say $OUT "Corrected desiredFolderPath for leading '.': $desiredFolderPath" if $debug;
    }
    
    # Handle leading '..' folderNames.
    # Make a new desiredFolderPath that is absolute, based on the folder history.
    if ( $desiredFolderPath =~ m/^\.\./ ) {
        my $topPath = "";
        while ( $desiredFolderPath =~ m/^\.\./ ) {
            my $historyLength = @$folderHistory; # scalar context yeilds length.
            pop(@$folderHistory) unless ($historyLength == 1);
            $desiredFolderPath =~ s/^\.\.//;
            if ( $desiredFolderPath =~ m/^\// ) { $desiredFolderPath =~ s/^\///; }
        }
        # Combine the truncated folderHistory with the desiredFolderPath.
        foreach my $element (@$folderHistory) {
            $topPath .= $element;
        }
        $desiredFolderPath = $topPath . $desiredFolderPath;
        say $OUT "Corrected desiredFolderPath for '..': $desiredFolderPath" if $debug;
    }
    
    if ( $desiredFolderPath =~ m/^\// ) {   # The path is absolute
        $desiredFolderPath =~ s/^\///;
        chHomeDir();
    }
    my @pathElements = split("/", $desiredFolderPath);
    
    foreach my $folder (@pathElements) {
        say "Trying to change to $folder" if $debug > 1;
        unless ( chDir($folder) ) {  # Return to the current folder for any error in the path.
            return 1 if ( parseDirPath($currentFolder) );
            $exitstatus++;
            return 0;
        }
    }
    pwd() if $debug;
    ls() if $autols;
    return 1;
}


# Parse a folder or file URL and return the full parent URL and leaf node name.
sub parseFolderUrl {
    my $folderpath = shift(@_);
    my $parenturl;
    my $leaf;
    if ( $folderpath =~ m/^(http:\/\/.*)\/(.*)$/ ) {
        # A server and path were provided.
        my $path = $1;
        $leaf = $2;
        parseDirPath($path);
        $parenturl = $path;
        # TODONEXT: Login to this server.
    
    } elsif ( $folderpath =~ m/^(.*)\/(.*)$/ ) {
        # A path was provided.
        my $path = $1;
        $leaf = $2;
        parseDirPath($path);
        $parenturl = $server->authority . $path;
        $parenturl =~ s/\/\//\//g;
        $parenturl =~ s/(http:\/)(\w)/http:\/\/$2/;
    } else {
        # Only a leaf name was provided.
        $leaf = $folderpath;
        $parenturl = $server->authority . getPwd();
        $parenturl =~ s/\/\//\//g;
        $parenturl =~ s/(http:\/)(\w)/http:\/\/$2/;
    }
    return ($parenturl, $leaf);
}


# Put a file on the server in the active folder.
sub putFile {
    unless ( serverSet() ) { return; }
    my $filename = shift(@_);
    my $contentRef = shift(@_);
    my $content;
    my $slug = $filename;
    $slug =~ s/^.*\/(.*)$/$1/; # remove any path components
    
    say $OUT "In putFile:" if $debug;
    unless ( $server->loggedIn ) {
        say $OUT "Files may only be added by an authenticated user.  Please log in first.";
        commandhelp("login");
        return;
    }
    
    if ( $contentRef ) {
        $content = $$contentRef;
    } else {
        # Read file from the local file system.
        unless ( open (FILEIN, "<", $filename) ) {
            say $OUT "Error:  Cannot read file from filesystem: $!";
            $exitstatus++;
            return 0;
        }
        while (<FILEIN>) {
            $content .= $_;
        }
        close (FILEIN) or
            warn "Warning: Could not close the filehandle: $!";
    }
    
    my $url = $server->folder;
    say $OUT "Attempting to put $filename into URL \"$url\"" if $debug;
    
    unless ($url) {
        say $OUT "Fatal Error: Could not determine the current folder.  Exiting.";
        exit(1);
    }
    
    # Place file on server.
    my $headers = HTTP::Headers->new;
    $headers->header( "Host" => $host );
    $headers->header( "Slug" => $slug );
    $headers->header( "Content-Type" => getContentType($filename) );
    my $req;
    my $serverfile = $server->files->{$slug}->{src};
    if ($serverfile) {
        $url = $server->files->{$slug}->{src};
        say $OUT "This file already exists.  Using PUT to $url." if $debug>1;
        $req = HTTP::Request->new("PUT", $url, $headers, $content);
    } else {
        say $OUT "This is a new file.  Using POST." if $debug>1;
        $req = HTTP::Request->new("POST", $url, $headers, $content);
    }
    if ($req) {
        $req->protocol('HTTP/1.1');
        say $OUT "REQUEST:" if ($debug > 1);
        say $OUT $req->as_string if ($debug > 1);
    
        # Pass request to the user agent and get a response back
        my $res = $ua->request($req);
        # Check the outcome of the response
        if ($res->is_success) {
            say $OUT "File request resulted in:  " . $res->status_line if $debug;
            say $OUT "File uploaded.";
            parseDirPath( ".", "suppress display" );
            return 1;
        } elsif ( $res->status_line =~ m/401/ ) {
            unless ( putFile($filename, $contentRef) ) {
                say $OUT "Error: Failed to upload file after responding to Digest request.  Authentication credentials may be invalid.  Trying logging in again.";
                $exitstatus++;
                return 0;
            }
            return 1;
        } else {
            say $OUT "Error: Failed to upload file.  The server reported: " . $res->status_line;
            print $OUT $res->as_string if $debug > 2;
            $exitstatus++;
            return 0;
        }
    } else {
        say $OUT "Error: Failed to upload file.  HTTP request could not be created.  This is likely a bug in Cash.  Sorry!";
        $exitstatus++;
        return 0;
    }
}

# Report the active folder.
sub pwd {
    unless ( serverSet() ) { return 0; }
    say $OUT "Folder: " . getPwd() unless $silent;
    return 1;
}

# Report the state of the shell's server object.
sub reportState {
    unless ( serverSet() ) { say $OUT "Server object not set."; }
    say $OUT "Server object state:\n";
    say $OUT "  Authority:  " . $server->authority;
    say $OUT "  Server version:  " . $server->version;
    say $OUT "  Logged in: " . $server->loggedIn;
    say $OUT "  Links for this server:";
    my $serverlinks = $server->links;
    say Dumper(%$serverlinks);
    say $OUT "  Home folder for this server: " . $server->homeFolder;
    say $OUT "  Active folder: " . $server->folder;
    say $OUT "  Active folder path: " . &getPwd();
    say $OUT "  Stored subfolders for this folder:";
    my $serverfolders = $server->folders;
    say Dumper(%$serverfolders);
    say $OUT "  Stored files for this folder:";
    my $serverfiles = $server->files;
    say Dumper(%$serverfiles);
}

sub reportVersion {
    say $OUT "$version";
}

# Download a file from the active folder.  Dispose of it via saving to a file or cat to STDOUT.
sub retrieveFile {
    unless ( serverSet() ) { return; }
    my $disposition = shift(@_);
    my $title = shift(@_);
    unless ($disposition eq "cat" or "get" or "return") {
        say "Error: retrieveFile() called with inappropriate parameter '$disposition'.";
        $exitstatus++;
        return 0;
    }
    
    my $url;
    if ( $server->files->{$title} ) {
        $url = $server->files->{$title}->{src};
    }
    unless ($url) {
        say $OUT "Error: Could not determine URL for file '$title'.";
        $exitstatus++;
        return 0;
    }
    say $OUT "Attempting to resolve URL: $url" if $debug;

    my $req = new HTTP::Request GET => $url;
    $req->header( "Host" => $host );
    say $OUT "REQUEST:" if $debug;
    say $OUT $req->as_string if $debug;
    
    # Pass request to the user agent and get a response back
    my $res = $ua->request($req);
    # Check the outcome of the response
    if ($res->is_success) {
        say $OUT "File request resulted in:  " . $res->status_line if $debug;
        my $content = $res->content;
        my $size = length $content;
        my $filename = $res->filename;
        say $OUT "The file is $size bytes in length." if $debug;
        say $OUT "The filename is " . $filename if $debug;
        $filename = $title unless $filename;
        
        if ($disposition eq "get") {
            return 1 if ( saveFile(\$content, $filename) );
        } elsif ($disposition eq "cat") {
            return 1 if ( catFile(\$content) );
        } elsif ($disposition eq "return") {
            return \$content if \$content;
        }
        $exitstatus++;
        return 0; # Error disposing of results.
        
    } else {
        say $OUT "Error: Failed to download file.  The server reported: " . $res->status_line;
        print $OUT $res->as_string if $debug;
        $exitstatus++;
        return 0;
    }
}

# Save the contents of a file to the local filesystem.
sub saveFile {
    my $content = shift(@_);
    my $filename = shift(@_);
    
    unless ($content) {
        say $OUT "Error: No content found!";
        $exitstatus++;
        return 0;
    }
    unless ($filename) {
        say $OUT "Error: saveFile() called without a filename parameter";
        $exitstatus++;
        return 0;
    }
    
    # Save the content to a file.
    unless ( open (FILEOUT, ">", $filename) ) {
        say $OUT "Error:  Cannot write file to filesystem.  Perhaps check permissions with 'exec' and try again.";
        $exitstatus++;
        return 0;
    }
    print FILEOUT $$content;
    close (FILEOUT) or
        warn "Warning: Could not close the filehandle: $!";
    
    say $OUT "Saved file as '$filename'." unless $silent;
    return 1;
}

# Set the Callimachus server and get its metadata.
sub server {
    my @args = split(/\s/, shift(@_));
    my $args_length = scalar(@args);
    
    my $proxy = "";
    $ua->no_proxy(); # Unset the user agent's proxy.
    
    $authority = "uninitialized";
    if ( $args_length == 3 and $args[0] eq '-p' ) {
        $proxy = $args[1];
        $authority = $args[2];
    } elsif ( $args_length == 1 ) {
        $authority = $args[0] if $args[0];
    } else {
        say $OUT "ERROR: No server authority provided.";
        $exitstatus++;
        commandhelp('server');
        return;
    }
    $authority .= "/" unless ($authority =~ m/\/$/);
    $host = $authority;
    $host =~ s/^http:\/\/(.*)[:\/].*$/$1/;
    
    if ($debug) {
        say $OUT "Proxy: $proxy";
        say $OUT "Authority: $authority";
        say $OUT "Host: $host";
    }
    
    if ( $authority ne "unitialized" and checkAuthority($authority) ) {
        $server->authority($authority);
        $server->proxy($proxy); # Store the HTTP proxy in the server object.
        $ua->proxy('http', $proxy); # Set the user agent's proxy.
        
        say $OUT "Proxy set in user agent: " . $ua->proxy('http') if $debug;
        
        my $links = getFolderLinks($server->authority); # A hash reference.
        if ( $links ) {
            say $OUT "Server set to $authority" unless $silent;
            # Set the server's folder params to the top level using rel=contents URL.
            $server->links($links); # Store the links in the server object.
            $server->folder($server->links->{contents}->{url});
            $server->homeFolder($server->links->{contents}->{url});
            $server->loggedIn(0);
            
            if ($debug) {
                say $OUT "Links for this server:";
                my $serverlinks = $server->links;
                say Dumper(%$serverlinks);
            }
            
            chHomeDir();
        }
    } elsif ( $server->folder and $server->authority ) {
        # Report the current server authority.
        say $OUT $server->authority;
    } else {
        say $OUT "ERROR: No server authority provided.";
        $exitstatus++;
        commandhelp('server');
    }
}

# Set global options for the shell environment.
sub setOptions {
    my $option = shift(@_);
    my $value = shift(@_);
    unless ($option ) {
        say $OUT "The set command must be given either an option name to unset or an option name and a value.";
        &commandhelp("set");
        return;
    }
    
    given ($option) {        
        when (/^debug$/) { $debug = $value; }
        when (/^autols$/) { $autols = $value; }
        default { say $OUT "Option $option not supported."; &commandhelp("set") unless $@; }
    }
}

# Report usage instructions.
sub usage {
    say $OUT "Please see 'perldoc $0' for usage information.";
    exit(0);
}

#################
# Server Object #
#################
package Server;
sub new {
    my $self  = {};
    $self->{AUTHORITY}   = undef;
    bless($self);
    return $self;
}
# Gets/sets the HTTP authority for a Callimachus server (e.g. 'http://example.com:8080/')
sub authority {
    my $self = shift;
    if (@_) { $self->{AUTHORITY} = shift }
    return $self->{AUTHORITY};
}
# Gets/sets the list of files in the active folder.
sub files {
    my $self = shift;
    if (@_) { $self->{FILES} = shift }
    return $self->{FILES};
}
# Gets/sets the active folder's URL.
sub folder {
    my $self = shift;
    if (@_) { $self->{FOLDER} = shift }
    return $self->{FOLDER};
}
# Gets/sets an array containing the names of the subfolders in the active folder.
sub folders {
    my $self = shift;
    if (@_) { $self->{FOLDERS} = shift }
    return $self->{FOLDERS};
}
# Gets/sets an array of folder names above and including the active folder.
sub folderHistory {
    my $self = shift;
    if (@_) { $self->{FOLDERHISTORY} = shift }
    return $self->{FOLDERHISTORY};
}
# Gets/sets the name of the active server's home folder.
sub homeFolder {
    my $self = shift;
    if (@_) { $self->{HOMEFOLDER} = shift }
    return $self->{HOMEFOLDER};
}
# Gets/sets the value of the active server's HTTP proxy.
sub proxy {
    my $self = shift;
    if (@_) { $self->{PROXY} = shift }
    return $self->{PROXY};
}
# Gets/sets a hash of links for the active server (from HTTP OPTIONS).
sub links {
    my $self = shift;
    if (@_) { $self->{LINKS} = shift }
    return $self->{LINKS};
}
# Gets/sets an indication of logged in state.
sub loggedIn {
    my $self = shift;
    if (@_) { $self->{LOGGEDIN} = shift }
    return $self->{LOGGEDIN};
}    # Gets/sets the active folder's URL.
sub version {
    my $self = shift;
    if (@_) { $self->{VERSION} = shift }
    return $self->{VERSION};
}
1;

__END__

=head1 cash.pl

A shell for the Callimachus Project (http://callimachusproject.org)
that implements a client for the Callimachus REST API.

The Callimachus shell may be used in either interactive or non-
interactive modes.  When used in interactive mode, a prompt is given
which accepts built-in commands.  The built-in command "help" may
be used to obtain usage information on built-in commands.

When used in non-interactive mode, the shell will accept a file on
STDIN for processing.  The file is expected to contain Callimachus
shell commands.

Usage: cash.pl -i
       cash.pl [-s] -e '<command>; <command>; ...'
       cash.pl -u
       cash.pl -v
       cash.pl < script-file

-e | --execute      Execute the following commands.  Commands are
                    separated with semi-colons.
                    
-i | --interactive  Provide an interactive shell environment with
                    commands specific to Callimachus operations.
                    
-s | --silent       Only provide output where necessary (e.g. errors and
                    cat command).

-u | --usage        Show usage directions and exit.

-v | --version      Report the version number of this shell.
