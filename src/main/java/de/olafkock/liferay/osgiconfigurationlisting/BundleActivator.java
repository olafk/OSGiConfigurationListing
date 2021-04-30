package de.olafkock.liferay.osgiconfigurationlisting;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.MetaTypeService;

public class BundleActivator implements org.osgi.framework.BundleActivator {

	@SuppressWarnings("unchecked")
	@Override
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
	    metatyperef = context.getServiceReference(MetaTypeService.class.getName());
	    mts = (MetaTypeService)context.getService(metatyperef);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		bundleContext = null;
	    context.ungetService(metatyperef);
	}

	public static BundleContext bundleContext = null;
	public static MetaTypeService mts = null;

	@SuppressWarnings("rawtypes")
	private ServiceReference metatyperef = null;
}
