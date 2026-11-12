mvn -f Source/GATTXMLParser package && ^
java -jar Source/GATTXMLParser/target/GATTXMLParser-1.0-SNAPSHOT-jar-with-dependencies.jar ^
	--characteristic-package-name=com.jht.gatt.characteristic ^
	--characteristic-package-dir=Source/ChimeraApp/GATTServer/src/main/java/com/jht/gatt/characteristic ^
	Source/GATTXMLParser/input/*.xml
