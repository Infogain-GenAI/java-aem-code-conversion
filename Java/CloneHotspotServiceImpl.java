package com.dcaadmin.core.services.impl;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dcaadmin.core.services.CloneHotspotService;
import com.dcaadmin.core.util.CommonConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;

@Component(enabled = true, immediate = true, service = CloneHotspotService.class)

public class CloneHotspotServiceImpl implements CloneHotspotService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloneHotspotServiceImpl.class);

    @Override
    public String addFolder(SlingHttpServletRequest request) {
        String result = "";
        ResourceResolver resourceResolver = request.getResourceResolver();
        Map<String, String> parameterMap = getParameterMap(request);
        Node fromNode = getNode(resourceResolver, parameterMap);
        Node toNode = gettoNode(resourceResolver, parameterMap);
        result = copyImages(resourceResolver, fromNode, toNode, parameterMap);
        return result;

    }

    private Node getNode(ResourceResolver resourceResolver, Map<String, String> parameterMap) {
        String modelName = "";
        String fromyear = parameterMap.containsKey(CommonConstants.FROM_YEAR)
                ? parameterMap.get(CommonConstants.FROM_YEAR)
                : null;
        String frommodel = parameterMap.containsKey(CommonConstants.FROM_MODEL)
                ? parameterMap.get(CommonConstants.FROM_MODEL)
                : null;
        String trim = parameterMap.containsKey(CommonConstants.FROM_TRIM) ? parameterMap.get(CommonConstants.FROM_TRIM)
                : null;
        String driveTrain = parameterMap.containsKey(CommonConstants.FROM_DRIVETRAIN)
                ? parameterMap.get(CommonConstants.FROM_DRIVETRAIN)
                : null;
        String transmission = parameterMap.containsKey(CommonConstants.FROM_TRANSMISSION)
                ? parameterMap.get(CommonConstants.FROM_TRANSMISSION)
                : null;
        String packages = parameterMap.containsKey(CommonConstants.FROM_PACKAGE)
                ? parameterMap.get(CommonConstants.FROM_PACKAGE)
                : null;
        frommodel = frommodel.toLowerCase();
        frommodel = frommodel.replaceAll("[^A-Za-z0-9]", "-");
        trim = trim.toLowerCase();
        trim = trim.replaceAll("[^A-Za-z0-9]", "-");

        modelName = transmission.concat("-").concat(packages).concat("-").concat(driveTrain);
        modelName = modelName.toLowerCase();
        modelName = modelName.replaceAll("[^A-Za-z0-9]", "-");
        Resource resource = resourceResolver.getResource(CommonConstants.HOTSPOT_DAM_PATH);
        Node node = resource.adaptTo(Node.class);
        Node yearNode = getNodeExists(node, fromyear);
        Node modelNode = getNodeExists(yearNode, frommodel);
        Node trimNode = getNodeExists(modelNode, trim);
        Node frommodelNode = getNodeExists(trimNode, modelName);
        return frommodelNode;
    }

    private Node gettoNode(ResourceResolver resourceResolver, Map<String, String> parameterMap) {
        String modelName = "";
        String toyear = parameterMap.containsKey(CommonConstants.TO_YEAR)
                ? parameterMap.get(CommonConstants.TO_YEAR)
                : null;
        String tomodel = parameterMap.containsKey(CommonConstants.TO_MODEL)
                ? parameterMap.get(CommonConstants.TO_MODEL)
                : null;
        String totrim = parameterMap.containsKey(CommonConstants.TO_TRIM) ? parameterMap.get(CommonConstants.TO_TRIM)
                : null;
        String todriveTrain = parameterMap.containsKey(CommonConstants.TO_DRIVETRAIN)
                ? parameterMap.get(CommonConstants.TO_DRIVETRAIN)
                : null;
        String totransmission = parameterMap.containsKey(CommonConstants.TO_TRANSMISSION)
                ? parameterMap.get(CommonConstants.TO_TRANSMISSION)
                : null;
        String topackages = parameterMap.containsKey(CommonConstants.TO_PACKAGE)
                ? parameterMap.get(CommonConstants.TO_PACKAGE)
                : null;

        modelName = totransmission.concat("-").concat(topackages).concat("-").concat(todriveTrain);
        Node toModelNode = null;
        try {
            Resource resource = resourceResolver.getResource(CommonConstants.HOTSPOT_DAM_PATH);
            Node node = resource.adaptTo(Node.class);
            Node yearNode = getNodeExists(node, toyear);
            Node modelNode = getNodeExists(yearNode, tomodel);
            Node trimNode = getNodeExists(modelNode, totrim);
            toModelNode = getNodeExists(trimNode, modelName);
            resourceResolver.commit();
        } catch (PersistenceException e) {
            LOGGER.error("PersistenceException e{}", e.getMessage(), e);
        }         
        return toModelNode;
    }

    private Node getNodeExists(Node node, String nodeName) {
        Node addNode = null;
        String titleName = nodeName;
        String pathName = nodeName.toLowerCase().replaceAll("[^A-Za-z0-9]", "-");
        try {
            if (node.hasNode(pathName)) {
                addNode = node.getNode(pathName);
        
            } else {
                addNode = node.addNode(pathName, CommonConstants.SLING_FOLDER);
                Node jcrNode = addNode.addNode(CommonConstants.JCR_CONTENT, CommonConstants.NT_UNSTRUCTURED);
                jcrNode.setProperty(CommonConstants.JCR_TITLE, titleName);
            }
        } catch (RepositoryException e) {
            LOGGER.error("Repository Exception {}", e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("Exception {}", e.getMessage(), e);
        }
        return addNode;
    }

    private Map<String, String> getParameterMap(SlingHttpServletRequest request) {
        Map<String, String> parameterMap = new HashMap<String, String>();
        String fromYear = request.getParameter(CommonConstants.FROM_YEAR);
        parameterMap.put(CommonConstants.FROM_YEAR, fromYear);
        String frommodel = request.getParameter(CommonConstants.FROM_MODEL);
        parameterMap.put(CommonConstants.FROM_MODEL, frommodel);
        String fromtrim = request.getParameter(CommonConstants.FROM_TRIM);
        parameterMap.put(CommonConstants.FROM_TRIM, fromtrim);
        String fromtransmission = request.getParameter(CommonConstants.FROM_TRANSMISSION);
        parameterMap.put(CommonConstants.FROM_TRANSMISSION, fromtransmission);
        String frompackages = request.getParameter(CommonConstants.FROM_PACKAGE);
        parameterMap.put(CommonConstants.FROM_PACKAGE, frompackages);
        String fromdriveTrain = request.getParameter(CommonConstants.FROM_DRIVETRAIN);
        parameterMap.put(CommonConstants.FROM_DRIVETRAIN, fromdriveTrain);
        String toyear = request.getParameter(CommonConstants.TO_YEAR);
        parameterMap.put(CommonConstants.TO_YEAR, toyear);
        String tomodel = request.getParameter(CommonConstants.TO_MODEL);
        parameterMap.put(CommonConstants.TO_MODEL, tomodel);
        String totrim = request.getParameter(CommonConstants.TO_TRIM);
        parameterMap.put(CommonConstants.TO_TRIM, totrim);
        String totransmission = request.getParameter(CommonConstants.TO_TRANSMISSION);
        parameterMap.put(CommonConstants.TO_TRANSMISSION, totransmission);
        String topackages = request.getParameter(CommonConstants.TO_PACKAGE);
        parameterMap.put(CommonConstants.TO_PACKAGE, topackages);
        String todriveTrain = request.getParameter(CommonConstants.TO_DRIVETRAIN);
        parameterMap.put(CommonConstants.TO_DRIVETRAIN, todriveTrain);
        String toModelCode = request.getParameter(CommonConstants.TO_MODELCODE);
        parameterMap.put(CommonConstants.TO_MODELCODE, toModelCode);
        String status = request.getParameter(CommonConstants.STATUS);
        parameterMap.put(CommonConstants.STATUS, status);
        return parameterMap;
    }
    
    private String copyImages(ResourceResolver resourceResolver, Node fromNode, Node toNode, Map<String, String> parameterMap) {
    	String status = parameterMap.containsKey(CommonConstants.STATUS)
                ? parameterMap.get(CommonConstants.STATUS)
                : null;
    	String result = "";    	
    	try {
			String destinationPath = toNode.getPath();
			long destinationChildNodeCount = toNode.getNodes().getSize();
			if (destinationChildNodeCount <= 1) {
				Node jcrContentNode = getNode(toNode, CommonConstants.JCR_CONTENT);
				jcrContentNode.setProperty(CommonConstants.STATUS, status);
				AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);
				Resource sourceResource = resourceResolver.getResource(fromNode.getPath());
				if (sourceResource.hasChildren()) {
					Iterator<Resource> resourceIterator = sourceResource.listChildren();
					while (resourceIterator.hasNext()) {
						Resource imageResource = resourceIterator.next();
						if (imageResource.isResourceType(CommonConstants.DAM_ASSET)) {
							Asset image = imageResource.adaptTo(Asset.class);
							String fileName = image.getName();
							InputStream inputStream = image.getOriginal().getStream();
							String mimeType = image.getMimeType();
							assetManager.createAsset(destinationPath + "/" + fileName, inputStream, mimeType, true);

							Node sourceNode = imageResource.adaptTo(Node.class);
							Node sourceJcrNode = getNode(sourceNode, CommonConstants.JCR_CONTENT);
							Node sourceMetadata = getNode(sourceJcrNode, CommonConstants.METADATA);
							String sourceCategory = getNodeProperty(sourceMetadata, CommonConstants.CATEGORY);
							String destinationModel = getModel(parameterMap);
							Resource newImageResource = resourceResolver.getResource(destinationPath + "/" + fileName);
							Node destinationNode = newImageResource.adaptTo(Node.class);
							Node destinationJcrNode = getNode(destinationNode, CommonConstants.JCR_CONTENT);
							Node destinationMetadata = getNode(destinationJcrNode, CommonConstants.METADATA);
							destinationMetadata.setProperty(CommonConstants.CATEGORY, sourceCategory);
							destinationMetadata.setProperty(CommonConstants.MODEL, destinationModel);
						}
					}
				}
				resourceResolver.commit();
				result = toNode.getPath();
			}			
		} catch (RepositoryException e) {
			LOGGER.error("Repository Exception {}", e.getMessage(), e);
		} catch (Exception e) {
			LOGGER.error("Exception {}", e.getMessage(), e);
		}
    	return result;
    }    
    
    private Node getNode(Node node, String nodeName) {
    	Node targetNode = null;
    	try {
			if (node.hasNode(nodeName)) {
				targetNode = node.getNode(nodeName);
			}			
		} catch (RepositoryException e) {
			LOGGER.error("Repository Exception {}", e.getMessage(), e);
		}
    	return targetNode;
    }
    
    private String getNodeProperty(Node node, String propertyName) {
    	String propertyValue = "";
    	try {
			if (node.hasProperty(propertyName)) {
				propertyValue = node.getProperty(propertyName).getString();
			}
		} catch (RepositoryException e) {
			LOGGER.error("Repository Exception {}", e.getMessage(), e);
		}
    	return propertyValue;
    }
    
    private String getModel(Map<String, String> parameterMap) {    
    	String toyear = parameterMap.containsKey(CommonConstants.TO_YEAR)
                ? parameterMap.get(CommonConstants.TO_YEAR)
                : null;
        String tomodel = parameterMap.containsKey(CommonConstants.TO_MODEL)
                ? parameterMap.get(CommonConstants.TO_MODEL)
                : null;
        String totrim = parameterMap.containsKey(CommonConstants.TO_TRIM) ? parameterMap.get(CommonConstants.TO_TRIM)
                : null;
        String todriveTrain = parameterMap.containsKey(CommonConstants.TO_DRIVETRAIN)
                ? parameterMap.get(CommonConstants.TO_DRIVETRAIN)
                : null;
        String totransmission = parameterMap.containsKey(CommonConstants.TO_TRANSMISSION)
                ? parameterMap.get(CommonConstants.TO_TRANSMISSION)
                : null;
        String topackages = parameterMap.containsKey(CommonConstants.TO_PACKAGE)
                ? parameterMap.get(CommonConstants.TO_PACKAGE)
                : null;
        String toModelCode = parameterMap.containsKey(CommonConstants.TO_MODELCODE)
                ? parameterMap.get(CommonConstants.TO_MODELCODE)
                : null;
        String model = toyear.concat("-").concat(tomodel).concat("-").concat(totrim).concat("-").concat(totransmission)
                .concat("-").concat(topackages).concat("-").concat(todriveTrain).concat("-").concat(toModelCode);
        return model;
    }

}