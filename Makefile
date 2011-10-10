all : classes
	echo 'Main-class: Stego' > bin/manifest.tmp
	cd bin; jar cvfm Stego.jar manifest.tmp *.class
	-rm bin/manifest.tmp
	mv bin/Stego.jar .

classes : Base64Coder.java DesEncrypter.java SortedList.java StegImage.java Stego.java
	mkdir -p bin
	javac *.java -d bin

clean :
	-rm bin/*

spotless : clean
	-rm Stego.jar
