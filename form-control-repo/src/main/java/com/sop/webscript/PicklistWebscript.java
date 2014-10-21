package com.sop.webscript;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ParameterCheck;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.sop.model.ValueAssistanceModel;

public class PicklistWebscript extends DeclarativeWebScript{

	private static final Log logger = LogFactory.getLog( PicklistWebscript.class);
    
    //picklist to retrieve
    public static final String PARAM_PICKLIST_NAME = "name";
    //include an empty value at the start of picklist
    public static final String PARAM_INCLUDE_BLANK_ITEM = "includeBlankItem";
    //returns label for the provided initialValues
    public static final String PARAM_LOAD_LABELS = "loadLabels";
    //used with loadLabels to get the label for a given value
    public static final String PARAM_INITIAL_VALUES= "initialValues";
    
    protected ServiceRegistry serviceRegistry;

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
    
    @Override
	protected Map<String, Object> executeImpl(final WebScriptRequest req,Status status, Cache cache) {
    	 final Map<String, Object> model = new HashMap<String, Object>();
         
         logger.debug("Begin");
         try
         {
             final RetryingTransactionCallback< String > transactionWork = new RetryingTransactionCallback< String >()
             {
                 public String execute() throws Throwable
                 {
                     process(req, model);
                     return null;
                 }
             };
             
             //perform this web script's work in a single write transaction
             serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction( transactionWork, false );
             
         }
         catch(Throwable t)
         {
             String error = "Unhandled exception: " + t.getMessage();
             logger.error(error, t);
             model.put( "error", error );
         }
         
         logger.debug("End");
         return model;
	}

    protected void process(WebScriptRequest req, Map<String, Object> model) throws Exception
    {
        String picklistName = req.getParameter( PARAM_PICKLIST_NAME );
        ParameterCheck.mandatoryString( PARAM_PICKLIST_NAME, picklistName );
        
        //get the folder for the datalist
		StringBuffer query = new StringBuffer();
		query.append("=" + ValueAssistanceModel.CONTENT_MODEL_PREFIX + ":" + ContentModel.PROP_TITLE.getLocalName() + ":\"" + picklistName + "\"");
		query.append(" AND TYPE:\"" + ValueAssistanceModel.DATALIST_MODEL_PREFIX + ":" + ValueAssistanceModel.TYPE_DATALIST.getLocalName() + "\"");
		
		logger.debug("Query = " + query);
	
		//Set search parameters
		SearchParameters searchParameters = new SearchParameters();
		searchParameters.addStore( StoreRef.STORE_REF_WORKSPACE_SPACESSTORE );
		searchParameters.setLanguage( SearchService.LANGUAGE_FTS_ALFRESCO );
		searchParameters.setQuery( query.toString() );
    
		ResultSet rs = serviceRegistry.getSearchService().query(searchParameters);
		if(rs == null || rs.length() < 1)
		{
			handleError("Unable to locate data list object with title " + picklistName + ".", model);
		}
		else
		{
			//query for the datalist item nodes
			StringBuffer query2 = new StringBuffer();
			query2.append("=" + "PARENT:\"" + rs.getNodeRef(0).toString() + "\"");
			
			query2.append(" AND TYPE:\"" + ValueAssistanceModel.SOP_VALUE_ASSISTANCE_MODEL_PREFIX + ":" + ValueAssistanceModel.TYPE_VALUE_ASSISTANCE_LIST_ITEM.getLocalName() + "\"");
			
			logger.debug("Query = " + query2);
		
			//Set search parameters
			SearchParameters searchParameters2 = new SearchParameters();
			searchParameters2.addStore( StoreRef.STORE_REF_WORKSPACE_SPACESSTORE );
			
			searchParameters2.addSort("@" + ValueAssistanceModel.PROP_SORT_ORDER, true);
			searchParameters2.addSort("@" + ContentModel.PROP_NAME, true);
			searchParameters2.setLanguage( SearchService.LANGUAGE_FTS_ALFRESCO );
			searchParameters2.setQuery( query2.toString() );
			
			ResultSet rs2 = serviceRegistry.getSearchService().query(searchParameters2);
			
	        //see if we're just supposed to send back some labels
	        String loadLabels = req.getParameter( PARAM_LOAD_LABELS );
	        if (StringUtils.isNotBlank( loadLabels ))
	        {
	            List<String> labels = new ArrayList<String>();
	            String initialValuesParam = req.getParameter( PARAM_INITIAL_VALUES );
	            String[] initialValues = (initialValuesParam == null) ? new String[] {""} : initialValuesParam.split( "," );
	            Map<String,String> valueLabelPairs = new HashMap<String,String>();

				if(picklistName.equals("Site List"))
				{
					for (String initialValue : initialValues)
					{
						labels.add(initialValue);
					}
				}
				else
				{
					for (NodeRef nodeRef : rs2.getNodeRefs())
					{
						Map<QName,Serializable> props = serviceRegistry.getNodeService().getProperties(nodeRef);
						valueLabelPairs.put((String)props.get(ValueAssistanceModel.PROP_VALUE),(String)props.get(ContentModel.PROP_NAME));
					}
		            
					String label;
					for (String initialValue : initialValues)
					{
						label = valueLabelPairs.get(initialValue);
						if(label != null)
						{
							labels.add(label);
						}
						else
						{
							labels.add(initialValue);
						}
					}
				}
	            model.put("labels", StringUtils.join( labels, "," ));
	        }
	        else
	        {
	            List<PicklistItem> picklistItems = new ArrayList<PicklistItem>();
	            boolean includeBlankItem = req.getParameter( PARAM_INCLUDE_BLANK_ITEM ) == null ? false : Boolean.parseBoolean( req.getParameter( PARAM_INCLUDE_BLANK_ITEM ) );
	            
	            if (includeBlankItem)
	            {
	                picklistItems.add( new PicklistItem("", "") );
	            }
	            
				for (NodeRef nodeRef : rs2.getNodeRefs())
				{
					Map<QName,Serializable> props = serviceRegistry.getNodeService().getProperties(nodeRef);

					if(picklistName.equals("Site List"))
					{
						picklistItems.add( new PicklistItem((String)props.get(ContentModel.PROP_NAME), (String)props.get(ContentModel.PROP_NAME)));
					}
					else
					{
						picklistItems.add( new PicklistItem((String)props.get(ContentModel.PROP_NAME), (String)props.get(ValueAssistanceModel.PROP_VALUE)));
					}
				}

				model.put( "picklistItems", picklistItems );
	        }
		}
    }
    
    protected void handleError(String error, Map<String, Object> model)
    {
        logger.error(error);
        model.put( "error", error );
    }
    protected void handleError(String error, Map<String, Object> model, Throwable t)
    {
        logger.error(error, t);
        model.put( "error", error );
    }
}
