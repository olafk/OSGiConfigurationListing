package de.olafkock.liferay.osgiconfigurationlisting.portlet;

import com.liferay.petra.string.StringUtil;
import com.liferay.portal.configuration.metatype.definitions.ExtendedMetaTypeService;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.patcher.PatcherUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.kernel.util.WebKeys;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.SortedSet;

import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.servlet.http.HttpServletRequest;

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
	public void doView(RenderRequest renderRequest, RenderResponse response)
			throws IOException, PortletException {
		ThemeDisplay themeDisplay = (ThemeDisplay) renderRequest.getAttribute(WebKeys.THEME_DISPLAY);
		HttpServletRequest request = PortalUtil.getHttpServletRequest(renderRequest);
		String[] installedPatches = PatcherUtil.getInstalledPatches();

		super.doView(renderRequest, response);

		PrintWriter writer = response.getWriter();
		writer.println( "<h1>"
				+ LanguageUtil.format(request, "report.title", ReleaseInfo.getReleaseInfo())
				+ "</h1>" 
				+ LanguageUtil.format(request, "report.patchlevel", (installedPatches != null ? StringUtil.merge(installedPatches, ", ") : "-"))
		);
		
		SortedSet<OCDContent> ocdContents = metaInfoExtractor.extractOCD(_extendedMetaTypeService, themeDisplay.getLocale());
		printToc(writer, ocdContents, request);
		printContent(writer, ocdContents, request);
	}
	
	
	@Override
	public void serveResource(ResourceRequest resourceRequest, ResourceResponse resourceResponse)
			throws IOException, PortletException {
		ThemeDisplay themeDisplay = (ThemeDisplay) resourceRequest.getAttribute(WebKeys.THEME_DISPLAY);
		HttpServletRequest request = PortalUtil.getHttpServletRequest(resourceRequest);
		String[] installedPatches = PatcherUtil.getInstalledPatches();

		resourceResponse.setContentType("text/html");
		PrintWriter writer = resourceResponse.getWriter();
		writer.println("<html><head><title>"
				+ LanguageUtil.format(request, "report.title", ReleaseInfo.getReleaseInfo())
				+ "</title>\n"
				+ "<style>"
				+ "td { border: 1px solid grey; vertical-align:top; padding-right:1em; }\n"
				+ "tr.attributename  { font-weight:bold; }\n"
				+ "</style>\n"
				+ "</head>"
				+ "<body>"
				+ "<h1>"
				+ LanguageUtil.format(request, "report.title", ReleaseInfo.getReleaseInfo())
				+ "</h1>"
				+ LanguageUtil.format(request, "report.patchlevel", (installedPatches != null ? StringUtil.merge(installedPatches, ", ") : "-"))
				);
		SortedSet<OCDContent> ocdContents = metaInfoExtractor.extractOCD(_extendedMetaTypeService, themeDisplay.getLocale());

		printToc(writer, ocdContents, request);
		printContent(writer, ocdContents, request);
		
		writer.println("\n</body></html>");
	}

	void printToc(PrintWriter out, SortedSet<OCDContent> ocdContents, HttpServletRequest request) {
		
		String prevScope = "";
		String prevCategory = "";
		boolean categoryListOpen = false;
		HashMap<String, Integer> totals = new HashMap<String, Integer> ();
		HashMap<String, Integer> required = new HashMap<String, Integer> ();
		HashMap<String, Integer> subchapters = new HashMap<String, Integer> ();
		
		for(OCDContent ocdContent : ocdContents ) {
			String key = ocdContent.scope+"-"+ocdContent.category;
			updateMap(totals, key, getTotalCount(ocdContent));
			updateMap(required, key, getRequiredContributionsCount(ocdContent));
			updateMap(subchapters, key, 1);
		}
		
		for (OCDContent ocdContent : ocdContents) {
			if(! ocdContent.scope.equals(prevScope)) {
				if(categoryListOpen) {
					out.print("</ul>");
				}
				out.println("<h2><a href=\"#scope-" 
						+ ocdContent.scope 
						+ "\">"
						+ ocdContent.localizedScope
						+ "</a>"
						+ "</h2>"
						);
				out.println("<ul>");
				categoryListOpen = true;
			}

			if(! ocdContent.category.equals(prevCategory)) {
				int requiredCount = required.get(ocdContent.scope+"-"+ocdContent.category);
				int totalCount = totals.get(ocdContent.scope+"-"+ocdContent.category);
				int subCount = subchapters.get(ocdContent.scope+"-"+ocdContent.category);
				int percent = (int)(100.0*(totalCount-requiredCount)/totalCount);
				out.println("<li>"
						+ " <a href=\"#scope-" 
						+ ocdContent.scope
						+ "-"
						+ ocdContent.category
						+ "\">"
						+ (ocdContent.category.isEmpty()? "<i>"
								+ LanguageUtil.get(request, "report.empty")
								+ "</i>" : ocdContent.localizedCategory)
						+ "</a> ("
						+ (requiredCount != 0 
								? "<span style=\"color:red;\">" + requiredCount + " contributions needed, " 
								: "<span>")
						+ "<strong>"
						+ percent+"%</strong> of " 
						+ totalCount + " entries documented in " 
						+ subCount + " sub-entries</span>)"
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
	
	
	
	void printContent(PrintWriter out, SortedSet<OCDContent> ocdContents, HttpServletRequest request) {
		String prevScope = "";
		String prevCategory = "";
		
		for (OCDContent ocdContent : ocdContents) {
	
			if(! ocdContent.scope.equals(prevScope)) {
				out.println("<a name=\"scope-" + ocdContent.scope + "\"></a>");
				out.println("<h2>" + ocdContent.localizedScope + "</h2>");
			}
			
			if(! ocdContent.category.equals(prevCategory)) {
				out.println("<a name=\"scope-" + ocdContent.scope + "-" + ocdContent.category + "\"></a>");
				out.println("<h3>"
						+ LanguageUtil.get(request, "ocd.category")
						+ " "
						+ ocdContent.localizedCategory 
						+ " (" + ocdContent.category + ")</h3>");
			}
			
			prevScope = ocdContent.scope;
			prevCategory = ocdContent.category;
			
	        String description = ocdContent.description;
	        if(description == null || description.isEmpty()) {
	       		description = "<i>"
						+ LanguageUtil.get(request, "cta-please-contribute")
	       				+ "</i>";
	        }
	        out.println("<h4>"
	        		+ ocdContent.name 
					+ "</h4>"
					+ "<p>"
					+ "<strong>"
					+ LanguageUtil.get(request, "ocd.id")
					+ "</strong> " 
					+ ocdContent.id
					+ "<br/>\n"
					+ "<strong>"
					+ LanguageUtil.get(request, "ocd.bundle")
					+ "</strong> "
					+ ocdContent.bundle
					+ "<br/>\n"
					+ "<strong>"
					+ LanguageUtil.get(request, "ocd.description")
					+ "</strong> " 
					+ description 
					+ "<br/>\n"
					+ "<strong>"
					+ LanguageUtil.get(request, "ocd.category")
					+ "</strong> "
					+ ocdContent.category
					+ "<br/>\n"
					+ "<strong>"
					+ LanguageUtil.get(request, "ocd.scope")
					+ "</strong> "
					+ ocdContent.scope
			);
	        if(ocdContent.comment != null) {
	            out.println(
	    			"<br/>\n"
   					+ LanguageUtil.get(request, "ocd.comment")
   					+ " "
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
					adDescription = "<i>"
							+ LanguageUtil.get(request, "cta-please-contribute")
							+ "</i>";
				}
				out.println("<tr class=\"attributename\"><td colspan=\"2\">" + adContent.name + "</td></tr>" 
	            		+ "<tr><td>"
						+ LanguageUtil.get(request, "ad.id")
	            		+ "</td><td>" + adContent.id + "</td></tr>" 
	            		+ "<tr><td>"
						+ LanguageUtil.get(request, "ad.description")
	            		+ "</td><td>" + adDescription + " </td></tr>"
	            		+ "<tr><td>"
						+ LanguageUtil.get(request, "ad.default")
	            		+ "</td><td>" + deflt + "</td></tr>" 
	            		+ "<tr><td>"
						+ LanguageUtil.get(request, "ad.type")
	            		+ "</td><td>" + adContent.type + adContent.cardinality + "</td></tr>" 
	            		+ ""
	            );
				if(!adContent.options.isEmpty()) {
					out.println("<tr><td>"
							+ LanguageUtil.get(request, "ad.options")
							+ "</td><td><ul>");
					for (String option : adContent.options) {
						out.println("<li>" + option + "</li>");
					}
					out.println("</ul></td></tr>");
				}
			}
	        out.println("</table>");
		}
	}

	int getRequiredContributionsCount(OCDContent ocdContent) {
		int result = 0;
		if(ocdContent.description == null || ocdContent.description.isEmpty()) {
			result++;
		}
		for (ADContent adContent : ocdContent.ads) {
			if(adContent.description == null || adContent.description.isEmpty()) {
				result++;
			}
		}
		return result;
	}
	
	int getTotalCount(OCDContent ocdContent) {
		int result = ocdContent.ads.size()+1;
		return result;
	}

	void updateMap(HashMap<String, Integer> map, String key, int count) {
		Integer value = map.get(key);
		if(value == null) value = 0;
		value += count;
		map.put(key, value);
	}
	
}