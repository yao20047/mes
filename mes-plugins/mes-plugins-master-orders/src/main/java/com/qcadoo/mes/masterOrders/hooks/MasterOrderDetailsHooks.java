package com.qcadoo.mes.masterOrders.hooks;

import static com.qcadoo.mes.basic.constants.ProductFields.UNIT;
import static com.qcadoo.mes.masterOrders.constants.MasterOrderFields.CUMULATED_ORDER_QUANTITY;
import static com.qcadoo.mes.masterOrders.constants.MasterOrderFields.DEFAULT_TECHNOLOGY;
import static com.qcadoo.mes.masterOrders.constants.MasterOrderFields.MASTER_ORDER_QUANTITY;
import static com.qcadoo.mes.masterOrders.constants.MasterOrderFields.NUMBER;
import static com.qcadoo.mes.masterOrders.constants.MasterOrderFields.PRODUCT;
import static com.qcadoo.mes.masterOrders.constants.MasterOrderFields.TECHNOLOGY;
import static com.qcadoo.mes.masterOrders.constants.MasterOrdersConstants.PLUGIN_IDENTIFIER;

import java.math.BigDecimal;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.masterOrders.constants.MasterOrderFields;
import com.qcadoo.mes.masterOrders.constants.MasterOrderType;
import com.qcadoo.mes.masterOrders.constants.MasterOrdersConstants;
import com.qcadoo.mes.orders.OrderService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ComponentState.MessageType;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.components.GridComponent;
import com.qcadoo.view.api.components.LookupComponent;
import com.qcadoo.view.api.utils.NumberGeneratorService;

@Service
public class MasterOrderDetailsHooks {

    @Autowired
    private NumberGeneratorService numberGeneratorService;

    @Autowired
    private OrderService orderService;

    public void generateMasterOrderNumer(final ViewDefinitionState view) {
        numberGeneratorService.generateAndInsertNumber(view, PLUGIN_IDENTIFIER, MasterOrdersConstants.MODEL_MASTER_ORDER, "form",
                NUMBER);
    }

    public void hideFieldDependOnMasterOrderType(final ViewDefinitionState view) {
        FieldComponent masterOrderType = (FieldComponent) view.getComponentByReference(MasterOrderFields.MASTER_ORDER_TYPE);
        Object masterOrderTypeValue = masterOrderType.getFieldValue();

        if (masterOrderTypeValue == null || StringUtils.isEmpty(masterOrderTypeValue.toString())
                || masterOrderTypeValue.equals(MasterOrderType.UNDEFINED.getStringValue())) {
            setFieldsVisibility(view, false, false);
        } else if (masterOrderTypeValue.equals(MasterOrderType.ONE_PRODUCT.getStringValue())) {
            setFieldsVisibility(view, true, false);
        } else {
            setFieldsVisibility(view, false, true);
        }
    }

    public void setFieldsVisibility(final ViewDefinitionState view, final boolean visibleFields, final boolean visibleGrid) {
        for (String reference : Arrays.asList(TECHNOLOGY, PRODUCT, DEFAULT_TECHNOLOGY, MASTER_ORDER_QUANTITY,
                CUMULATED_ORDER_QUANTITY)) {
            FieldComponent field = (FieldComponent) view.getComponentByReference(reference);
            field.setVisible(visibleFields);
        }

        GridComponent masterOrderProducts = (GridComponent) view.getComponentByReference("productsGrid");
        masterOrderProducts.setVisible(visibleGrid);

        ComponentState borderLayoutProductQuantity = view.getComponentByReference("borderLayoutProductQuantity");
        borderLayoutProductQuantity.setVisible(visibleFields);
    }

    public void fillUnitField(final ViewDefinitionState view) {
        LookupComponent productField = (LookupComponent) view.getComponentByReference(PRODUCT);
        Entity product = productField.getEntity();

        if (product == null) {
            return;
        }

        String unit = product.getStringField(UNIT);

        for (String reference : Arrays.asList("cumulatedOrderQuantityUnit", "masterOrderQuantityUnit")) {
            FieldComponent field = (FieldComponent) view.getComponentByReference(reference);
            field.setFieldValue(unit);
            field.requestComponentUpdateState();
        }
    }

    public void fillDefaultTechnology(final ViewDefinitionState view) {
        orderService.fillDefaultTechnology(view);
    }

    public void showErrorWhenCumulatedQuantity(final ViewDefinitionState view) {
        FormComponent form = (FormComponent) view.getComponentByReference("form");
        Entity masterOrder = form.getEntity();
        if (masterOrder == null) {
            return;
        }
        if (!masterOrder.getStringField(MasterOrderFields.MASTER_ORDER_TYPE).equals(MasterOrderType.ONE_PRODUCT.getStringValue())) {
            return;
        }
        BigDecimal cumulatedQuantity = masterOrder.getDecimalField(CUMULATED_ORDER_QUANTITY);
        BigDecimal masterQuantity = masterOrder.getDecimalField(MASTER_ORDER_QUANTITY);

        if (cumulatedQuantity != null && masterQuantity != null && cumulatedQuantity.compareTo(masterQuantity) == -1) {
            form.addMessage("masterOrders.masterOrder.masterOrderCumulatedQuantityField.wrongQuantity", MessageType.INFO, false);
        }
    }

}
