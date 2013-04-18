#!/usr/bin/perl

use Modern::Perl;

use File::Basename;
use File::Slurp;
use String::Util qw/trim/;
use Time::HiRes qw/time/;

my $script_path = dirname(__FILE__);

if (not -f "$script_path/../build/classes/at/jku/aig2qbf/aig2qbf.class") {
	print "Cannot find aig2qbf class. aig2qbf not built into folder 'build'!\n";

	exit 2;
}

my $max_k = (@ARGV) ? int($ARGV[0]) : 10;

while (1) {
	print 'k =';
	
	my $base_file = "$script_path/../output/fuzz-tt";
	print `$script_path/../tools/aigfuzz -s -c > $base_file.aig`;

	for my $k(1..$max_k) {
		print " $k";

		my $time_start = time;
		my $check_out = `$script_path/../scripts/check.pl $k $base_file.aig`;
		my $time_end = time;

		if ($? == 0) {
			printf('(%d ms)', ($time_end - $time_start) * 1000.0);
		}
		else {
			print "\nCHECK NOT OK\n";
			print "$check_out\n";

			my ($k) = $check_out =~ m/error on K=(\d+)/;
		
			print "Reduce $base_file.aig to $base_file-reduced.aig with k=$k\n";

			print `$script_path/../scripts/dd.sh "$base_file.aig" "$base_file-reduced.aig" $k`;
			print "Done\n";

			exit 1;
		}
	}

	print "\n";
}
