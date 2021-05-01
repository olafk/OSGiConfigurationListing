package de.olafkock.liferay.osgiconfigurationlisting.portlet;

import org.osgi.service.metatype.AttributeDefinition;

public class ADContent {
	public String id;
	public String name;
	public String description;
	public String[] deflts;
	public String type = "undetected";
	public String cardinality = "undetected";
	
	@SuppressWarnings("deprecation")
	public void resolveType(AttributeDefinition ad) {
		int cd = ad.getCardinality();
		
		if(cd < 0) {
			if(cd == Integer.MIN_VALUE)
				cardinality = "[] as List";
			else
				cardinality = "[max "+ (-cd) + "] as List";
		} else if (cd > 0) {
			if(cd == Integer.MAX_VALUE)
				cardinality = "[]";
			else
				cardinality = "[max "+ (cd) + "]";
		} else
			cardinality = "";
		
		switch(ad.getType()) {
		case AttributeDefinition.STRING:
			type ="String"; break;
		case AttributeDefinition.LONG:
			type="Long"; break;
		case AttributeDefinition.INTEGER:
			type="Integer"; break;
		case AttributeDefinition.SHORT:
			type="Short"; break;
		case AttributeDefinition.CHARACTER:
			type="Character"; break;
		case AttributeDefinition.BYTE:
			type="Byte"; break;
		case AttributeDefinition.DOUBLE:
			type="Double"; break;
		case AttributeDefinition.FLOAT:
			type="Float"; break;
		case AttributeDefinition.BOOLEAN:
			type="Boolean"; break;
		case AttributeDefinition.PASSWORD:
			type="Password"; break;
		case AttributeDefinition.BIGDECIMAL:
			type="Bigdecimal-deprecated"; break;
		default:
			type="unknown type:" + ad.getType();
		}
	}
}
