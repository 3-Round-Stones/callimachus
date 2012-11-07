#!/usr/bin/perl
# encoding: utf-8

use strict;
#####################################################################
# callilog.pl
#
# Parse a Callimachus log file and create RDF from it.  Used for
# common usage reporting on a Callimachus server.  Takes a date
# as input in YYYY-MM-DD format.
#
# Created by David Wood on 2012-10-23.
# Copyright (c) 2012 3 Round Stones Inc. All rights reserved.
#####################################################################

require 5.10.0;
use feature qw(switch say);
use constant false => 0;
use constant true  => 1;
use constant TIMEOUT => 2;

###########################
# URL Patterns to Exclude #
# CHANGE THIS as needed   #
###########################
my $exclude_pattern = '(^\/callimachus|^\/change|^\/themes)';

########
# TODO #
########
# Perform location lookups on top 10 IPs
# Use 'say $OUT' throughout.

# Debug level
my $debug = 0;

# Globals
my $version = "0.1";
my $date = shift;
my $baseURL = 'http://callimachusproject.org/rdf/2012/log/';
my $processing = false;
my $count = 0;
my %ip_addresses;
my %top_ten_ip_addresses;
my %urls;
my %top_ten_urls;
my %not_found_urls = ();
my %top_ten_not_found_urls = ();
my %server_errors = ();
my %top_ten_server_errors = ();
my %users = ();
my %top_ten_users = ();
my @GETs = (0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0);
my @PUTs = (0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0);
my @POSTs = (0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0);
my @DELETEs = (0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0);
my $num_200 = 0;
my $num_201 = 0;
my $num_204 = 0;
my $num_206 = 0;
my $num_302 = 0;
my $num_303 = 0;
my $num_304 = 0;
my $num_400 = 0;
my $num_401 = 0;
my $num_403 = 0;
my $num_404 = 0;
my $num_405 = 0;
my $num_406 = 0;
my $num_409 = 0;
my $num_500 = 0;
my $num_504 = 0;
my $OUT = *STDOUT;
# Used for reverse DNS lookups
$SIG{ALRM} = sub { say STDERR "Warning: Timeout during reverse DNS lookup." };
my %CACHE;

unless ($date) {
    exec ("perldoc $0") or usage();
}

while ( <> ) {
    my $line = $_;
    chomp($line);
    $count++; 
    if ($line =~ m/^#\s*Date:\s+$date/) {
        $processing = true;
    } elsif ($processing and $line =~ m/^#\s*Date:/) {
        $processing = false;
    }
    
    say "Line $count: $line (Processing: $processing.)" if ($debug > 1);
    
    if ($processing) {
        my (@components) = split(/\s+/, $line);
        next unless ( scalar(@components) == "7" );
        my ($time, $ip_address, $user, $request, $url, $http_version, $return_code) = @components;
        
        next unless ($ip_address =~ m/\d+\.\d+\.\d+\.\d+/);
        $url =~ s/^http:\/\/.*?\//\//;
        next if ( $url =~ /$exclude_pattern/ );
        
        $request =~ s/"//;
        $http_version =~ s/"//;
        my ($hour, $minute) = split(':', $time);
        
        # Keep track of the number of times we have seen this IP address.
        if ($ip_addresses{$ip_address}) {
            $ip_addresses{$ip_address}++;
        } else {
            $ip_addresses{$ip_address} = 1;
        }

        # Keep track of the number of times we have seen this URL.
        if ($urls{$url}) {
            $urls{$url}++;
        } else {
            $urls{$url} = 1;
        }
        
        $GETs[$hour]++ if $request eq "GET";
        $PUTs[$hour]++ if $request eq "PUT";
        $POSTs[$hour]++ if $request eq "POST";
        $DELETEs[$hour]++ if $request eq "DELETE";
        
        given ($return_code) {
            when (/^200$/) { $num_200++; }
            when (/^201$/) { $num_201++; }
            when (/^204$/) { $num_204++; }
            when (/^206$/) { $num_206++; }
            when (/^302$/) { $num_302++; }
            when (/^303$/) { $num_303++; }
            when (/^304$/) { $num_304++; }
            when (/^400$/) { $num_400++; }
            when (/^401$/) { $num_401++; }
            when (/^403$/) { $num_403++; }
            when (/^404$/) { $num_404++; }
            when (/^405$/) { $num_405++; }
            when (/^406$/) { $num_406++; }
            when (/^409$/) { $num_409++; }
            when (/^500$/) { $num_500++; }
            when (/^504$/) { $num_504++; }
        }
        
        # Keep track of the number of times we have seen "not found" errors for this URL.
        if ( $return_code == "404" and $not_found_urls{$url}) {
            $not_found_urls{$url}++;
        } elsif ( $return_code == "404" ) {
            $not_found_urls{$url} = 1;
        }

        # Keep track of the number of times we have seen server errors for this URL.
        if ( $return_code == "500" and $server_errors{$url}) {
            $server_errors{$url}++;
        } elsif ( $return_code == "500" ) {
            $server_errors{$url} = 1;
        }
        
        # Keep track of the number of times we have seen this user.
        if ( $user ne "-" and $users{$user}) {
            $users{$user}++;
        } elsif ( $user ne "-" ) {
            $users{$user} = 1;
        }
    }
}

# Collect usage statistics
my $unique_ip_addresses = scalar(keys(%ip_addresses));
my $unique_urls = scalar(keys(%urls));
my $ip_count = 0;
foreach my $key (sort{$ip_addresses{$b} <=> $ip_addresses{$a}} keys %ip_addresses) {
    $top_ten_ip_addresses{$key} = $ip_addresses{$key} if ($ip_count < 10);
    $ip_count++;
}
my $url_count = 0;
foreach my $key (sort{$urls{$b} <=> $urls{$a}} keys %urls) {
    $top_ten_urls{$key} = $urls{$key} if ($url_count < 10);
    $url_count++;
}
my $error_count = 0;
foreach my $key (sort{$server_errors{$b} <=> $server_errors{$a}} keys %server_errors) {
    $top_ten_server_errors{$key} = $server_errors{$key} if ($error_count < 10);
    $error_count++;
}
my $not_found_count = 0;
foreach my $key (sort{$not_found_urls{$b} <=> $not_found_urls{$a}} keys %not_found_urls) {
    $top_ten_not_found_urls{$key} = $not_found_urls{$key} if ($not_found_count < 10);
    $not_found_count++;
}
my $user_count = 0;
foreach my $key (sort{$users{$b} <=> $users{$a}} keys %users) {
    $top_ten_users{$key} = $users{$key} if ($user_count < 10);
    $user_count++;
}

# Debug
if ($debug > 2) {
    say "Unique IPs sorted by the number of requests:";
    my $ip_count = 0;
    foreach my $key (sort{$ip_addresses{$b} <=> $ip_addresses{$a}} keys %ip_addresses) {
        say "  " . $ip_addresses{$key} . ": $key";
    }
    
    say "Unique URLs sorted by the number of requests:";
    my $url_count = 0;
    foreach my $key (sort{$urls{$b} <=> $urls{$a}} keys %urls) {
        say "  " . $urls{$key} . ": $key";
    }
}

# Make Turtle
print $OUT <<"ENDOFPREFIXES";
\@base <$baseURL> .
\@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
\@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
\@prefix calli: <http://callimachusproject.org/rdf/2009/framework#> .
\@prefix callilog: <http://callimachusproject.org/rdf/2012/log#> .
\@prefix owl: <http://www.w3.org/2002/07/owl#> .

<http://callimachusproject.org/log/Log> a owl:Class .

<$date> a <http://callimachusproject.org/log/Log>
  ; callilog:uniqueIPs "$unique_ip_addresses"
  ; callilog:uniqueURLs "$unique_urls"
ENDOFPREFIXES

say $OUT "  ; callilog:total200 $num_200" if $num_200;
say $OUT "  ; callilog:total201 $num_201" if $num_201;
say $OUT "  ; callilog:total204 $num_204" if $num_204;
say $OUT "  ; callilog:total206 $num_206" if $num_206;
say $OUT "  ; callilog:total302 $num_302" if $num_302;
say $OUT "  ; callilog:total303 $num_303" if $num_303;
say $OUT "  ; callilog:total304 $num_304" if $num_304;
say $OUT "  ; callilog:total400 $num_400" if $num_400;
say $OUT "  ; callilog:total401 $num_401" if $num_401;
say $OUT "  ; callilog:total403 $num_403" if $num_403;
say $OUT "  ; callilog:total404 $num_404" if $num_404;
say $OUT "  ; callilog:total405 $num_405" if $num_405;
say $OUT "  ; callilog:total406 $num_406" if $num_406;
say $OUT "  ; callilog:total409 $num_409" if $num_409;
say $OUT "  ; callilog:total500 $num_500" if $num_500;
say $OUT "  ; callilog:total504 $num_504" if $num_504;

say $OUT ".\n"; # Close <$date> block

# GET requests per hour
my $num_gets = scalar(@GETs);
my $hour = 0;
if ($num_gets) {
    for my $num (@GETs) {
        if ($num) {
            say "<$date> callilog:hour <$date/hour$hour> .";
            say "<$date/hour$hour> rdfs:label \"Hour $hour\"";
            say "  ; callilog:gets \"$num\" .\n";
        }
        $hour++;
    }
}

# PUT requests per hour
my $num_puts = scalar(@PUTs);
my $hour = 0;
if ($num_puts) {
    for my $num (@PUTs) {
        if ($num) {
            say "<$date> callilog:hour <$date/hour$hour> .";
            say "<$date/hour$hour> rdfs:label \"Hour $hour\"";
            say "  ; callilog:puts \"$num\" .\n";
        }
        $hour++;
    }
}

# POST requests per hour
my $num_posts = scalar(@POSTs);
my $hour = 0;
if ($num_posts) {
    for my $num (@POSTs) {
        if ($num){
            say "<$date> callilog:hour <$date/hour$hour> .";
            say "<$date/hour$hour> rdfs:label \"Hour $hour\"";
            say "  ; callilog:posts \"$num\" .\n";
        }
        $hour++;
    }
}

# DELETE requests per hour
my $num_deletes = scalar(@DELETEs);
my $hour = 0;
if ($num_deletes) {
    for my $num (@DELETEs) {
        if ($num){
            say "<$date> callilog:hour <$date/hour$hour> .";
            say "<$date/hour$hour> rdfs:label \"Hour $hour\"";
            say "  ; callilog:deletes \"$num\" .\n";
        }
        $hour++;
    }
}

# Top Ten URLs returning a 404 error
if ( scalar(keys %top_ten_not_found_urls) ) {
    say "<$date> callilog:topTenNotFound <$date/topTenNotFound> .";
    foreach my $url (keys %top_ten_not_found_urls) {
        say "<$date/topTenNotFound> callilog:notFoundDescription <$date/topTenNotFound$url> .";
        say "<$date/topTenNotFound$url> callilog:url \"$url\"";
        say "  ; callilog:hits \"$top_ten_not_found_urls{$url}\" .\n";
    }
    say "";
}

# Top Ten URLs returning a 500 error
if ( scalar(keys %top_ten_server_errors) ) {
    say "<$date> callilog:topTenServerErrors <$date/topTenServerErrors> .";
    foreach my $url (keys %top_ten_server_errors) {
        say "<$date/topTenServerErrors> callilog:serverErrorDescription <$date/topTenServerErrors$url> .";
        say "<$date/topTenServerErrors$url> callilog:url \"$url\"";
        say "  ; callilog:hits \"$top_ten_server_errors{$url}\" .\n";
    }
    say "";
}

# Top 10 IP addresses
if ( scalar(%top_ten_ip_addresses) ) {
    say "<$date> callilog:topTenIPs <$date/topTenIPs> .";
    foreach my $ip (keys %top_ten_ip_addresses) {
        say "<$date/topTenIPs> callilog:iPDescription <$date/topTenIPs/$ip> .";
        say "<$date/topTenIPs/$ip> callilog:iPAddress \"$ip\"";
        say "  ; callilog:hits \"$top_ten_ip_addresses{$ip}\"";

        my $name = lookupHostname($ip);
        unless ($name =~ /\d+\.\d+\.\d+\.\d+/) {
            say $OUT "  ; callilog:hostName \"$name\"";
        }
        say ".\n";
        
    }
}

# Top 10 URLs
if ( scalar(%top_ten_urls) ) {
    say "<$date> callilog:topTenURLs <$date/topTenURLs> .";
    foreach my $url (keys %top_ten_urls) {
        say "<$date/topTenURLs> callilog:urlDescription <$date/topTenURLs$url> .";
        say "<$date/topTenURLs$url> callilog:url \"$url\"";
        say "  ; callilog:hits \"$top_ten_urls{$url}\" .\n";
    }
}

# Top 10 Users
if ( scalar(%top_ten_users) ) {
    say "<$date> callilog:topTenUsers <$date/topTenUsers> .";
    foreach my $url (keys %top_ten_users) {
        say "<$date/topTenUsers> callilog:userDescription <$date/topTenUsers$url> .";
        say "<$date/topTenUsers$url> callilog:url \"$url\"";
        say "  ; callilog:actions \"$top_ten_users{$url}\" .\n";
    }
}


############
# End main #
############

# Determine if a value is in an array.
sub inArray {
    my $element = shift(@_);
    my @array_to_check = @_;
    return 1 if (grep {$_ eq $element} @array_to_check);
    return 0;
}

sub lookupHostname {
    my $ip = shift;
    return $ip unless ($ip =~ /\d+\.\d+\.\d+\.\d+/);
    unless (exists $CACHE{$ip}) {
        my @h = eval <<'END';
        alarm(TIMEOUT);
        my @i = gethostbyaddr(pack('C4',split('\.',$ip)),2);
        alarm(0);
        @i;
END
    $CACHE{$ip} = $h[0] || undef;
    }
    return $CACHE{$ip} || $ip;
}

# Report usage instructions.
sub usage {
    say $OUT "Please see 'perldoc $0' for usage information.";
    exit(0);
}

1;

__END__

=head1 callilog.pl

Parses log files from the Callimachus Project (http://callimachusproject.org)
and outputs RDF triples representing common usage statistics.

Usage: callilog.pl [date] < /path/to/logfile > rdf-output.ttl

[date]  A date in YYYY-MM-DD format (e.g from `date -v-1d "+%Y-%m-%d"` or its equivalent)
