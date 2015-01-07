run: compile
	java -classpath ./bin screensaver.Main

compile:
	javac -d bin/ src/screensaver/*.java

clean:
	rm bin/screensaver/*.class