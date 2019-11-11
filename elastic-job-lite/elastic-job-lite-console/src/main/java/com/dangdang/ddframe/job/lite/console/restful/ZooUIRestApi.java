package com.dangdang.ddframe.job.lite.console.restful;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.zookeeper.CreateMode;

import com.dangdang.ddframe.job.lite.console.domain.NodeData;
import com.dangdang.ddframe.job.lite.console.domain.TreeNode;
import com.dangdang.ddframe.job.lite.console.spring.SpringRegistrySettings;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;

/**
 * 展示Zookeeper树节点
 * @author xiongsl
 */
@Path("zkui")
public class ZooUIRestApi {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final ZookeeperRegistryCenter registry =
					SpringRegistrySettings.getRegistryCenter();
	private final String ROOT = "/elastic-job";
	
	@POST
	@Path("getTree")
	public List<TreeNode> getTree(@FormParam("path")String path) throws Exception {
		List<TreeNode> treeList = new ArrayList<TreeNode>();
		if (path == null) {
			TreeNode tree = new TreeNode();
			tree.setId(0);
			tree.setName(ROOT.substring(1));
			tree.setParentId(-1);
			tree.setFullPath(ROOT);
			tree.setIsParent("true");
			tree.setOpen(true);
			treeList.add(tree);
		}
		
		this.getChildren(path, treeList);
		return treeList;
	}
	
	@POST
	@Path("getNodeInfo")
	public NodeData getNodeInfo(@FormParam("path")String path) throws IOException {
		NodeData info = new NodeData();
		try {
			NodeCache nodeCache = new NodeCache(registry.getClient(), path);
			nodeCache.start(true); //这个参数要给true  不然下边空指针...
			byte[] data = nodeCache.getCurrentData().getData();
			info.setData(data==null ? "" : new String(data));
			info.setStat(nodeCache.getCurrentData().getStat());
			nodeCache.close();
		} catch(Exception e){
			logger.error("", e);
		}
		return info;
	}
	
	@POST
	@Path("addPath")
	public AjaxMessage addPath(String path, String data, int flag) {
		AjaxMessage msg = new AjaxMessage(true, "添加成功!");
		try {
			if (registry.isExisted(path)) {
				CreateMode model = CreateMode.fromFlag(flag);
				if (CreateMode.EPHEMERAL == model) {
					registry.persistEphemeral(path, data);
				} else if (CreateMode.PERSISTENT == model){
					registry.persist(path, data);
				} else if (CreateMode.PERSISTENT_SEQUENTIAL == CreateMode.fromFlag(flag)) {
					registry.persistSequential(path, data);
				} else {
					registry.persistEphemeralSequential(path);
				}
			} else {
				msg.setSuccess(false);
				msg.setContent("该节点已经存在!");
			}
		} catch (Exception e) {
			logger.error("", e);
			msg.setSuccess(false);
			msg.setContent("服务端异常，" + e.getMessage());
		}
		return msg;
	}
	
	@POST
	@Path("deletePath")
	public AjaxMessage deletePath(String path) {
		AjaxMessage msg = new AjaxMessage(true, "删除成功!");
		try {
			if (registry.isExisted(path)) {
				registry.remove(path);
			} else {
				msg.setSuccess(false);
				msg.setContent("该节点不存在!");
			}
		} catch (Exception e) {
			logger.error("", e);
			msg.setSuccess(false);
			msg.setContent("服务端异常，" + e.getMessage());
		}
		return msg;
	}
	
	@POST
	@Path("updatePathData")
	public AjaxMessage updatePathData(String path, String data) {
		AjaxMessage msg = new AjaxMessage(true, "修改成功!");
		try {
			if (registry.isExisted(path)) {
				registry.update(path, data);
			} else {
				msg.setSuccess(false);
				msg.setContent("无此节点信息!");
			}
		} catch (Exception e) {
			logger.error("", e);
			msg.setSuccess(false);
			msg.setContent("服务端异常");
		}
		return msg;
	}
	
	private void getChildren(String path, List<TreeNode> treeList) throws Exception {
		final int parentId = (path==null) ? 0 : path.hashCode();
		final String FORMAT = (path==null) ? "/%s" : (path + "/%s");
		if (path == null) {
			path = "/";
		}
		for (String string : registry.getChildrenKeys(path)) {
			String newPath = String.format(FORMAT, string);
			TreeNode tree = new TreeNode();
			tree.setId(newPath.hashCode());
			tree.setParentId(parentId);
			tree.setName(string);
			tree.setFullPath(newPath);
			
			if (registry.getNumChildren(newPath) > 0) {
				tree.setIsParent("true");
			} else {
				tree.setIsParent("false");
			}
			treeList.add(tree);
		}
	}

	public class AjaxMessage{
		private boolean success;
		private String content;
		
		public AjaxMessage(boolean success, String content) {
			this.setSuccess(success);
			this.setContent(content);
		}
		public boolean isSuccess() {
			return success;
		}
		public void setSuccess(boolean isSuccess) {
			this.success = isSuccess;
		}
		public String getContent() {
			return content;
		}
		public void setContent(String content) {
			this.content = content;
		}
	}
}
