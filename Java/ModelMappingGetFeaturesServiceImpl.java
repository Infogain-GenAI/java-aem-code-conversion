package com.dcaadmin.core.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Value;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dcaadmin.core.services.ModelMappingGetFeaturesService;
import com.dcaadmin.core.util.CommonConstants;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.google.gson.Gson;

@Component(enabled = true, immediate = true, service = ModelMappingGetFeaturesService.class)
public class ModelMappingGetFeaturesServiceImpl implements ModelMappingGetFeaturesService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ModelMappingGetFeaturesServiceImpl.class);

	@Reference
	private QueryBuilder queryBuilder;

	@Override
	public String getItemsJSON(SlingHttpServletRequest request) {
		JSONArray masterListJson = new JSONArray();
		try {
			ResourceResolver resourceResolver = request.getResourceResolver();
			Map<String, String> parameterMap = getParameterMap(request);

			String modelName = getModelName(parameterMap);
			String category = parameterMap.get(CommonConstants.CATEGORY);
			String year = parameterMap.get(CommonConstants.YEAR);
			String addedFeaturePath = CommonConstants.ADD_UNIQUE_FEATURE_PATH.concat(year).concat("/").concat(modelName)
					.concat("/").concat(CommonConstants.FEATURE).concat("/").concat(category).concat("/")
					.concat(CommonConstants.ADDED_FEATURES);
			JSONArray addedFeaturesJson = getAddedFeaturesJSON(resourceResolver, addedFeaturePath);

			if (addedFeaturesJson.length() == 0) {
				Node modelNode = createNode(resourceResolver, parameterMap, request);
				if(modelNode == null) {
					String modelNodePath = CommonConstants.ADD_UNIQUE_FEATURE_PATH.concat(year).concat("/").concat(modelName);
					Resource modelNodeResource = resourceResolver.getResource(modelNodePath);
					modelNode = modelNodeResource.adaptTo(Node.class);
					if(!modelNode.hasProperty(CommonConstants.FEATURES)) {
						Resource masterListResource = resourceResolver.getResource(CommonConstants.JSON_PATH);
						Node masterListNode = masterListResource.adaptTo(Node.class);
						String masterList=filterMasterList(masterListNode);
						modelNode.setProperty(CommonConstants.FEATURES, masterList);
						masterListJson = getMasterListJSON(modelNode, category);
					}
					else
						masterListJson = getMasterListJSON(modelNode, category);
				} 
				else {
					Resource masterListResource = resourceResolver.getResource(CommonConstants.JSON_PATH);
					Node masterListNode = masterListResource.adaptTo(Node.class);
					String masterList=filterMasterList(masterListNode);
					modelNode.setProperty(CommonConstants.FEATURES, masterList);
					masterListJson = getMasterListJSON(modelNode, category);
				}
			}
			else {
				String modelNodePath = CommonConstants.ADD_UNIQUE_FEATURE_PATH.concat(year).concat("/").concat(modelName);
				Resource modelNodeResource = resourceResolver.getResource(modelNodePath);
				Node modelNode = modelNodeResource.adaptTo(Node.class);
				masterListJson = getMasterListJSON(modelNode, category);
				masterListJson = mergeJson(masterListJson, addedFeaturesJson);
			}
			resourceResolver.commit();

		} catch (Exception e) {
			LOGGER.error("Error in model mapping getItemsJSON : {}", e.getMessage(), e);
		}
		return masterListJson.toString();
	}

	private Map<String, String> getParameterMap(SlingHttpServletRequest request) {
		Map<String, String> parameterMap = new HashMap<String, String>();
		String category = request.getParameter(CommonConstants.CATEGORY);
		parameterMap.put(CommonConstants.CATEGORY, category);
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
		String modelCode = request.getParameter(CommonConstants.MODELCODE);
		parameterMap.put(CommonConstants.MODELCODE, modelCode);
		String packageId = request.getParameter(CommonConstants.PACKAGEID);
		parameterMap.put(CommonConstants.PACKAGEID, packageId);
		return parameterMap;
	}

	private String getModelName(Map<String, String> parameterMap) {
		String modelName = "";
		String model = parameterMap.containsKey(CommonConstants.MODEL) ? parameterMap.get(CommonConstants.MODEL) : null;
		String modelCode = parameterMap.containsKey(CommonConstants.MODELCODE)
				? parameterMap.get(CommonConstants.MODELCODE)
				: null;
		String packages = parameterMap.containsKey(CommonConstants.PACKAGE) ? parameterMap.get(CommonConstants.PACKAGE)
				: null;
		modelName = model.concat("-").concat(modelCode).concat("-").concat(packages);
		return modelName;
	}

	private Node createNode(ResourceResolver resourceResolver, Map<String, String> parameterMap, SlingHttpServletRequest request) {
		String year = parameterMap.containsKey(CommonConstants.YEAR) ? parameterMap.get(CommonConstants.YEAR) : null;
		String model = parameterMap.containsKey(CommonConstants.MODEL) ? parameterMap.get(CommonConstants.MODEL) : null;
		String trim = parameterMap.containsKey(CommonConstants.TRIM) ? parameterMap.get(CommonConstants.TRIM) : null;
		String driveTrain = parameterMap.containsKey(CommonConstants.DRIVELINE)
				? parameterMap.get(CommonConstants.DRIVELINE)
				: null;
		String transmission = parameterMap.containsKey(CommonConstants.TRANSMISSION)
				? parameterMap.get(CommonConstants.TRANSMISSION)
				: null;
		String packages = parameterMap.containsKey(CommonConstants.PACKAGE) ? parameterMap.get(CommonConstants.PACKAGE)
				: null;
		String[] modelCode = request.getParameterValues(CommonConstants.MODELCODE);
		String packageId = parameterMap.containsKey(CommonConstants.PACKAGEID) ? parameterMap.get(CommonConstants.PACKAGEID)
				: null;
		Node modelNode = null;
		try {
			Resource resource = resourceResolver.getResource(CommonConstants.ADD_UNIQUE_FEATURE_PATH);
			Node node = resource.adaptTo(Node.class);
			Node yearNode = getNodeExists(node, parameterMap.get(CommonConstants.YEAR));
			String modelName = getModelName(parameterMap);
			if(!yearNode.hasNode(modelName)) {
			modelNode = yearNode.addNode(modelName);
			modelNode.setProperty(CommonConstants.YEAR, year);
			modelNode.setProperty(CommonConstants.MODEL, model);
			modelNode.setProperty(CommonConstants.TRIM, trim);
			modelNode.setProperty(CommonConstants.DRIVELINE, driveTrain);
			modelNode.setProperty(CommonConstants.TRANSMISSION, transmission);
			modelNode.setProperty(CommonConstants.PACKAGE, packages);
			if (modelCode.length > 1)
				modelNode.setProperty(CommonConstants.MODELCODE, modelCode);
			else
				modelNode.setProperty(CommonConstants.MODELCODE, modelCode[0]);
			modelNode.setProperty(CommonConstants.STATUS, CommonConstants.TRUE);
			modelNode.setProperty(CommonConstants.PACKAGEID, packageId);
			modelNode.addNode(CommonConstants.FEATURE);
			resourceResolver.commit();
			}
		} catch (PersistenceException e) {
			LOGGER.error("Persistence Exception in createNode {}", e.getMessage(), e);
		} catch (RepositoryException e) {
			LOGGER.error("Repository Exception in createNode {}", e.getMessage());
		}
		return modelNode;
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

	private JSONArray getAddedFeaturesJSON(ResourceResolver resourceResolver, String resourcePath) {
		List<Map<String, Object>> oMap = new ArrayList<Map<String, Object>>();
		JSONArray jsonArray = null;
		try {
			Session session = resourceResolver.adaptTo(Session.class);
			Map<String, String> map = new HashMap<String, String>();
			map.put(CommonConstants.TYPE, CommonConstants.NT_UNSTRUCTURED);
			map.put(CommonConstants.PATH, resourcePath);
			map.put(CommonConstants.P_LIMIT, CommonConstants.MINUS_1);
			PredicateGroup predicateGroup = PredicateGroup.create(map);
			Query query = queryBuilder.createQuery(predicateGroup, session);

			SearchResult result = query.getResult();
			List<Hit> hits = result.getHits();
			
			for (Hit hit : hits) {
				ValueMap properties = hit.getProperties();
				Map<String, Object> hashMap = new HashMap<String, Object>();
				for (String key : properties.keySet()) {
					if (!(key.equalsIgnoreCase(CommonConstants.JCR_PRIMARYTYPE)))
						hashMap.put(key, properties.get(key));
				}
				oMap.add(hashMap);
			}
			String json = new Gson().toJson(oMap);
			jsonArray = new JSONArray(json);

		} catch (Exception e) {
			LOGGER.error("Error in getAddedFeaturesJSON : {}", e.getMessage(), e);
		}
		return jsonArray;
	}

	private JSONArray getMasterListJSON(Node modelNode, String category) {
		JSONArray newFeatureArray = new JSONArray();
		try {
			String masterListJson = modelNode.getProperty(CommonConstants.FEATURES).getString();
			List<String> includeList = getProperty(modelNode, CommonConstants.INCLUDE);
			List<String> recommendedList = getProperty(modelNode, CommonConstants.RECOMMENDED);

			JSONArray masterListArray = new JSONArray(masterListJson);
			for (int i = 0; i < masterListArray.length(); i++) {
				JSONObject feature = masterListArray.getJSONObject(i);
				if (feature.has(CommonConstants.CATEGORY)) {
					if (feature.get(CommonConstants.CATEGORY).equals(category)) {
						if (includeList.contains(feature.getString(CommonConstants.UID)))
							feature.put(CommonConstants.INCLUDE, CommonConstants.TRUE);
						else
							feature.put(CommonConstants.INCLUDE, CommonConstants.FALSE);

						if (recommendedList.contains(feature.getString(CommonConstants.UID)))
							feature.put(CommonConstants.RECOMMENDED, CommonConstants.TRUE);
						else
							feature.put(CommonConstants.RECOMMENDED, CommonConstants.FALSE);

						newFeatureArray.put(feature);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error in getMasterListJSON : {}", e.getMessage(), e);
		}
		return newFeatureArray;
	}

	private List<String> getProperty(Node node, String property) {
		List<String> valueList = new ArrayList<String>();
		try {
			if (node.hasProperty(property)) {
				Value[] values = node.getProperty(property).getValues();

				for (Value v : values) {
					valueList.add(v.getString());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error in getProperty : {}", e.getMessage(), e);
		}
		return valueList;
	}

	private JSONArray mergeJson(JSONArray jsonArray, JSONArray jsonArray1) {
		try {
			for (int i = 0; i < jsonArray1.length(); i++) {
				JSONObject jsonObject = jsonArray1.getJSONObject(i);
				jsonArray.put(jsonObject);
			}
		} catch (Exception e) {
			LOGGER.error("Error in mergeJson : {}", e.getMessage(), e);
		}
		return jsonArray;
	}

	@Override
	public String getAllFeatures(SlingHttpServletRequest request) {
		String json = null;
		JSONArray masterListJson = null;
		try {
			ResourceResolver resourceResolver = request.getResourceResolver();
			Map<String, String> parameterMap = getParameterMap(request);

			String modelName = getModelName(parameterMap);
			String year = parameterMap.get(CommonConstants.YEAR);
			String addedFeaturePath = CommonConstants.ADD_UNIQUE_FEATURE_PATH.concat(year).concat("/").concat(modelName)
					.concat("/").concat(CommonConstants.FEATURE);

			masterListJson = getAllMasterListJSON(resourceResolver, modelName, year);

			List<Map<String, Object>> oMap = new ArrayList<Map<String, Object>>();
			Session session = resourceResolver.adaptTo(Session.class);
			Map<String, String> map = new HashMap<String, String>();
			map.put(CommonConstants.TYPE, CommonConstants.NT_UNSTRUCTURED);
			map.put(CommonConstants.PATH, addedFeaturePath);
			map.put(CommonConstants.P_LIMIT, CommonConstants.MINUS_1);
			PredicateGroup predicateGroup = PredicateGroup.create(map);
			Query query = queryBuilder.createQuery(predicateGroup, session);

			SearchResult result = query.getResult();
			List<Hit> hits = result.getHits();

			for (Hit hit : hits) {
				String path = hit.getPath();
				Resource categoryResource = resourceResolver.getResource(path);
				Node categoryNode = categoryResource.adaptTo(Node.class);
				if (categoryNode.hasNodes()) {
					NodeIterator categoryIterator = categoryNode.getNodes();
					while (categoryIterator.hasNext()) {
						Node categoryIteratorNode = categoryIterator.nextNode();
						if (categoryIteratorNode.hasProperties()) {
							categoryIteratorNode.getProperties();
							Resource childResource = resourceResolver.getResource(categoryIteratorNode.getPath());
							ValueMap property = childResource.adaptTo(ValueMap.class);
							Map<String, Object> hashMap = new HashMap<>();
							for (String key : property.keySet()) {
								if (!(key.equalsIgnoreCase(CommonConstants.JCR_PRIMARYTYPE)))
									hashMap.put(key, property.get(key));
							}
							if(!hashMap.isEmpty() && !hashMap.containsKey(CommonConstants.CATEGORY)) {
								String category = categoryIteratorNode.getAncestor(categoryIteratorNode.getDepth()-2).getName();
								hashMap.put(CommonConstants.CATEGORY, category);
							}
							oMap.add(hashMap);
						}
					}
				}
			}

			json = new Gson().toJson(oMap);
			JSONArray addedFeatureJson = new JSONArray(json);

			masterListJson = mergeJson(masterListJson, addedFeatureJson);

		} catch (Exception e) {
			LOGGER.error("Error in model mapping getAllFeatures : {}", e.getMessage(), e);
		}
		return masterListJson.toString();
	}

	private JSONArray getAllMasterListJSON(ResourceResolver resourceResolver, String modelName, String year) {
		JSONArray newFeatureArray = new JSONArray();
		try {
			String modelResourcePath = CommonConstants.ADD_UNIQUE_FEATURE_PATH.concat(year).concat("/")
					.concat(modelName);
			Resource modelResource = resourceResolver.getResource(modelResourcePath);
			Node modelNode = modelResource.adaptTo(Node.class);
			String masterListJson = modelNode.getProperty(CommonConstants.FEATURES).getString();
			List<String> includeList = getProperty(modelNode, CommonConstants.INCLUDE);
			List<String> recommendedList = getProperty(modelNode, CommonConstants.RECOMMENDED);

			JSONArray masterListArray = new JSONArray(masterListJson);
			for (int i = 0; i < masterListArray.length(); i++) {
				JSONObject feature = masterListArray.getJSONObject(i);
				if (includeList.contains(feature.getString(CommonConstants.UID)))
					feature.put(CommonConstants.INCLUDE, CommonConstants.TRUE);
				else
					feature.put(CommonConstants.INCLUDE, CommonConstants.FALSE);

				if (recommendedList.contains(feature.getString(CommonConstants.UID)))
					feature.put(CommonConstants.RECOMMENDED, CommonConstants.TRUE);
				else
					feature.put(CommonConstants.RECOMMENDED, CommonConstants.FALSE);

				newFeatureArray.put(feature);
			}
		} catch (Exception e) {
			LOGGER.error("Error in getAllMasterListJSON : {}", e.getMessage(), e);
		}
		return newFeatureArray;
	}
	
	private String filterMasterList(Node allMasterfeaturesNode) {
			JSONArray activeMasterArray = new JSONArray();
			try {
				String allfeatures = allMasterfeaturesNode.getProperty(CommonConstants.FEATURES).getString();
				JSONArray masterListArray = new JSONArray(allfeatures);
				for (int i = 0; i < masterListArray.length(); i++) {
					JSONObject features = masterListArray.getJSONObject(i);
					if (features.has(CommonConstants.ACTIVE)) {
						if((boolean) features.get(CommonConstants.ACTIVE).equals(CommonConstants.TRUE)) {
							LOGGER.debug("Active Masterlist {}",features);
							activeMasterArray.put(features);
						}
					}
				}
			} catch (Exception e) {
				LOGGER.error("Error in filterMasterList: {}", e.getMessage(), e);
			}
			return activeMasterArray.toString();
		}

}
