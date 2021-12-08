package de.olafkock.liferay.osgiconfigurationlisting.portlet;

import java.util.Collections;
import java.util.Enumeration;
import java.util.ResourceBundle;

class EmptyResourceBundle extends ResourceBundle {
	@Override
	public Enumeration<String> getKeys() {
		return Collections.emptyEnumeration();
	}

	@Override
	protected Object handleGetObject(String key) {
		return null;
	}
}