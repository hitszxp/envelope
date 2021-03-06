package com.cloudera.labs.envelope.store;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Properties;

import com.cloudera.labs.envelope.utils.PropertiesUtils;
import com.google.common.collect.Maps;

import scala.Tuple2;

/**
 * The singleton of all storage system instances opened by the executor JVM. This allows storage
 * system connections to be held open across functions within a micro-batch, and across micro-batches.
 */
public enum StorageSystems {
    
    // The singleton of StorageSystems. There can only be one!
    INSTANCE;
    
    private Map<Tuple2<String, String>, StorageSystem> systems = Maps.newHashMap();
    
    public static StorageTable tableFor(Properties props) throws Exception {
        return INSTANCE.getStorageSystem(props).getStorageTable(props);
    }
    
    public StorageSystem getStorageSystem(Properties props) throws Exception {
        String type = props.getProperty("storage");
        String instance = props.getProperty("storage.connection");
        // A StorageSystem is identified by its type and its connection string
        Tuple2<String, String> systemReference = new Tuple2<String, String>(type, instance);
        
        if (!systems.containsKey(systemReference)) {
            StorageSystem system = storageSystemFor(props);
            system.connect();
            systems.put(systemReference, system);
        }
        
        return systems.get(systemReference);
    }
    
    private StorageSystem storageSystemFor(Properties props) throws Exception {
        Properties storageProps = PropertiesUtils.prefixProperties(props, "storage.");
        String storageTableName = props.getProperty("storage");
        
        StorageSystem storage = null;
        
        if (storageTableName.equals("kudu")) {
            storage = new KuduStorageSystem(storageProps);
        }
        else {
            Class<?> clazz = Class.forName(storageTableName);
            Constructor<?> constructor = clazz.getConstructor(Properties.class);
            storage = (StorageSystem)constructor.newInstance(storageProps);
        }
        
        return storage;
    }
    
}
