package com.sop.model;

import org.alfresco.service.namespace.QName;


/**
 * References to all of the items defined in valueAssistanceModel.xml
 * 
 */
public class ValueAssistanceModel
{
 
    /**Value Assistance Model URI */
    public static final String SOP_VALUE_ASSISTANCE_MODEL_URI = "http://www.sujayopillai.com/model/sop/1.0";
    public static final String DATALIST_MODEL_URI = "http://www.alfresco.org/model/dictionary/1.0";

    public static final String SOP_VALUE_ASSISTANCE_MODEL_PREFIX = "sop";
    public static final String CONTENT_MODEL_PREFIX = "cm";
    public static final String DATALIST_MODEL_PREFIX = "dl";
    
    public static final QName TYPE_VALUE_ASSISTANCE_LIST_ITEM = QName.createQName(SOP_VALUE_ASSISTANCE_MODEL_URI, "valueAssistanceListItem");
    public static final QName TYPE_DATALIST = QName.createQName(DATALIST_MODEL_URI, "dataList");
    
    public static final QName PROP_SORT_ORDER = QName.createQName(SOP_VALUE_ASSISTANCE_MODEL_URI, "sortOrder");
    public static final QName PROP_VALUE = QName.createQName(SOP_VALUE_ASSISTANCE_MODEL_URI, "value");
}
