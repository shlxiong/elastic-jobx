package com.dangdang.ddframe.job.lite.internal.manager;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JobEventNode Listener
 * @author xiongsl
 */
public abstract class JobNodeListener implements TreeCacheListener {
	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	/**
	 * 数据改变时处理
	 * @param path
	 * @param eventType
	 * @param data
	 */
	protected abstract void dataChanged(final String path, final Type eventType, final String data);
    
    @Override
    public final void childEvent(final CuratorFramework client, final TreeCacheEvent event) throws Exception {
        ChildData childData = event.getData();
        if (null == childData) {
            return;
        }
        String path = childData.getPath();
        if (path.isEmpty()) {
            return;
        }
        String data = (null==childData.getData()) ? "" : new String(childData.getData(),"UTF-8");
        Type eventType = event.getType();
        this.dataChanged(path, eventType, data);
    }
    
}
