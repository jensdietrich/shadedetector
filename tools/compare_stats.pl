#!/usr/bin/perl

use strict;
use warnings;

my @order;
my %t;
my @wins;

my @fNames = @ARGV;
my $iFile = 0;
foreach my $fName (@ARGV) {
	print STDERR "Reading $fName...\n";
	open(my $f, "<", $fName) or die $!;
	while (<$f>) {
		chomp;
		my ($k, $v) = split /=/;
		if (!exists $t{$k}) {
			push @order, $k;
			$t{$k} = [];
		}

		push @{$t{$k}}, [ $v, $iFile ];
	}

	push @wins, [ 0, $iFile ];
	++$iFile;
}

my $nDifferent = 0;
foreach (@order) {
	my @ordered = sort { $b->[0] <=> $a->[0] || $a->[1] <=> $b->[1] } @{$t{$_}};
	#print "$_=$ordered[0]->[0]\tis best from $fNames[$ordered[0]->[1]], second is $ordered[1]->[0] from $fNames[$ordered[1]->[1]]\n" if $ordered[0]->[0] != $ordered[1]->[0];
	if ($ordered[0]->[0] != $ordered[1]->[0]) {
		print "$_=$ordered[0]->[0]\tis best from $fNames[$ordered[0]->[1]], second is $ordered[1]->[0] from $fNames[$ordered[1]->[1]]\n";
		++$nDifferent;
		++$wins[$ordered[0]->[1]][0];
	}
}

print STDERR "All counts equal!\n" if !$nDifferent;

@wins = sort { $b->[0] <=> $a->[0] || $a->[1] <=> $b->[1] } @wins;
foreach (@wins) {
	if ($_->[0] > 0) {
		print STDERR "$_->[0] outright wins for $fNames[$_->[1]]\n";
	}
}
