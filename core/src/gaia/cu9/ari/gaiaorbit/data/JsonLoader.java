package gaia.cu9.ari.gaiaorbit.data;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.I18n;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.zip.DataFormatException;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonValue.ValueType;

/**
 * Implements the loading of scene graph nodes using libgdx's json library.
 * @author Toni Sagrista
 *
 * @param <T>
 */
public class JsonLoader<T extends SceneGraphNode> implements ISceneGraphNodeProvider {
    private static final String COMPONENTS_PACKAGE = "gaia.cu9.ari.gaiaorbit.scenegraph.component.";
    /** Params to skip in the normal processing **/
    private static final List<String> PARAM_SKIP = Arrays.asList("args", "impl");

    /** Contains all the files to be loaded by this loader **/
    private String[] filePaths;

    @Override
    public void initialize(Properties properties) {
	filePaths = properties.getProperty("files").split("\\s+");
    }

    @Override
    public List<? extends SceneGraphNode> loadObjects() {
	List<T> bodies = new ArrayList<T>();

	try {
	    JsonReader json = new JsonReader();
	    for (String filePath : filePaths) {
		JsonValue model = json.parse(FileLocator.getStream(filePath));
		JsonValue child = model.get("objects").child;
		while (child != null) {
		    String clazzName = child.getString("impl");

		    @SuppressWarnings("unchecked")
		    Class<Object> clazz = (Class<Object>) Class.forName(clazzName);

		    // Convert to object and add to list
		    T object = (T) convertJsonToObject(child, clazz);

		    // Only load and add if it display is activated
		    Method m = clazz.getMethod("initialize");
		    m.invoke(object);

		    bodies.add(object);

		    child = child.next;
		}
		EventManager.getInstance().post(Events.POST_NOTIFICATION, this.getClass().getSimpleName(), I18n.bundle.format("notif.nodeloader", model.size, filePath));
	    }

	} catch (Exception e) {
	    EventManager.getInstance().post(Events.JAVA_EXCEPTION, e);
	}

	return bodies;
    }

    /**
     * Converts the given {@link JsonValue} to a java object of the given {@link Class}.
     * @param json The {@link JsonValue} for the object to convert.
     * @param clazz The class of the object.
     * @return The java object of the given class.
     * @throws SecurityException 
     * @throws NoSuchMethodException 
     * @throws InvocationTargetException 
     * @throws IllegalArgumentException 
     * @throws IllegalAccessException 
     * @throws ClassNotFoundException 
     * @throws InstantiationException 
     */
    private Object convertJsonToObject(JsonValue json, Class<?> clazz) throws DataFormatException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {
	Object instance;
	try {
	    if (json.has("args")) {
		//Creator arguments
		JsonValue args = json.get("args");
		Class<?>[] argumentTypes = new Class<?>[args.size];
		Object[] arguments = new Object[args.size];
		for (int i = 0; i < args.size; i++) {
		    JsonValue arg = args.get(i);
		    argumentTypes[i] = getValueClass(arg);
		    arguments[i] = getValue(arg);
		}
		Constructor<?> constructor = clazz.getConstructor(argumentTypes);
		instance = constructor.newInstance(arguments);
	    } else {
		instance = clazz.newInstance();
	    }
	} catch (Exception e) {
	    throw new DataFormatException("Unable to instantiate class: " + e.getMessage());
	}
	JsonValue attribute = json.child;
	while (attribute != null) {
	    // We skip some param names
	    if (!PARAM_SKIP.contains(attribute.name)) {
		ValueType type = attribute.type();
		Class<?> valueClass = null;
		Object value = null;
		if (attribute.isValue()) {
		    valueClass = getValueClass(attribute);
		    value = getValue(attribute);
		} else if (attribute.isArray()) {
		    // We suppose are childs are of the same type
		    switch (attribute.child.type()) {
		    case stringValue:
			valueClass = String[].class;
			value = attribute.asStringArray();
			break;
		    case doubleValue:
			valueClass = float[].class;
			value = attribute.asFloatArray();
			break;
		    case booleanValue:
			valueClass = boolean[].class;
			value = attribute.asBooleanArray();
			break;
		    case longValue:
			valueClass = int[].class;
			value = attribute.asIntArray();
			break;
		    }
		    Method m = clazz.getMethod("set" + GlobalResources.propertyToMethodName(attribute.name), valueClass);
		    m.invoke(instance, value);

		} else if (attribute.isObject()) {
		    String clazzName = attribute.has("impl") ? attribute.getString("impl") : GlobalResources.capitalise(attribute.name) + "Component";
		    valueClass = Class.forName(COMPONENTS_PACKAGE + clazzName);
		    value = convertJsonToObject(attribute, valueClass);

		}
		Method m = clazz.getMethod("set" + GlobalResources.propertyToMethodName(attribute.name), valueClass);
		m.invoke(instance, value);
	    }
	    attribute = attribute.next;
	}
	return instance;
    }

    private Object getValue(JsonValue val) {
	Object value = null;
	switch (val.type()) {
	case stringValue:
	    value = val.asString();
	    break;
	case doubleValue:
	    value = val.asFloat();
	    break;
	case booleanValue:
	    value = val.asBoolean();
	    break;
	case longValue:
	    value = val.asLong();
	    break;
	}
	return value;
    }

    private Class<?> getValueClass(JsonValue val) {
	Class<?> valueClass = null;
	switch (val.type()) {
	case stringValue:
	    valueClass = String.class;
	    break;
	case doubleValue:
	    valueClass = Float.class;
	    break;
	case booleanValue:
	    valueClass = Boolean.class;
	    break;
	case longValue:
	    valueClass = Long.class;
	    break;
	}
	return valueClass;
    }
}