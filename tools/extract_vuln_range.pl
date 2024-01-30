#!/usr/bin/perl

use strict;
use warnings;

while (<>) {
	if (my ($cve, $g, $a, $v, $result) = m!^.* tests in \S+/([^/]+)/(\S+?)__(\S+?)__(\S+?): .* -> vuln is (present|absent)$!) {
		print join("\t", $cve, "$g:$a", $v, $result), "\n";
	}
}
