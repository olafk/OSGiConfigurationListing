package de.olafkock.liferay.osgiconfigurationlisting.portlet;

import com.liferay.configuration.admin.category.ConfigurationCategory;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

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
	

	@Reference( cardinality = ReferenceCardinality.MULTIPLE,
		    policyOption = ReferencePolicyOption.GREEDY,
		    unbind = "doUnRegister" )
	void doRegister(ConfigurationCategory configurationCategory) {
		configurationCategories.add(configurationCategory);
	}
	
	void doUnRegister(ConfigurationCategory configurationCategory) {
		configurationCategories.remove(configurationCategory);
	}

	private List<ConfigurationCategory> configurationCategories = new LinkedList<ConfigurationCategory>();
	MetaInfoExtractor metaInfoExtractor = new MetaInfoExtractor();
	
	@Override
	public void doView(RenderRequest renderRequest, RenderResponse response)
			throws IOException, PortletException {
		ThemeDisplay themeDisplay = (ThemeDisplay) renderRequest.getAttribute(WebKeys.THEME_DISPLAY);
		HttpServletRequest request = PortalUtil.getHttpServletRequest(renderRequest);
		String[] installedPatches = PatcherUtil.getInstalledPatches();

		super.doView(renderRequest, response);

		PrintWriter writer = response.getWriter();
		writer.println("<style>");
		writer.println(getStyleSheet(".portlet-boundary_" + OSGiConfigurationListingPortletKeys.METALISTER + "_"));
		writer.println("</style>");
		
		writer.println( "<h1>"
				+ LanguageUtil.format(request, "report.title", ReleaseInfo.getReleaseInfo())
				+ "</h1>" 
				+ LanguageUtil.format(request, "report.patchlevel", (installedPatches != null ? StringUtil.merge(installedPatches, ", ") : "-"))
		);
		
		SortedSet<OCDContent> ocdContents = metaInfoExtractor.extractOCD(_extendedMetaTypeService, themeDisplay.getLocale());
		printToc(writer, ocdContents, request);
		writer.println("<hr/>");
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
		writer.println("<!DOCTYPE html><html><head><title>"
				+ LanguageUtil.format(request, "report.title", ReleaseInfo.getReleaseInfo())
				+ "</title>\n"
				+ "<style>\n"
				+ "body { font-family: sans-serif; }"
				+ "button { display:none; }"
				+ getStyleSheet("")
				+ "</style>\n"
				+ "</head>"
				+ "<body>"
				+ "<h1>"
				+ LanguageUtil.format(request, "report.title", ReleaseInfo.getReleaseInfo())
				+ "</h1>"
				+ LanguageUtil.format(request, "report.patchlevel", (installedPatches != null ? StringUtil.merge(installedPatches, ", ") : "-"))
				);
		SortedSet<OCDContent> ocdContents = metaInfoExtractor.extractOCD(_extendedMetaTypeService, themeDisplay.getLocale());

		try {
			printToc(writer, ocdContents, request);
			printContent(writer, ocdContents, request);
		} catch (Exception e) {
			e.printStackTrace(writer);
		}
		
		writer.println("\n</body></html>");
	}
	
	private String getStyleSheet(String scope) {
		scope += " ";
		return 
			  scope + "td { border: 1px solid grey; vertical-align:top; padding-right:1em; }\n"
			+ scope + "tr.attributename  { font-weight:bold; }\n"
			+ scope + ".osgi-collapsed { height:0px; overflow: hidden; transition: height 1000ms ease-in-out; }"
			+ scope + ".osgi-open { height:auto; overflow: auto; transition: height 3000ms ease-in-out; }"
			+ scope + "h2 { margin-left: 0.5em; margin-top: 1em; }"
			+ scope + "h3 { margin-left: 1em; margin-top: 1em; }"
			+ scope + "h4 { margin-left: 1.5em; margin-top: 1em; }"
			+ scope + "h5 { margin-left: 2em; margin-top: 1em; }"
			+ scope + "p.ocdcontent { margin-left: 1.5em; }"
			+ scope + "table.adcontent { margin-left: 1.5em; }"
			+ scope + "span.requiredContributions { margin-left:3em; }"
			;	
	}

	void printToc(PrintWriter out, SortedSet<OCDContent> ocdContents, HttpServletRequest request) {
		HashMap<String, String> categoryTranslations = new HashMap<String, String>();

		
		Collection<String> sections = getSections();
		Collection<String> scopes = getScopes(ocdContents);
		for(String section: sections) {
			Collection<String> categoryKeys = getCategoryKeys(section);
			for(String category: categoryKeys) {
				Collection<OCDContent> categoryOCDContent = getCategoryOCDContent(ocdContents, category);
				for(OCDContent ocdContent : categoryOCDContent ) {
					categoryTranslations.put(ocdContent.category, ocdContent.localizedCategory);
				}
			}
		}
		
		for(String scope : scopes) {
			int totalL1 = 0;
			int requiredL1 = 0;
			String elementName = "scope-" + scope;
			out.println("<h1>"
					+ "<a href=\"#" 
							+ elementName
							+ "\">"
					+ _localizedScopes.get(scope)
					+ "</a> <button data-toggle=\"collapse\" data-target=\""
					+ "#" + elementName + "-collapsible"
					+ "\">open/close</button>"
					+ "</h1>"
					+ "<div id=\""
					+ elementName + "-collapsible"
					+ "\""
					+ " class=\"osgi-open collapse\""
					+ ">"
				);
			for(String section: sections) {
				int totalL2 = 0;
				int requiredL2 = 0;
				elementName = "section-" + scope + "-" + section;
				out.println("<h2>"
						+ "<a href=\"#"
						+ elementName
						+ "\">"
						+ section
						+ "</a> "
						+ "<button data-toggle=\"collapse\" data-target=\""
						+ "#" + elementName + "-collapsible"
						+ "\">open/close</button>"
						+ "</h2>"
						+ "<div id=\""
						+ elementName + "-collapsible"
						+ "\""
						+ "class=\"osgi-open collapse\""
						+ ">"
						);
				Collection<String> categoryKeys = getCategoryKeys(section);
				for (String category : categoryKeys) {
					int requiredL3 = 0;
					int totalL3 = 0;
					elementName = "section-" + scope + "-" + section + "-" + category.replace(' ', '-');
					out.println("<h3>"
							+ "<a href=\"#"
							+ elementName
							+ "\">"
							+ (categoryTranslations.get(category)==null? category : categoryTranslations.get(category))
							+ "</a> "
							+ "<button data-toggle=\"collapse\" data-target=\""
							+ "#" + elementName + "-collapsible"
							+ "\">open/close</button>"
							+ "</h2>"
							+ "<div id=\""
							+ elementName + "-collapsible"
							+ "\""
							+ "class=\"osgi-open collapse\""
							+ ">"
							+ "</h3>"
							+ "<ul>"
							);
					Collection<OCDContent> categoryOCDContent = getCategoryOCDContent(ocdContents, category);
					for (OCDContent ocdContent : categoryOCDContent) {
						elementName = "ocd-" + scope + "-" + section + "-" + category.replace(' ', '-') + "-" + ocdContent.name.replace(' ', '-');
						out.println("<li>"
								+ "<a href=\"#"
								+ elementName
								+ "\">"
								+ ocdContent.name
								+ "</a>"
								+ "</li>"
								);
						
					}
					if(categoryOCDContent.isEmpty()) {
						out.println("<li>empty</li>");
					}
					out.println("</ul>");
					if(totalL3 > 0) {
						out.println(requiredL3 != 0 
							? "L3: <span style=\"color:red;\">" + requiredL3 + " contribution(s) needed, for " + totalL3 + " total entries.</span>" 
							: "L3: <span>No contributions required. Good job!");
					}
					out.println("</div>");
					requiredL2 += requiredL3;
					totalL2 += totalL3;
				}
				out.println(requiredL2 != 0 
						? "L2: <span style=\"color:red;\">" + requiredL2 + " contribution(s) needed, for " + totalL2 + " total entries.</span>" 
						: "L2: <span>No contributions required. Good job!");
				out.println("</div>");
				requiredL1 += requiredL2;
				totalL1 += totalL2;
			}
			out.println(requiredL1 != 0 
					? "L1: <span style=\"color:red;\">" + requiredL1 + " contribution(s) needed, for " + totalL1 + " total entries.</span>" 
					: "L1: <span>No contributions required. Good job!");
			out.println("</div>");
		}
	}

	void printContent(PrintWriter out, SortedSet<OCDContent> ocdContents, HttpServletRequest request) {
        final String PLEASE_CONTRIBUTE = LanguageUtil.get(request, "cta-please-contribute");

		HashMap<String, String> categoryTranslations = new HashMap<String, String>();
		
		for(OCDContent ocdContent : ocdContents ) {
			categoryTranslations.put(ocdContent.category, ocdContent.localizedCategory);
		}
		
		Collection<String> sections = getSections();
		Collection<String> scopes = getScopes(ocdContents);
		for(String scope : scopes) {
			String elementName = "scope-" + scope;
			out.println("<h1 id=\""
							+ elementName
							+ "\">"
					+ _localizedScopes.get(scope)
					+ "</h1>"
				);
			for(String section: sections) {
				elementName = "section-" + scope + "-" + section;
				out.println("<h2 id=\""
						+ elementName
						+ "\">"
						+ section
						+ "</h2>"
						);

				Collection<String> categoryKeys = getCategoryKeys(section);
				for (String category : categoryKeys) {
					elementName = "category-" + scope + "-" + section + "-" + category.replace(' ', '-');
					out.println("<h3 id=\""
							+ elementName
							+ "\">"
							+ (categoryTranslations.get(category)==null? category : categoryTranslations.get(category))
							+ "</h3>"
							);
					Collection<OCDContent> categoryOCDContent = getCategoryOCDContent(ocdContents, category);
					for (OCDContent ocdContent : categoryOCDContent) {
						elementName = "ocd-" + scope + "-" + section + "-" + category.replace(' ', '-') + "-" + ocdContent.name.replace(' ', '-');
						out.println("<h4 id=\""
								+ elementName
								+ "\">"
								+ ocdContent.name
								+ "</h4>"
						);
				        String description = ocdContent.description;
						if(description == null || description.isEmpty() || description.equals("null")) {
				       		description = "<i>"	+ PLEASE_CONTRIBUTE	+ "</i>";
				        }
				        out.println( "<p class=\"ocdcontent\">"
				        		+ ocdLine(request, "ocd.id", ocdContent.id)
				        		+ ocdLine(request, "ocd.bundle", ocdContent.bundle)
				        		+ ocdLine(request, "ocd.description", description)
				        		+ ocdLine(request, "ocd.category", ocdContent.category)
				        		+ ocdLine(request, "ocd.scope", ocdContent.id)
				        		+ ocdLine(request, "ocd.liferayLearn", 
				        				(ocdContent.learnMessageResource == null || ocdContent.learnMessageResource.length()==0
										? PLEASE_CONTRIBUTE 
										: ocdContent.learnMessageResource + " / " + ocdContent.learnMessageKey))
						);

				        if(ocdContent.comment != null && ocdContent.comment.trim().length()>0 ) {
				            out.println(
				    			"<br/>\n"
			   					+ LanguageUtil.get(request, "ocd.comment")
			   					+ " "
				    			+ ocdContent.comment
				            );
				        }
				        out.println("</p>");
						out.println("<table class=\"adcontent\">");
						for (ADContent adContent : ocdContent.ads) {
							String adDescription = adContent.description;
							String[] deflts = adContent.deflts;
							String deflt = (deflts == null) ? "<i>null</i>" : StringUtil.merge(deflts, "<br/>");
							if(adDescription == null || adDescription.isEmpty()) {
								adDescription = "<i>"
										+ PLEASE_CONTRIBUTE
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
					if(categoryOCDContent.isEmpty()) {
						out.println("<p class=\"ocdcontent\">empty</p>");
					}
				}
			}
		}
	}

	Collection<String> getLocalizedScopes(Collection<OCDContent> ocdContents) {
		Set<String> result = new HashSet<String>();
		for (OCDContent ocdContent : ocdContents) {
			result.add(ocdContent.localizedScope);
		}
		return result;
	}
	
	// ugly side effect: Remembers translations and needs to be called before translations are used.
	Collection<String> getScopes(Collection<OCDContent> ocdContents) {
		Set<String> result = new HashSet<String>();
		for (OCDContent ocdContent : ocdContents) {
			result.add(ocdContent.scope);
			_localizedScopes.put(ocdContent.scope, ocdContent.localizedScope);
		}
		return result;
	}
	
	Collection<String> getSections() {
		SortedSet<String> result = new TreeSet<String>();
		for (ConfigurationCategory configurationCategory : configurationCategories) {
			result.add(configurationCategory.getCategorySection());
		}
		return result;
	}
	
	Collection<String> getCategoryKeys(String section) {
		SortedSet<String> result = new TreeSet<String>();
		for (ConfigurationCategory configurationCategory : configurationCategories) {
			if(configurationCategory.getCategorySection().equals(section)) {
				result.add(configurationCategory.getCategoryKey());
			}
		}
		return result;
	}
	
	Collection<OCDContent> getCategoryOCDContent(Collection<OCDContent> ocdContents, String categoryKey) {
		LinkedList<OCDContent> result = new LinkedList<OCDContent>();
		for (OCDContent ocdContent : ocdContents) {
			if(categoryKey.equals(ocdContent.category)) {
				result.add(ocdContent);
			}
		}
		return result;
	}
	
	String ocdLine(HttpServletRequest request, String key, String value) {
		return "<strong>"
				+ LanguageUtil.get(request, key)
				+ "</strong> " 
				+ value
				+ "<br/>\n";
	}
	
	
	
	HashMap<String, String> _localizedScopes = new HashMap<String, String>();
	
}