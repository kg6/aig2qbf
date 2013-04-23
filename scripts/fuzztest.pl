#!/usr/bin/perl

use Modern::Perl;

use File::Basename;
use File::Slurp;
use Getopt::Compact;
use String::Util qw/trim/;
use Time::HiRes qw/time/;

my $script_path = dirname(__FILE__);

my $options = Getopt::Compact->new(
	name => 'fuzz test for aig2qbf',
	struct => [
		[ 'k', 'Max unnrolling steps k', ':i' ],
		[ 'verbose', 'Verbose output' ],
	]
);

my $opts = $options->opts();

if (not $options->status()) {
	say $options->usage();

	exit 1;
}

$opts->{k} ||= 10;

my $max_k = $opts->{k};

if (not -f "$script_path/../build/classes/at/jku/aig2qbf/aig2qbf.class") {
	print "Cannot find aig2qbf class. aig2qbf not built into folder 'build'!\n";

	exit 2;
}

my $checker_options = '';

if ($opts->{verbose}) {
	$checker_options .= ' --verbose';
}

while (1) {
	if (not $opts->{verbose}) {
		print 'k =';
	}
	
	my $base_file = "$script_path/../output/fuzz-tt";
	print `$script_path/../tools/aigfuzz -s -c > $base_file.aig`;

	for my $k(1..$max_k) {
		if (not $opts->{verbose}) {
			print " $k";
		}

		time_start();
		my $check_out = `$script_path/../scripts/check.pl --k $k --input $base_file.aig$checker_options`;
		time_end();

		if ($? == 0) {
			if ($opts->{verbose}) {
				print "$check_out";
			}
			else {
				print_elapsed_time();
			}
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

	if (not $opts->{verbose}) {
		print "\n";
	}
}

exit 0;

my ($time_start, $time_end);
sub time_start { $time_start = time; }
sub time_end { $time_end = time; }
sub print_elapsed_time {
	printf("(%d ms)", ($time_end - $time_start) * 1000.0);
}
