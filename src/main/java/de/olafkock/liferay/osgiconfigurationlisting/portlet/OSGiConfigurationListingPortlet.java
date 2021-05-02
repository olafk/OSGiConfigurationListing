package de.olafkock.liferay.osgiconfigurationlisting.portlet;

import com.liferay.petra.string.StringUtil;
import com.liferay.portal.configuration.metatype.definitions.ExtendedMetaTypeService;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.util.ReleaseInfo;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.SortedSet;

import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

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
 * * Use proper Services (There are (internal) Liferay Services that are wrapping 
 * ExtendedObjectClassDefinition and others, e.g. in ConfigurationModelRetrieverImpl)
 * 
 * @author Olaf Kock
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
	
	@Reference
	ExtendedMetaTypeService _extendedMetaTypeService;
	
	MetaInfoExtractor metaInfoExtractor = new MetaInfoExtractor();
	
	@Override
	public void doView(RenderRequest request, RenderResponse response)
			throws IOException, PortletException {
	
		super.doView(request, response);

		PrintWriter writer = response.getWriter();
		writer.println( "<h1>"
				+ "Updated OSGi configuration info for "
				+ ReleaseInfo.getReleaseInfo()
				+ "</h1>" 
		);

		SortedSet<OCDContent> ocdContents = metaInfoExtractor.extractOCD(_extendedMetaTypeService);
		printToc(writer, ocdContents);
		printContent(writer, ocdContents);
	}
	
	
	@Override
	public void serveResource(ResourceRequest resourceRequest, ResourceResponse resourceResponse)
			throws IOException, PortletException {

		resourceResponse.setContentType("text/html");
		PrintWriter writer = resourceResponse.getWriter();
		
		writer.println("<html><head><title>"
				+ "OSGi configuration info for "
				+ ReleaseInfo.getReleaseInfo()
				+ "</title>\n"
				+ "<style>"
				+ "td { border: 1px solid grey; vertical-align:top; padding-right:1em; }\n"
				+ "tr.attributename  { font-weight:bold; }\n"
				+ "</style>\n"
				+ "</head>"
				+ "<body>"
				+ "<h1>"
				+ "OSGi configuration info for "
				+ ReleaseInfo.getReleaseInfo()
				+ "</h1>"
				);
		SortedSet<OCDContent> ocdContents = metaInfoExtractor.extractOCD(_extendedMetaTypeService);

		printToc(writer, ocdContents);
		printContent(writer, ocdContents);
		
		writer.println("\n</body></html>");
	}

	void printToc(PrintWriter out, SortedSet<OCDContent> ocdContents) {
		String prevScope = "";
		String prevCategory = "";
		boolean categoryListOpen = false;
		
		for (OCDContent ocdContent : ocdContents) {
			if(! ocdContent.scope.equals(prevScope)) {
				if(categoryListOpen) {
					out.print("</ul>");
				}
				out.println("<h2>Scope: <a href=\"#scope-" 
						+ ocdContent.scope 
						+ "\">"
						+ ocdContent.scope
						+ "</a>"
						+ "</h2>"
						);
				out.println("<ul>");
				categoryListOpen = true;
			}
			
			if(! ocdContent.category.equals(prevCategory)) {
				out.println("<li><a href=\"#scope-" 
						+ ocdContent.scope
						+ "-"
						+ ocdContent.category
						+ "\">"
						+ (ocdContent.category.isEmpty()? "! <i>empty</i> !" : ocdContent.category)
						+ "</a>"
						+ "</li>"
						);
			}
			
			prevScope = ocdContent.scope;
			prevCategory = ocdContent.category;
		}
		if(categoryListOpen) {
			out.print("</ul>");
		}
	}
	
	
	
	void printContent(PrintWriter out, SortedSet<OCDContent> ocdContents) {
		String prevScope = "";
		String prevCategory = "";
		
		for (OCDContent ocdContent : ocdContents) {
	
			if(! ocdContent.scope.equals(prevScope)) {
				out.println("<a name=\"scope-" + ocdContent.scope + "\"></a>");
				out.println("<h2>Scope: " + ocdContent.scope + "</h2>");
			}
			
			if(! ocdContent.category.equals(prevCategory)) {
				out.println("<a name=\"scope-" + ocdContent.scope + "-" + ocdContent.category + "\"></a>");
				out.println("<h3>Category: " + ocdContent.category + "</h3>");
			}
			
			
			prevScope = ocdContent.scope;
			prevCategory = ocdContent.category;
			
	        String description = ocdContent.description;
	        if(description == null || description.isEmpty()) {
	       		description = "<i>please contribute</i>";
	        }
	        String ocdid = ocdContent.id;
	        out.println("<h4>"
	        		+ ocdContent.name 
					+ "</h4>"
					+ "<p>"
					+ "<strong>Id:</strong> " 
					+ ocdContent.id
					+ "<br/>\n"
					+ "<strong>Bundle:</strong> "
					+ ocdContent.bundle
					+ "<br/>\n"
					+ "<strong>Description:</strong> " 
					+ description 
					+ "<br/>\n"
					+ "<strong>Category:</strong> "
					+ ocdContent.category
					+ "<br/>\n"
					+ "<strong>Scope:</strong> "
					+ ocdContent.scope
			);
	        if(ocdContent.comment != null) {
	            out.println(
	    			"<br/>\n"
	    			+ "Comment: "
	    			+ ocdContent.comment
	            );
	        }
	        out.println("</p>");
			out.println("<table>");
			for (ADContent adContent : ocdContent.ads) {
				String adDescription = adContent.description;
				String[] deflts = adContent.deflts;
				String deflt = (deflts == null) ? "<i>null</i>" : StringUtil.merge(deflts, "<br/>");
				if(adDescription == null || adDescription.isEmpty()) {
					adDescription = "<i>please contribute</i>";
				}
				out.println("<tr class=\"attributename\"><td colspan=\"2\">" + adContent.name + "</td></tr>" 
	            		+ "<tr><td>Id</td><td>" + adContent.id + "</td></tr>" 
	            		+ "<tr><td>Description</td><td>" + adDescription + " </td></tr>"
	            		+ "<tr><td>Default</td><td>" + deflt + "</td></tr>" 
	            		+ "<tr><td>Type</td><td>" + adContent.type + adContent.cardinality + "</td></tr>" 
	            		+ ""
	            );
			}
	        out.println("</table>");
		}
	}

	
}