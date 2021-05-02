package de.olafkock.liferay.osgiconfigurationlisting.portlet;

import java.util.Comparator;

public class OCDContentComparator implements Comparator<OCDContent>{

	@Override
	public int compare(OCDContent o1, OCDContent o2) {
		return ("" + o1.scope + o1.category + o1.name).compareTo(
				"" + o2.scope + o2.category + o2.name);
	}
}
