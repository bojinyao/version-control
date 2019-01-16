JFLAGS = -g -Xlint:unchecked -Xlint:deprecation

# A CLASSPATH value that (seems) to work on both Windows and Unix systems.
# To Unix, it looks like ..:$(CLASSPATH):JUNK and to Windows like
# JUNK;..;$(CLASSPATH).
CPATH = "..:$(CLASSPATH):;..;$(CLASSPATH)"

# All .java files in this directory.
SRCS := $(wildcard *.java)

default: $(SRCS)
	@javac $(JFLAGS) -cp $(CPATH) $(SRCS)
	@touch sentinel

clean:
	$(RM) *~ *.class sentinel

	