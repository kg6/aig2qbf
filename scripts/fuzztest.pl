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
		[ 'l', 'Generate large circuits' ],
		[ 'no-reduction', 'Do not apply any reduction' ],
		[ 'no-dd', 'Do not delta debug any errors' ],
		[ 'verbose', 'Verbose output' ],
		[ 'with-sanity', 'Apply sanity checks.' ],
	]
);

my $opts = $options->opts();

sub options_validate {
	if ($opts->{k} and $opts =~ m/^\d+$/) {
		return;
	}

	return 1;
}

if (not $options->status() or not options_validate()) {
	say $options->usage();

	exit 3;
}

$opts->{k} ||= 10;

my $max_k = $opts->{k};

my $build_file = "$script_path/../build/classes/at/jku/aig2qbf/aig2qbf.class";

if (not -f $build_file) {
	print `make -C "$script_path/../" 2>&1`;

	if (not -f $build_file) {
		print "Cannot find aig2qbf class. aig2qbf not built into folder 'build'!\n";

		exit 2;
	}
}

if ($opts->{verbose}) {
	$|++;
}

my $checker_options = '';

if ($opts->{'no-reduction'}) {
	$checker_options .= ' --no-reduction';
}
if ($opts->{'verbose'}) {
	$checker_options .= ' --verbose';
}
if ($opts->{'with-sanity'}) {
	$checker_options .= ' --with-sanity';
}

my $fuzzer_options = '-m -1';

if ($opts->{l}) {
	$fuzzer_options .= ' -l';
}
else {
	$fuzzer_options .= ' -s';
}

while (1) {
	if (not $opts->{verbose}) {
		print 'k =';
	}
	else {
		print "---\n";
	}

	my $base_file = "$script_path/../output/fuzz-tt";
	print `"$script_path/../tools/aigfuzz" $fuzzer_options > "$base_file.aig"`;

	for my $k(1..$max_k) {
		if ($opts->{verbose}) {
			print "-\n";
		}
		else {
			print " $k";
		}

		time_start();
		my $check_out = `"$script_path/../scripts/check.pl" --k $k --input "$base_file.aig" $checker_options`;
		time_end();

		if ($? == 0) {
			if ($opts->{verbose}) {
				print "$check_out";
			}
			else {
				print_elapsed_time();
			}
		}
		elsif ($? == 8) {
			print "IGNORE mcaiger = 2 output\n";
		}
		else {
			if (not $opts->{verbose}) {
				print "\n";
			}

			print "CHECK NOT OK\n";

			if ($check_out =~ m/error on k=(\d+)/) {
				print "$check_out\n";

				if (not $opts->{'no-dd'}) {
					print "Reduce $base_file.aig to $base_file-reduced.aig with k=$k\n";

					print `"$script_path/../scripts/dd.sh" "$base_file.aig" "$base_file-reduced.aig" $k $checker_options`;
					print "Done\n";

					exit 1;
				}
			}
			else {
				print "Error in check\n";
				print "$check_out\n";

				exit 4;
			}
		}
	}

	if (not $opts->{verbose}) {
		print "\n";
	}
	else {
		print "-\n";
	}
}

exit 0;

my ($time_start, $time_end);
sub time_start { $time_start = time; }
sub time_end { $time_end = time; }
sub print_elapsed_time {
	printf("(%d ms)", ($time_end - $time_start) * 1000.0);
}
