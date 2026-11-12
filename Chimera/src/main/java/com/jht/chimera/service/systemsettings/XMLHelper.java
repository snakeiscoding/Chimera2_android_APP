package com.jht.chimera.service.systemsettings;

import android.util.Log;

import org.jetbrains.annotations.NonNls;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class XMLHelper {


    final private String TAG = "XMLHelper";

    public static class XMLAttribute
    {

        public String Name;
        public String Value;

        public XMLAttribute( String name, String value )
        {
            Name = name;
            Value = value;
        }

    }
    public static class XMLNode
    {
        public String Name;
        public String Value;

        public List<XMLAttribute> Attributes = new ArrayList<>();
        public List<XMLNode> Children = new ArrayList<>();

        public XMLNode(@NonNls String name, @NonNls String value )
        {
            Name = name;
            Value = value;
        }

        public void writeXML( XmlSerializer xml ) throws IllegalArgumentException, IllegalStateException, IOException
        {

            xml.startTag("", Name);

            for( int i = 0; i < Attributes.size(); i++ )
            {
                XMLAttribute a = Attributes.get( i );
                xml.attribute("", a.Name, a.Value);
            }
            if( Children.size() > 0 )
            {
                for( int i = 0; i < Children.size(); i++ )
                {
                    XMLNode n = Children.get( i );
                    n.writeXML( xml );
                }
            }
            else if( Value != null && Value.compareTo( "" ) != 0 )
                xml.text(Value);

            xml.endTag( "", Name );

        }

        public XMLNode AddChild( XMLNode child )
        {
            Children.add( child );
            return child;
        }
    }

    public List<XMLNode> children = new ArrayList<>();

    public XMLHelper()
    {
    }

    public XMLNode AddNode( XMLNode node )
    {
        children.add( node );
        return node;
    }

    public void RemoveNode( XMLNode parent, XMLNode child )
    {
        parent.Children.remove( child );
    }

    public void RemoveNode( XMLNode node )
    {
        children.remove( node );
    }

    public String getXML()
    {
        try
        {
            XmlSerializer xml = XmlPullParserFactory.newInstance().newSerializer();
            StringWriter writer = new StringWriter();

            xml.setOutput(writer);
            xml.startDocument("UTF-8", true);
            xml.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true); //NON-NLS
            for( int i = 0; i < children.size(); i++ )
                children.get(i).writeXML(xml);
            xml.endDocument();

            return writer.toString();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return "";
        }

    }
}
