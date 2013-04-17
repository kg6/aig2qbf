#!/usr/bin/perl

use Modern::Perl;

use File::Basename;
use File::Slurp;
use String::Util qw/trim/;

my $script_path = dirname(__FILE__);

while (1) {
	print '.';
	
	my $base_file = "$script_path/../output/fuzz-tt";
	print `$script_path/../tools/aigfuzz -s -c > $base_file.aig`;

	my $check_out = `$script_path/../scripts/check.pl $base_file.aig`;

	if ($? == 0) {
		# ok
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
