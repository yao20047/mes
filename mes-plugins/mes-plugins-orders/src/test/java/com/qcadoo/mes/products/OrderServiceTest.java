/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 0.3.0
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */

package com.qcadoo.mes.products;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.mes.orders.OrderService;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.ExpressionService;
import com.qcadoo.model.api.FieldDefinition;
import com.qcadoo.model.api.search.Restriction;
import com.qcadoo.model.api.search.SearchResult;
import com.qcadoo.model.internal.EntityListImpl;
import com.qcadoo.model.internal.EntityTreeImpl;
import com.qcadoo.model.internal.types.BooleanType;
import com.qcadoo.model.internal.types.StringType;
import com.qcadoo.security.api.SecurityService;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ComponentState.MessageType;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.utils.NumberGeneratorService;
import com.qcadoo.view.components.FieldComponentState;
import com.qcadoo.view.components.form.FormComponentState;
import com.qcadoo.view.components.grid.GridComponentState;
import com.qcadoo.view.components.lookup.LookupComponentState;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ FormComponentState.class, LookupComponentState.class, FieldComponentState.class, EntityTreeImpl.class,
        EntityListImpl.class, GridComponentState.class })
public class OrderServiceTest {

    private OrderService orderService;

    private SecurityService securityService;

    private DataDefinitionService dataDefinitionService;

    private TranslationService translationService;

    private NumberGeneratorService numberGeneratorService;

    @Before
    public void init() {
        securityService = mock(SecurityService.class);
        dataDefinitionService = mock(DataDefinitionService.class, RETURNS_DEEP_STUBS);
        translationService = mock(TranslationService.class);
        numberGeneratorService = mock(NumberGeneratorService.class);
        ExpressionService expressionService = mock(ExpressionService.class);
        orderService = new OrderService();
        setField(orderService, "securityService", securityService);
        setField(orderService, "dataDefinitionService", dataDefinitionService);
        setField(orderService, "translationService", translationService);
        setField(orderService, "numberGeneratorService", numberGeneratorService);
        setField(orderService, "expressionService", expressionService);
    }

    @Test
    public void shouldClearOrderFieldsOnCopy() throws Exception {
        // given
        Entity order = mock(Entity.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);

        // when
        boolean result = orderService.clearOrderDatesAndWorkersOnCopy(dataDefinition, order);

        // then
        assertTrue(result);
        verify(order).setField("state", "01pending");
        verify(order).setField("effectiveDateTo", null);
        verify(order).setField("endWorker", null);
        verify(order).setField("effectiveDateFrom", null);
        verify(order).setField("startWorker", null);
        verify(order).setField("doneQuantity", null);
    }

    @Test
    public void shouldPrintOrder() throws Exception {
        // given
        Entity order = mock(Entity.class);
        ComponentState state = mock(ComponentState.class);
        given(state.getFieldValue()).willReturn(13L);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(dataDefinitionService.get("orders", "order").get(13L)).willReturn(order);

        // when
        orderService.printOrder(viewDefinitionState, state, new String[] { "pdf" });

        // then
        verify(viewDefinitionState).redirectTo("/orders/order.pdf?id=13", true, false);
    }

    @Test
    public void shouldFailPrintIfEntityNotFound() throws Exception {
        // given
        ComponentState state = mock(ComponentState.class);
        given(state.getFieldValue()).willReturn(13L);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(dataDefinitionService.get("orders", "order").get(13L)).willReturn(null);
        given(translationService.translate("core.message.entityNotFound", Locale.ENGLISH)).willReturn(
                "core.message.entityNotFound.pl");

        // when
        orderService.printOrder(viewDefinitionState, state, new String[] { "pdf" });

        // then
        verify(state).addMessage("core.message.entityNotFound.pl", MessageType.FAILURE);
    }

    @Test
    public void shouldFailPrintIfNoRowIsSelected() throws Exception {
        // given
        ComponentState state = mock(ComponentState.class);
        given(state.getFieldValue()).willReturn(null);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(translationService.translate("core.grid.noRowSelectedError", Locale.ENGLISH)).willReturn(
                "core.grid.noRowSelectedError.pl");

        // when
        orderService.printOrder(viewDefinitionState, state, new String[] { "pdf" });

        // then
        verify(state).addMessage("core.grid.noRowSelectedError.pl", MessageType.FAILURE);
    }

    @Test
    public void shouldFailPrintIfFormHasNoIdentifier() throws Exception {
        // given
        FormComponentState state = mock(FormComponentState.class);
        given(state.getFieldValue()).willReturn(null);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(translationService.translate("core.form.entityWithoutIdentifier", Locale.ENGLISH)).willReturn(
                "core.form.entityWithoutIdentifier.pl");

        // when
        orderService.printOrder(viewDefinitionState, state, new String[] { "pdf" });

        // then
        verify(state).addMessage("core.form.entityWithoutIdentifier.pl", MessageType.FAILURE);
    }

    @Test
    public void shouldChangeOrderProductToNull() throws Exception {
        // given
        LookupComponentState product = mock(LookupComponentState.class);
        LookupComponentState technology = mock(LookupComponentState.class);
        FieldComponentState defaultTechnology = mock(FieldComponentState.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(viewDefinitionState.getComponentByReference("defaultTechnology")).willReturn(defaultTechnology);
        given(product.getFieldValue()).willReturn(null);

        // when
        orderService.changeOrderProduct(viewDefinitionState, product, new String[0]);

        // then
        verify(defaultTechnology).setFieldValue("");
        verify(technology).setFieldValue(null);
    }

    @Test
    public void shouldChangeOrderProductWithoutDefaultTechnology() throws Exception {
        // given
        SearchResult searchResult = mock(SearchResult.class);
        LookupComponentState product = mock(LookupComponentState.class);
        LookupComponentState technology = mock(LookupComponentState.class);
        FieldComponentState defaultTechnology = mock(FieldComponentState.class);
        FieldDefinition masterField = mock(FieldDefinition.class);
        FieldDefinition productField = mock(FieldDefinition.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        DataDefinition dataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(viewDefinitionState.getComponentByReference("defaultTechnology")).willReturn(defaultTechnology);
        given(product.getFieldValue()).willReturn(13L);
        given(dataDefinitionService.get("technologies", "technology")).willReturn(dataDefinition);
        given(
                dataDefinition.find().withMaxResults(1).restrictedWith(any(Restriction.class))
                        .restrictedWith(any(Restriction.class)).list()).willReturn(searchResult);
        given(dataDefinition.getField("master")).willReturn(masterField);
        given(dataDefinition.getField("product")).willReturn(productField);
        given(masterField.getType()).willReturn(new BooleanType());
        given(productField.getType()).willReturn(new StringType());
        given(searchResult.getTotalNumberOfEntities()).willReturn(0);

        // when
        orderService.changeOrderProduct(viewDefinitionState, product, new String[0]);

        // then
        verify(defaultTechnology).setFieldValue("");
        verify(technology).setFieldValue(null);
    }

    @Test
    public void shouldChangeOrderProductWithDefaultTechnology() throws Exception {
        // given
        SearchResult searchResult = mock(SearchResult.class);
        Entity entity = mock(Entity.class);
        LookupComponentState product = mock(LookupComponentState.class);
        LookupComponentState technology = mock(LookupComponentState.class);
        FieldComponentState defaultTechnology = mock(FieldComponentState.class);
        FieldDefinition masterField = mock(FieldDefinition.class);
        FieldDefinition productField = mock(FieldDefinition.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        DataDefinition dataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(viewDefinitionState.getComponentByReference("defaultTechnology")).willReturn(defaultTechnology);
        given(product.getFieldValue()).willReturn(13L);
        given(dataDefinitionService.get("technologies", "technology")).willReturn(dataDefinition);
        given(
                dataDefinition.find().withMaxResults(1).restrictedWith(any(Restriction.class))
                        .restrictedWith(any(Restriction.class)).list()).willReturn(searchResult);
        given(dataDefinition.getField("master")).willReturn(masterField);
        given(dataDefinition.getField("product")).willReturn(productField);
        given(masterField.getType()).willReturn(new BooleanType());
        given(productField.getType()).willReturn(new StringType());
        given(searchResult.getTotalNumberOfEntities()).willReturn(1);
        given(searchResult.getEntities()).willReturn(Collections.singletonList(entity));
        given(entity.getId()).willReturn(117L);

        // when
        orderService.changeOrderProduct(viewDefinitionState, product, new String[0]);

        // then
        verify(defaultTechnology).setFieldValue("");
        verify(technology).setFieldValue(117L);
    }

    @Test
    public void shouldSetAndDisableState() throws Exception {
        // given
        FormComponentState form = mock(FormComponentState.class);
        FieldComponentState orderState = mock(FieldComponentState.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("form")).willReturn(form);
        given(viewDefinitionState.getComponentByReference("state")).willReturn(orderState);
        given(form.getEntityId()).willReturn(null);

        // when
        orderService.setAndDisableState(viewDefinitionState);

        // then
        verify(orderState).setEnabled(false);
        verify(orderState).setFieldValue("01pending");
    }

    @Test
    public void shouldDisableState() throws Exception {
        // given
        FormComponentState form = mock(FormComponentState.class);
        FieldComponentState orderState = mock(FieldComponentState.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("form")).willReturn(form);
        given(viewDefinitionState.getComponentByReference("state")).willReturn(orderState);
        given(form.getEntityId()).willReturn(1L);

        // when
        orderService.setAndDisableState(viewDefinitionState);

        // then
        verify(orderState).setEnabled(false);
        verify(orderState, never()).setFieldValue("01pending");
    }

    @Test
    public void shouldGenerateOrderNumber() throws Exception {
        // given
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);

        // when
        orderService.generateOrderNumber(viewDefinitionState);

        // then
        verify(numberGeneratorService).generateAndInsertNumber(viewDefinitionState, "orders", "order", "form", "number");
    }

    @Test
    public void shouldNotFillDefaultTechnologyIfThereIsNoProduct() throws Exception {
        // given
        LookupComponentState product = mock(LookupComponentState.class);
        FieldComponentState defaultTechnology = mock(FieldComponentState.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("product")).willReturn(product);
        given(viewDefinitionState.getComponentByReference("defaultTechnology")).willReturn(defaultTechnology);
        given(product.getFieldValue()).willReturn(null);

        // when
        orderService.fillDefaultTechnology(viewDefinitionState);

        // then
        verify(defaultTechnology, never()).setFieldValue(anyString());
    }

    @Test
    public void shouldNotFillDefaultTechnologyIfThereIsNoDefaultTechnology() throws Exception {
        // given
        LookupComponentState product = mock(LookupComponentState.class);
        FieldComponentState defaultTechnology = mock(FieldComponentState.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("product")).willReturn(product);
        given(viewDefinitionState.getComponentByReference("defaultTechnology")).willReturn(defaultTechnology);
        given(product.getFieldValue()).willReturn(117L);

        FieldDefinition masterField = mock(FieldDefinition.class);
        FieldDefinition productField = mock(FieldDefinition.class);
        DataDefinition dataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
        SearchResult searchResult = mock(SearchResult.class);
        given(dataDefinitionService.get("technologies", "technology")).willReturn(dataDefinition);
        given(
                dataDefinition.find().withMaxResults(1).restrictedWith(any(Restriction.class))
                        .restrictedWith(any(Restriction.class)).list()).willReturn(searchResult);
        given(dataDefinition.getField("master")).willReturn(masterField);
        given(dataDefinition.getField("product")).willReturn(productField);
        given(masterField.getType()).willReturn(new BooleanType());
        given(productField.getType()).willReturn(new StringType());
        given(searchResult.getTotalNumberOfEntities()).willReturn(0);

        // when
        orderService.fillDefaultTechnology(viewDefinitionState);

        // then
        verify(defaultTechnology, never()).setFieldValue(anyString());
    }

    @Test
    public void shouldFillDefaultTechnology() throws Exception {
        // given
        LookupComponentState product = mock(LookupComponentState.class);
        FieldComponentState defaultTechnology = mock(FieldComponentState.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("product")).willReturn(product);
        given(viewDefinitionState.getComponentByReference("defaultTechnology")).willReturn(defaultTechnology);
        given(product.getFieldValue()).willReturn(117L);

        Entity entity = mock(Entity.class);
        FieldDefinition masterField = mock(FieldDefinition.class);
        FieldDefinition productField = mock(FieldDefinition.class);
        DataDefinition dataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
        SearchResult searchResult = mock(SearchResult.class);
        given(dataDefinitionService.get("technologies", "technology")).willReturn(dataDefinition);
        given(
                dataDefinition.find().withMaxResults(1).restrictedWith(any(Restriction.class))
                        .restrictedWith(any(Restriction.class)).list()).willReturn(searchResult);
        given(dataDefinition.getField("master")).willReturn(masterField);
        given(dataDefinition.getField("product")).willReturn(productField);
        given(masterField.getType()).willReturn(new BooleanType());
        given(productField.getType()).willReturn(new StringType());
        given(searchResult.getTotalNumberOfEntities()).willReturn(1);
        given(searchResult.getEntities()).willReturn(Collections.singletonList(entity));

        // when
        orderService.fillDefaultTechnology(viewDefinitionState);

        // then
        verify(defaultTechnology).setFieldValue(anyString());
    }

    @Test
    public void shouldDisableTechnologyIfThereIsNoProduct() throws Exception {
        // given
        LookupComponentState product = mock(LookupComponentState.class);
        LookupComponentState technology = mock(LookupComponentState.class);
        FieldComponentState defaultTechnology = mock(FieldComponentState.class);
        FieldComponentState plannedQuantity = mock(FieldComponentState.class);

        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("product")).willReturn(product);
        given(viewDefinitionState.getComponentByReference("defaultTechnology")).willReturn(defaultTechnology);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(viewDefinitionState.getComponentByReference("plannedQuantity")).willReturn(plannedQuantity);
        given(product.getFieldValue()).willReturn(null);

        // when
        orderService.disableTechnologiesIfProductDoesNotAny(viewDefinitionState);

        // then
        verify(defaultTechnology).setEnabled(false);
        verify(technology).setEnabled(false);
        verify(technology).setRequired(false);
        verify(plannedQuantity).setRequired(false);
    }

    @Test
    public void shouldDisableTechnologyIfProductHasNoTechnologies() throws Exception {
        // given
        LookupComponentState product = mock(LookupComponentState.class);
        LookupComponentState technology = mock(LookupComponentState.class);
        FieldComponentState defaultTechnology = mock(FieldComponentState.class);
        FieldComponentState plannedQuantity = mock(FieldComponentState.class);

        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("product")).willReturn(product);
        given(viewDefinitionState.getComponentByReference("defaultTechnology")).willReturn(defaultTechnology);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(viewDefinitionState.getComponentByReference("plannedQuantity")).willReturn(plannedQuantity);
        given(product.getFieldValue()).willReturn(117L);

        FieldDefinition productField = mock(FieldDefinition.class);
        DataDefinition dataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
        SearchResult searchResult = mock(SearchResult.class);
        given(dataDefinitionService.get("technologies", "technology")).willReturn(dataDefinition);
        given(dataDefinition.find().withMaxResults(1).restrictedWith(any(Restriction.class)).list()).willReturn(searchResult);
        given(dataDefinition.getField("product")).willReturn(productField);
        given(productField.getType()).willReturn(new StringType());
        given(searchResult.getTotalNumberOfEntities()).willReturn(0);

        // when
        orderService.disableTechnologiesIfProductDoesNotAny(viewDefinitionState);

        // then
        verify(defaultTechnology).setEnabled(false);
        verify(technology).setEnabled(false);
        verify(technology).setRequired(false);
        verify(plannedQuantity).setRequired(false);
    }

    @Test
    public void shouldSetTechnologyAndPlannedQuantityAsRequired() throws Exception {
        // given
        LookupComponentState product = mock(LookupComponentState.class);
        LookupComponentState technology = mock(LookupComponentState.class);
        FieldComponentState defaultTechnology = mock(FieldComponentState.class);
        FieldComponentState plannedQuantity = mock(FieldComponentState.class);

        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("product")).willReturn(product);
        given(viewDefinitionState.getComponentByReference("defaultTechnology")).willReturn(defaultTechnology);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(viewDefinitionState.getComponentByReference("plannedQuantity")).willReturn(plannedQuantity);
        given(product.getFieldValue()).willReturn(117L);

        FieldDefinition productField = mock(FieldDefinition.class);
        DataDefinition dataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
        SearchResult searchResult = mock(SearchResult.class);
        given(dataDefinitionService.get("technologies", "technology")).willReturn(dataDefinition);
        given(dataDefinition.find().withMaxResults(1).restrictedWith(any(Restriction.class)).list()).willReturn(searchResult);
        given(dataDefinition.getField("product")).willReturn(productField);
        given(productField.getType()).willReturn(new StringType());
        given(searchResult.getTotalNumberOfEntities()).willReturn(1);

        // when
        orderService.disableTechnologiesIfProductDoesNotAny(viewDefinitionState);

        // then
        verify(defaultTechnology).setEnabled(false);
        verify(technology).setRequired(true);
        verify(plannedQuantity).setRequired(true);
    }

    @Test
    public void shouldNotDisableFormIfThereIsNoIdentifier() throws Exception {
        // given
        FormComponentState order = mock(FormComponentState.class);
        LookupComponentState technology = mock(LookupComponentState.class);

        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("form")).willReturn(order);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(order.getFieldValue()).willReturn(null);

        // when
        orderService.disableFormForDoneOrder(viewDefinitionState);

        // then
        verify(order).setEnabledWithChildren(true);
        verify(technology).setEnabled(true);
    }

    @Test
    public void shouldNotDisableFormIfThereIsNoOrder() throws Exception {
        // given
        FormComponentState order = mock(FormComponentState.class);
        LookupComponentState technology = mock(LookupComponentState.class);

        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("form")).willReturn(order);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(order.getFieldValue()).willReturn(117L);
        given(dataDefinitionService.get("orders", "order").get(117L)).willReturn(null);

        // when
        orderService.disableFormForDoneOrder(viewDefinitionState);

        // then
        verify(order).setEnabledWithChildren(true);
        verify(technology).setEnabled(true);
    }

    @Test
    public void shouldNotDisableFormIfOrderIsNotDone() throws Exception {
        // given
        FormComponentState order = mock(FormComponentState.class);
        LookupComponentState technology = mock(LookupComponentState.class);
        Entity entity = mock(Entity.class);

        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("form")).willReturn(order);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(order.getFieldValue()).willReturn(117L);
        given(dataDefinitionService.get("orders", "order").get(117L)).willReturn(entity);
        given(entity.getStringField("state")).willReturn("01pending");
        given(order.isValid()).willReturn(true);

        // when
        orderService.disableFormForDoneOrder(viewDefinitionState);

        // then
        verify(order).setEnabledWithChildren(true);
        verify(technology).setEnabled(true);
    }

    @Test
    public void shouldNotDisableFormIfOrderIsNotValid() throws Exception {
        // given
        FormComponentState order = mock(FormComponentState.class);
        LookupComponentState technology = mock(LookupComponentState.class);
        Entity entity = mock(Entity.class);

        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("form")).willReturn(order);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(order.getFieldValue()).willReturn(117L);
        given(dataDefinitionService.get("orders", "order").get(117L)).willReturn(entity);
        given(entity.getStringField("state")).willReturn("03done");
        given(order.isValid()).willReturn(false);

        // when
        orderService.disableFormForDoneOrder(viewDefinitionState);

        // then
        verify(order).setEnabledWithChildren(true);
        verify(technology).setEnabled(true);
    }

    @Test
    public void shouldNotDisableFormForDoneOrder() throws Exception {
        // given
        FormComponentState order = mock(FormComponentState.class);
        LookupComponentState technology = mock(LookupComponentState.class);
        Entity entity = mock(Entity.class);

        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("form")).willReturn(order);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(order.getEntityId()).willReturn(117L);
        given(dataDefinitionService.get("orders", "order").get(117L)).willReturn(entity);
        given(entity.getStringField("state")).willReturn("03done");
        given(order.isValid()).willReturn(true);

        // when
        orderService.disableFormForDoneOrder(viewDefinitionState);

        // then
        verify(order).setEnabledWithChildren(false);
        verify(technology).setEnabled(false);
    }

    @Test
    public void shouldReturnTrueForValidOrderDates() throws Exception {
        // given
        DataDefinition dataDefinition = mock(DataDefinition.class);
        Entity entity = mock(Entity.class);
        given(entity.getField("dateFrom")).willReturn(new Date(System.currentTimeMillis() - 10000));
        given(entity.getField("dateTo")).willReturn(new Date());

        // when
        boolean results = orderService.checkOrderDates(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnTrueForNullFromDate() throws Exception {
        // given
        DataDefinition dataDefinition = mock(DataDefinition.class);
        Entity entity = mock(Entity.class);
        given(entity.getField("dateFrom")).willReturn(null);
        given(entity.getField("dateTo")).willReturn(new Date());

        // when
        boolean results = orderService.checkOrderDates(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnTrueForNullToDate() throws Exception {
        // given
        DataDefinition dataDefinition = mock(DataDefinition.class);
        Entity entity = mock(Entity.class);
        given(entity.getField("dateFrom")).willReturn(new Date());
        given(entity.getField("dateTo")).willReturn(null);

        // when
        boolean results = orderService.checkOrderDates(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnFalseForInvalidOrderDates() throws Exception {
        // given
        DataDefinition dataDefinition = mock(DataDefinition.class);
        FieldDefinition dateToField = mock(FieldDefinition.class);
        Entity entity = mock(Entity.class);
        given(entity.getField("dateFrom")).willReturn(new Date());
        given(entity.getField("dateTo")).willReturn(new Date(System.currentTimeMillis() - 10000));
        given(dataDefinition.getField("dateTo")).willReturn(dateToField);

        // when
        boolean results = orderService.checkOrderDates(dataDefinition, entity);

        // then
        assertFalse(results);
        verify(entity).addError(dateToField, "orders.validate.global.error.datesOrder");
    }

    @Test
    public void shouldReturnTrueForPlannedQuantityValidationIfThereIsNoProduct() throws Exception {
        // given
        DataDefinition dataDefinition = mock(DataDefinition.class);
        Entity entity = mock(Entity.class);
        given(entity.getBelongsToField("product")).willReturn(null);

        // when
        boolean results = orderService.checkOrderPlannedQuantity(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnTrueForPlannedQuantityValidation() throws Exception {
        // given
        DataDefinition dataDefinition = mock(DataDefinition.class);
        Entity entity = mock(Entity.class);
        Entity product = mock(Entity.class);
        given(entity.getBelongsToField("product")).willReturn(product);
        given(entity.getField("plannedQuantity")).willReturn(BigDecimal.ONE);

        // when
        boolean results = orderService.checkOrderPlannedQuantity(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnFalseForPlannedQuantityValidation() throws Exception {
        // given
        DataDefinition dataDefinition = mock(DataDefinition.class);
        FieldDefinition plannedQuantityField = mock(FieldDefinition.class);
        Entity entity = mock(Entity.class);
        Entity product = mock(Entity.class);
        given(entity.getBelongsToField("product")).willReturn(product);
        given(entity.getField("plannedQuantity")).willReturn(null);
        given(dataDefinition.getField("plannedQuantity")).willReturn(plannedQuantityField);

        // when
        boolean results = orderService.checkOrderPlannedQuantity(dataDefinition, entity);

        // then
        assertFalse(results);
        verify(entity).addError(plannedQuantityField, "orders.validate.global.error.plannedQuantityError");
    }

    @Test
    public void shouldReturnTrueForTechnologyValidationIfThereIsNoProduct() throws Exception {
        // given
        DataDefinition dataDefinition = mock(DataDefinition.class);
        Entity entity = mock(Entity.class);
        given(entity.getBelongsToField("product")).willReturn(null);

        // when
        boolean results = orderService.checkOrderTechnology(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnTrueForTechnologyValidation() throws Exception {
        // given
        DataDefinition dataDefinition = mock(DataDefinition.class);
        Entity entity = mock(Entity.class);
        Entity product = mock(Entity.class);
        Entity technology = mock(Entity.class);
        given(entity.getBelongsToField("product")).willReturn(product);
        given(entity.getField("technology")).willReturn(technology);

        // when
        boolean results = orderService.checkOrderTechnology(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnTrueForTechnologyValidationIfProductDoesNotHaveAnyTechnologies() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        Entity product = mock(Entity.class);
        given(entity.getBelongsToField("product")).willReturn(product);
        given(entity.getField("technology")).willReturn(null);
        given(product.getId()).willReturn(117L);

        FieldDefinition productField = mock(FieldDefinition.class);
        DataDefinition dataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
        SearchResult searchResult = mock(SearchResult.class);
        given(dataDefinitionService.get("technologies", "technology")).willReturn(dataDefinition);
        given(dataDefinition.find().withMaxResults(1).restrictedWith(any(Restriction.class)).list()).willReturn(searchResult);
        given(dataDefinition.getField("product")).willReturn(productField);
        given(productField.getType()).willReturn(new StringType());
        given(searchResult.getTotalNumberOfEntities()).willReturn(0);

        // when
        boolean results = orderService.checkOrderTechnology(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnFalseForTechnologyValidation() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        Entity product = mock(Entity.class);
        given(entity.getBelongsToField("product")).willReturn(product);
        given(entity.getField("technology")).willReturn(null);
        given(product.getId()).willReturn(117L);

        FieldDefinition technologyField = mock(FieldDefinition.class);
        FieldDefinition productField = mock(FieldDefinition.class);
        DataDefinition dataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
        SearchResult searchResult = mock(SearchResult.class);
        given(dataDefinitionService.get("technologies", "technology")).willReturn(dataDefinition);
        given(dataDefinition.find().withMaxResults(1).restrictedWith(any(Restriction.class)).list()).willReturn(searchResult);
        given(dataDefinition.getField("product")).willReturn(productField);
        given(productField.getType()).willReturn(new StringType());
        given(searchResult.getTotalNumberOfEntities()).willReturn(1);
        given(dataDefinition.getField("technology")).willReturn(technologyField);

        // when
        boolean results = orderService.checkOrderTechnology(dataDefinition, entity);

        // then
        assertFalse(results);
        verify(entity).addError(technologyField, "orders.validate.global.error.technologyError");
    }

    @Test
    public void shouldNotFillOrderDatesAndWorkers() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);

        // when
        orderService.fillOrderDatesAndWorkers(dataDefinition, entity);

        // then
        verify(entity, atLeastOnce()).getField("state");
        verifyNoMoreInteractions(entity);
    }

    @Test
    public void shouldFillInProgressOrderDatesAndWorkers() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        given(entity.getField("state")).willReturn("02inProgress");
        given(securityService.getCurrentUserName()).willReturn("user");

        // when
        orderService.fillOrderDatesAndWorkers(dataDefinition, entity);

        // then
        verify(entity).setField(eq("effectiveDateFrom"), any(Date.class));
        verify(entity).setField("startWorker", "user");
    }

    @Test
    public void shouldFillDoneOrderDatesAndWorkers() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        given(entity.getField("state")).willReturn("03done");
        given(securityService.getCurrentUserName()).willReturn("user", "admin");

        // when
        orderService.fillOrderDatesAndWorkers(dataDefinition, entity);

        // then
        verify(entity).setField(eq("effectiveDateFrom"), any(Date.class));
        verify(entity).setField("startWorker", "user");
        verify(entity).setField(eq("effectiveDateTo"), any(Date.class));
        verify(entity).setField("endWorker", "admin");
    }

    @Test
    public void shouldNotFillExistingDatesAndWorkers() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        given(entity.getField("state")).willReturn("03done");
        given(entity.getField("effectiveDateFrom")).willReturn(new Date());
        given(entity.getField("effectiveDateTo")).willReturn(new Date());
        given(securityService.getCurrentUserName()).willReturn("user", "admin");

        // when
        orderService.fillOrderDatesAndWorkers(dataDefinition, entity);

        // then
        verify(entity, never()).setField(eq("effectiveDateFrom"), any(Date.class));
        verify(entity, never()).setField("startWorker", "user");
        verify(entity, never()).setField(eq("effectiveDateTo"), any(Date.class));
        verify(entity, never()).setField("endWorker", "admin");
    }

    @Test
    public void shouldReturnTrueForTechnologyValidationIfThereIsNoOrder() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);

        // when
        boolean results = orderService.checkIfOrderHasTechnology(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnTrueForTechnologyValidationIfOrderHasTechnology() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        Entity order = mock(Entity.class);
        Entity technology = mock(Entity.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        given(entity.getBelongsToField("order")).willReturn(order);
        given(order.getField("technology")).willReturn(technology);

        // when
        boolean results = orderService.checkIfOrderHasTechnology(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnFalseForTechnologyValidationIfOrderDoesNotHaveTechnology() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        Entity order = mock(Entity.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        FieldDefinition orderField = mock(FieldDefinition.class);
        given(entity.getBelongsToField("order")).willReturn(order);
        given(dataDefinition.getField("order")).willReturn(orderField);

        // when
        boolean results = orderService.checkIfOrderHasTechnology(dataDefinition, entity);

        // then
        assertFalse(results);
        verify(entity).addError(orderField, "orders.validate.global.error.orderMustHaveTechnology");
    }

    @Test
    public void shouldReturnTrueForOperationValidationIfThereIsNoOrder() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);

        // when
        boolean results = orderService.checkIfOrderTechnologyHasOperations(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnTrueForOperationValidationIfOrderDoesNotHaveTechnology() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        Entity order = mock(Entity.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        given(entity.getBelongsToField("order")).willReturn(order);

        // when
        boolean results = orderService.checkIfOrderTechnologyHasOperations(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnTrueForOperationValidationIfTechnologyHasOperations() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        Entity order = mock(Entity.class);
        Entity technology = mock(Entity.class);
        EntityTreeImpl operations = mock(EntityTreeImpl.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        given(entity.getBelongsToField("order")).willReturn(order);
        given(order.getField("technology")).willReturn(technology);
        given(order.getBelongsToField("technology")).willReturn(technology);
        given(technology.getTreeField("operationComponents")).willReturn(operations);
        given(operations.isEmpty()).willReturn(false);

        // when
        boolean results = orderService.checkIfOrderTechnologyHasOperations(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnTrueForOperationValidationIfTechnologyDoesNotHaveOperations() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        Entity order = mock(Entity.class);
        Entity technology = mock(Entity.class);
        EntityTreeImpl operations = mock(EntityTreeImpl.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        given(entity.getBelongsToField("order")).willReturn(order);
        given(order.getField("technology")).willReturn(technology);
        given(order.getBelongsToField("technology")).willReturn(technology);
        given(technology.getTreeField("operationComponents")).willReturn(operations);
        given(operations.isEmpty()).willReturn(true);
        FieldDefinition orderField = mock(FieldDefinition.class);
        given(dataDefinition.getField("order")).willReturn(orderField);

        // when
        boolean results = orderService.checkIfOrderTechnologyHasOperations(dataDefinition, entity);

        // then
        assertFalse(results);
        verify(entity).addError(orderField, "orders.validate.global.error.orderTechnologyMustHaveOperation");
    }

    @Test
    public void shouldFailActivationOrderIfGridHasNoSelectedEntity() throws Exception {
        // given
        ComponentState state = mock(ComponentState.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        given(translationService.translate("core.grid.noRowSelectedError", Locale.ENGLISH)).willReturn(
                "core.grid.noRowSelectedError.pl");

        // when
        orderService.activateOrder(viewDefinitionState, state, new String[0]);

        // then
        verify(state).addMessage("core.grid.noRowSelectedError.pl", MessageType.FAILURE);
    }

    @Test
    public void shouldFailActivationOrderIfFormHasNoIdentifier() throws Exception {
        // given
        FormComponentState state = mock(FormComponentState.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        given(translationService.translate("core.form.entityWithoutIdentifier", Locale.ENGLISH)).willReturn(
                "core.form.entityWithoutIdentifier.pl");

        // when
        orderService.activateOrder(viewDefinitionState, state, new String[0]);

        // then
        verify(state).addMessage("core.form.entityWithoutIdentifier.pl", MessageType.FAILURE);
    }

    @Test
    public void shouldSetStateAsInProgressForFormOrderActivation() throws Exception {
        // given
        FormComponentState state = mock(FormComponentState.class);
        FieldComponentState orderState = mock(FieldComponentState.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(state.getFieldValue()).willReturn(117L);
        given(viewDefinitionState.getComponentByReference("state")).willReturn(orderState);

        // when
        orderService.activateOrder(viewDefinitionState, state, new String[] { "true" });

        // then
        verify(state).performEvent(viewDefinitionState, "save", new String[0]);
        verify(orderState).setFieldValue("02inProgress");
    }

    @Test
    public void shouldSetStateAsInProgressForGridOrderActivation() throws Exception {
        // given
        Entity order = mock(Entity.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        GridComponentState state = mock(GridComponentState.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(state.getFieldValue()).willReturn(117L);
        given(dataDefinitionService.get("orders", "order")).willReturn(dataDefinition);
        given(dataDefinition.get(117L)).willReturn(order);

        // when
        orderService.activateOrder(viewDefinitionState, state, new String[] { "true" });

        // then
        verify(order).setField("state", "02inProgress");
        verify(dataDefinition).save(order);
        verify(state).performEvent(viewDefinitionState, "refresh", new String[0]);
    }

    @Test
    public void shouldFailFormOrderActivationIfCheckRequiredFailed() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        FormComponentState state = mock(FormComponentState.class);
        FieldComponentState orderState = mock(FieldComponentState.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(state.getFieldValue()).willReturn(117L);
        given(dataDefinitionService.get("orders", "order")).willReturn(dataDefinition);
        given(dataDefinition.get(117L)).willReturn(order);
        given(viewDefinitionState.getComponentByReference("state")).willReturn(orderState);
        prepareCheckRequiredBatch(order, false);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        given(translationService.translate("genealogies.message.batchNotFound", Locale.ENGLISH)).willReturn(
                "genealogies.message.batchNotFound.pl");
        Entity parameter = mock(Entity.class);
        given(dataDefinitionService.get("basic", "parameter").find().withMaxResults(1).list().getEntities()).willReturn(
                Collections.singletonList(parameter));
        given(parameter.getField("batchForDoneOrder")).willReturn("02");
        given(parameter.getField("checkDoneOrderForQuality")).willReturn(true);

        // when
        orderService.activateOrder(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(state).addMessage("genealogies.message.batchNotFound.pl", MessageType.FAILURE);
    }

    @Test
    public void shouldFailGridOrderActivationIfCheckRequiredFailed() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        GridComponentState state = mock(GridComponentState.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(state.getFieldValue()).willReturn(117L);
        given(dataDefinitionService.get("orders", "order")).willReturn(dataDefinition);
        given(dataDefinition.get(117L)).willReturn(order);
        prepareCheckRequiredBatch(order, false);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        given(translationService.translate("genealogies.message.batchNotFound", Locale.ENGLISH)).willReturn(
                "genealogies.message.batchNotFound.pl");
        Entity parameter = mock(Entity.class);
        given(dataDefinitionService.get("basic", "parameter").find().withMaxResults(1).list().getEntities()).willReturn(
                Collections.singletonList(parameter));
        given(parameter.getField("batchForDoneOrder")).willReturn("02");
        given(parameter.getField("checkDoneOrderForQuality")).willReturn(true);

        // when
        orderService.activateOrder(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(state).addMessage("genealogies.message.batchNotFound.pl", MessageType.INFO);
    }

    @Test
    public void shouldFailFormOrderActivationIfIsQualityControlAutoCheckEnabledAndNotCheckIfAllQualityControlsAreClosed()
            throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        FormComponentState state = mock(FormComponentState.class);
        FieldComponentState orderState = mock(FieldComponentState.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(state.getFieldValue()).willReturn(117L);
        given(dataDefinitionService.get("orders", "order")).willReturn(dataDefinition);
        given(dataDefinition.get(117L)).willReturn(order);
        given(viewDefinitionState.getComponentByReference("state")).willReturn(orderState);
        prepareCheckRequiredBatch(order, true);
        prepareIsQualityControlAutoCheckEnabled(true);
        prepareCheckIfAllQualityControlsAreClosed(order, false);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        given(translationService.translate("qualityControls.qualityControls.not.closed", Locale.ENGLISH)).willReturn(
                "qualityControls.qualityControls.not.closed.pl");
        Entity parameter = mock(Entity.class);
        given(dataDefinitionService.get("basic", "parameter").find().withMaxResults(1).list().getEntities()).willReturn(
                Collections.singletonList(parameter));
        given(parameter.getField("batchForDoneOrder")).willReturn("02");
        given(parameter.getField("checkDoneOrderForQuality")).willReturn(true);

        // when
        orderService.activateOrder(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(state).addMessage("qualityControls.qualityControls.not.closed.pl", MessageType.FAILURE);
    }

    @Test
    public void shouldFailGridOrderActivationIfIsQualityControlAutoCheckEnabledAndNotCheckIfAllQualityControlsAreClosed()
            throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        GridComponentState state = mock(GridComponentState.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(state.getFieldValue()).willReturn(117L);
        given(dataDefinitionService.get("orders", "order")).willReturn(dataDefinition);
        given(dataDefinition.get(117L)).willReturn(order);
        prepareCheckRequiredBatch(order, true);
        prepareIsQualityControlAutoCheckEnabled(true);
        prepareCheckIfAllQualityControlsAreClosed(order, false);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        given(translationService.translate("qualityControls.qualityControls.not.closed", Locale.ENGLISH)).willReturn(
                "qualityControls.qualityControls.not.closed.pl");
        Entity parameter = mock(Entity.class);
        given(dataDefinitionService.get("basic", "parameter").find().withMaxResults(1).list().getEntities()).willReturn(
                Collections.singletonList(parameter));
        given(parameter.getField("batchForDoneOrder")).willReturn("02");
        given(parameter.getField("checkDoneOrderForQuality")).willReturn(true);

        // when
        orderService.activateOrder(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(state).addMessage("qualityControls.qualityControls.not.closed.pl", MessageType.FAILURE);
    }

    @Test
    public void shouldSetStateAsDoneForFormOrderActivation() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        FormComponentState state = mock(FormComponentState.class);
        FieldComponentState orderState = mock(FieldComponentState.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(state.getFieldValue()).willReturn(117L);
        given(dataDefinitionService.get("orders", "order")).willReturn(dataDefinition);
        given(dataDefinition.get(117L)).willReturn(order);
        given(viewDefinitionState.getComponentByReference("state")).willReturn(orderState);
        prepareCheckRequiredBatch(order, true);
        prepareIsQualityControlAutoCheckEnabled(false);
        prepareCheckIfAllQualityControlsAreClosed(order, false);

        // when
        orderService.activateOrder(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(state).performEvent(viewDefinitionState, "save", new String[0]);
        verify(orderState).setFieldValue("03done");
    }

    @Test
    public void shouldSetStateAsDoneForFormOrderActivation2() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        FormComponentState state = mock(FormComponentState.class);
        FieldComponentState orderState = mock(FieldComponentState.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(state.getFieldValue()).willReturn(117L);
        given(dataDefinitionService.get("orders", "order")).willReturn(dataDefinition);
        given(dataDefinition.get(117L)).willReturn(order);
        given(viewDefinitionState.getComponentByReference("state")).willReturn(orderState);
        prepareCheckRequiredBatch(order, true);
        prepareIsQualityControlAutoCheckEnabled(true);
        prepareCheckIfAllQualityControlsAreClosed(order, true);
        Entity parameter = mock(Entity.class);
        given(dataDefinitionService.get("basic", "parameter").find().withMaxResults(1).list().getEntities()).willReturn(
                Collections.singletonList(parameter));
        given(parameter.getField("batchForDoneOrder")).willReturn("02");
        given(parameter.getField("checkDoneOrderForQuality")).willReturn(true);

        // when
        orderService.activateOrder(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(state).performEvent(viewDefinitionState, "save", new String[0]);
        verify(orderState).setFieldValue("03done");
    }

    @Test
    public void shouldSetStateAsDoneForGridOrderActivation() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        GridComponentState state = mock(GridComponentState.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(state.getFieldValue()).willReturn(117L);
        given(dataDefinitionService.get("orders", "order")).willReturn(dataDefinition);
        given(dataDefinition.get(117L)).willReturn(order);
        prepareCheckRequiredBatch(order, true);
        prepareIsQualityControlAutoCheckEnabled(false);
        prepareCheckIfAllQualityControlsAreClosed(order, false);

        // when
        orderService.activateOrder(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(order).setField("state", "03done");
        verify(dataDefinition).save(order);
        verify(state).performEvent(viewDefinitionState, "refresh", new String[0]);
    }

    @Test
    public void shouldSetStateAsDoneForGridOrderActivation2() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        GridComponentState state = mock(GridComponentState.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(state.getFieldValue()).willReturn(117L);
        given(dataDefinitionService.get("orders", "order")).willReturn(dataDefinition);
        given(dataDefinition.get(117L)).willReturn(order);
        prepareCheckRequiredBatch(order, true);
        prepareIsQualityControlAutoCheckEnabled(true);
        prepareCheckIfAllQualityControlsAreClosed(order, true);
        Entity parameter = mock(Entity.class);
        given(dataDefinitionService.get("basic", "parameter").find().withMaxResults(1).list().getEntities()).willReturn(
                Collections.singletonList(parameter));
        given(parameter.getField("batchForDoneOrder")).willReturn("02");
        given(parameter.getField("checkDoneOrderForQuality")).willReturn(true);

        // when
        orderService.activateOrder(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(order).setField("state", "03done");
        verify(dataDefinition).save(order);
        verify(state).performEvent(viewDefinitionState, "refresh", new String[0]);
    }

    private void prepareCheckIfAllQualityControlsAreClosed(final Entity order, final boolean expected) {
        if (expected) {
            DataDefinition dataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
            given(order.getBelongsToField("technology").getField("qualityControlType").toString()).willReturn("01forBatch");
            given(dataDefinitionService.get("qualityControls", "qualityControl")).willReturn(dataDefinition);
            given(
                    dataDefinition.find().restrictedWith(any(Restriction.class)).restrictedWith(any(Restriction.class)).list()
                            .getTotalNumberOfEntities()).willReturn(0);
        } else {
            given(order.getBelongsToField("technology").getField("qualityControlType")).willReturn(null);
        }
    }

    private void prepareIsQualityControlAutoCheckEnabled(final boolean expected) {
        if (expected) {
            Entity entity = mock(Entity.class);
            List<Entity> entities = new ArrayList<Entity>();
            entities.add(entity);
            given(entity.getField("checkDoneOrderForQuality")).willReturn(true);
            given(dataDefinitionService.get("basic", "parameter").find().withMaxResults(1).list().getEntities().size())
                    .willReturn(1);
            given(dataDefinitionService.get("basic", "parameter").find().withMaxResults(1).list().getEntities()).willReturn(
                    entities);
        } else {
            given(dataDefinitionService.get("basic", "parameter").find().withMaxResults(1).list().getEntities().size())
                    .willReturn(0);
        }
    }

    private void prepareCheckRequiredBatch(final Entity order, final boolean expected) {
        if (expected) {
            given(order.getField("technology")).willReturn(null);
        } else {
            Entity technology = mock(Entity.class);
            given(order.getField("technology")).willReturn(technology);
            given(order.getHasManyField("genealogies").size()).willReturn(0);
            given(technology.getField("batchRequired")).willReturn(false);
            given(technology.getField("shiftFeatureRequired")).willReturn(true);
        }
    }

    @Test
    public void shouldReturnTrueIfThereIsNoOpenQualityForUnit() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        DataDefinition dataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
        given(order.getBelongsToField("technology").getField("qualityControlType").toString()).willReturn("02forUnit");
        given(dataDefinitionService.get("qualityControls", "qualityControl")).willReturn(dataDefinition);
        given(
                dataDefinition.find().restrictedWith(any(Restriction.class)).restrictedWith(any(Restriction.class)).list()
                        .getTotalNumberOfEntities()).willReturn(0);

        // when
        boolean results = callCheckIfAllQualityControlsAreClosed(order);

        // then
        assertTrue(results);
    }

    private boolean callCheckIfAllQualityControlsAreClosed(final Entity order) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        Method method = OrderService.class.getDeclaredMethod("checkIfAllQualityControlsAreClosed", Entity.class);
        method.setAccessible(true);
        boolean results = (Boolean) method.invoke(orderService, order);
        return results;
    }

    @Test
    public void shouldReturnTrueIfThereIsNoOpenQualityForOrder() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        DataDefinition dataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
        given(order.getBelongsToField("technology").getField("qualityControlType").toString()).willReturn("03forOrder");
        given(dataDefinitionService.get("qualityControls", "qualityControl")).willReturn(dataDefinition);
        given(
                dataDefinition.find().restrictedWith(any(Restriction.class)).restrictedWith(any(Restriction.class)).list()
                        .getTotalNumberOfEntities()).willReturn(0);

        boolean results = callCheckIfAllQualityControlsAreClosed(order);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnFalseIfThereIsOpenQualityForOperation() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        DataDefinition dataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
        given(order.getBelongsToField("technology").getField("qualityControlType").toString()).willReturn("04forOperation");
        given(dataDefinitionService.get("qualityControls", "qualityControl")).willReturn(dataDefinition);
        given(
                dataDefinition.find().restrictedWith(any(Restriction.class)).restrictedWith(any(Restriction.class)).list()
                        .getTotalNumberOfEntities()).willReturn(1);

        boolean results = callCheckIfAllQualityControlsAreClosed(order);

        // then
        assertFalse(results);
    }

    @Test
    public void shouldFailCheckingRequiredBatchForBatchRequired() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getHasManyField("genealogies").size()).willReturn(0);
        given(order.getField("technology")).willReturn(technology);
        given(technology.getField("batchRequired")).willReturn(true);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertFalse(results);
    }

    @Test
    public void shouldFailCheckingRequiredBatchForPostBatchRequired() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").size()).willReturn(0);
        given(technology.getField("batchRequired")).willReturn(false);
        given(technology.getField("shiftFeatureRequired")).willReturn(false);
        given(technology.getField("postFeatureRequired")).willReturn(true);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertFalse(results);
    }

    @Test
    public void shouldFailCheckingRequiredBatchForOtherBatchRequired() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").size()).willReturn(0);
        given(technology.getField("batchRequired")).willReturn(false);
        given(technology.getField("shiftFeatureRequired")).willReturn(false);
        given(technology.getField("postFeatureRequired")).willReturn(false);
        given(technology.getField("otherFeatureRequired")).willReturn(true);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertFalse(results);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFailCheckingRequiredBatchForOperationComponentBatchRequired() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity operationComponent = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity operationProductInComponents = mock(Entity.class, RETURNS_DEEP_STUBS);
        Iterator<Entity> iterator = mock(Iterator.class);
        Iterator<Entity> iterator2 = mock(Iterator.class);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").size()).willReturn(0);
        given(technology.getField("batchRequired")).willReturn(false);
        given(technology.getField("shiftFeatureRequired")).willReturn(false);
        given(technology.getField("postFeatureRequired")).willReturn(false);
        given(technology.getField("otherFeatureRequired")).willReturn(false);
        given(technology.getTreeField("operationComponents").iterator()).willReturn(iterator);
        given(iterator.hasNext()).willReturn(true, false);
        given(iterator.next()).willReturn(operationComponent);
        given(operationComponent.getHasManyField("operationProductInComponents").iterator()).willReturn(iterator2);
        given(iterator2.hasNext()).willReturn(true, false);
        given(iterator2.next()).willReturn(operationProductInComponents);
        given(operationProductInComponents.getField("batchRequired")).willReturn(true);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertFalse(results);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFailCheckingRequiredBatchForGenealogyBatchRequired() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Iterator<Entity> iterator = mock(Iterator.class);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity genealogy = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").size()).willReturn(1);
        given(order.getHasManyField("genealogies").iterator()).willReturn(iterator);
        given(iterator.hasNext()).willReturn(true, false);
        given(iterator.next()).willReturn(genealogy);
        given(technology.getField("batchRequired")).willReturn(true);
        given(genealogy.getField("batch")).willReturn(null);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertFalse(results);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFailCheckingRequiredBatchForGenealogyShiftBatchRequired() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Iterator<Entity> iterator = mock(Iterator.class);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity genealogy = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").size()).willReturn(1);
        given(order.getHasManyField("genealogies").iterator()).willReturn(iterator);
        given(iterator.hasNext()).willReturn(true, false);
        given(iterator.next()).willReturn(genealogy);
        given(technology.getField("batchRequired")).willReturn(true);
        given(technology.getField("shiftFeatureRequired")).willReturn(true);
        given(genealogy.getHasManyField("shiftFeatures").size()).willReturn(0);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertFalse(results);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFailCheckingRequiredBatchForGenealogyPostBatchRequired() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Iterator<Entity> iterator = mock(Iterator.class);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity genealogy = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").size()).willReturn(1);
        given(order.getHasManyField("genealogies").iterator()).willReturn(iterator);
        given(iterator.hasNext()).willReturn(true, false);
        given(iterator.next()).willReturn(genealogy);
        given(technology.getField("batchRequired")).willReturn(false);
        given(technology.getField("shiftFeatureRequired")).willReturn(true);
        given(genealogy.getHasManyField("shiftFeatures").size()).willReturn(1);
        given(technology.getField("postFeatureRequired")).willReturn(true);
        given(genealogy.getHasManyField("postFeatures").size()).willReturn(0);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertFalse(results);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFailCheckingRequiredBatchForGenealogyOtherBatchRequired() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Iterator<Entity> iterator = mock(Iterator.class);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity genealogy = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").size()).willReturn(1);
        given(order.getHasManyField("genealogies").iterator()).willReturn(iterator);
        given(iterator.hasNext()).willReturn(true, false);
        given(iterator.next()).willReturn(genealogy);
        given(technology.getField("batchRequired")).willReturn(false);
        given(technology.getField("shiftFeatureRequired")).willReturn(false);
        given(technology.getField("postFeatureRequired")).willReturn(true);
        given(genealogy.getHasManyField("postFeatures").size()).willReturn(1);
        given(technology.getField("otherFeatureRequired")).willReturn(true);
        given(genealogy.getHasManyField("otherFeatures").size()).willReturn(0);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertFalse(results);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFailCheckingRequiredBatchForGenealogyComponentsBatchRequired() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity productInComponent = mock(Entity.class, RETURNS_DEEP_STUBS);
        Iterator<Entity> iterator = mock(Iterator.class);
        Iterator<Entity> iterator2 = mock(Iterator.class);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity genealogy = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").size()).willReturn(1);
        given(order.getHasManyField("genealogies").iterator()).willReturn(iterator);
        given(iterator.hasNext()).willReturn(true, false);
        given(iterator.next()).willReturn(genealogy);
        given(technology.getField("batchRequired")).willReturn(false);
        given(technology.getField("shiftFeatureRequired")).willReturn(false);
        given(technology.getField("postFeatureRequired")).willReturn(false);
        given(technology.getField("otherFeatureRequired")).willReturn(true);
        given(genealogy.getHasManyField("otherFeatures").size()).willReturn(1);
        given(genealogy.getHasManyField("productInComponents").iterator()).willReturn(iterator2);
        given(iterator2.hasNext()).willReturn(true, false);
        given(iterator2.next()).willReturn(productInComponent);
        given(productInComponent.getBelongsToField("productInComponent").getField("batchRequired")).willReturn(true);
        given(productInComponent.getHasManyField("batch").size()).willReturn(0);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertFalse(results);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFailCheckingRequiredBatchForGenealogyComponentsBatchRequired2() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity productInComponent = mock(Entity.class, RETURNS_DEEP_STUBS);
        Iterator<Entity> iterator = mock(Iterator.class);
        Iterator<Entity> iterator2 = mock(Iterator.class);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity genealogy = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").size()).willReturn(1);
        given(order.getHasManyField("genealogies").iterator()).willReturn(iterator);
        given(iterator.hasNext()).willReturn(true, false);
        given(iterator.next()).willReturn(genealogy);
        given(technology.getField("batchRequired")).willReturn(false);
        given(technology.getField("shiftFeatureRequired")).willReturn(false);
        given(technology.getField("postFeatureRequired")).willReturn(false);
        given(technology.getField("otherFeatureRequired")).willReturn(true);
        given(genealogy.getHasManyField("otherFeatures").size()).willReturn(1);
        given(genealogy.getHasManyField("productInComponents").iterator()).willReturn(iterator2);
        given(iterator2.hasNext()).willReturn(true, false);
        given(iterator2.next()).willReturn(productInComponent);
        given(productInComponent.getBelongsToField("productInComponent").getField("batchRequired")).willReturn(true);
        given(productInComponent.getHasManyField("batch").size()).willReturn(1);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertTrue(results);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFailCheckingRequiredBatchForGenealogyComponentsBatchRequired3() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity productInComponent = mock(Entity.class, RETURNS_DEEP_STUBS);
        Iterator<Entity> iterator = mock(Iterator.class);
        Iterator<Entity> iterator2 = mock(Iterator.class);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity genealogy = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").size()).willReturn(1);
        given(order.getHasManyField("genealogies").iterator()).willReturn(iterator);
        given(iterator.hasNext()).willReturn(true, false);
        given(iterator.next()).willReturn(genealogy);
        given(technology.getField("batchRequired")).willReturn(false);
        given(technology.getField("shiftFeatureRequired")).willReturn(false);
        given(technology.getField("postFeatureRequired")).willReturn(false);
        given(technology.getField("otherFeatureRequired")).willReturn(true);
        given(genealogy.getHasManyField("otherFeatures").size()).willReturn(1);
        given(genealogy.getHasManyField("productInComponents").iterator()).willReturn(iterator2);
        given(iterator2.hasNext()).willReturn(true, false);
        given(iterator2.next()).willReturn(productInComponent);
        given(productInComponent.getBelongsToField("productInComponent").getField("batchRequired")).willReturn(false);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldSuccessCheckingRequiredBatch() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").size()).willReturn(1);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldSuccessCheckingRequiredBatchIfThereIsNoTechnology() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(null);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertTrue(results);
    }

    private boolean callCheckRequiredBatch(final Entity order) throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        Method method = OrderService.class.getDeclaredMethod("checkRequiredBatch", Entity.class);
        method.setAccessible(true);
        boolean results = (Boolean) method.invoke(orderService, order);
        return results;
    }

}