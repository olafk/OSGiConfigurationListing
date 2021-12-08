package de.olafkock.liferay.osgiconfigurationlisting.portlet;

import java.util.LinkedList;
import java.util.List;

public class OCDContent {
	public String id;
	public String name;
	public String description;
	public String category = "undeclared";
	public String localizedCategory = "undeclared category";
	public String scope = "undeclared";
	public String localizedScope = "undeclared scope";
	public List<ADContent> ads = new LinkedList<ADContent>();
	public String comment = "";
	public String bundle;
	public String learnMessageResource;
	public String learnMessageKey;
}
