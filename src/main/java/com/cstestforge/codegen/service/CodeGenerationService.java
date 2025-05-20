package com.cstestforge.codegen.service;

import com.cstestforge.codegen.model.*;
import com.cstestforge.codegen.model.event.*;

import java.util.List;

/**
 * Service for generating code from test definitions and recordings
 */
public interface CodeGenerationService {
    // Basic code generation
    GeneratedCode generateFromTest(String testId, String framework, String language);
    GeneratedCode generateFromRecording(String recordingId, String framework, String language);
    GeneratedCode generateFromSteps(List<Event> events, String framework, String language);
    List<CodeTemplate> getTemplates(String framework, String language);
    CodeTemplate getTemplateById(String id);
    List<FrameworkDefinition> getSupportedFrameworks();
    boolean validateCode(String code, String framework, String language);
    
    // Advanced code generation for complex structures
    String generateCodeForConditional(ConditionalEvent event, String language, String framework);
    String generateCodeForLoop(LoopEvent event, String language, String framework);
    String generateCodeForCapture(CaptureEvent event, String language, String framework);
    String generateCodeForAssertion(AssertionEvent event, String language, String framework);
    String generateCodeForGroup(GroupEvent event, String language, String framework);
    String generateCodeForTryCatch(TryCatchEvent event, String language, String framework);
    String generateCodeForAction(ActionEvent event, String language, String framework);
    
    // Code customization
    CodeGenerationConfig saveCustomization(String projectId, CodeGenerationConfig config);
    CodeGenerationConfig getCustomization(String projectId);
    GeneratedCode regenerateWithCustomization(String codeId, CodeGenerationConfig config);
    
    // Framework export
    String exportFramework(ExportRequest request, String destinationPath);
} 