# OSGiConfigurationListing

Generator for documentation for all @OCD elements found on the classpath of all enumerated bundles.

[Public announcement is here](https://liferay.dev/blogs/-/blogs/exploring-documentation).

The current implementation is a starting point, and kept deliberately simple (single class) and ugly (HTML tables). 

As it needs to run within Liferay (to get access to all configuration of the running system), it's implemented as a portlet. You can see the documentation on a page by just displaying the portlet, or you can generate a HTML-only document through a link on top of the output of this portlet.

## TO DO

* Implement with proper services instead of low-level access, as they can be seen in ConfigurationModelRetrieverImpl
* Prettify code (from explorational style)
* Prettify output
* Display available options for configuration values in places where they're enumerated in their AD.
* [Include in publication process](https://issues.liferay.com/browse/LRDOCS-9384)