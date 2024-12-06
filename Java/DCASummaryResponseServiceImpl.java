package com.haea.daehyundai.core.dca.services.impl;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.haea.daehyundai.core.dca.pojos.*;
import com.haea.daehyundai.core.dca.services.DCASummaryResponseService;
import com.haea.daehyundai.core.dca.util.CommonDCAConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.*;

/**
 * @author Sindhu Kandaiyan
 *
 */
@Component(service = DCASummaryResponseService.class, immediate = true)
public class DCASummaryResponseServiceImpl implements DCASummaryResponseService {

    private static final Logger LOG = LoggerFactory.getLogger(DCASummaryResponseServiceImpl.class);

    @Override
    public String getSummaryResponse(String response, QueryBuilder queryBuilder, ResourceResolver resolver, SlingHttpServletRequest request) {
        SearchResult result = getModelQueryResult(request, queryBuilder, resolver);
        List<Feature> featureJsonList = getModelDetails(result, resolver);
        String category = getCategoryDetail(response);
        List<Feature> featureList = getFeatureJsonWithOOE(featureJsonList, category, resolver, queryBuilder);
        List<SummarySharedWithCustomer> sharedWithCustomerList = getSummarySharedQuestionList(response, featureList);
        List<SummaryNotReviewedItem> notReviewedList = getSummaryNonReviewedList(response, featureList, queryBuilder, request);
        List<SummarySkippedItem> skippedList = getSummarySkippedList(response, featureList, request, queryBuilder);
        List<SummaryNotSharedWithCustomer> notSharedWithCustomerList = getSummaryNotSharedQuestionList(response, resolver, queryBuilder, sharedWithCustomerList, featureList, result);
        String resp = getResponse(sharedWithCustomerList, notReviewedList, notSharedWithCustomerList, skippedList, response);
        return resp;
    }

    private SearchResult getModelQueryResult(SlingHttpServletRequest request, QueryBuilder queryBuilder, ResourceResolver resolver) {
        SearchResult result = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            Session session = resolver.adaptTo(Session.class);
            String modelDetails = request.getHeader(CommonDCAConstants.MODELDETAILS);
            Vehicle vehicle = mapper.readValue(modelDetails, Vehicle.class);
            String year = vehicle.getYear();
            String model = vehicle.getModel();
            String modelCode = vehicle.getModelCode();
            String packages = vehicle.getPackages();
            Map<String, String> map = new HashMap<>();
            map.put(CommonDCAConstants.PATH, CommonDCAConstants.MODELMAPPINGPATH);
            map.put(CommonDCAConstants.TYPE, CommonDCAConstants.NT_UNSTRUCTURED);
            map.put(CommonDCAConstants.P_LIMIT, CommonDCAConstants.MINUS_1);
            map.put(CommonDCAConstants.ONE_PROPERTY, CommonDCAConstants.YEAR);
            map.put(CommonDCAConstants.ONE_PROPERTY_VALUE, year);
            map.put(CommonDCAConstants.TWO_PROPERTY, CommonDCAConstants.MODEL);
            map.put(CommonDCAConstants.TWO_PROPERTY_VALUE, model);
            map.put(CommonDCAConstants.THREE_PROPERTY, CommonDCAConstants.MODELCODE);
            map.put(CommonDCAConstants.THREE_PROPERTY_VALUE, modelCode);
            map.put(CommonDCAConstants.FOUR_PROPERTY, CommonDCAConstants.PACKAGE);
            map.put(CommonDCAConstants.FOUR_PROPERTY_VALUE, packages);
            PredicateGroup predicateGroup = PredicateGroup.create(map);
            Query query = queryBuilder.createQuery(predicateGroup, session);
            result = query.getResult();
        } catch(IOException e) {
            LOG.error("IOException {}",e.getMessage());
        }
        return result;
    }

    private List<Feature> getFeatureJsonWithOOE(List<Feature> featureJsonList, String category, ResourceResolver resolver, QueryBuilder queryBuilder) {
        List<Feature> featureList = new ArrayList<>();
        SearchResult result = getSearchResponse(category, resolver,queryBuilder);
        featureList = getOOEResponseWithDC(result, featureJsonList, resolver);
        return featureList;
    }

    private List<Feature> getOOEResponseWithDC(SearchResult result, List<Feature> featureJsonList, ResourceResolver resolver) {
        try {
            List<Hit> hits = result.getHits();
            for (Hit hit : hits) {
                String path = hit.getPath();
                Resource questionResource = resolver.getResource(path);
                Node questionNode = questionResource.adaptTo(Node.class);
                if (questionNode.hasProperties()) {
                    if (questionNode.getProperty(CommonDCAConstants.ACTIVE).getString().equalsIgnoreCase("true")) {
                        Feature feature = new Feature();
                        feature.setCategory(questionNode.getProperty(CommonDCAConstants.CATEGORY).getString());
                        feature.setVideoLink(questionNode.getProperty(CommonDCAConstants.VIDEOLINK).getString());
                        feature.setAttachment(questionNode.getProperty(CommonDCAConstants.ATTACHMENT).getString());
                        feature.setFeatureName(questionNode.getProperty(CommonDCAConstants.FEATURENAME).getString());
                        feature.setUid(questionNode.getProperty(CommonDCAConstants.UID).getString());
                        feature.setStatus(questionNode.getProperty(CommonDCAConstants.ACTIVE).getString());
                        featureJsonList.add(feature);
                    }
                }
            }
        } catch(RepositoryException e) {
            LOG.debug("Repository Exception {}",e.getMessage());
        }
        return featureJsonList;
    }

    private SearchResult getSearchResponse(String category, ResourceResolver resolver, QueryBuilder queryBuilder) {
        SearchResult result = null;
        Session session = resolver.adaptTo(Session.class);
        Map<String, String> map = new HashMap<>();
        map.put(CommonDCAConstants.PATH, CommonDCAConstants.ADD_FEATURE_PATH);
        map.put(CommonDCAConstants.TYPE, CommonDCAConstants.NT_UNSTRUCTURED);
        map.put(CommonDCAConstants.P_LIMIT, CommonDCAConstants.MINUS_1);
        map.put(CommonDCAConstants.ONE_PROPERTY, CommonDCAConstants.CATEGORY);
        map.put(CommonDCAConstants.ONE_PROPERTY_VALUE, category);
        PredicateGroup predicateGroup = PredicateGroup.create(map);
        Query query = queryBuilder.createQuery(predicateGroup, session);
        result = query.getResult();
        return result;
    }

    private String getCategoryDetail(String response) {
        ObjectMapper mapper = new ObjectMapper();
        String category = "";
        try {
            SummaryResponse summaryResponse = new SummaryResponse();
            summaryResponse = mapper.readValue(response, SummaryResponse.class);
            List<SummaryResponseString> responseString = summaryResponse.getResponseString();
            for (SummaryResponseString respString : responseString) {
                if(respString.getIce().equalsIgnoreCase("1")) {
                    category = "ice";
                } else if (respString.getEv().equalsIgnoreCase("1")) {
                    category = "ev";
                } else if(respString.getFcev().equalsIgnoreCase("1")) {
                    category = "fcev";
                } else {
                    category = "phev";
                }
            }
        } catch(IOException e) {
            LOG.debug("Json Exception {}", e.getMessage());
        }
        return category;
    }

    private List<SummaryNotSharedWithCustomer> getSummaryNotSharedQuestionList(String response, ResourceResolver resolver, QueryBuilder queryBuilder, List<SummarySharedWithCustomer> sharedWithCustomerList, List<Feature> featureList, SearchResult result) {
        List<SummaryNotSharedWithCustomer> notSharedWithCustomer = new ArrayList<SummaryNotSharedWithCustomer>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            SummaryResponse resp = mapper.readValue(response, SummaryResponse.class);
            List<SummaryResponseString> responseString = resp.getResponseString();
            List<String> includedList = getIncludedArray(result, resolver);
            List<String> recommendedList = getRecommendedArray(result, resolver);

            notSharedWithCustomer = getNotSharedResponse(sharedWithCustomerList, responseString, queryBuilder,featureList, includedList, recommendedList);

        } catch(IOException e) {
            LOG.debug("IOException {}", e.getMessage());
        }
        return notSharedWithCustomer;
    }

    private List<String> getRecommendedArray(SearchResult result, ResourceResolver resolver) {
        List<String> recommendedList = new ArrayList<>();
        String[] recommendedArray = null;
        try {
            List<Hit> hits = result.getHits();
            for (Hit hit : hits) {
                String path = hit.getPath();
                Resource resource = resolver.getResource(path);
                Node node = resource.adaptTo(Node.class);
                if (node.hasProperty(CommonDCAConstants.RECOMMENDED)) {
                    ValueMap contentValueMap = resource.getValueMap();
                    recommendedArray = contentValueMap.get(CommonDCAConstants.RECOMMENDED, String[].class);
                }
            }
            recommendedList = Arrays.asList(recommendedArray);
        } catch (RepositoryException e) {
            LOG.error("Repository Exception {}", e.getMessage());
        }
        return recommendedList;
    }

    private List<String> getIncludedArray(SearchResult result, ResourceResolver resolver) {
       List<String> includeList = new ArrayList<>();
       String[] includedArray = null;
        try {
             List<Hit> hits = result.getHits();
             for (Hit hit : hits) {
                String path = hit.getPath();
                Resource resource = resolver.getResource(path);
                Node node = resource.adaptTo(Node.class);
                 if (node.hasProperty(CommonDCAConstants.INCLUDE)) {
                     ValueMap contentValueMap = resource.getValueMap();
                     includedArray = contentValueMap.get(CommonDCAConstants.INCLUDE, String[].class);
                 }
             }
                includeList = Arrays.asList(includedArray);
        } catch (RepositoryException e) {
                LOG.error("Repository Exception {}", e.getMessage());
        }
        return includeList;
    }

    private List<SummaryNotSharedWithCustomer> getNotSharedResponse(List<SummarySharedWithCustomer> sharedWithCustomerList, List<SummaryResponseString> responseString, QueryBuilder queryBuilder, List<Feature> featureList, List<String> includedList, List<String> recommendedList) {
        List<SummaryNotSharedWithCustomer> summaryNotSharedList = new ArrayList<SummaryNotSharedWithCustomer>();
        Map<String,Integer> categoryMap = getCategoryDetails(responseString);
        for (Feature feature : featureList) {
            SummaryNotSharedWithCustomer summaryNotShared = getSummaryNotShared(feature, sharedWithCustomerList, includedList, recommendedList);
            if(categoryMap.containsKey(summaryNotShared.getCategory())) {
                summaryNotShared.setCategorieId(categoryMap.get(summaryNotShared.getCategory()));
                summaryNotSharedList.add(summaryNotShared);
            }
        }
        return summaryNotSharedList;
    }

    private List<SummaryNotSharedWithCustomer> getSummaryNotSharedList(List<SummarySharedWithCustomer> sharedWithCustomerList, List<Feature> featurefinalList) {
        List<SummaryNotSharedWithCustomer> summaryNotSharedList = new ArrayList<>();
        for(Feature feature : featurefinalList) {
            if(!sharedWithCustomerList.contains(feature)) {
                if((!feature.getAttachment().equalsIgnoreCase(""))|| (!!feature.getVideoLink().equalsIgnoreCase("")) ) {
                    SummaryNotSharedWithCustomer summaryNotSharedWithCustomer = new SummaryNotSharedWithCustomer();
                    summaryNotSharedWithCustomer.setQuestion(feature.getFeatureName());
                    summaryNotSharedWithCustomer.setCategory(feature.getCategory());
                    summaryNotSharedWithCustomer.setVideoLink(feature.getVideoLink());
                    summaryNotSharedList.add(summaryNotSharedWithCustomer);
                }
            }
        }
        return summaryNotSharedList;
    }

    private Map<String,Integer> getCategoryDetails(List<SummaryResponseString> responseString) {
        Map<String,Integer> categoryMap = new HashMap<String,Integer>();
        String ev = responseString.get(0).getEv();
        String ice = responseString.get(0).getIce();
        String fcev = responseString.get(0).getFcev();
        String phev = responseString.get(0).getPhev();
        if(ice.equalsIgnoreCase("1")) {
            categoryMap.put("ice",0);
        } else if(ev.equalsIgnoreCase("1")) {
            categoryMap.put("ev",0);
        } else if(fcev.equalsIgnoreCase("1")) {
            categoryMap.put("fcev",0);
        } else if(phev.equalsIgnoreCase("1")) {
            categoryMap.put("phev",0);
        }
        categoryMap.put("convenience",2);
        categoryMap.put("driver-information",3);
        categoryMap.put("technology",4);
        categoryMap.put("safety",5);
        return categoryMap;
    }

    private SummaryNotSharedWithCustomer getSummaryNotShared(Feature feature, List<SummarySharedWithCustomer> sharedWithCustomerList, List<String> includedList, List<String> recommendedList) {
        SummaryNotSharedWithCustomer summaryNotShared = new SummaryNotSharedWithCustomer();
        try {
            if ((!sharedWithCustomerList.contains(feature.getUid())) && (!feature.getAttachment().equalsIgnoreCase("")) || (!feature.getVideoLink().equalsIgnoreCase(""))) {
                if(includedList.contains(feature.getUid()) || (recommendedList.contains(feature.getUid()))) {
                    summaryNotShared.setAssetShared("");
                    summaryNotShared.setAttachment(feature.getAttachment());
                    summaryNotShared.setCategory(feature.getCategory());
                    summaryNotShared.setVideoLink(feature.getVideoLink());
                    summaryNotShared.setQuestionId(feature.getUid());
                    summaryNotShared.setQuestion(feature.getFeatureName());
                    summaryNotShared.setResponse("");
                }
            }
        } catch(Exception e) {
            LOG.debug("getSummaryNotShared Exception {}", e);
        }
        return summaryNotShared;
    }

    private List<Feature> getModelDetails(SearchResult result, ResourceResolver resolver) {
        List<Feature> featureJsonList = new ArrayList<Feature>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Hit> hits = result.getHits();
            for (Hit hit : hits) {
                String path = hit.getPath();
                Resource resource = resolver.getResource(path);
                Node node = resource.adaptTo(Node.class);
                if (node.hasProperty(CommonDCAConstants.FEATURES)) {
                    String featureValue = node.getProperty(CommonDCAConstants.FEATURES).getString();
                    CollectionType typeReference =
                            TypeFactory.defaultInstance().constructCollectionType(List.class, Feature.class);
                    featureJsonList = mapper.readValue(featureValue, typeReference);
                }
                featureJsonList = getUniqueQuestions(node, featureJsonList);
            }
        } catch(IOException | RepositoryException e) {
            LOG.debug("IOEXception {}", e.getMessage());
        }
        return featureJsonList;
    }

    private List<Feature> getUniqueQuestions(Node node, List<Feature> featureJsonList) {
        try {
            if (node.hasNode(CommonDCAConstants.FEATURE)) {
                Node mainNode = node.getNode(CommonDCAConstants.FEATURE);
                if (mainNode.hasNodes()) {
                    NodeIterator categoryIterator = mainNode.getNodes();
                    while (categoryIterator.hasNext()) {
                        Node categoryNode = categoryIterator.nextNode();
                        if (categoryNode.hasNode(CommonDCAConstants.ADDED_FEATURES)) {
                            Node addedFeatureNode = categoryNode.getNode(CommonDCAConstants.ADDED_FEATURES);
                            if (addedFeatureNode.hasNodes()) {
                                NodeIterator featureIterator = addedFeatureNode.getNodes();
                                while (featureIterator.hasNext()) {
                                    Node featureNode = featureIterator.nextNode();
                                    Feature feature = new Feature();
                                    feature.setFeatureName(
                                            getNodeProperty(featureNode, CommonDCAConstants.FEATURE_NAME));
                                    feature.setCategory(getNodeProperty(featureNode, CommonDCAConstants.CATEGORY));
                                    feature.setUid(getNodeProperty(featureNode, CommonDCAConstants.UID));
                                    feature.setVideoLink(getNodeProperty(featureNode, CommonDCAConstants.VIDEOLINK));
                                    feature.setAttachment(getNodeProperty(featureNode, CommonDCAConstants.ATTACHMENT));
                                    feature.setStatus(getNodeProperty(featureNode, CommonDCAConstants.ACTIVE));
                                    featureJsonList.add(feature);
                                }
                            }
                        }
                    }
                }

            }
        } catch (RepositoryException e) {
            LOG.error("Repository Exception {}", e.getMessage(), e);
        }
        return featureJsonList;
    }

    private List<SummarySkippedItem> getSummarySkippedList(String response, List<Feature> featureList, SlingHttpServletRequest request, QueryBuilder queryBuilder) {
        List<SummarySkippedItem> summaryList = new ArrayList<SummarySkippedItem>();
        List<SummarySkippedItem> summarySkipedItemList = new ArrayList<SummarySkippedItem>();
        ResourceResolver resolver = request.getResourceResolver();
        try {
            ObjectMapper mapper = new ObjectMapper();
            SummaryResponse resp = mapper.readValue(response, SummaryResponse.class);
            List<SummaryResponseString> responseString = resp.getResponseString();
            for (SummaryResponseString respString : responseString) {
                summarySkipedItemList = respString.getSkippedItems();
            }
            if(summarySkipedItemList != null) {
                for (SummarySkippedItem customer : summarySkipedItemList) {
                    SummarySkippedItem summaryCustomer = new SummarySkippedItem();
                    summaryCustomer = getSummarySkippedObj(customer, featureList);
                    summaryList.add(summaryCustomer);

                }
            }
        }catch(IOException e) {
            LOG.debug("IOEXception {} ",e.getMessage());
        }
        return summaryList;
    }

    private List<SummarySkippedItem> getSummarySkippedObjUnique(SummarySkippedItem customer, ResourceResolver resolver, QueryBuilder queryBuilder, SlingHttpServletRequest request) {
        List<SummarySkippedItem> summarySkippedItemList = new ArrayList<SummarySkippedItem>();
        SearchResult mappingResult = getMappingResult(resolver, request, queryBuilder);
        String[] categoryArray = new String[]{"convenience", "driver-information", "technology", "safety"};
        try {
            List<Hit> hits = mappingResult.getHits();
            for (Hit hit : hits) {
                String path = hit.getPath();
                Resource resource = resolver.getResource(path);
                Node node = resource.adaptTo(Node.class);
                if (node.hasNode(CommonDCAConstants.FEATURE)) {
                    Node featureNode = node.getNode(CommonDCAConstants.FEATURE);
                    NodeIterator nodeList = featureNode.getNodes(categoryArray);
                    while (nodeList.hasNext()) {
                        Node childNode = nodeList.nextNode();
                        if (childNode.hasNode(CommonDCAConstants.ADDED_FEATURES)) {
                            Node addedFeature = childNode.getNode(CommonDCAConstants.ADDED_FEATURES);
                            NodeIterator modelNode = addedFeature.getNodes();
                            while (modelNode.hasNext()) {
                                Node uniqueNode = modelNode.nextNode();
                                SummarySkippedItem skippedItem = new SummarySkippedItem();
                                skippedItem.setQuestion(getNodeProperty(uniqueNode, CommonDCAConstants.FEATURE_NAME));
                                skippedItem.setVideoLink(getNodeProperty(uniqueNode, CommonDCAConstants.VIDEO_LINK));
                                skippedItem.setAttachment(getNodeProperty(uniqueNode, CommonDCAConstants.ATTACHMENT));
                                skippedItem.setQuestionId(getNodeProperty(uniqueNode, CommonDCAConstants.UID));
                                String[] urlArray = childNode.getPath().split("/");
                                String category = urlArray[urlArray.length - 1];
                                skippedItem.setCategory(category);
                                summarySkippedItemList.add(skippedItem);
                            }
                        }
                    }
                }
            }
        }catch(RepositoryException e) {
            LOG.debug("Repository Exception {}", e.getMessage());
        }
        return summarySkippedItemList;
    }

    private String getNodeProperty(Node node, String property) {
        String value = "";
        try {
            if (node.hasProperty(property)) {
                value = node.getProperty(property).getString();
            }
        } catch (RepositoryException e) {
            LOG.error("Repository Exception {}", e.getMessage());
        }
        return value;
    }

    private SearchResult getMappingResult(ResourceResolver resolver, SlingHttpServletRequest request, QueryBuilder queryBuilder) {
        SearchResult result = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            Session session = resolver.adaptTo(Session.class);
            String modelDetails = request.getHeader(CommonDCAConstants.MODELDETAILS);
            Vehicle vehicle = mapper.readValue(modelDetails, Vehicle.class);
            String year = vehicle.getYear();
            String model = vehicle.getModel();
            String modelCode = vehicle.getModelCode();
            String packages = vehicle.getPackages();
            Map<String, String> map = new HashMap<>();
            map.put(CommonDCAConstants.PATH, CommonDCAConstants.MODELMAPPINGPATH);
            map.put(CommonDCAConstants.TYPE, CommonDCAConstants.NT_UNSTRUCTURED);
            map.put(CommonDCAConstants.P_LIMIT, CommonDCAConstants.MINUS_1);
            map.put(CommonDCAConstants.ONE_PROPERTY, CommonDCAConstants.YEAR);
            map.put(CommonDCAConstants.ONE_PROPERTY_VALUE, year);
            map.put(CommonDCAConstants.TWO_PROPERTY, CommonDCAConstants.MODEL);
            map.put(CommonDCAConstants.TWO_PROPERTY_VALUE, model);
            map.put(CommonDCAConstants.THREE_PROPERTY, CommonDCAConstants.MODELCODE);
            map.put(CommonDCAConstants.THREE_PROPERTY_VALUE, modelCode);
            map.put(CommonDCAConstants.FOUR_PROPERTY, CommonDCAConstants.PACKAGE);
            map.put(CommonDCAConstants.FOUR_PROPERTY_VALUE, packages);
            PredicateGroup predicateGroup = PredicateGroup.create(map);
            Query query = queryBuilder.createQuery(predicateGroup, session);
            result = query.getResult();
        } catch(JsonMappingException e) {
            LOG.error("JsonMappingException {}", e.getMessage());
        } catch(IOException e) {
            LOG.error("IOException {}", e.getMessage());
        }
        return result;
    }

    private SummarySkippedItem getSummarySkippedObj(SummarySkippedItem customer, List<Feature> featureList) {
        SummarySkippedItem summarySkippedItem = new SummarySkippedItem();
        for(Feature feature : featureList) {
            if(feature.getUid().equalsIgnoreCase(customer.getQuestionId())) {
                summarySkippedItem.setQuestion(feature.getFeatureName());
                summarySkippedItem.setAttachment(feature.getAttachment());
                summarySkippedItem.setVideoLink(feature.getVideoLink());
                summarySkippedItem.setQuestionId(customer.getQuestionId());
                break;
            }
        }
        return summarySkippedItem;
    }

    private List<SummaryNotReviewedItem> getSummaryNonReviewedList(String response, List<Feature> featureList, QueryBuilder queryBuilder, SlingHttpServletRequest request) {
        List<SummaryNotReviewedItem> summaryList = new ArrayList<SummaryNotReviewedItem>();
        List<SummaryNotReviewedItem> summaryNotReviewedItemList = new ArrayList<SummaryNotReviewedItem>();
        try {
            ResourceResolver resolver = request.getResourceResolver();
            ObjectMapper mapper = new ObjectMapper();
            SummaryResponse resp = mapper.readValue(response, SummaryResponse.class);
            List<SummaryResponseString> responseString = resp.getResponseString();
            for (SummaryResponseString respString : responseString) {
                summaryNotReviewedItemList = respString.getNotReviewedItems();
            }
            if(summaryNotReviewedItemList != null) {
                for (SummaryNotReviewedItem customer : summaryNotReviewedItemList) {
                    SummaryNotReviewedItem summaryCustomer = new SummaryNotReviewedItem();
                    summaryCustomer = getSharedNotReviewedObj(customer, featureList);
                    summaryCustomer.setResponse(customer.getResponse());
                    summaryCustomer.setComments(customer.getComments());
                    summaryCustomer.setQuestionId(customer.getQuestionId());
                    summaryList.add(summaryCustomer);
                    //summaryList = getSummaryNotReviewedObjUnique(summaryNotReviewedItemList, resolver, queryBuilder, request);
                }
            }
        }catch(IOException e) {
            LOG.debug("IOEXception {} ",e.getMessage());
        }
        return summaryList;
    }

    private List<SummaryNotReviewedItem> getSummaryNotReviewedObjUnique(List<SummaryNotReviewedItem> notReviewedList, ResourceResolver resolver, QueryBuilder queryBuilder, SlingHttpServletRequest request) {
        List<SummaryNotReviewedItem> summaryNotReviewedList = new ArrayList<SummaryNotReviewedItem>();
        SearchResult mappingResult = getMappingResult(resolver, request, queryBuilder);
        String[] categoryArray = new String[]{"convenience", "driver-information", "technology", "safety"};
        try {
            List<Hit> hits = mappingResult.getHits();
            for (Hit hit : hits) {
                String path = hit.getPath();
                Resource resource = resolver.getResource(path);
                Node node = resource.adaptTo(Node.class);
                if (node.hasNode(CommonDCAConstants.FEATURE)) {
                    Node featureNode = node.getNode(CommonDCAConstants.FEATURE);
                    NodeIterator nodeList = featureNode.getNodes(categoryArray);
                    while (nodeList.hasNext()) {
                        Node childNode = nodeList.nextNode();
                        if (childNode.hasNode(CommonDCAConstants.ADDED_FEATURES)) {
                            Node addedFeature = childNode.getNode(CommonDCAConstants.ADDED_FEATURES);
                            NodeIterator modelNode = addedFeature.getNodes();
                            while (modelNode.hasNext()) {
                                Node uniqueNode = modelNode.nextNode();
                                SummaryNotReviewedItem notReviewedItem = new SummaryNotReviewedItem();
                                notReviewedItem.setQuestion(getNodeProperty(uniqueNode, CommonDCAConstants.FEATURE_NAME));
                                notReviewedItem.setVideoLink(getNodeProperty(uniqueNode, CommonDCAConstants.VIDEO_LINK));
                                notReviewedItem.setAttachment(getNodeProperty(uniqueNode, CommonDCAConstants.ATTACHMENT));
                                notReviewedItem.setQuestionId(getNodeProperty(uniqueNode, CommonDCAConstants.UID));
                                String[] urlArray = childNode.getPath().split("/");
                                String category = urlArray[urlArray.length - 1];
                                notReviewedItem.setCategory(category);
                                for(SummaryNotReviewedItem customer : notReviewedList) {
                                    if(customer.getQuestionId().equalsIgnoreCase(notReviewedItem.getQuestionId())) {
                                        notReviewedItem.setComments(customer.getComments());
                                        notReviewedItem.setResponse(customer.getResponse());
                                        summaryNotReviewedList.add(notReviewedItem);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }catch(RepositoryException e) {
            LOG.debug("Repository Exception {}", e.getMessage());
        }
        return summaryNotReviewedList;
    }

    private String getResponse(List<SummarySharedWithCustomer> sharedList, List<SummaryNotReviewedItem> summaryNotReviewedItemList, List<SummaryNotSharedWithCustomer> notSharedWithCustomerList, List<SummarySkippedItem> skippedItem,  String resp) {
        ObjectMapper mapper = new ObjectMapper();
        String responseObj = "";
        SummaryResponse response = new SummaryResponse();
        try {
            response = mapper.readValue(resp, SummaryResponse.class);
            List<SummaryResponseString> responseString = response.getResponseString();
            List<SummaryResponseString> responseStringList = new ArrayList<>();
            SummaryResponse summaryResponse = new SummaryResponse();
            for(SummaryResponseString respString : responseString) {
                SummaryResponseString responseString1 = new SummaryResponseString();
                responseString1.setEmail(respString.getEmail());
                responseString1.setDealerCode(respString.getDealerCode());
                responseString1.setFirstName(respString.getFirstName());
                responseString1.setLastName(respString.getLastName());
                responseString1.setSalesPersonCode(respString.getSalesPersonCode());
                responseString1.setVinNumber(respString.getVinNumber());
                responseString1.setTransmission(respString.getTransmission());
                responseString1.setYear(respString.getYear());
                responseString1.setModel(respString.getModel());
                responseString1.setPackage(respString.getPackage());
                responseString1.setTrim(respString.getTrim());
                responseString1.setDriverTrain(respString.getDriverTrain());
                responseString1.setSalePersonFirstName(respString.getSalePersonFirstName());
                responseString1.setSalePersonLastName(respString.getSalePersonLastName());
                responseString1.setSalesPersonCode(respString.getSalesPersonCode());
                responseString1.setSDuration(respString.getSDuration());
                responseString1.setSharedWithCustomer(sharedList);
                responseString1.setSkippedItems(skippedItem);
                responseString1.setReviewedItems(respString.getReviewedItems());
                responseString1.setNotSharedWithCustomer(notSharedWithCustomerList);
                responseString1.setNotReviewedItems(summaryNotReviewedItemList);
                responseString1.setEv(respString.getEv());
                responseString1.setFcev(respString.getFcev());
                responseString1.setIce(respString.getIce());
                responseString1.setPhev(respString.getPhev());
                responseString1.setIsAppointmentSchedule(respString.getIsAppointmentSchedule());
                responseString1.setCName(respString.getCName());
                responseString1.setDPhone(respString.getDPhone());
                responseString1.setSCName(respString.getSCName());
                responseString1.setRc(respString.getRc());
                responseStringList.add(responseString1);
                summaryResponse.setResponseString(responseStringList);
                summaryResponse.setEIffailmsg(response.getEIffailmsg());
                summaryResponse.setEIfresult(response.getEIfresult());
                responseObj = mapper.writeValueAsString(summaryResponse);
            }
        } catch(IOException e) {
            LOG.debug("IOException {}", e.getMessage());
        }
        return responseObj;
    }

    private List<SummarySharedWithCustomer> getSummarySharedQuestionList(String resp, List<Feature> featureList) {
        List<SummarySharedWithCustomer> summaryList = new ArrayList<SummarySharedWithCustomer>();
        List<SummarySharedWithCustomer> summarySharedList = new ArrayList<SummarySharedWithCustomer>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            SummaryResponse response = mapper.readValue(resp, SummaryResponse.class);
            List<SummaryResponseString> responseString = response.getResponseString();
            for (SummaryResponseString respString : responseString) {
                summarySharedList = respString.getSharedWithCustomer();

            }
            if(summarySharedList != null) {
                for (SummarySharedWithCustomer customer : summarySharedList) {
                    SummarySharedWithCustomer summaryCustomer = new SummarySharedWithCustomer();
                    summaryCustomer = getSharedWithCustomerObj(customer,featureList);
                    summaryCustomer.setAssetShared(customer.getAssetShared());
                    summaryCustomer.setResponse(customer.getResponse());
                    summaryCustomer.setQuestionId(customer.getQuestionId());
                    summaryCustomer.setCategorieId(customer.getCategorieId());
                    summaryList.add(summaryCustomer);
                }
            }
        }catch(IOException e) {
            LOG.error("IOException {}", e.getMessage());
        }
        return summaryList;
    }

    private SummarySharedWithCustomer getSharedWithCustomerObj(SummarySharedWithCustomer customer, List<Feature> featureList) {
        SummarySharedWithCustomer summarySharedWithCustomer = new SummarySharedWithCustomer();

        for(Feature feature : featureList) {
            if(feature.getUid().equalsIgnoreCase(customer.getQuestionId())) {
                summarySharedWithCustomer.setQuestion(feature.getFeatureName());
                summarySharedWithCustomer.setAttachment(feature.getAttachment());
                summarySharedWithCustomer.setVideoLink(feature.getVideoLink());
                summarySharedWithCustomer.setQuestionId(customer.getQuestionId());
            }

        }
        return summarySharedWithCustomer;
    }

    private SummaryNotReviewedItem getSharedNotReviewedObj(SummaryNotReviewedItem customer, List<Feature> featureList) {
        SummaryNotReviewedItem summaryNotReviewedItem = new SummaryNotReviewedItem();
        for(Feature feature : featureList) {
            if(feature.getUid().equalsIgnoreCase(customer.getQuestionId())) {
                summaryNotReviewedItem.setQuestion(feature.getFeatureName());
                summaryNotReviewedItem.setAttachment(feature.getAttachment());
                summaryNotReviewedItem.setVideoLink(feature.getVideoLink());
                summaryNotReviewedItem.setQuestionId(customer.getQuestionId());
                break;
            }
        }
        return summaryNotReviewedItem;
    }
}