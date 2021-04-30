package de.olafkock.liferay.osgiconfigurationlisting.portlet;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

import de.olafkock.liferay.osgiconfigurationlisting.BundleActivator;
import de.olafkock.liferay.osgiconfigurationlisting.constants.OSGiConfigurationListingPortletKeys;

/**
 * Simple solution (starting point) to generate documentation from OSGi's OCD,
 * as used in Liferay's configuration.
 * So far, the OSGi configuration was not documented in a single place, like its
 * predecessor, portal.properties, is. This is the starting point for a generator
 * to create such a document.
 * 
 * TODO:
 * * Extract category, scope, default values 
 * * Use proper Services (There are Liferay Services that are wrapping 
 * ExtendedObjectClassDefinition and others, e.g. in ConfigurationModelRetrieverImpl)
 * 
 * @author olaf.kock@liferay.com
 */
@Component(
	immediate = true,
	property = {
		"com.liferay.portlet.display-category=category.sample",
		"com.liferay.portlet.header-portlet-css=/css/main.css",
		"com.liferay.portlet.instanceable=true",
		"javax.portlet.display-name=MetaLister",
		"javax.portlet.init-param.template-path=/",
		"javax.portlet.init-param.view-template=/view.jsp",
		"javax.portlet.name=" + OSGiConfigurationListingPortletKeys.METALISTER,
		"javax.portlet.resource-bundle=content.Language",
		"javax.portlet.security-role-ref=power-user,user"
	},
	service = Portlet.class
)
public class OSGiConfigurationListingPortlet extends MVCPortlet {
	
	@Override
	public void doView(RenderRequest request, RenderResponse response)
			throws IOException, PortletException {
	
		BundleContext bc = BundleActivator.bundleContext;
		MetaTypeService mts = BundleActivator.mts;
		Bundle[] bundles = bc.getBundles();

		super.doView(request, response);

		PrintWriter writer = response.getWriter();
		writer.println( "<h1>"
				+ "OSGi configuration info for "
				+ ReleaseInfo.getReleaseInfo()
				+ "</h1>" 
		);
		for (Bundle bundle : bundles) {
			printMetaTypes(writer, mts, bundle);
		}
		
	}
	
	
	@Override
	public void serveResource(ResourceRequest resourceRequest, ResourceResponse resourceResponse)
			throws IOException, PortletException {
		BundleContext bc = BundleActivator.bundleContext;
		MetaTypeService mts = BundleActivator.mts;
		Bundle[] bundles = bc.getBundles();

		resourceResponse.setContentType("text/html");

		PrintWriter writer = resourceResponse.getWriter();
		
		writer.println("<html><head><title>"
				+ "OSGi configuration info for "
				+ ReleaseInfo.getReleaseInfo()
				+ "</title>\n"
				+ "<generator>"
				+ "<style>"
				+ "td { border: 1px solid grey; vertical-align:top; }\n"
				+ "td:first-child { font-weight: bold; }\n"
				+ "</style>\n"
				+ "</head>"
				+ "<body>"
				+ "<h1>"
				+ "OSGi configuration info for "
				+ ReleaseInfo.getReleaseInfo()
				+ "</h1>"
				);
		for (Bundle bundle : bundles) {
			printMetaTypes(writer, mts, bundle);
		}
		
		writer.println("\n</body></html>");
	}
	
	/**
	 * adapted from http://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.metatype.html
	 * @param out 
	 * @param mts
	 * @param b
	 */
	
	void printMetaTypes(PrintWriter out, MetaTypeService mts,Bundle b) {
	    MetaTypeInformation mti = mts.getMetaTypeInformation(b);
	    String [] pids = mti.getPids();
	    if(pids == null || pids.length == 0) {
	    	return;
	    }
        for (int i=0; i< pids.length; i++) {
            ObjectClassDefinition ocd = mti.getObjectClassDefinition(pids[i], null);
            AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
            ResourceBundle rb;
            try{
            	rb = ResourceBundleUtil.getBundle(Locale.ENGLISH, b.adapt(BundleWiring.class).getClassLoader());
            } catch(MissingResourceException e) {
            	out.println("Bundle " + b.getSymbolicName() + " does not have a resource bundle content.Language");
                rb = new EmptyResourceBundle();
            }
            String name = ocd.getName();
            String description = ocd.getDescription();
            if(description == null || description.isEmpty()) {
	            if(name.endsWith("-configuration-name")) {
	            	String key = name.substring(0, name.length()-19) + "-description";
	            	String translated = LanguageUtil.get(rb, key);
	            	if(!translated.equals(key)) {
	            		description = translated;
	            	}
	            }
	            if((description == null || description.isEmpty()) && name.endsWith("-name")) {
	            	String key = name.substring(0, name.length()-5) + "-description";
	            	String translated = LanguageUtil.get(rb, key);
	            	if(!translated.equals(key)) {
	            		description = translated;
	            	} else {
	            		description = "<i>please contribute</i>";
	            	}
	            } else {
            		description = "<i>please contribute</i>";
            	}
            } else {
            	String translated = LanguageUtil.get(rb, description);
            	if(!translated.equals(description)) {
            		description = translated;
            	}
            }
            String ocdid = ocd.getID();
            out.println("<h2>"
            		+ LanguageUtil.get(rb, name) 
					+ "</h2>"
					+ "<p>"
					+ "Id: " 
					+ ocdid
					+ "<br/>\n"
					+ "Bundle: "
					+ b.getSymbolicName()
					+ "<br/>\n"
					+ "Description: " 
					+ description 
					+ "</p>");
			out.println("<table>");
            for (int j=0; j< ads.length; j++) {
                String adsName = LanguageUtil.get(rb, ads[j].getName());
				String adsId = ads[j].getID();
				String adsDescription = LanguageUtil.get(rb, ads[j].getDescription());
				if(adsDescription == null) adsDescription = "<i>please contribute</i>";
				out.println("<tr>"
                		+ "<td>" + adsName + "</td>" 
                		+ "<td>" + adsId + "</td>" 
                		+ "<td>" + adsDescription + " </td>"
                		+ "</tr>"
                		);
            }
            out.println("</table>");
        }
	}
}