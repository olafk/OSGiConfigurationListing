package de.olafkock.liferay.osgiconfigurationlisting.portlet;

import com.liferay.portal.configuration.metatype.definitions.ExtendedMetaTypeInformation;
import com.liferay.portal.configuration.metatype.definitions.ExtendedMetaTypeService;
import com.liferay.portal.configuration.metatype.definitions.ExtendedObjectClassDefinition;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

import de.olafkock.liferay.osgiconfigurationlisting.BundleActivator;

/**
 * Extract OCD/AD Metainformation from the OSGi runtime for documentation purposes.
 * 
 * adapted from http://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.metatype.html
 * 
 * @author Olaf Kock
 */

public class MetaInfoExtractor {

	public SortedSet<OCDContent> extractOCD(ExtendedMetaTypeService ems, Locale locale) {
		SortedSet<OCDContent> result = new TreeSet<OCDContent>(new OCDContentComparator());
		BundleContext bc = BundleActivator.bundleContext;
		Bundle[] bundles = bc.getBundles();
		ResourceBundle cfgAdminRb = new EmptyResourceBundle();
		
		for (Bundle bundle : bundles) {
			if(bundle.getSymbolicName().equals("com.liferay.configuration.admin.web")) {
				cfgAdminRb = ResourceBundleUtil.getBundle(locale, 
						bundle.adapt(BundleWiring.class).getClassLoader());
			}
		}
		
		for (Bundle bundle : bundles) {
			result.addAll(extractOCD(bundle, ems, locale, cfgAdminRb));
		}
		
		return result;
	}
	
	public List<OCDContent> extractOCD(Bundle b, ExtendedMetaTypeService ems, Locale locale, ResourceBundle cfgAdminRb) {
		LinkedList<OCDContent> result = new LinkedList<OCDContent>();
		MetaTypeService mts = BundleActivator.getMts();
	    MetaTypeInformation mti = mts.getMetaTypeInformation(b);
	    ExtendedMetaTypeInformation emti = ems.getMetaTypeInformation(b);

	    String [] pids = mti.getPids();
	    if(pids == null || pids.length == 0) {
	    	return result;
	    }
        
	    for (int i=0; i< pids.length; i++) {
			OCDContent ocdContent = new OCDContent();
			result.add(ocdContent);
			ResourceBundle rb;
			String errorContext = "none";
            try {
				ObjectClassDefinition ocd = mti.getObjectClassDefinition(pids[i], null);
				errorContext = ocd.getID();
				ExtendedObjectClassDefinition eocd = null;
				try {
					eocd = emti.getObjectClassDefinition(ocd.getID(), null);
				} catch (Exception e1) {
					ocdContent.comment = e1.getClass().getName() + " " + e1.getMessage() + " ";
				}
				
				AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
				try{
					rb = ResourceBundleUtil.getBundle(locale, b.adapt(BundleWiring.class).getClassLoader());
				} catch(MissingResourceException e) {
					ocdContent.comment += "Bundle " + b.getSymbolicName() + " does not have a resource bundle content.Language";
				    rb = new EmptyResourceBundle();
				}
				
				String description = ocd.getDescription();
				if(description != null && !description.isEmpty()) {
					String translated = LanguageUtil.get(rb, description);
					if(!translated.equals(description)) {
						description = translated;
					}
				}
				
				ocdContent.id = ocd.getID();
				ocdContent.name = LanguageUtil.get(rb, ocd.getName());
				ocdContent.description = description;
				ocdContent.bundle = b.getSymbolicName();
				ocdContent.scope = "no-scope-given";
				ocdContent.localizedScope = "no scope given";
				ocdContent.category = "third-party"; // best guess as default. Not sure if it's accurate, but looks good
				Set<String> extensionUris = Collections.emptySet();
				if(eocd != null) extensionUris = eocd.getExtensionUris();

				for (String extention : extensionUris) {
					String category = eocd.getExtensionAttributes(extention).get("category");
					String scope = eocd.getExtensionAttributes(extention).get("scope");

					if(category != null) {
						ocdContent.category = category;
						if(!category.isEmpty())
							ocdContent.localizedCategory = LanguageUtil.get(cfgAdminRb, "category." + category);
							if(ocdContent.localizedCategory.startsWith("category."))
								ocdContent.localizedCategory = LanguageUtil.get(rb, "category." + category);
					} else {
						ocdContent.localizedCategory = LanguageUtil.get(cfgAdminRb, "category.third-party");
					}
					if(scope != null) {
						ocdContent.scope = scope;
						ocdContent.localizedScope = LanguageUtil.get(cfgAdminRb, "scope." + scope);
					}
					
					ocdContent.learnMessageKey = eocd.getExtensionAttributes(extention).get("liferayLearnMessageKey");
					ocdContent.learnMessageResource = eocd.getExtensionAttributes(extention).get("liferayLearnMessageResource");
				}
				
				for (int j=0; j< ads.length; j++) {
					ADContent adContent = new ADContent();
					ocdContent.ads.add(adContent);
					
					adContent.id = ads[j].getID();
					adContent.name = LanguageUtil.get(rb, ads[j].getName());
					adContent.description = LanguageUtil.get(rb, ads[j].getDescription());
					adContent.deflts = ads[j].getDefaultValue();
					adContent.resolveType(ads[j]);
					adContent.resolveOptions(ads[j], rb);
				}
			} catch (Exception e) {
				e.printStackTrace();
				ocdContent.comment = e.getClass().getName() 
						+ " " 
						+ e.getMessage() 
						+ " in context " 
						+ errorContext;
			}
        }
        return result;
	}
}
