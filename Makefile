JFLAGS = -d classes/ 
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

crop: 
	$(JC) $(JFLAGS)	-cp "lib/ImageJ" src/crop/SanskritUtil.java src/crop/SanskritCrop.java src/Main.java
clean:
		$(RM) *.class
