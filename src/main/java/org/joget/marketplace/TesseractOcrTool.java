package org.joget.marketplace;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import org.joget.apps.app.model.AppDefinition;

import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.model.WorkflowAssignment;
import org.springframework.context.ApplicationContext;
import java.io.File;
import net.sourceforge.tess4j.Tesseract;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FileUtil;
import org.joget.apps.form.service.FormUtil;

public class TesseractOcrTool extends DefaultApplicationPlugin{
    private final static String MESSAGE_PATH = "messages/TesseractOcrTool";

    @Override
    public String getName() {
        return AppPluginUtil.getMessage("processtool.tesseractocrtool.name", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getVersion() {
        final Properties projectProp = new Properties();
        try {
            projectProp.load(this.getClass().getClassLoader().getResourceAsStream("project.properties"));
        } catch (IOException ex) {
            LogUtil.error(getClass().getName(), ex, "Unable to get project version from project properties...");
        }
        return projectProp.getProperty("version");
    }
    
    @Override
    public String getClassName() {
        return getClass().getName();
    }
    
    @Override
    public String getLabel() {
        //support i18n
        return AppPluginUtil.getMessage("processtool.tesseractocrtool.name", getClassName(), MESSAGE_PATH);
    }
    
    @Override
    public String getDescription() {
        //support i18n
        return AppPluginUtil.getMessage("processtool.tesseractocrtool.desc", getClassName(), MESSAGE_PATH);
    }
 
    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/TesseractOcrTool.json", null, true, MESSAGE_PATH);
    }

    @Override
    public Object execute(Map map) {
        ApplicationContext ac = AppUtil.getApplicationContext();
        AppService appService = (AppService) ac.getBean("appService");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();

        String language = (String) map.get("language");
        String languageModelPath = (String) map.get("languageModelPath");
        String formDefId = (String) map.get("formDefId");
        String loadData = (String) map.get("loadData");
        String storeData = (String) map.get("storeData");

        String recordId;
        WorkflowAssignment wfAssignment = (WorkflowAssignment) properties.get("workflowAssignment");
        if (wfAssignment != null) {
            recordId = appService.getOriginProcessId(wfAssignment.getProcessId());
        } else {
            recordId = (String)properties.get("recordId");
        }

        Form loadForm = null;
        String primaryKey = null;
        File file = null;
        Tesseract tesseract = new Tesseract();

        if (formDefId != null) {
            try {
                FormData formData = new FormData();
                primaryKey = appService.getOriginProcessId(wfAssignment.getProcessId());
                formData.setPrimaryKeyValue(primaryKey);
                loadForm = appService.viewDataForm(appDef.getId(), appDef.getVersion().toString(), formDefId, null, null, null, formData, null, null);
                Element el = FormUtil.findElement(loadData, loadForm, formData);
                file = FileUtil.getFile(FormUtil.getElementPropertyValue(el, formData), loadForm, primaryKey);
            
                tesseract.setDatapath(languageModelPath);
                tesseract.setLanguage(language);
                String fullText = tesseract.doOCR(file);

                FormRowSet set = new FormRowSet();
                FormRow r1 = new FormRow();
                r1.put(storeData, fullText);
                set.add(r1);
                set.add(0, r1);
                appService.storeFormData(appDef.getAppId(), appDef.getVersion().toString(), formDefId, set, recordId);
            } catch (Exception ex) {
                LogUtil.error(getClassName(), ex, ex.getMessage());
            }
        }
        return null;
    }
}