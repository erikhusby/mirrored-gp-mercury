#!/usr/bin/perl

# Parses a Mercury ETL sqlplus error log, which contains sql merge errors that id the file and line number
# of the failing record.  This script opens the errored file and outputs the errored records.
#
# Takes as an argument the sqlplus error log filename.
# Expects to be passed the sqlplus log file, and to find the .dat files in ./done/

use strict;
use warnings;
use Tie::File;

die "\nUsage: dw_etl_parse_merge_errors.pl sqlplusLogFilename doneDirectory/\n" if ($#ARGV + 1 != 2);
my $mergeErrorFilename = $ARGV[0];
my $doneDir = $ARGV[1];

my @lines;
my %errorStrings;
my %fileMap;
my $parsableError = 0;
my $fileHasErrors = 0;

tie @lines, 'Tie::File', $mergeErrorFilename or die 'Cannot find file: ' . $mergeErrorFilename;

# Only processes the errors that contain a .dat filename, an integer line number, and/or an ORA error.
foreach my $line (@lines) {
    if ( $line =~ /^[0-9]+/ ) {
        # Handled error lines begin with timestamp and may be mixed within rowcount logging statistics
        $fileHasErrors = 1;
        my @tokens = split(/\s+/, $line, 4);
        if ( defined $tokens[3] && ($tokens[0] =~ /.*\.dat/) &&
                 ($tokens[1] eq "line") && 
                 ($tokens[2] =~ /^[+-]?\d+$/ )) {
            my $errMsg = ($tokens[3]);
            $errorStrings{$errMsg} = '';
            $parsableError = 1;
        }
    } else {
        if ( $line =~ /ORA-/ ) {
            # Unhandled error lines contain ORA-
            $fileHasErrors = 1;
            $errorStrings{$line} = '';
        }
    }
}

if ($fileHasErrors == 1 && $parsableError == 1) {
    foreach my $errString (keys(%errorStrings)) {
        print "\n----- Sql merge error: " . $errString . "\n";

        # For every error that matches the current iteration, makes a map of filename to space delimited list of line numbers.
        %fileMap = ();
        foreach my $line (@lines) {
            my @tokens = split(/\s+/, $line, 4);
            if ( defined $tokens[3] && ($tokens[0] =~ /.*\.dat/) &&
                    ($tokens[1] eq "line") && 
                    ($tokens[2] =~ /^[+-]?\d+$/ )) {
                my $filename = $tokens[0];
                my $lineno = $tokens[2];
                my $errMsg = ($tokens[3]);
                if ($errString eq $errMsg) {
                    $fileMap{$filename} .= ' ' . $lineno;
                }
            }
        }

        # Opens the dat file and outputs the errored lines.
        foreach my $filename (keys %fileMap) {
            my $datFile = $doneDir . $filename;
            die 'Cannot find file: ' . $datFile if (! -f $datFile);
            my @datLines;
            tie @datLines, 'Tie::File', $datFile or die 'Cannot read file: ' . $datFile;
            my @linenos = split(/\s+/, $fileMap{$filename});
            print "\nIn sqlLoader file: " . $datFile . "\n";
            foreach my $lineno (@linenos) {
                if (defined $lineno && $lineno =~ /^[+-]?\d+$/ ) {  # Tests if lineno is an integer.
                    if (defined $datLines[$lineno - 1]) {
                        print $datLines[$lineno - 1] . "\n";
                    } else {
                        print "Cannot find line " . $lineno . " in file " . $datFile . "\n";
                    }
                 }
            }
            untie @datLines;
        }
    }
}

untie @lines;
if ( ( $fileHasErrors == 1 ) && ( $parsableError == 0 ) ) {
    foreach my $errString (keys(%errorStrings)) {
        print "\n----- Unexpected sql error: " . $errString . "\n";
    }
}
print "\n";
