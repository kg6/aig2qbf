#!/usr/bin/perl

use Modern::Perl;

use File::Basename;
use File::Slurp;
use String::Util qw/trim/;

my $script_path = dirname(__FILE__);

my $file;
my @ks;

if (@ARGV == 1) {
	$file = $ARGV[0];
	@ks = (1..3);
}
else {
	$file = $ARGV[1];
	@ks = ($ARGV[0]);
}

for my $k (@ks) {
	my $out = `java -cp "$script_path/../build/classes/:$script_path/../lib/commons-cli-1.2.jar" at.jku.aig2qbf.aig2qbf -k $k --input $file 2>&1`;

	if ($out =~ m/Exception in thread/) {
		print "aig2qbf load error on K=$k\n";
		print "\n$out\n";

		exit 1;
	}
	
	write_file("$file-$k.qbf", $out);

	my $debqbf_out = trim(`$script_path/../tools/depqbf $file-$k.qbf 2>&1`);
	my $debqbf_sat;
	
	if ($debqbf_out eq 'SAT') {
		$debqbf_sat = 1;
	}
	elsif ($debqbf_out eq 'UNSAT') {
		$debqbf_sat = 0;
	}
	else {
		print "debqf solve error on K=$k\n";
		print "\n$debqbf_out\n";
	
		exit 2;
	}

	my $mcaiger_out = trim(`$script_path/../tools/mcaiger -r $k $file-or.aig 2>&1`);
	my $mcaiger_sat;

	if ($mcaiger_out eq '1') {
		$mcaiger_sat = 1;
	}
	elsif ($mcaiger_out eq '0') {
		$mcaiger_sat = 0;
	}
	else {
		print "mcaiger solve error on K=$k\n";
		print "\n$mcaiger_out\n";
	
		exit 3;
	}

	if ($debqbf_sat != $mcaiger_sat) {
		print "debqbf and mcaiger are divided over the satisfiability, error on K=$k\n";
		print "debqbf says " . ($debqbf_sat ? 'SAT' : 'UNSAT') . "\n";
		print "mcaiger says " . ($mcaiger_sat ? 'SAT' : 'UNSAT') . "\n";

		exit 4;
	}
}

print "Check OK\n";

exit 0;
