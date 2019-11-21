package pixfret;

import javax.swing.*;
import javax.swing.text.*;

import java.awt.*;              //for layout managers and more
import java.awt.event.*;        //for action events

import java.net.URL;
import java.io.IOException;
		
public class About extends JPanel {

	public About() {
		super();
		JTextPane textPane = createTextPane();
		add(textPane);
	}
	
   private JTextPane createTextPane() {
   		 String newline = "\n";
         String[] initString =
                { 	newline + newline + "PixFRET" + newline + newline,
                  	"Version 1.5.1, 14 June 2006" + newline,
                  	"Plugin of ImageJ " + newline,
                  	"Pixel by Pixel analysis of FRET with ImageJ" + newline + newline,
                  	
					"Jerome Feige, Laurent Gelman" + newline,
                  	"Center for Integrative Genomics" + newline,
                  	"NCCR frontiers in Genetics" + newline,
                  	"University of Lausanne" + newline,
                  	"Lausanne, Switzerland" + newline+ newline,
                  	
                  	"Daniel Sage" + newline,
                  	"Biomedical Imaging Group" + newline,
                  	"Swiss Federal Institute of Technology Lausanne (EPFL)" + newline,
                  	"Lausanne, Switzerland" + newline + newline + newline,
                  	
                  	"More info at:" + newline, 
                  	"http://www.unil.ch/cig/page16989.html"
                  	
                 };

        String[] initStyles = { "bold", "italic", "regular", "bold", 
        						"italic", "bold", "regular", "regular", "regular",
        						"italic", "bold", "regular", "regular", 
        						"bold", "button"};

        JTextPane textPane = new JTextPane();
        textPane.setBackground(this.getBackground());
        StyledDocument doc = textPane.getStyledDocument();
        addStylesToDocument(doc);
		
        try {
            for (int i=0; i < initString.length; i++) {
                doc.insertString(doc.getLength(), initString[i], doc.getStyle(initStyles[i]));
            }
        } catch (BadLocationException ble) {
            System.err.println("Couldn't insert initial text into text pane.");
        }

        return textPane;
    }

    protected void addStylesToDocument(StyledDocument doc) {
        //Initialize some styles.
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

        Style regular = doc.addStyle("regular", def);
        StyleConstants.setFontFamily(def, "SansSerif");
        StyleConstants.setAlignment(regular, StyleConstants.ALIGN_CENTER);

        Style s = doc.addStyle("italic", regular);
        StyleConstants.setItalic(s, true);

        s = doc.addStyle("bold", regular);
        StyleConstants.setBold(s, true);

        s = doc.addStyle("small", regular);
        StyleConstants.setFontSize(s, 10);

        s = doc.addStyle("large", regular);
        StyleConstants.setFontSize(s, 16);

        s = doc.addStyle("icon", regular);
        StyleConstants.setAlignment(s, StyleConstants.ALIGN_CENTER);

        s = doc.addStyle("button", regular);
        StyleConstants.setAlignment(s, StyleConstants.ALIGN_CENTER);
       
        
    }
    

 }
