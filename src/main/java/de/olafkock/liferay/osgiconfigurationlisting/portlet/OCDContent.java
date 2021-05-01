package de.olafkock.liferay.osgiconfigurationlisting.portlet;

import java.util.LinkedList;
import java.util.List;

public class OCDContent {
	public String id;
	public String name;
	public String description;
	public String category = "undetected";
	public String scope = "undetected";
	public List<ADContent> ads = new LinkedList<ADContent>();
	public String comment;
	public String bundle;
}
