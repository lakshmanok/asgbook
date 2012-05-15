/**
 * 
 */
package edu.ou.asgbook.io;

import java.awt.image.ColorModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.LatLonGrid;

/**
 * Writes data out in KML form, for display in Google Earth or similar program
 * 
 * @author Valliappa.Lakshmanan
 *
 */
public class KmlWriter {
	public static void write(LatLonGrid grid, File outputDir, String dataName, ColorModel colormap) throws Exception {
		// write image
		File imgFileName = new File(outputDir.getAbsolutePath() + "/" + dataName + ".png");
		PngWriter.writeAutoScaled(grid, imgFileName, colormap);
		
		// create KML
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("kml");
		doc.appendChild(root);
		Element folder = doc.createElement("Folder");
		root.appendChild(folder);
		Element folderName = doc.createElement("name");
		folder.appendChild(folderName);
		folderName.setTextContent(dataName);
		Element folderDesc = doc.createElement("description");
		folderDesc.setTextContent(dataName + " created by " + KmlWriter.class.getCanonicalName() + " for " + System.getProperty("java.user") + " on " + new Date());
		Element goverlay = doc.createElement("GroundOverlay");
		folder.appendChild(goverlay);
		Element icon = doc.createElement("Icon");
		goverlay.appendChild(icon);
		Element href = doc.createElement("href");
		icon.appendChild(href);
		href.setTextContent(dataName + ".png");
		Element box = doc.createElement("LatLonBox");
		goverlay.appendChild(box);
		Element north = doc.createElement("north");
		north.setTextContent("" + grid.getNwCorner().getLat());
		box.appendChild(north);
		Element south = doc.createElement("south");
		south.setTextContent("" + grid.getSeCorner().getLat());
		box.appendChild(south);
		Element east = doc.createElement("east");
		east.setTextContent("" + grid.getSeCorner().getLon());
		box.appendChild(east);
		Element west = doc.createElement("west");
		west.setTextContent("" + grid.getNwCorner().getLon());
		box.appendChild(west);
		box.appendChild(north);

		// write KML
		File kmlFileName = new File(outputDir.getAbsolutePath() + "/" + dataName + ".kml");
		Transformer t = TransformerFactory.newInstance().newTransformer();
		t.setOutputProperty(OutputKeys.INDENT, "yes");
		t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		t.transform(new DOMSource(doc), new StreamResult(kmlFileName));
		System.out.println("Wrote " + kmlFileName + " to refer to image");
	}
	
	public static void write(List<LatLon> points, File outputDir, String dataName) throws Exception {
		List<String> names = new ArrayList<String>();
		for (int i=0; i < points.size(); ++i){
			names.add(dataName + " " + (i+1) );
		}
		write(points, names, outputDir, dataName);
	}
	
	public static void write(List<LatLon> points, List<String> names, File outputDir, String dataName) throws Exception {
		// create KML doc
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("kml");
		doc.appendChild(root);
		Element docE = doc.createElement("Document");
		root.appendChild(docE);
		for (int i=0; i < points.size(); ++i){
			Element placemark = doc.createElement("Placemark");
			docE.appendChild(placemark);
			Element name = doc.createElement("name");
			placemark.appendChild(name);
			if (names != null && i < names.size()){
				name.setTextContent(names.get(i));
			} else {
				name.setTextContent(dataName + "#" + (i+1));
			}
			Element point = doc.createElement("Point");
			placemark.appendChild(point);
			Element coords = doc.createElement("coordinates");
			point.appendChild(coords);
			coords.setTextContent(points.get(i).getLon() + "," + points.get(i).getLat() + ",0.");
		}
		
		// write out
		File filename = new File(outputDir.getAbsolutePath() + "/" + dataName + ".kml");
		Transformer t = TransformerFactory.newInstance().newTransformer();
		t.setOutputProperty(OutputKeys.INDENT, "yes");
		t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		t.transform(new DOMSource(doc), new StreamResult(filename));
		System.out.println("Wrote " + filename + " to refer to " + names.size() + " placemarks");
	}
	
	public static void debugWrite(LatLonGrid grid, File out, String name){
		if (out != null){
			try {
				KmlWriter.write(grid, out, name, PngWriter.createCoolToWarmColormap());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		LatLonGrid grid = new LatLonGrid(100, 200, -1, new LatLon(35,-97), 0.1, 0.1);
		for (int i=0; i < grid.getNumLat(); ++i){
			for (int j=0; j < grid.getNumLon(); ++j){
				grid.getData()[i][j] = i + j;
				if ( i%10 == 0 || j%20 == 0){
					grid.getData()[i][j] = grid.getMissing();
				}
			}
		}
		File outputDir = OutputDirectory.getDefault("kmlwriter");
		KmlWriter.write(grid, outputDir, "kmlwriter", PngWriter.createHotColormap());
	}
}
