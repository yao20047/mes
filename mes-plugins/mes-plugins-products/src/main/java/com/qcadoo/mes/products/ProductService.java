package com.qcadoo.mes.products;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.api.DataDefinitionService;
import com.qcadoo.mes.api.Entity;
import com.qcadoo.mes.api.SecurityService;
import com.qcadoo.mes.beans.users.UsersUser;
import com.qcadoo.mes.enums.RestrictionOperator;
import com.qcadoo.mes.model.DataDefinition;
import com.qcadoo.mes.model.search.Restrictions;
import com.qcadoo.mes.model.search.SearchCriteriaBuilder;
import com.qcadoo.mes.model.search.SearchResult;
import com.qcadoo.mes.view.ViewValue;
import com.qcadoo.mes.view.components.LookupData;
import com.qcadoo.mes.view.components.SimpleValue;

@Service
public final class ProductService {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private SecurityService securityService;

    @SuppressWarnings("unchecked")
    public void afterOrderDetailsLoad(final ViewValue<Long> value, final String triggerComponentName) {
        ViewValue<LookupData> productValue = (ViewValue<LookupData>) value.lookupValue("mainWindow.orderDetailsForm.product");
        ViewValue<SimpleValue> defaultInstructionValue = (ViewValue<SimpleValue>) value
                .lookupValue("mainWindow.orderDetailsForm.defaultInstruction");
        ViewValue<LookupData> instructionValue = (ViewValue<LookupData>) value
                .lookupValue("mainWindow.orderDetailsForm.instruction");

        if (defaultInstructionValue == null || productValue == null) {
            return;
        }

        defaultInstructionValue.setEnabled(false);
        defaultInstructionValue.setValue(new SimpleValue(""));

        if (productValue.getValue() != null && productValue.getValue().getValue() != null) {
            Entity defaultInstructionEntity = getDefaultInstruction(productValue);
            if (defaultInstructionEntity != null) {
                String defaultInstructionName = defaultInstructionEntity.getField("name").toString();
                defaultInstructionValue.getValue().setValue(defaultInstructionName);
                selectDefaultInstruction(triggerComponentName, instructionValue, defaultInstructionEntity);
            }
        }
    }

    private void selectDefaultInstruction(final String triggerComponentName, final ViewValue<LookupData> instructionValue,
            final Entity defaultInstructionEntity) {
        Long selectedInstructinId = instructionValue.getValue().getSelectedEntityId();
        if (selectedInstructinId == null && "mainWindow.orderDetailsForm.product".equals(triggerComponentName)) {
            instructionValue.getValue().setValue(defaultInstructionEntity.getId());
        }
    }

    private Entity getDefaultInstruction(final ViewValue<LookupData> productValue) {
        DataDefinition instructionDD = dataDefinitionService.get("products", "instruction");

        SearchCriteriaBuilder searchCriteria = instructionDD
                .find()
                .withMaxResults(1)
                .restrictedWith(Restrictions.eq(instructionDD.getField("master"), true))
                .restrictedWith(
                        Restrictions.belongsTo(instructionDD.getField("product"), productValue.getValue().getSelectedEntityId()));

        SearchResult searchResult = searchCriteria.list();

        if (searchResult.getTotalNumberOfEntities() == 1) {
            return searchResult.getEntities().get(0);
        } else {
            return null;
        }
    }

    public boolean checkInstructionDefault(final DataDefinition dataDefinition, final Entity entity) {
        Boolean master = (Boolean) entity.getField("master");

        if (!master) {
            return true;
        }

        SearchCriteriaBuilder searchCriteria = dataDefinition.find().withMaxResults(1)
                .restrictedWith(Restrictions.eq(dataDefinition.getField("master"), true))
                .restrictedWith(Restrictions.belongsTo(dataDefinition.getField("product"), entity.getField("product")));

        if (entity.getId() != null) {
            searchCriteria.restrictedWith(Restrictions.idRestriction(entity.getId(), RestrictionOperator.NE));
        }

        SearchResult searchResult = searchCriteria.list();

        return searchResult.getTotalNumberOfEntities() == 0;
    }

    public boolean checkSubstituteDates(final DataDefinition dataDefinition, final Entity entity) {
        Date dateFrom = (Date) entity.getField("effectiveDateFrom");
        Date dateTo = (Date) entity.getField("effectiveDateTo");

        return compareDates(dateFrom, dateTo);
    }

    public boolean checkOrderDates(final DataDefinition dataDefinition, final Entity entity) {
        Date dateFrom = (Date) entity.getField("dateFrom");
        Date dateTo = (Date) entity.getField("dateTo");

        return compareDates(dateFrom, dateTo);
    }

    public boolean checkInstructionDates(final DataDefinition dataDefinition, final Entity entity) {
        Date dateFrom = (Date) entity.getField("dateFrom");
        Date dateTo = (Date) entity.getField("dateTo");

        return compareDates(dateFrom, dateTo);
    }

    private String getFullNameOfLoggedUser() {
        UsersUser user = securityService.getCurrentUser();
        return user.getFirstName() + " " + user.getLastName();
    }

    public void fillOrderDatesAndWorkers(final DataDefinition dataDefinition, final Entity entity) {
        if (("pending".equals(entity.getField("state")) || "done".equals(entity.getField("state")))
                && entity.getField("effectiveDateFrom") == null) {
            entity.setField("effectiveDateFrom", new Date());
            entity.setField("startWorker", getFullNameOfLoggedUser());
        }
        if ("done".equals(entity.getField("state")) && entity.getField("effectiveDateTo") == null) {
            entity.setField("effectiveDateTo", new Date());
            entity.setField("endWorker", getFullNameOfLoggedUser());

        }

        if (entity.getField("effectiveDateTo") != null) {
            entity.setField("state", "done");
        } else if (entity.getField("effectiveDateFrom") != null) {
            entity.setField("state", "pending");
        }
    }

    private boolean compareDates(final Date dateFrom, final Date dateTo) {
        if (dateFrom == null || dateTo == null) {
            return true;
        }

        return !dateFrom.after(dateTo);
    }

}
