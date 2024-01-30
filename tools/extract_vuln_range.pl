#!/usr/bin/perl

use strict;
use warnings;

# Call with two refs to arrays of numbers.
sub compareNatural($$);		# Forward declaration
sub compareNatural($$) {
	#my ($x, $y) = @_;
	my ($x, $y) = ([ @{$_[0]} ], [ @{$_[1]} ]);		# Need to copy to prevent shift messing up the originals!
	if (!@$x) {
		return !@$y ? 0 : -1;
	}

	return 1 if !@$y;

	die "Non-numeric component '$x->[0]' seen!" if $x->[0] !~ /\A\d+\z/;
	die "Non-numeric component '$y->[0]' seen!" if $y->[0] !~ /\A\d+\z/;

	if ($x->[0] == $y->[0]) {
		shift @$x;
		shift @$y;
		return compareNatural($x, $y);
	}

	return $x->[0] <=> $y->[0];
}

sub extractSimpleVersionArray($) {
	my ($prefix) = $_[0] =~ /^([0-9.]+)/ or die "Could not extract a simple version prefix from '$_[0]'!";
	return map { $_ + 0 } split /\./, $prefix;
}

sub getMavenMetadataXmlUrlFor($$) {
	my ($g, $a) = @_;
	$g =~ tr|.|/|;
	return "https://repo1.maven.org/maven2/$g/$a/maven-metadata.xml";
}


# Heuristically check that maven-metadata.xml versions "look right", i.e., look like they're in the correct order, and die if not
sub checkMavenMetadataVersionsAreSensiblyOrdered($) {
	my ($mapVersionToPosition) = @_;
	my @original;
	foreach (keys %$mapVersionToPosition) {
		$original[$mapVersionToPosition->{$_}] = [ extractSimpleVersionArray($_) ];
	}

	my @sorted = sort compareNatural @original;

	for (my $i = 0; $i < @original; ++$i) {
		die "Missing version info for position $i!" if !defined $original[$i];
		my $o = join(".", @{$original[$i]});
		my $s = join(".", @{$sorted[$i]});
		die "Expected version #$i to be '$s' but saw '$o'!" if $o ne $s;
	}
}

sub downloadMavenMetadataXml($$) {
	my ($g, $a) = @_;
	my $url = getMavenMetadataXmlUrlFor($g, $a);

	my %mapVersionToPosition;
	my $i = 0;
	local $_;
	print STDERR "Downloading metadata $url for $g:$a...\n";
	foreach (`curl $url`) {
		if (m|<version>(.*)</version>\s*$|) {
			#push @versions, $1;
			$mapVersionToPosition{$1} = $i++;
		}
	}
	print STDERR "Extracted metadata on $i versions from $url for $g:$a...\n";

	checkMavenMetadataVersionsAreSensiblyOrdered(\%mapVersionToPosition);		# Dies if it "looks wrong"
	print STDERR "Version order from $url for $g:$a 'looks right'.\n";

	return \%mapVersionToPosition;
}

my %mavenMetadataXmlCache = ();
sub getMavenMetadataXml($$) {
	my ($g, $a) = @_;
	if (!exists $mavenMetadataXmlCache{"$g:$a"}) {
		$mavenMetadataXmlCache{"$g:$a"} = downloadMavenMetadataXml($g, $a);
	}

	return $mavenMetadataXmlCache{"$g:$a"};
}

sub positionInVersionList($$$) {
	my ($g, $a, $v) = @_;
	my $mapVersionToPosition = getMavenMetadataXml($g, $a);
	die "Could not find Maven metadata for $g:$a!" if !defined $mapVersionToPosition;
	my $pos = $mapVersionToPosition->{$v};
	die "Could not find version $v in Maven metadata for $g:$a!" if !defined $pos;

	return $pos;
}

sub compareByMavenMetadata($$$$) {
	my ($g, $a, $v1, $v2) = @_;
	return positionInVersionList($g, $a, $v1) <=> positionInVersionList($g, $a, $v2);
}

# Main program

my @validModes = qw/--output-json --output-tsv/;
my $mode = "--output-json";
if (@ARGV && $ARGV =~ /^--/) {
	$mode = shift;
}

die "Valid modes: " . join(", ", @validModes) if !grep { $mode eq $_ } @validModes;

my %versions;
while (<>) {
	if (my ($cve, $g, $a, $v, $result) = m!^.* tests in \S+/([^/]+)/(\S+?)__(\S+?)__(\S+?): .* -> vuln is (present|absent)$!) {
		#print join("\t", $cve, "$g:$a", $v, $result), "\n";
		push @{$versions{$cve}{$g}{$a}}, [$v, $result];
	}
}

foreach my $cve (sort keys %versions) {
	my @affected = ();
	foreach my $g (sort keys %{$versions{$cve}}) {
		foreach my $art (sort keys %{$versions{$cve}{$g}}) {
			my @sortedVersions = sort { compareByMavenMetadata($g, $art, $a->[0], $b->[0]) } @{$versions{$cve}{$g}{$art}};
			my $vulnIntroduced;
			my $someVersionWasFixed = 0;
			my @events = ();
			my @vulnVersions = ();
			foreach my $vAndResult (@sortedVersions) {
				my ($v, $result) = @$vAndResult;
				if ($mode eq '--output-tsv') {
					print join("\t", $cve, "$g:$art", $v, $result), "\n";
				} elsif ($mode eq '--output-json') {
					if ($result eq 'present') {
						if (!defined $vulnIntroduced) {
							$vulnIntroduced = $v;
							push @events, '{"introduced": "' . $v . '"}';
						}

						push @vulnVersions, $v;
					}

					if (defined($vulnIntroduced) && $result eq 'absent') {
						$vulnIntroduced = undef;
						push @events, '{"fixed": "' . $v . '"}';
						$someVersionWasFixed = 1;
					}
				}
			}

			if (@events) {
				my $package = '{"package": {"ecosystem": "Maven", "name": "' . "$g:$art" . '"}, ';
				if ($someVersionWasFixed) {
					push @affected, $package . '"ranges": [{"type": "ECOSYSTEM", "events": [' . join(", ", @events) . ']}]}';
				} else {
					# GitHub can't handle multiple versions in the "versions" array. Workaround: Make per-version affected packages (suggested by darakian: https://github.com/github/advisory-database/pull/2841#issuecomment-1787952423)
					#$package .= '"versions": [' . join(", ", map { "\"$_\"" } @vulnVersions) . ']';
					push @affected, map { $package . '"versions": [' . "\"$_\"]}" } @vulnVersions;
				}
			}
		}
	}

	if ($mode eq '--output-json' && @affected) {
		print '{"affected": [', join(", ", @affected), "]}\n";
	}
}
