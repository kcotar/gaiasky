package gaia.cu9.ari.gaiaorbit.data;

import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;
import gaia.cu9.ari.gaiaorbit.util.PrefixedProperties;

import java.util.List;
import java.util.Properties;

/**
 * This class acts as a hub that redirects to one or another implementation
 * depending on the properties.
 * @author tsagrista
 *
 */
public class HubCatalogLoader implements ISceneGraphNodeProvider {

    /** Props **/
    Properties properties;

    @Override
    public void initialize(Properties properties) {
        this.properties = properties;
    }

    @Override
    public List<? extends SceneGraphNode> loadObjects() {
        ISceneGraphNodeProvider loader = null;
        // Always use local
        loader = new StarLoader();
        PrefixedProperties pp = new PrefixedProperties(properties, loader.getClass().getSimpleName() + ".");
        loader.initialize(pp);
        return loader.loadObjects();
    }

}
