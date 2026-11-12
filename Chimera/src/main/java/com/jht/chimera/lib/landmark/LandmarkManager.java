package com.jht.chimera.lib.landmark;

import android.annotation.SuppressLint;
import android.util.Log;

import org.jetbrains.annotations.NonNls;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

// Used to load the landmark list from a xml file
@SuppressLint("SDCardPath")
public class LandmarkManager {
    private static final int GOAL_TYPE_FLOORS = 4;
    private static final double FT_PER_METER = 3.28084;

    // XML NODES
    //private final static String XML_NODE_ROOT = "Landmarks";
    @NonNls private final static String XML_NODE_SETUP_ANIMATION = "SetupAnimation";
    @NonNls private final static String XML_NODE_LAUNCH_ANIMATION = "LaunchAnimation";
    @NonNls private final static String XML_NODE_LANDMARK = "Landmark";
    @NonNls private final static String XML_NODE_TITLE = "Title";

    @NonNls private final static String XML_NODE_LOCATION = "Location";
    @NonNls private final static String XML_NODE_DESCRIPTION = "Description";

    @NonNls private final static String XML_NODE_BACKGROUND_IMAGE = "BackgroundImages";
    @NonNls private final static String XML_NODE_IMAGE = "Image";
    @NonNls private final static String XML_NODE_SILHOUETTE_FULL = "SilhouetteFull";
    @NonNls private final static String XML_NODE_SILHOUETTE_NORMAL = "SilhouetteNormal";
    @NonNls private final static String XML_NODE_START_VIDEO = "StartVideo";
    @NonNls private final static String XML_NODE_EVENTS = "Events";
    @NonNls private final static String XML_NODE_EVENT = "Event";
    @NonNls private final static String XML_NODE_MESSAGE = "Message";
    @NonNls private final static String XML_NODE_AUDIOSFX = "AudioSFX";

    // XML ATTRIBUTES
    //private final static String XML_ATTRIBUTE_VERSION = "version";
    //private final static String XML_ATTRIBUTE_NAME = "name";
    @NonNls private final static String XML_ATTRIBUTE_FLOORS = "floors";
    @NonNls private final static String XML_ATTRIBUTE_FLOORS_RETAIL = "floors_retail";
    @NonNls private final static String XML_ATTRIBUTE_METERS = "meters";
    @NonNls private final static String XML_ATTRIBUTE_GOALTYPE = "goalType";
    @NonNls private final static String XML_ATTRIBUTE_SOURCE = "source";
    //private final static String XML_ATTRIBUTE_OFFSET_X = "offset_x";
    //private final static String XML_ATTRIBUTE_OFFSET_Y = "offset_y";
    //private final static String XML_ATTRIBUTE_WIDTH = "width";
    //private final static String XML_ATTRIBUTE_HEIGHT = "height";
    @NonNls private final static String XML_ATTRIBUTE_STEP = "step";

    @NonNls private final static String SD_PATH = "/sdcard/jht";
    @NonNls private final static String LANDMARK_DATA = "/sdcard/jht/landmarks";

    //public final static String BUILDINGS_PATH = LANDMARK_DATA + "/" + "buildings"; // Used for setup screen

    //public final static String SLIDESHOWS_PATH = LANDMARK_DATA + "/" + "slideshows"; // Used for the background switch on running screen

    //public final static String SHADOWS_PATH = LANDMARK_DATA + "/" + "shadows"; // Used for progress background

    private List<Landmark> _landmarks = new ArrayList<>();
    //private List<Bitmap> _snapshots = new ArrayList<Bitmap>();

    public List<Landmark> loadFromXmlFile(String xmlFile) throws ParserConfigurationException, SAXException, NumberFormatException, IOException {
        @NonNls final String TAG = "LandmarkManager";

        _landmarks.clear();

        FileInputStream is = new FileInputStream(LANDMARK_DATA + "/" + xmlFile);
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document doc = documentBuilder.parse(is);

        Node setupAnimationNode = doc.getElementById(XML_NODE_SETUP_ANIMATION);
        if (null != setupAnimationNode) {
            String setupAnimationFile = setupAnimationNode.getAttributes().getNamedItem(XML_ATTRIBUTE_SOURCE).getNodeValue();
            Log.i(TAG, "setupAnimationFile:" + setupAnimationFile);
        }

        Node launchAnimationNode = doc.getElementById(XML_NODE_LAUNCH_ANIMATION);
        if (null != launchAnimationNode) {
            String launchAnimationFile = launchAnimationNode.getAttributes().getNamedItem(XML_ATTRIBUTE_SOURCE).getNodeValue();
            Log.i(TAG, "launchAnimationFile:" + launchAnimationFile);
        }

        NodeList landmarkNodes = doc.getElementsByTagName(XML_NODE_LANDMARK);
        for (int i = 0; i < landmarkNodes.getLength(); ++i) {
            Node landmarkNode = landmarkNodes.item(i);

            // get landmark attributes
            NamedNodeMap landmarkAttributes = landmarkNode.getAttributes();

            String tmp;
            Landmark landmark = new Landmark();
            tmp = "1";
            Log.i(TAG, "version:" + tmp);
            landmark._version = tmp;


            // landmark floors
            tmp = landmarkAttributes.getNamedItem(XML_ATTRIBUTE_FLOORS).getNodeValue();
            Log.i(TAG, "floors:" + tmp);
            landmark._floors = Integer.valueOf(tmp);

            // landmark floors
            tmp = landmarkAttributes.getNamedItem(XML_ATTRIBUTE_FLOORS_RETAIL).getNodeValue();
            Log.i(TAG, "floors_retail:" + tmp);
            landmark._floorsRetail = Integer.valueOf(tmp);

            // landmark meters
            tmp = landmarkAttributes.getNamedItem(XML_ATTRIBUTE_METERS).getNodeValue();
            Log.i(TAG, "meters:" + tmp);
            double meters = Double.valueOf(tmp);
            landmark._meters = (int) Math.round(meters);

            double feets = meters * FT_PER_METER;
            landmark._feets = (int) Math.round(feets);

            // landmark goal type
            tmp = landmarkAttributes.getNamedItem(XML_ATTRIBUTE_GOALTYPE).getNodeValue();
            Log.i(TAG, "goalType:" + tmp);
            landmark._goalType = GOAL_TYPE_FLOORS;

            // retrieve landmark nodes
            NodeList landmarkChildNodes = landmarkNode.getChildNodes();
            for (int j = 0; j < landmarkChildNodes.getLength(); ++j) {
                Node currentNode = landmarkChildNodes.item(j);
                String nodeName = currentNode.getNodeName();
                switch (nodeName) {
                    case XML_NODE_TITLE:
                        landmark._title = currentNode.getTextContent();
                        break;
                    case XML_NODE_LOCATION:
                        landmark._location = currentNode.getTextContent();
                        break;
                    case XML_NODE_DESCRIPTION:
                        landmark._description = currentNode.getTextContent();
                        break;
                    case XML_NODE_BACKGROUND_IMAGE:

                        NodeList backgroundImagesChildNodes = currentNode.getChildNodes();
                        for (int k = 0; k < backgroundImagesChildNodes.getLength(); ++k) { // <Image>
                            Node backgroundImagesItem = backgroundImagesChildNodes.item(k);
                            if (backgroundImagesItem.getNodeName().equals(XML_NODE_IMAGE)) {
                                BackgroundImage image = new BackgroundImage();
                                String source = backgroundImagesItem.getAttributes().getNamedItem(XML_ATTRIBUTE_SOURCE).getNodeValue();
                                Log.i(TAG, "image source:" + source);

                                image._source = SD_PATH + "/" + source;//BitmapFactory.decodeFile(LANDMARK_DATA + "/" + source);
                                image._pivotX = Float.valueOf(backgroundImagesItem.getAttributes().getNamedItem("pivot_x").getNodeValue());
                                image._pivotY = Float.valueOf(backgroundImagesItem.getAttributes().getNamedItem("pivot_y").getNodeValue());
                                image._scaleX = Float.valueOf(backgroundImagesItem.getAttributes().getNamedItem("scale_x").getNodeValue());
                                image._scaleY = Float.valueOf(backgroundImagesItem.getAttributes().getNamedItem("scale_y").getNodeValue());

                                landmark._backgroundImageFiles.add(image);
                            } // end Image
                        } // </Image>
                        break;
                    case XML_NODE_SILHOUETTE_FULL: {
                        String source = currentNode.getAttributes().getNamedItem(XML_ATTRIBUTE_SOURCE).getNodeValue();
                        Log.i(TAG, "Silhouette full:" + source);
                        landmark._silhouetteFullFile = SD_PATH + "/" + source;
                        break;
                    }
                    case XML_NODE_SILHOUETTE_NORMAL: {
                        String source = currentNode.getAttributes().getNamedItem(XML_ATTRIBUTE_SOURCE).getNodeValue();
                        Log.i(TAG, "Silhouette normal:" + source);
                        landmark._silhouetteNormalFile = SD_PATH + "/" + source;
                        break;
                    }
                    case XML_NODE_START_VIDEO: {
                        String source = currentNode.getAttributes().getNamedItem(XML_ATTRIBUTE_SOURCE).getNodeValue();
                        Log.i(TAG, "StartVideo:" + source);
                        landmark._startVideoFile = SD_PATH + "/" + source;
                        break;
                    }
                    case XML_NODE_EVENTS:
                        NodeList eventsChildNodes = currentNode.getChildNodes();
                        for (int k = 0; k < eventsChildNodes.getLength(); ++k) { // <Events>
                            Node eventsItem = eventsChildNodes.item(k);
                            if (eventsItem.getNodeName().equals(XML_NODE_EVENT)) {
                                String stepValue = eventsItem.getAttributes().getNamedItem(XML_ATTRIBUTE_STEP).getNodeValue();
                                Log.i(TAG, "event step:" + stepValue);
                                int steps = Integer.valueOf(stepValue);

                                NodeList eventChildNodes = eventsItem.getChildNodes();
                                for (int n = 0; n < eventChildNodes.getLength(); ++n) { // <Event>
                                    Node eventItem = eventChildNodes.item(n);
                                    if (eventItem.getNodeName().equals(XML_NODE_MESSAGE)) {
                                        NodeList messageChildNodes = eventItem.getChildNodes();
                                        for (int z = 0; z < messageChildNodes.getLength(); ++z) { // <Message>
                                            Node messageItem = messageChildNodes.item(z);
                                            if (messageItem.getNodeName().equals("en_US")) {
                                                Log.i(TAG, "message:" + messageItem.getTextContent());
                                                landmark._events.add(new StepEvent.MessageEvent(steps, messageItem.getTextContent()));
                                            } // end en_US
                                        } // </Location>
                                    } else if (eventItem.getNodeName().equals(XML_NODE_AUDIOSFX)) {
                                        String soundFile = eventItem.getAttributes().getNamedItem(XML_ATTRIBUTE_SOURCE).getNodeValue();
                                        Log.i(TAG, "soundFile:" + soundFile);
                                        landmark._events.add(new StepEvent.SoundEvent(steps, LANDMARK_DATA + "/" + soundFile));
                                    }
                                } // </Event>
                            } // end Event
                        } // </Events>
//					landmark._events.add(new Event.SoundEvent(0, ""));
                        break;
                }
            } // end for
            _landmarks.add(landmark);
        } // end for

        Log.i(TAG, "End document");

        return _landmarks;
    }


    public List<Landmark> getLandmarks() {

        return _landmarks;
    }
}
