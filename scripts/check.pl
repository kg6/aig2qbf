#!/usr/bin/perl

use Modern::Perl;

use File::Basename;
use File::Slurp;
use Getopt::Compact;
use String::Util qw/trim/;
use Time::HiRes qw/time/;

my $script_path = dirname(__FILE__);

my $options = Getopt::Compact->new(
	name => 'checker if the output of aig2qbf is ok',
	struct => [
		[ 'input', 'Input aig file to check', '=s' ],
		[ 'k', 'Unrolling step k', ':i' ],
		[ 'verbose', 'Verbose output' ],
	]
);

my $opts = $options->opts();

sub options_validate {
	if (not $opts->{input}) {
		return;
	}

	if ($opts->{k} and $opts =~ m/^\d+$/) {
		return;
	}

	return 1;
}

if (not $options->status() or not options_validate()) {
	say $options->usage();

	exit 1;
}

my $file = $opts->{input};
my @ks = ($opts->{k}) ? ($opts->{k}) : (1..10);

if (not -f "$script_path/../build/classes/at/jku/aig2qbf/aig2qbf.class") {
	print "Cannot find aig2qbf class. aig2qbf not built into folder 'build'!\n";

	exit 5;
}

# ignore empty AIG files
if (trim(`md5sum $file 2>&1`) eq 'd392065d0606079baa34d135fd01953e') {
	print "We ignore empty aiger files.\n";

	exit 6;
}

for my $k (@ks) {
	if ($opts->{verbose}) {
		print "Check k=$k\n";
	}

	time_start();
	my $out = `java -cp "$script_path/../build/classes/:$script_path/../lib/commons-cli-1.2.jar" at.jku.aig2qbf.aig2qbf -k $k --input $file 2>&1`;
	time_end();
	print_elapsed_time('aig2qbf');

	if ($out =~ m/Exception in thread/) {
		print "aig2qbf load error on K=$k\n";
		print "\n$out\n";

		exit 1;
	}
	
	write_file("$file-$k.qbf", $out);

	time_start();
	my $debqbf_out = trim(`$script_path/../tools/depqbf $file-$k.qbf 2>&1`);
	time_end();
	print_elapsed_time('debqbf');
	
	my $debqbf_sat;

	if ($debqbf_out eq 'SAT') {
		$debqbf_sat = 1;
	}
	elsif ($debqbf_out eq 'UNSAT') {
		$debqbf_sat = 0;
	}
	else {
		print "depqbf solve error on K=$k\n";
		print "\n$debqbf_out\n";
	
		exit 2;
	}
	
	if ($opts->{verbose}) {
		print "debqbf says " . ($debqbf_sat ? 'SAT' : 'UNSAT') . "\n";
	}
	
	print `$script_path/../tools/aigor $file $file-or.aig`;

	time_start();
	my $mcaiger_out = trim(`$script_path/../tools/mcaiger -r $k $file-or.aig 2>&1`);
	time_end();
	print_elapsed_time('mcaiger');

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
	
	if ($opts->{verbose}) {
		print "mcaiger says " . ($mcaiger_sat ? 'SAT' : 'UNSAT') . "\n";
	}

	if ($debqbf_sat != $mcaiger_sat) {
		print "debqbf and mcaiger are divided over the satisfiability, error on K=$k\n";
		print "debqbf says " . ($debqbf_sat ? 'SAT' : 'UNSAT') . "\n";
		print "mcaiger says " . ($mcaiger_sat ? 'SAT' : 'UNSAT') . "\n";

		exit 4;
	}
	
	if ($opts->{verbose}) {
		print "AGREED\n";
	}
}

print "Check OK\n";

exit 0;

my ($time_start, $time_end);
sub time_start { $time_start = time; }
sub time_end { $time_end = time; }
sub print_elapsed_time {
	my ($text) = @_;

	$text ||= '';

	if ($opts->{verbose}) {
		printf("%s (%d ms)\n", $text, ($time_end - $time_start) * 1000.0);
	}
}
