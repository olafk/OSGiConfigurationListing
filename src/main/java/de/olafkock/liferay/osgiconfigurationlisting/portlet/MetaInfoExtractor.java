package de.olafkock.liferay.osgiconfigurationlisting.portlet;

import com.liferay.portal.configuration.metatype.definitions.ExtendedMetaTypeInformation;
import com.liferay.portal.configuration.metatype.definitions.ExtendedMetaTypeService;
import com.liferay.portal.configuration.metatype.definitions.ExtendedObjectClassDefinition;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

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

		for (Bundle bundle : bundles) {
			result.addAll(extractOCD(bundle, ems, locale));
		}
		
		return result;
	}
	
	public List<OCDContent> extractOCD(Bundle b, ExtendedMetaTypeService ems, Locale locale) {
		LinkedList<OCDContent> result = new LinkedList<OCDContent>();
		MetaTypeService mts = BundleActivator.mts;
	    MetaTypeInformation mti = mts.getMetaTypeInformation(b);
	    ExtendedMetaTypeInformation emti = ems.getMetaTypeInformation(b);

	    String [] pids = mti.getPids();
	    if(pids == null || pids.length == 0) {
	    	return result;
	    }
        
	    for (int i=0; i< pids.length; i++) {
            ObjectClassDefinition ocd = mti.getObjectClassDefinition(pids[i], null);
            ExtendedObjectClassDefinition eocd = emti.getObjectClassDefinition(ocd.getID(), null);
            
            AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
            OCDContent ocdContent = new OCDContent();
            result.add(ocdContent);
            ResourceBundle rb;
            try{
            	rb = ResourceBundleUtil.getBundle(locale, b.adapt(BundleWiring.class).getClassLoader());
            } catch(MissingResourceException e) {
            	ocdContent.comment = "Bundle " + b.getSymbolicName() + " does not have a resource bundle content.Language";
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
            Set<String> extensionUris = eocd.getExtensionUris();
            for (String extention : extensionUris) {
				String category = eocd.getExtensionAttributes(extention).get("category");
				String scope = eocd.getExtensionAttributes(extention).get("scope");
				if(category != null) {
					ocdContent.category = category;
				}
				if(scope != null) {
					ocdContent.scope = scope;
				}
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
        }
        return result;
	}
}
