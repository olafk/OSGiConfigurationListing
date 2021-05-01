package de.olafkock.liferay.osgiconfigurationlisting.portlet;

import com.liferay.petra.string.StringUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.util.ReleaseInfo;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;

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

		List<OCDContent> ocdContents = metaInfoExtractor.extractOCD();
		for (OCDContent ocdContent : ocdContents) {
			printContent(writer, ocdContent);
		}
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
				+ "<generator>"
				+ "<style>"
				+ "td { border: 1px solid grey; vertical-align:top; padding-right:1em; }\n"
				+ "tr.adname  { font-weight:bold; }\n"
				+ "</style>\n"
				+ "</head>"
				+ "<body>"
				+ "<h1>"
				+ "OSGi configuration info for "
				+ ReleaseInfo.getReleaseInfo()
				+ "</h1>"
				);
		List<OCDContent> ocdContents = metaInfoExtractor.extractOCD();
		for (OCDContent ocdContent : ocdContents) {
			printContent(writer, ocdContent);
		}
		
		writer.println("\n</body></html>");
	}

	void printContent(PrintWriter out, OCDContent ocdContent) {
        String description = ocdContent.description;
        if(description == null || description.isEmpty()) {
       		description = "<i>please contribute</i>";
        }
        String ocdid = ocdContent.id;
        out.println("<h2>"
        		+ ocdContent.name 
				+ "</h2>"
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
			out.println("<tr class=\"adname\"><td colspan=\"2\">" + adContent.name + "</td></tr>" 
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