package com.dcaadmin.core.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dcaadmin.core.services.EditFeatureService;
import com.dcaadmin.core.services.FeatureService;
import com.dcaadmin.core.util.CommonConstants;

@Component(enabled = true, immediate = true, service = EditFeatureService.class)
public class EditFeatureServiceImpl implements EditFeatureService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EditFeatureServiceImpl.class);
	
	@Reference
	private FeatureService featureService;

	@Override
	public String editNode(SlingHttpServletRequest request) {
		String result = "";
		ResourceResolver resourceResolver = request.getResourceResolver();
		Map<String, String> parameterMap = getParameterMap(request);
		Node node = getNode(resourceResolver, parameterMap);
		result = addNodeProperties(node, resourceResolver, parameterMap);
		return result;
	}

	private String addNodeProperties(Node node, ResourceResolver resourceResolver, Map<String, String> parameterMap) {
		String result = "";
		String featureName = parameterMap.containsKey(CommonConstants.FEATURENAME)
				? parameterMap.get(CommonConstants.FEATURENAME)
				: null;
		String videoLink = parameterMap.containsKey(CommonConstants.VIDEOLINK)
				? parameterMap.get(CommonConstants.VIDEOLINK)
				: null;
		String attachment = parameterMap.containsKey(CommonConstants.ATTACHMENT)
				? parameterMap.get(CommonConstants.ATTACHMENT)
				: null;
		String include = parameterMap.containsKey(CommonConstants.INCLUDE) ? parameterMap.get(CommonConstants.INCLUDE)
				: null;
		String recommended = parameterMap.containsKey(CommonConstants.RECOMMENDED)
				? parameterMap.get(CommonConstants.RECOMMENDED)
				: null;

		String category = parameterMap.containsKey(CommonConstants.CATEGORY)
				? parameterMap.get(CommonConstants.CATEGORY)
				: null;
		String newCategory = parameterMap.containsKey(CommonConstants.NEW_CATEGORY)
				? parameterMap.get(CommonConstants.NEW_CATEGORY)
				: null;
		String newFeatureName = parameterMap.containsKey(CommonConstants.NEW_FEATURENAME)
				? parameterMap.get(CommonConstants.NEW_FEATURENAME)
				: null;
		String newVideoLink = parameterMap.containsKey(CommonConstants.NEW_VIDEOLINK)
				? parameterMap.get(CommonConstants.NEW_VIDEOLINK)
				: null;
		String newAttachment = parameterMap.containsKey(CommonConstants.NEW_ATTACHMENT)
				? parameterMap.get(CommonConstants.NEW_ATTACHMENT)
				: null;
		String newInclude = parameterMap.containsKey(CommonConstants.NEW_INCLUDE)
				? parameterMap.get(CommonConstants.NEW_INCLUDE)
				: null;
		String newRecommended = parameterMap.containsKey(CommonConstants.NEW_RECOMMENDED)
				? parameterMap.get(CommonConstants.NEW_RECOMMENDED)
				: null;
		boolean featureNotInAddedFeatures = true;
		boolean newFeatureExists = false;
		boolean featureExistsInMasterList = false;
		try {
			String year = parameterMap.containsKey(CommonConstants.YEAR)
					? parameterMap.get(CommonConstants.YEAR)
					: null;
			String modelName = getModelName(parameterMap);
			String modelPath = CommonConstants.ADD_UNIQUE_FEATURE_PATH.concat(year).concat("/").concat(modelName);
			Resource modelResource = resourceResolver.getResource(modelPath);
			Node modelNode = modelResource.adaptTo(Node.class);
			String masterListJson = modelNode.getProperty(CommonConstants.FEATURES).getString();
			JSONArray masterListArray = new JSONArray(masterListJson);
			JSONArray newMasterListArray = new JSONArray();
			
			if(node != null) {
				NodeIterator iterator = node.getNodes();
				newFeatureExists = checkFeatureExistance(resourceResolver, newFeatureName, parameterMap);
				if (featureName.equalsIgnoreCase(newFeatureName))
					newFeatureExists = false;
				
				if (newFeatureExists) {
					return CommonConstants.FEATURE_EXISTS;
				}
				else {
					iterator = node.getNodes();
					if (!newCategory.equals(category)) {
						if (checkFeatureInMasterList(masterListArray, featureName)) {
							if(!checkFeatureInMasterList(masterListArray, newFeatureName) || featureName.equalsIgnoreCase(newFeatureName)) {								
								newMasterListArray = updateFeatureCategory(masterListArray, modelNode, parameterMap);
								modelNode.setProperty(CommonConstants.FEATURES, newMasterListArray.toString());
								featureNotInAddedFeatures = false;
							} 
							else
								return CommonConstants.FEATURE_EXISTS;
							
						} else {
							Node toNode = gettoNode(resourceResolver, parameterMap);
							featureExistsInMasterList = checkFeatureInMasterList(masterListArray, newFeatureName);
							if (featureExistsInMasterList) {
								return CommonConstants.FEATURE_EXISTS;
							} else {
								return moveNode(resourceResolver, iterator, toNode, parameterMap);
							}
						}
					}

					if(checkFeatureExistance(resourceResolver, featureName, parameterMap)) {
						featureExistsInMasterList = checkFeatureInMasterList(masterListArray, newFeatureName);
						if(featureExistsInMasterList)
							return CommonConstants.FEATURE_EXISTS;
						else {
							while (iterator.hasNext()) {
								Node featureNode = iterator.nextNode();
								String nodeFeatureName = featureNode.getProperty(CommonConstants.FEATURENAME).getString();
								String nodeVideoLink = featureNode.getProperty(CommonConstants.VIDEOLINK).getString();
								String nodeAttachment = featureNode.getProperty(CommonConstants.ATTACHMENT).getString();
								String nodeInclude = featureNode.getProperty(CommonConstants.INCLUDE).getString();
								String nodeRecommended = featureNode.getProperty(CommonConstants.RECOMMENDED).getString();
		
								if (nodeFeatureName.equals(featureName) && nodeVideoLink.equals(videoLink)
										&& nodeAttachment.equals(attachment) && nodeInclude.equals(include)
										&& nodeRecommended.equals(recommended)) {
									featureNode.setProperty(CommonConstants.FEATURENAME, newFeatureName);
									featureNode.setProperty(CommonConstants.VIDEOLINK, newVideoLink);
									featureNode.setProperty(CommonConstants.ATTACHMENT, newAttachment);
									featureNode.setProperty(CommonConstants.INCLUDE, newInclude);
									featureNode.setProperty(CommonConstants.RECOMMENDED, newRecommended);
									featureNotInAddedFeatures = false;
									break;
								}
							}
						}
					}
				}
			}

			if (featureNotInAddedFeatures) {
				if (featureName.equalsIgnoreCase(newFeatureName)) 
					featureExistsInMasterList = false;
				else 
					featureExistsInMasterList = checkFeatureInMasterList(masterListArray, newFeatureName);
				
				if(featureExistsInMasterList) {
					return CommonConstants.FEATURE_EXISTS;
				} else {
					if(newCategory.equals(category)) {
						if(checkFeatureInMasterList(masterListArray, newFeatureName) && !featureName.equals(newFeatureName)) 
							return CommonConstants.FEATURE_EXISTS;
						else
							newMasterListArray = updateFeature(masterListArray, modelNode, newCategory, parameterMap);
					} else {
						featureExistsInMasterList = checkFeatureInMasterList(masterListArray, newFeatureName);
						boolean featureExistsInAddedList = checkFeatureExistance(resourceResolver, newFeatureName, parameterMap);
						if ((featureName.equals(newFeatureName) && videoLink.equals(newVideoLink)&& attachment.equals(newAttachment))
		                        || (featureName.equals(newFeatureName) && videoLink.equals(newVideoLink))
		                        || (featureName.equals(newFeatureName) && attachment.equals(newAttachment))
		                        || (videoLink.equals(newVideoLink) && attachment.equals(newAttachment))) {
							featureExistsInMasterList = false;
							featureExistsInAddedList = false;
						}
						if(featureExistsInMasterList || featureExistsInAddedList) {
							return CommonConstants.FEATURE_EXISTS;
						}
						else {
							newMasterListArray = updateFeatureCategory(masterListArray, modelNode, parameterMap);
						}
					}
					modelNode.setProperty(CommonConstants.FEATURES, newMasterListArray.toString());
				}
			}
			resourceResolver.commit();

		} catch (RepositoryException e) {
			LOGGER.error("Repository Exception {}", e.getMessage(), e);
		} catch (PersistenceException e) {
			LOGGER.error("Persistence Exception {}", e.getMessage());
		} catch (JSONException e) {
			LOGGER.error("JSON Exception {}", e.getMessage());
		}
		result = CommonConstants.SAVE_SUCCESS;
		return result;
	}
	
	private void updateJsonProperty(JSONObject obj, String key, String value) {
		try {
			obj.remove(key);
			obj.put(key, value);
		} catch (JSONException e) {
			LOGGER.error("JSON Exception in updateJsonProperty : {}", e.getMessage());
		}
		
	}
	
	private void updateProperty(Node node, String property, String newValue, String uid) {
		try {
			if (node.hasProperty(property)) {
				Value[] values = node.getProperty(property).getValues();
				List<String> valueList = new ArrayList<String>();

				for (Value v : values) {
					valueList.add(v.getString());
				}

				if (newValue.equals(CommonConstants.TRUE)) {
					if (!(valueList.contains(uid)))
						valueList.add(uid);
				} else {
					if (valueList.contains(uid))
						valueList.remove(uid);
				}
				String[] propertyArray = valueList.toArray(new String[valueList.size()]);
				node.setProperty(property, propertyArray);
			} else {
				String[] propertyArray = new String[1];
				if (newValue.equals(CommonConstants.TRUE))
					propertyArray[0] = uid;
				node.setProperty(property, propertyArray);
			}
		} catch (RepositoryException e) {
			LOGGER.error("Repository Exception in updateProperty : {}", e.getMessage(), e);
		}
	}

	private Node getNode(ResourceResolver resourceResolver, Map<String, String> parameterMap) {
		String category = parameterMap.containsKey(CommonConstants.CATEGORY)
				? parameterMap.get(CommonConstants.CATEGORY)
				: null;
		String year = parameterMap.containsKey(CommonConstants.YEAR) ? parameterMap.get(CommonConstants.YEAR) : null;

		String modelName = getModelName(parameterMap);
		String resourcePath = CommonConstants.ADD_UNIQUE_FEATURE_PATH.concat(year).concat("/").concat(modelName)
				.concat("/").concat(CommonConstants.FEATURE).concat("/").concat(category).concat("/")
				.concat(CommonConstants.ADDED_FEATURES);
		Node node = null;
		Resource resource = resourceResolver.getResource(resourcePath);
		if(resource != null)
			node = resource.adaptTo(Node.class);
		else {
			resource = resourceResolver.getResource(CommonConstants.ADD_UNIQUE_FEATURE_PATH);
			node = resource.adaptTo(Node.class);
			Node yearNode = getNodeExists(node, parameterMap.get(CommonConstants.YEAR));
			Node modelNode = getNodeExists(yearNode, modelName);
			Node featureNode = getNodeExists(modelNode, CommonConstants.FEATURE);
			Node categoryNode = getNodeExists(featureNode, category);
			return categoryNode;
		}
		return node;
	}

	private String getModelName(Map<String, String> parameterMap) {
		String modelName = "";
		String model = parameterMap.containsKey(CommonConstants.MODEL) ? parameterMap.get(CommonConstants.MODEL) : null;
		String modelCode = parameterMap.containsKey(CommonConstants.MODELCODE) ? parameterMap.get(CommonConstants.MODELCODE) : null;
		String packages = parameterMap.containsKey(CommonConstants.PACKAGE) ? parameterMap.get(CommonConstants.PACKAGE)
				: null;
		modelName = model.concat("-").concat(modelCode).concat("-").concat(packages);
		return modelName;
	}
	
	private Node getNodeExists(Node node, String nodeName) {
		Node addNode = null;
		try {
			if (node.hasNode(nodeName)) {
				addNode = node.getNode(nodeName);
			} else {
				addNode = node.addNode(nodeName, CommonConstants.NT_UNSTRUCTURED);
			}
		} catch (RepositoryException e) {
			LOGGER.error("Repository Exception {}", e.getMessage());
		}
		return addNode;
	}

	private Map<String, String> getParameterMap(SlingHttpServletRequest request) {
		Map<String, String> parameterMap = new HashMap<String, String>();
		String category = request.getParameter(CommonConstants.CATEGORY);
		parameterMap.put(CommonConstants.CATEGORY, category);
		String featureName = request.getParameter(CommonConstants.FEATURENAME);
		parameterMap.put(CommonConstants.FEATURENAME, featureName);
		String videoLink = request.getParameter(CommonConstants.VIDEOLINK);
		parameterMap.put(CommonConstants.VIDEOLINK, videoLink);
		String attachment = request.getParameter(CommonConstants.ATTACHMENT);
		parameterMap.put(CommonConstants.ATTACHMENT, attachment);
		String year = request.getParameter(CommonConstants.YEAR);
		parameterMap.put(CommonConstants.YEAR, year);
		String model = request.getParameter(CommonConstants.MODEL);
		parameterMap.put(CommonConstants.MODEL, model);
		String trim = request.getParameter(CommonConstants.TRIM);
		parameterMap.put(CommonConstants.TRIM, trim);
		String transmission = request.getParameter(CommonConstants.TRANSMISSION);
		parameterMap.put(CommonConstants.TRANSMISSION, transmission);
		String packages = request.getParameter(CommonConstants.PACKAGE);
		parameterMap.put(CommonConstants.PACKAGE, packages);
		String driveTrain = request.getParameter(CommonConstants.DRIVELINE);
		parameterMap.put(CommonConstants.DRIVELINE, driveTrain);
		String include = request.getParameter(CommonConstants.INCLUDE);
		parameterMap.put(CommonConstants.INCLUDE, include);
		String recommended = request.getParameter(CommonConstants.RECOMMENDED);
		parameterMap.put(CommonConstants.RECOMMENDED, recommended);

		String newCategory = request.getParameter(CommonConstants.NEW_CATEGORY);
		parameterMap.put(CommonConstants.NEW_CATEGORY, newCategory);
		String newFeatureName = request.getParameter(CommonConstants.NEW_FEATURENAME);
		parameterMap.put(CommonConstants.NEW_FEATURENAME, newFeatureName);
		String newVideoLink = request.getParameter(CommonConstants.NEW_VIDEOLINK);
		parameterMap.put(CommonConstants.NEW_VIDEOLINK, newVideoLink);
		String newAttachment = request.getParameter(CommonConstants.NEW_ATTACHMENT);
		parameterMap.put(CommonConstants.NEW_ATTACHMENT, newAttachment);
		String newInclude = request.getParameter(CommonConstants.NEW_INCLUDE);
		parameterMap.put(CommonConstants.NEW_INCLUDE, newInclude);
		String newRecommended = request.getParameter(CommonConstants.NEW_RECOMMENDED);
		parameterMap.put(CommonConstants.NEW_RECOMMENDED, newRecommended);
		String modelCode = request.getParameter(CommonConstants.MODELCODE);
		parameterMap.put(CommonConstants.MODELCODE, modelCode);
		return parameterMap;

	}
	
	private boolean checkFeatureExistance(ResourceResolver resolver, String featureName, Map<String, String> parameterMap) {
		Boolean featureExists = false;
		String year = parameterMap.containsKey(CommonConstants.YEAR) ? parameterMap.get(CommonConstants.YEAR)
				: null;
		String modelName = getModelName(parameterMap);
		String path = CommonConstants.ADD_UNIQUE_FEATURE_PATH.concat("/").concat(year).concat("/").concat(modelName)
				.concat("/").concat(CommonConstants.FEATURE);
		Resource resource = resolver.getResource(path);
		Node modelNode = resource.adaptTo(Node.class);
		try {
			if(modelNode.hasNodes()) {
				NodeIterator categoryIterator = modelNode.getNodes();
				while (categoryIterator.hasNext()) {
					Node categoryNode = categoryIterator.nextNode();
					if (categoryNode.hasNode(CommonConstants.ADDED_FEATURES)) {
					Node addedFeaturesNode = categoryNode.getNode(CommonConstants.ADDED_FEATURES);
					if (addedFeaturesNode.hasNodes()) {
						NodeIterator iterator = addedFeaturesNode.getNodes();
						while (iterator.hasNext()) {
							Node node = iterator.nextNode();
							Boolean featureNameCheck = node.getProperty(CommonConstants.FEATURENAME).getString()
									.equalsIgnoreCase(featureName);
							featureExists = featureNameCheck;
							if (featureExists)
								return featureExists;
							}
						}
					}
				}
			}
		} catch (RepositoryException e) {
			LOGGER.error("Exception in checkFeatureExistanceNew : {}", e.getMessage(), e);
		}
		return featureExists;
	}

	private boolean checkFeatureInMasterList(JSONArray masterListArray, String featureName) {
		Boolean featureExists = false;
		try {
			for (int i = 0; i < masterListArray.length(); i++) {
				JSONObject feature = masterListArray.getJSONObject(i);
				if (feature.has(CommonConstants.CATEGORY)) {
					String objFeatureName = feature.getString(CommonConstants.FEATURENAME);
					if (objFeatureName.equalsIgnoreCase(featureName))
						return true;
				}
			}
		} catch (JSONException e) {
			LOGGER.error("JSON Exception in checkFeatureInMasterList : {}", e.getMessage(), e);
		}
		return featureExists;
	}
	
	private JSONArray updateFeature(JSONArray masterListArray, Node modelNode, String category, Map<String, String> parameterMap) {
		String featureName = parameterMap.containsKey(CommonConstants.FEATURENAME)
				? parameterMap.get(CommonConstants.FEATURENAME)
				: null;
		String videoLink = parameterMap.containsKey(CommonConstants.VIDEOLINK)
				? parameterMap.get(CommonConstants.VIDEOLINK)
				: null;
		String attachment = parameterMap.containsKey(CommonConstants.ATTACHMENT)
				? parameterMap.get(CommonConstants.ATTACHMENT)
				: null;
		String newFeatureName = parameterMap.containsKey(CommonConstants.NEW_FEATURENAME)
				? parameterMap.get(CommonConstants.NEW_FEATURENAME)
				: null;
		String newVideoLink = parameterMap.containsKey(CommonConstants.NEW_VIDEOLINK)
				? parameterMap.get(CommonConstants.NEW_VIDEOLINK)
				: null;
		String newAttachment = parameterMap.containsKey(CommonConstants.NEW_ATTACHMENT)
				? parameterMap.get(CommonConstants.NEW_ATTACHMENT)
				: null;
		String newInclude = parameterMap.containsKey(CommonConstants.NEW_INCLUDE)
				? parameterMap.get(CommonConstants.NEW_INCLUDE)
				: null;
		String newRecommended = parameterMap.containsKey(CommonConstants.NEW_RECOMMENDED)
				? parameterMap.get(CommonConstants.NEW_RECOMMENDED)
				: null;
		
		JSONArray newMasterListArray = new JSONArray();
		try {
			for (int i = 0; i < masterListArray.length(); i++) {
				JSONObject feature = masterListArray.getJSONObject(i);
				if (feature.has(CommonConstants.CATEGORY)) {
					if (feature.get(CommonConstants.CATEGORY).equals(category)) {
						String objFeatureName = feature.getString(CommonConstants.FEATURENAME);
						String objVideoLink = feature.getString(CommonConstants.VIDEOLINK);
						String objAttachment = feature.getString(CommonConstants.ATTACHMENT);
						String objUid = feature.getString(CommonConstants.UID);
						
						if(objFeatureName.equals(featureName) && objVideoLink.equals(videoLink) && objAttachment.equals(attachment)) {
							updateJsonProperty(feature, CommonConstants.FEATURENAME, newFeatureName);
							updateJsonProperty(feature, CommonConstants.VIDEOLINK, newVideoLink);
							updateJsonProperty(feature, CommonConstants.ATTACHMENT, newAttachment);
							updateProperty(modelNode, CommonConstants.INCLUDE, newInclude, objUid);
							updateProperty(modelNode, CommonConstants.RECOMMENDED, newRecommended, objUid);
						}
					}
					newMasterListArray.put(feature);
				}
			}
		} catch (JSONException e) {
			LOGGER.error("JSON Exception in updateFeature()", e.getMessage(), e);
		}
		return newMasterListArray;
	}
	
	private Node gettoNode(ResourceResolver resourceResolver, Map<String, String> parameterMap) {
        Node toNode = null;
        String category = parameterMap.containsKey(CommonConstants.NEW_CATEGORY)
				? parameterMap.get(CommonConstants.NEW_CATEGORY)
				: null;
		String year = parameterMap.containsKey(CommonConstants.YEAR) ? parameterMap.get(CommonConstants.YEAR) : null;

		String modelName = getModelName(parameterMap);
		String resourcePath = CommonConstants.ADD_UNIQUE_FEATURE_PATH.concat(year).concat("/").concat(modelName)
				.concat("/").concat(CommonConstants.FEATURE).concat("/").concat(category).concat("/")
				.concat(CommonConstants.ADDED_FEATURES);
		Resource resource = resourceResolver.getResource(resourcePath);
		if(resource != null)
			toNode = resource.adaptTo(Node.class);
		else {
			resource = resourceResolver.getResource(CommonConstants.ADD_UNIQUE_FEATURE_PATH);
			toNode = resource.adaptTo(Node.class);
			Node yearNode = getNodeExists(toNode, parameterMap.get(CommonConstants.YEAR));
			Node modelNode = getNodeExists(yearNode, modelName);
			Node featureNode = getNodeExists(modelNode, CommonConstants.FEATURE);
			Node categoryNode = getNodeExists(featureNode, category);
			Node addedNode = getNodeExists(categoryNode, CommonConstants.ADDED_FEATURES);
			return addedNode;
		}
        return toNode;
    }
	
	private String moveNode(ResourceResolver resolver, NodeIterator fromIterator, Node toNode, Map<String, String> parameterMap) {
		String result = "Error while editing";
		String featureName = parameterMap.containsKey(CommonConstants.FEATURENAME)
				? parameterMap.get(CommonConstants.FEATURENAME)
				: null;
		String videoLink = parameterMap.containsKey(CommonConstants.VIDEOLINK)
				? parameterMap.get(CommonConstants.VIDEOLINK)
				: null;
		String attachment = parameterMap.containsKey(CommonConstants.ATTACHMENT)
				? parameterMap.get(CommonConstants.ATTACHMENT)
				: null;
		String include = parameterMap.containsKey(CommonConstants.INCLUDE) ? parameterMap.get(CommonConstants.INCLUDE)
				: null;
		String recommended = parameterMap.containsKey(CommonConstants.RECOMMENDED)
				? parameterMap.get(CommonConstants.RECOMMENDED)
				: null;
		String newFeatureName = parameterMap.containsKey(CommonConstants.NEW_FEATURENAME)
				? parameterMap.get(CommonConstants.NEW_FEATURENAME)
				: null;
		String newVideoLink = parameterMap.containsKey(CommonConstants.NEW_VIDEOLINK)
				? parameterMap.get(CommonConstants.NEW_VIDEOLINK)
				: null;
		String newAttachment = parameterMap.containsKey(CommonConstants.NEW_ATTACHMENT)
				? parameterMap.get(CommonConstants.NEW_ATTACHMENT)
				: null;
		String newInclude = parameterMap.containsKey(CommonConstants.NEW_INCLUDE)
				? parameterMap.get(CommonConstants.NEW_INCLUDE)
				: null;
		String newRecommended = parameterMap.containsKey(CommonConstants.NEW_RECOMMENDED)
				? parameterMap.get(CommonConstants.NEW_RECOMMENDED)
				: null;
		try {
			Node featureNode = null;
			while (fromIterator.hasNext()) {
				featureNode = fromIterator.nextNode();
				String nodeFeatureName = featureNode.getProperty(CommonConstants.FEATURENAME).getString();
				String nodeVideoLink = featureNode.getProperty(CommonConstants.VIDEOLINK).getString();
				String nodeAttachment = featureNode.getProperty(CommonConstants.ATTACHMENT).getString();
				String nodeInclude = featureNode.getProperty(CommonConstants.INCLUDE).getString();
				String nodeRecommended = featureNode.getProperty(CommonConstants.RECOMMENDED).getString();
	
				if (nodeFeatureName.equals(featureName) && nodeVideoLink.equals(videoLink)
						&& nodeAttachment.equals(attachment) && nodeInclude.equals(include)
						&& nodeRecommended.equals(recommended)) {
					featureNode.setProperty(CommonConstants.FEATURENAME, newFeatureName);
					featureNode.setProperty(CommonConstants.VIDEOLINK, newVideoLink);
					featureNode.setProperty(CommonConstants.ATTACHMENT, newAttachment);
					featureNode.setProperty(CommonConstants.INCLUDE, newInclude);
					featureNode.setProperty(CommonConstants.RECOMMENDED, newRecommended);
					resolver.commit();
					break;
				}
			}
			String fromPath = featureNode.getPath();
			String toPath = toNode.getPath()+ '/' + newFeatureName;
			Session session = resolver.adaptTo(Session.class);
			Workspace workspace = session.getWorkspace();
			workspace.move(fromPath, toPath);
			session.save();
			result = CommonConstants.SAVE_SUCCESS;
		} catch(RepositoryException e) {
			LOGGER.error("Repository Exception in moveNode : {}", e.getMessage(), e);
		} catch(PersistenceException e) {
			LOGGER.error("Persistence Exception in moveNode : {}", e.getMessage(), e);
		} catch(Exception e) {
			LOGGER.error("Exception in moveNode : {}", e.getMessage(), e);
		}
		return result;
	}
	
	private JSONArray updateFeatureCategory(JSONArray masterListArray, Node modelNode, Map<String, String> parameterMap) {
		String featureName = parameterMap.containsKey(CommonConstants.FEATURENAME)
				? parameterMap.get(CommonConstants.FEATURENAME)
				: null;
		String newCategory = parameterMap.containsKey(CommonConstants.NEW_CATEGORY)
				? parameterMap.get(CommonConstants.NEW_CATEGORY)
				: null;
		String newFeatureName = parameterMap.containsKey(CommonConstants.NEW_FEATURENAME)
				? parameterMap.get(CommonConstants.NEW_FEATURENAME)
				: null;
		String newVideoLink = parameterMap.containsKey(CommonConstants.NEW_VIDEOLINK)
				? parameterMap.get(CommonConstants.NEW_VIDEOLINK)
				: null;
		String newAttachment = parameterMap.containsKey(CommonConstants.NEW_ATTACHMENT)
				? parameterMap.get(CommonConstants.NEW_ATTACHMENT)
				: null;
		String newInclude = parameterMap.containsKey(CommonConstants.NEW_INCLUDE)
				? parameterMap.get(CommonConstants.NEW_INCLUDE)
				: null;
		String newRecommended = parameterMap.containsKey(CommonConstants.NEW_RECOMMENDED)
				? parameterMap.get(CommonConstants.NEW_RECOMMENDED)
				: null;
		JSONArray newMasterListArray = new JSONArray();
		try {
			for(int i=0; i<masterListArray.length(); i++) {
				JSONObject feature = masterListArray.getJSONObject(i);
				if (feature.has(CommonConstants.FEATURENAME)) {
					if (feature.get(CommonConstants.FEATURENAME).equals(featureName)) {
						String objUid = feature.getString(CommonConstants.UID);
						updateJsonProperty(feature, CommonConstants.CATEGORY, newCategory);
						updateJsonProperty(feature, CommonConstants.FEATURENAME, newFeatureName);
						updateJsonProperty(feature, CommonConstants.VIDEOLINK, newVideoLink);
						updateJsonProperty(feature, CommonConstants.ATTACHMENT, newAttachment);
						updateProperty(modelNode, CommonConstants.INCLUDE, newInclude, objUid);
						updateProperty(modelNode, CommonConstants.RECOMMENDED, newRecommended, objUid);
					}
				}
				newMasterListArray.put(feature);
			}
		} catch(JSONException e) {
			LOGGER.error("JSONException in updateFeatureCategory : {}", e.getMessage(), e);
		}
		return newMasterListArray;
	}

}
