# OSGiConfigurationListing

Generator for documentation for all @OCD elements found on the classpath of all enumerated bundles.

The current implementation is a starting point, and kept deliberately simple (single class) and ugly (HTML tables). 

As it needs to run within Liferay (to get access to all configuration of the running system), it's implemented as a portlet. You can see the documentation on a page by just displaying the portlet, or you can generate a HTML-only document through a link on top of the output of this portlet.

## TO DO

* Extract (and sort by) category (Liferay's ExtendedObjectClassDefinition)
* Extract (and sort by) scope (System, Instance)
* Implement with proper services instead of low-level access, as they can be seen in ConfigurationModelRetrieverImpl (also helps with accessing ExtendedObjectClassDefinition)

