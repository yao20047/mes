package com.qcadoo.mes.view.components.tree;

import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import com.qcadoo.mes.model.FieldDefinition;
import com.qcadoo.mes.model.types.HasManyType;
import com.qcadoo.mes.view.ComponentDefinition;
import com.qcadoo.mes.view.ComponentOption;
import com.qcadoo.mes.view.ComponentState;
import com.qcadoo.mes.view.ViewComponent;
import com.qcadoo.mes.view.patterns.AbstractComponentPattern;

@ViewComponent("tree")
public final class TreeComponentPattern extends AbstractComponentPattern {

    private static final String JSP_PATH = "elements/tree.jsp";

    private static final String JS_OBJECT = "QCD.components.elements.Tree";

    private FieldDefinition belongsToFieldDefinition;

    private String correspondingView;

    private String correspondingComponent;

    private String nodeLabelExpression;

    public TreeComponentPattern(final ComponentDefinition componentDefinition) {
        super(componentDefinition);
    }

    @Override
    public ComponentState getComponentStateInstance() {
        return new TreeComponentState(belongsToFieldDefinition, nodeLabelExpression);
    }

    @Override
    protected void initializeComponent() throws JSONException {
        getBelongsToFieldDefinition();

        for (ComponentOption option : getOptions()) {
            if ("correspondingView".equals(option.getType())) {
                correspondingView = option.getValue();
            } else if ("correspondingComponent".equals(option.getType())) {
                correspondingComponent = option.getValue();
            } else if ("nodeLabelExpression".equals(option.getType())) {
                nodeLabelExpression = option.getValue();
            } else {
                throw new IllegalStateException("Unknown option for tree: " + option.getType());
            }
        }
    }

    @Override
    protected JSONObject getJsOptions(final Locale locale) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("correspondingView", correspondingView);
        json.put("correspondingComponent", correspondingComponent);
        if (belongsToFieldDefinition != null) {
            json.put("belongsToFieldName", belongsToFieldDefinition.getName());
        }
        return json;
    }

    private void getBelongsToFieldDefinition() {
        if (getScopeFieldDefinition() != null) {
            if (HasManyType.class.isAssignableFrom(getScopeFieldDefinition().getType().getClass())) {
                HasManyType hasManyType = (HasManyType) getScopeFieldDefinition().getType();
                belongsToFieldDefinition = hasManyType.getDataDefinition().getField(hasManyType.getJoinFieldName());
            } else {
                throw new IllegalStateException("Scope field for grid be a hasMany one");
            }
        }
    }

    @Override
    public String getJspFilePath() {
        return JSP_PATH;
    }

    @Override
    public String getJsFilePath() {
        return JS_PATH;
    }

    @Override
    public String getJsObjectName() {
        return JS_OBJECT;
    }
}
