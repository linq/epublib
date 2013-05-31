package nl.siegmann.epublib.domain;

import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.service.MediatypeService;
import nl.siegmann.epublib.util.StringUtil;

import java.io.Serializable;
import java.util.*;

/**
 * All the resources that make up the book.
 * XHTML files, images and epub xml documents must be here.
 * 
 * @author paul
 *
 */
public class Resources implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2450876953383871451L;
	private static final String IMAGE_PREFIX = "image_";
	private static final String ITEM_PREFIX = "item_";
	private int lastId = 1;
	
	private Map<String, Resource> resources = new HashMap<String, Resource>();
	
	/**
	 * Adds a resource to the resources.
	 * 
	 * Fixes the resources id and href if necessary.
	 * 
	 * @param resource
	 * @return
	 */
	public Resource add(Resource resource) {
		fixResourceHref(resource);
		fixResourceId(resource);
		this.resources.put(resource.getHref(), resource);
		return resource;
	}

	/**
	 * Checks the id of the given resource and changes to a unique identifier if it isn't one already.
	 * 
	 * @param resource
	 */
	public void fixResourceId(Resource resource) {
		String  resourceId = resource.getId();
		
		// first try and create a unique id based on the resource's href
		if (StringUtil.isBlank(resource.getId())) {
			resourceId = StringUtil.substringBeforeLast(resource.getHref(), '.');
			resourceId = StringUtil.substringAfterLast(resourceId, '/');
		}
		
		resourceId = makeValidId(resourceId, resource);
		
		// check if the id is unique. if not: create one from scratch
		if (StringUtil.isBlank(resourceId) || containsId(resourceId)) {
			resourceId = createUniqueResourceId(resource);
		}
		resource.setId(resourceId);
	}

	/**
	 * Check if the id is a valid identifier. if not: prepend with valid identifier
	 * 
	 * @param resource
	 * @return
	 */
	private String makeValidId(String resourceId, Resource resource) {
		if (StringUtil.isNotBlank(resourceId) && ! Character.isJavaIdentifierStart(resourceId.charAt(0))) {
			resourceId = getResourceItemPrefix(resource) + resourceId;
		}
		return resourceId;
	}
	
	private String getResourceItemPrefix(Resource resource) {
		String result;
		if (MediatypeService.isBitmapImage(resource.getMediaTypeProperty())) {
			result = IMAGE_PREFIX;
		} else {
			result = ITEM_PREFIX;
		}
		return result;
	}
	
	/**
	 * Creates a new resource id that is guarenteed to be unique for this set of Resources
	 * 
	 * @param resource
	 * @return
	 */
	private String createUniqueResourceId(Resource resource) {
		int counter = lastId;
		if (counter == Integer.MAX_VALUE) {
			if (resources.size() == Integer.MAX_VALUE) {
				throw new IllegalArgumentException("Resources contains " + Integer.MAX_VALUE + " elements: no new elements can be added");
			} else {
				counter = 1;
			}
		}
		String prefix = getResourceItemPrefix(resource);
		String result = prefix + counter;
		while (containsId(result)) {
			result = prefix + (++ counter);
		}
		lastId = counter;
		return result;
	}

	/**
	 * Whether the map of resources already contains a resource with the given id.
	 * 
	 * @param id
	 * @return
	 */
	public boolean containsId(String id) {
		if (StringUtil.isBlank(id)) {
			return false;
		}
		for (Resource resource: resources.values()) {
			if (id.equals(resource.getId())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Gets the resource with the given id.
	 * 
	 * @param id
	 * @return null if not found
	 */
	public Resource getById(String id) {
		if (StringUtil.isBlank(id)) {
			return null;
		}
		for (Resource resource: resources.values()) {
			if (id.equals(resource.getId())) {
				return resource;
			}
		}
		return null;
	}
	
	/**
	 * Remove the resource with the given href.
	 * 
	 * @param href
	 * @return the removed resource, null if not found
	 */
	public Resource remove(String href) {
		return resources.remove(href);
	}
	
	private void fixResourceHref(Resource resource) {
		if(StringUtil.isNotBlank(resource.getHref())
				&& ! resources.containsKey(resource.getHref())) {
			return;
		}
		if(StringUtil.isBlank(resource.getHref())) {
			if(resource.getMediaTypeProperty() == null) {
				throw new IllegalArgumentException("Resource must have either a MediaTypeProperties or a href");
			}
			int i = 1;
			String href = createHref(resource.getMediaTypeProperty(), i);
			while(resources.containsKey(href)) {
				href = createHref(resource.getMediaTypeProperty(), (++i));
			}
			resource.setHref(href);
		}
	}
	
	private String createHref(MediaTypeProperty mediaTypeProperty, int counter) {
		if(MediatypeService.isBitmapImage(mediaTypeProperty)) {
			return "image_" + counter + mediaTypeProperty.getDefaultExtension();
		} else {
			return "item_" + counter + mediaTypeProperty.getDefaultExtension();
		}
	}
	
	
	public boolean isEmpty() {
		return resources.isEmpty();
	}
	
	/**
	 * The number of resources
	 * @return
	 */
	public int size() {
		return resources.size();
	}
	
	/**
	 * The resources that make up this book.
	 * Resources can be xhtml pages, images, xml documents, etc.
	 * 
	 * @return
	 */
	public Map<String, Resource> getResourceMap() {
		return resources;
	}
	
	public Collection<Resource> getAll() {
		return resources.values();
	}
	
	
	/**
	 * Whether there exists a resource with the given href
	 * @param href
	 * @return
	 */
	public boolean containsByHref(String href) {
		if (StringUtil.isBlank(href)) {
			return false;
		}
		return resources.containsKey(StringUtil.substringBefore(href, Constants.FRAGMENT_SEPARATOR_CHAR));
	}
	
	/**
	 * Sets the collection of Resources to the given collection of resources
	 * 
	 * @param resources
	 */
	public void set(Collection<Resource> resources) {
		this.resources.clear();
		addAll(resources);
	}
	
	/**
	 * Adds all resources from the given Collection of resources to the existing collection.
	 * 
	 * @param resources
	 */
	public void addAll(Collection<Resource> resources) {
		for(Resource resource: resources) {
			fixResourceHref(resource);
			this.resources.put(resource.getHref(), resource);
		}
	}

	/**
	 * Sets the collection of Resources to the given collection of resources
	 * 
	 * @param resources A map with as keys the resources href and as values the Resources
	 */
	public void set(Map<String, Resource> resources) {
		this.resources = new HashMap<String, Resource>(resources);
	}
	
	
	/**
	 * First tries to find a resource with as id the given idOrHref, if that 
	 * fails it tries to find one with the idOrHref as href.
	 * 
	 * @param idOrHref
	 * @return
	 */
	public Resource getByIdOrHref(String idOrHref) {
		Resource resource = getById(idOrHref);
		if (resource == null) {
			resource = getByHref(idOrHref);
		}
		return resource;
	}
	
	
	/**
	 * Gets the resource with the given href.
	 * If the given href contains a fragmentId then that fragment id will be ignored.
	 * 
	 * @param href
	 * @return null if not found.
	 */
	public Resource getByHref(String href) {
		if (StringUtil.isBlank(href)) {
			return null;
		}
		href = StringUtil.substringBefore(href, Constants.FRAGMENT_SEPARATOR_CHAR);
		Resource result = resources.get(href);
		return result;
	}
	
	/**
	 * Gets the first resource (random order) with the give mediatype.
	 * 
	 * Useful for looking up the table of contents as it's supposed to be the only resource with NCX mediatype.
	 * 
	 * @param mediaTypeProperty
	 * @return
	 */
	public Resource findFirstResourceByMediaType(MediaTypeProperty mediaTypeProperty) {
		return findFirstResourceByMediaType(resources.values(), mediaTypeProperty);
	}
	
	/**
	 * Gets the first resource (random order) with the give mediatype.
	 * 
	 * Useful for looking up the table of contents as it's supposed to be the only resource with NCX mediatype.
	 * 
	 * @param mediaTypeProperty
	 * @return
	 */
	public static Resource findFirstResourceByMediaType(Collection<Resource> resources, MediaTypeProperty mediaTypeProperty) {
		for (Resource resource: resources) {
			if (resource.getMediaTypeProperty() == mediaTypeProperty) {
				return resource;
			}
		}
		return null;
	}

	/**
	 * All resources that have the given MediaType.
	 * 
	 * @param mediaTypeProperty
	 * @return
	 */
	public List<Resource> getResourcesByMediaType(MediaTypeProperty mediaTypeProperty) {
		List<Resource> result = new ArrayList<Resource>();
		if (mediaTypeProperty == null) {
			return result;
		}
		for (Resource resource: getAll()) {
			if (resource.getMediaTypeProperty() == mediaTypeProperty) {
				result.add(resource);
			}
		}
		return result;
	}

	/**
	 * All Resources that match any of the given list of MediaTypes
	 * 
	 * @param mediaTypePropertieses
	 * @return
	 */
	public List<Resource> getResourcesByMediaTypes(MediaTypeProperty[] mediaTypePropertieses) {
		List<Resource> result = new ArrayList<Resource>();
		if (mediaTypePropertieses == null) {
			return result;
		}
		
		// this is the fastest way of doing this according to 
		// http://stackoverflow.com/questions/1128723/in-java-how-can-i-test-if-an-array-contains-a-certain-value
		List<MediaTypeProperty> mediaTypesListProperties = Arrays.asList(mediaTypePropertieses);
		for (Resource resource: getAll()) {
			if (mediaTypesListProperties.contains(resource.getMediaTypeProperty())) {
				result.add(resource);
			}
		}
		return result;
	}


	public Collection<String> getAllHrefs() {
		return resources.keySet();
	}
}
