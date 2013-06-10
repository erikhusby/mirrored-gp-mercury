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


tie @lines, 'Tie::File', $mergeErrorFilename or die 'Cannot find file: ' . $mergeErrorFilename;

foreach my $line (@lines) {
    my @tokens = split(/\s+/, $line, 4);
    # Only keeps the errors that contain a filename and line number.
    if ( defined $tokens[3] && $tokens[1] eq "line") {
	my $errMsg = ($tokens[3]);
	$errorStrings{$errMsg} = '';
    }
}

foreach my $errString (keys(%errorStrings)) {
    print "\n----- Sql merge error: " . $errString . "\n";

    # For every error that matches the current iteration, makes a map of filename to space delimited list of line numbers.
    %fileMap = ();
    foreach my $line (@lines) {
	my @tokens = split(/\s+/, $line, 4);
	my $filename = $tokens[0];
	my $lineno = $tokens[2];
	my $errMsg = ($tokens[3]);
	if ($errString eq $errMsg) {
	    $fileMap{$filename} .= ' ' . $lineno;
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
untie @lines;
print "\n";
