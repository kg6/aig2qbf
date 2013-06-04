
all: aig2qbf
aig2qbf:
	ant
clean:
	ant clean
test:
	java -cp "build/classes/:lib/commons-cli-1.2.jar:/usr/share/java/junit4.jar" org.junit.runner.JUnitCore at.jku.aig2qbf.test.OnlyFastTests
